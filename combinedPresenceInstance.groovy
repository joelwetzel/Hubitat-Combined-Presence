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
		description:		"The result of the combination.",
		multiple:			false,
		required:			true
	]


preferences {
	page(name: "mainPage", title: "<b>Presence Sensors:</b>", install: true, uninstall: true) {
		section("") {
			input inputSensors
			input outputSensor
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
	subscribe(inputSensors, "presence", presenceChangedHandler)
	
	app.updateLabel("Combined Presence for ${outputSensor.displayName}")
}


def presenceChangedHandler(evt) {
	//log.debug "PRESENCE CHANGED for one input sensor."	
	
	def present = false
	
	inputSensors.each { inputSensor ->
		if (inputSensor.currentValue("presence") == "present") {
			present = true	
		}
	}
	
	if (present) {
		outputSensor.arrived()	
	}
	else {
		outputSensor.departed()
	}
}












