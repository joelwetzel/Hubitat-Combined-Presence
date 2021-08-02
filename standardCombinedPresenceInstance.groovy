/**
 *  Combined Presence - Standard Combiner v2.2
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
import groovy.json.*
	
definition(
    name: "Combined Presence - Standard Combiner",
	parent: "joelwetzel:Combined Presence",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "This is the instance that is best to use for combining both wifi and gps-based inputs for a single person.  It is a child app of Combined Presence.",
    category: "Safety & Security",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

def inputSensorsGps = [
		name:				"inputSensorsGps",
		type:				"capability.presenceSensor",
		title:				"Phone GPS-based, phone geofencing, and fob sensors",
		multiple:			true,
		required:			true
	]

def inputSensorsWifi = [
		name:				"inputSensorsWifi",
		type:				"capability.presenceSensor",
		title:				"Phone Wifi-based sensors",
		multiple:			true,
		required:			false
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
		section(getFormat("title", "Standard Combiner")) {
		}
		section(hideable: true, hidden: false, "Input Sensors") {
            input inputSensorsGps
            paragraph "You must include at least one gps, geofencing, or fob presence sensor.  Examples of this would be the Hubitat phone app, Life360, or using Alexa or HomeKit to update the state of a virtual presence sensor.  Best performance will come from having more than one sensor in here."
            input inputSensorsWifi
            paragraph "Phone Wifi-based sensors are optional.  They can help detect arrival faster.  They are not used for detecting departures, because some smartphones periodically put their Wifi to sleep.  My recommended phone wifi sensor can be found <a target=\"_blank\" href=\"https://community.hubitat.com/t/updated-iphone-wifi-presence-sensor\">here</a> and can most easily be installed by using <a target=\"_blank\" href=\"https://community.hubitat.com/t/beta-hubitat-package-manager\">Hubitat Package Manager</a>."
        }
        section() {
			input outputSensor
		}
		section() {
			paragraph ""	
		}
		section(hideable: true, hidden: true, "Notifications") {
			input notificationDevice
			input notifyAboutStateChanges
			paragraph "This will send a notification any time the state of the Output Sensor is changed by Combined Presence."
			input notifyAboutInconsistencies
			paragraph "This will send notifications if your geofencing sensors stay inconsistent for more than 30 minutes.  That usually means one of the sensors has stopped reporting, and should be checked."
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

    // Wifi-based sensors can only trigger arrival
	subscribe(inputSensorsWifi, "presence.present", arrivedHandler)

    // Geofencing sensors can trigger EITHER arrival or departure
    subscribe(inputSensorsGps, "presence.present", arrivedHandler)
    subscribe(inputSensorsGps, "presence.not present", departedHandler)
	
	app.updateLabel("Standard Combiner for ${outputSensor.displayName}")
    
    use (groovy.time.TimeCategory) {
        state.lastInconsistencyWarningTime = new Date()-24.hours
        state.lastConsistentTime = new Date()
    }
	
	runEvery1Minute(checkForInconsistencies)
}


def checkForInconsistencies() {
    log "***** checkForInconsistencies()"
    
	def inputsAreAllPresent = true
	def inputsAreAllNotPresent = true
	
	inputSensorsGps.each { inputSensor ->
		if (inputSensor.currentValue("presence") == "present") {
			inputsAreAllNotPresent = false	
		}
		
		if (inputSensor.currentValue("presence") == "not present") {
			inputsAreAllPresent = false	
		}
	}
	
	def inputsAreInconsistent = !(inputsAreAllPresent || inputsAreAllNotPresent)
	
	log "inputsAreAllPresent ${inputsAreAllPresent}"
	log "inputsAreAllNotPresent ${inputsAreAllNotPresent}"
	log "inputsAreInconsistent ${inputsAreInconsistent}"
	
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
		
        log "timeSinceConsistency.minutes: ${timeSinceConsistency.minutes}"
        log "timeSinceConsistency.hours: ${timeSinceConsistency.hours}"
        log "timeSinceConsistency.days: ${timeSinceConsistency.days}"
        log "timeSinceLastWarning.hours ${timeSinceLastWarning.hours}"
        log "timeSinceLastWarning.days ${timeSinceLastWarning.days}"
        
		if ((timeSinceConsistency.minutes > 30 || timeSinceConsistency.hours > 0 || timeSinceConsistency.days > 0) && (timeSinceLastWarning.hours > 18 || timeSinceLastWarning.days > 0)) {
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
		if (notificationDevice) {
			notificationDevice.deviceNotification(msg)
		}
	}
}


def arrivedHandler(evt) {
	log "${evt.device.name} arrived."	
	//log.debug groovy.json.JsonOutput.toJson(evt)

	def oldPresent = outputSensor.currentValue("presence") == "present"
	def newPresent = true            // Something arrived!
	
	if (!oldPresent && newPresent) {
		outputSensor.arrived()
		
        log "${outputSensor.displayName}.arrived()"	

        if (notifyAboutStateChanges) {
            sendNotification("Arrived: ${outputSensor.displayName}")
        }
	}
}


def departedHandler(evt) {
	log "${evt.device.name} departed."	
	//log.debug groovy.json.JsonOutput.toJson(evt)

	def oldPresent = outputSensor.currentValue("presence") == "present"
	def newPresent = false
	
	if (oldPresent && !newPresent) {
		outputSensor.departed()

    	log "${outputSensor.displayName}.departed()"
			
        if (notifyAboutStateChanges) {
            sendNotification("Departed: ${outputSensor.displayName}")
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



