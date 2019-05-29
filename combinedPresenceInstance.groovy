/**
 *  Combined Presence Instance
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.time.*
	
definition(
    name: "Combined Presence Instance",
	parent: "joelwetzel:Combined Presence",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "This will set a virtual presence sensor to the logical-OR of all the input sensors.  It is the child app of Combined Presence.",
    category: "Safety & Security",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


def inputSensors = [
		name:				"inputSensors",
		type:				"capability.presenceSensor",
		title:				"Input Sensors",
		description:		"The sensors that will be combined.",
		multiple:			true,
		required:			true
	]


def outputSensor = [
		name:				"outputSensor",
		type:				"capability.presenceSensor",
		title:				"Output Sensor",
		description:		"The virtual presence sensor that will be controlled by this instance.",
		multiple:			false,
		required:			true
	]

def notificationDevice = [
		name:				"notificationDevice",
		type:				"capability.notification",
		title:				"Devices for Notifications",
		description:		"Send notifications to devices.  ie. push notifications to a phone.",
		required:			false,
		multiple:			true
	]

def notificationNumber = [
		name:				"notificationNumber",
		type:				"string",
		title:				"Phone Number for SMS Notifications",
		description:		"Phone number for notifications.  Must be in the form (for US) +1xxxyyyzzzz.",
		required:			false
	]

def notifyAboutStateChanges = [
		name:				"notifyAboutStateChanges",
		type:				"bool",
		title:				"Notify about state changes to the Output sensor",
		default:			false	
	]

def notifyAboutInconsistencies = [
		name:				"notifyAboutInconsistencies",
		type:				"bool",
		title:				"Notify about inconsistent Inputs for more than 30 minutes",
		description:		"Send notifications if input sensors have inconsistent values for an extended period.",
		default:			false	
	]

def enableLogging = [
		name:				"enableLogging",
		type:				"bool",
		title:				"Enable Debug Logging?",
		defaultValue:		false,
		required:			true
	]

preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
		section(getFormat("title", "Combined Presence Instance")) {
		}
		section() {
			input inputSensors
			input outputSensor
		}
		section(hideable: true, hidden: true, "Notifications") {
			input notificationDevice
			input notificationNumber
			input notifyAboutStateChanges
			paragraph "This will send a notification any time the state of the Output Sensor is changed by Combined Presence."
			input notifyAboutInconsistencies
			paragraph "This will send notifications if your input sensors stay inconsistent for more than 30 minutes.  That usually means one of the sensors has stopped reporting, and should be checked."
		}
		section() {
			input enableLogging
		}
	}
}


def installed() {
	log.info "Installed with settings: ${settings}"

	initialize()
}


def updated() {
	log.info "Updated with settings: ${settings}"

	initialize()
}


def initialize() {
	unschedule()
	unsubscribe()

	subscribe(inputSensors, "presence", presenceChangedHandler)
	
	app.updateLabel("Combined Presence for ${outputSensor.displayName}")
	
	runEvery1Minute(checkForInconsistencies)
}


def checkForInconsistencies() {
	def inputsAreAllPresent = true
	def inputsAreAllNotPresent = true
	
	inputSensors.each { inputSensor ->
		if (inputSensor.currentValue("presence") == "present") {
			inputsAreAllNotPresent = false	
		}
		
		if (inputSensor.currentValue("presence") == "not present") {
			inputsAreAllPresent = false	
		}
	}
	
	def inputsAreInconsistent = !(inputsAreAllPresent || inputsAreAllNotPresent)
	
	//log.debug "inputsAreAllPresent ${inputsAreAllPresent}"
	//log.debug "inputsAreAllNotPresent ${inputsAreAllNotPresent}"
	//log.debug "inputsAreInconsistent ${inputsAreInconsistent}"
	
	def currentTime = new Date()
	
	if (inputsAreInconsistent) {
		def lastConsistentTime = new Date()
		if (state.lastConsistentTime) {
			lastConsistentTime = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state.lastConsistentTime)
		}
		
		def lastInconsistencyWarningTime = new Date()
		if (state.lastInconsistencyWarningTime) {
			lastInconsistencyWarningTime = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state.lastInconsistencyWarningTime)
		}
		
		def timeSinceConsistency = TimeCategory.minus(currentTime, lastConsistentTime)
		def timeSinceLastWarning = TimeCategory.minus(currentTime, lastInconsistencyWarningTime)
		
		if (timeSinceConsistency.minutes > 30 && timeSinceLastWarning.hours > 24) {
			def msg = "Input sensors for ${outputSensor.displayName} have been inconsistent for 30 minutes.  This may mean one of your presence sensors is not updating."
			
			log(msg)
			if (notifyAboutInconsistencies) {
				sendNotification(msg)
			}
			
			state.lastInconsistencyWarningTime = currentTime
		}
	}
	else {
		state.lastConsistentTime = currentTime
	}
}


def sendNotification(msg) {
	if (msg && msg.size() > 0) {
		if (notificationNumber && notificationNumber.size() > 0) {
			sendSms(notificationNumber, msg)
		}
		
		if (notificationDevice) {
			notificationDevice.deviceNotification(msg)
		}
	}
}


def presenceChangedHandler(evt) {
	log "PRESENCE CHANGED for: ${evt.device.name}"
	
	def present = false
	
	inputSensors.each { inputSensor ->
		if (inputSensor.currentValue("presence") == "present") {
			present = true	
		}
	}
	
	def oldPresent = outputSensor.currentValue("presence")
	
	if (present) {
		outputSensor.arrived()
		
		if (oldPresent != "present") {
			log "${outputSensor.displayName}.arrived()"
			
			if (notifyAboutStateChanges) {
				sendNotification("Arrived: ${outputSensor.displayName}")
			}
		}
	}
	else {
		outputSensor.departed()

		if (oldPresent == "present") {
			log "${outputSensor.displayName}.departed()"
			
			if (notifyAboutStateChanges) {
				sendNotification("Departed: ${outputSensor.displayName}")
			}
		}
	}
}


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def log(msg) {
	if (enableLogging) {
		log.debug msg
	}
}












