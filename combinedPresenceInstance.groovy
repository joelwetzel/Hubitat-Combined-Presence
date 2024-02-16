/**
 *  Combined Presence Instance v2.2.1
 *
 *  Copyright 2020 Joel Wetzel
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
    name: "Combined Presence Boolean Combiner",
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
		description:		"The sensors that will be combined with a boolean OR operation.",
		multiple:			true,
		required:			true
	]


def outputSensor = [
		name:				"outputSensor",
		type:				"capability.presenceSensor",
		title:				"Output Sensor",
		description:		"The virtual presence sensor that will be controlled by this combiner.",
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

def notifyAboutStateChanges = [
		name:				"notifyAboutStateChanges",
		type:				"bool",
		title:				"Notify about state changes to the Output sensor",
		defaultValue:			false
	]

def enableLogging = [
		name:				"enableLogging",
		type:				"bool",
		title:				"Enable Debug Logging?",
		defaultValue:		false,
		required:			true
	]

preferences {
	page(name: "mainPage", title: "Combined Presence - Boolean-OR Combiner", install: true, uninstall: true) {
		section(hideable: true, hidden: false, "Input Sensors") {
			input inputSensors
			input outputSensor
		}
		section(hideable: true, hidden: true, "Notifications") {
			input notificationDevice
			input notifyAboutStateChanges
			paragraph "This will send a notification any time the state of the Output Sensor is changed by Combined Presence."
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

	subscribe(inputSensors, "presence", "presenceChangedHandler")

	app.updateLabel("Boolean-OR Combiner for ${outputSensor.displayName}")

    setBooleanOrOutputState()
}


def sendNotification(msg) {
	if (msg && msg.size() > 0) {
		if (notificationDevice) {
			notificationDevice.deviceNotification(msg)
		}
	}
}

def setBooleanOrOutputState() {
    def present = false

	inputSensors.each { inputSensor ->
		if (inputSensor.currentValue("presence") == "present") {
			present = true
		}
	}

	def oldPresent = outputSensor.currentValue("presence")

	if (present) {
		if (oldPresent != "present") {
			log "${outputSensor.displayName}.arrived()"
     		outputSensor.arrived()

			if (notifyAboutStateChanges) {
				sendNotification("Arrived: ${outputSensor.displayName}")
			}
		}
	}
	else {
		if (oldPresent == "present") {
			log "${outputSensor.displayName}.departed()"
    		outputSensor.departed()

			if (notifyAboutStateChanges) {
				sendNotification("Departed: ${outputSensor.displayName}")
			}
		}
	}
}

def presenceChangedHandler(evt) {
	log "PRESENCE CHANGED for: ${evt.device.name}"

	setBooleanOrOutputState()
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
