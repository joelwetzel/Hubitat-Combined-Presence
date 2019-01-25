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


definition(
    name: "Combined Presence Instance",
	parent: "joelwetzel:Combined Presence",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "This will set a virtual presence sensor to the logical-OR of all the input sensors",
    category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


def inputSensors = [
		name:				"inputSensors",
		type:				"capability.presenceSensor",
		title:				"Input Sensors",
		//description:		"",
		multiple:			true,
		required:			true
	]


def outputSensor = [
		name:				"outputSensor",
		type:				"capability.presenceSensor",
		title:				"Output Sensor",
		//description:		"",
		multiple:			false,
		required:			true
	]

def thresholdInput = [
		name:				"threshold",
		type:				"int",
		title:				"Threshold",
		//description:		"",
		multiple:			false,
		required:			true
	]


preferences {
	page(name: "mainPage", title: "<b>Presence Sensors:</b>", install: true, uninstall: true) {
		section("") {
			input inputSensors
			input outputSensor
			input thresholdInput
		}
		
		
		section("") {
            input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
        }
	}
}


def installed() {
	log.info "Installed with settings: ${settings}"

	initialize()
}


def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}



def initialize() {
	subscribe(inputSensors, "presence.present", presenceChangedHandler)
	subscribe(inputSensors, "presence.not present", presenceChangedHandler)
	app.updateLabel("Combined Presence for ${outputSensor.displayName}")
}


def presenceChangedHandler(evt) {
	ifDebug("$evt.value")
	sendEvent(name:"Presence Changed", value: " $evt.value", displayed:false, isStateChange: false)
	def present = false
	switch(evt.value){
		case "not present":
			int count = 0
			inputSensors.each { inputSensor ->
				if (inputSensor.currentValue("presence") == "not present") {
					ifDebug("${inputSensor.label} not present")
					count++
				}
			}
			ifDebug("$count sensors not present")
			if (count >= Integer.parseInt(threshold)){
				ifDebug("Threshold met setting not present")
				present = false
			}	
				break
		case "present":
			int count = 0
			inputSensors.each { inputSensor ->
				if (inputSensor.currentValue("presence") == "present") {
					ifDebug("${inputSensor.label} present")
					count++
				}
			}
			ifDebug("$count sensors present")
			if (count >= Integer.parseInt(threshold)){
				ifDebug("Threshold met setting present")
				present = true	
			}	
				break
	}

	
	if (present) {
		outputSensor.arrived()	
	}
	else {
		outputSensor.departed()
	}
}

private ifDebug(msg)     
{  
    if (msg && isDebug)  log.debug "Combined Presence for $outputSensor.displayName: " + msg  
}
