package joelwetzel.combined_presence.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.validation.Flags

/**
 * Basic tests for combinedPresenceInstance.groovy (Boolean-OR Combiner)
 */
class BooleanOrCombinerTests extends IntegrationAppSpecification {
    def inputSensor1 = PresenceSensorFixtureFactory.create('sensor1')
    def inputSensor2 = PresenceSensorFixtureFactory.create('sensor2')
    def outputSensor = PresenceSensorFixtureFactory.create('output')
    def inputSensors = [inputSensor1, inputSensor2]

    @Override
    def setup() {
        super.initializeEnvironment(
            appScriptFilename: "combinedPresenceInstance.groovy",
            validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
            userSettingValues: [
                inputSensors: inputSensors,
                outputSensor: outputSensor,
                enableLogging: true
            ]
        )
        appScript.installed()
    }

    void "Boolean-OR sets output to present when any sensor arrives"() {
        given:
        inputSensor1.initialize(appExecutor, [presence: "not present"])
        inputSensor2.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "not present"])

        when:
        inputSensor1.arrived()

        then:
        inputSensor1.currentValue('presence') == "present"
        inputSensor2.currentValue('presence') == "not present"
        outputSensor.currentValue('presence') == "present"
    }

    void "Boolean-OR keeps output present while at least one sensor remains present"() {
        given:
        inputSensor1.initialize(appExecutor, [presence: "present"])
        inputSensor2.initialize(appExecutor, [presence: "present"])
        outputSensor.initialize(appExecutor, [presence: "present"])

        when:
        inputSensor1.departed()

        then:
        inputSensor1.currentValue('presence') == "not present"
        inputSensor2.currentValue('presence') == "present"
        outputSensor.currentValue('presence') == "present"
    }

    void "Boolean-OR sets output to not present when the last sensor departs"() {
        given:
        inputSensor1.initialize(appExecutor, [presence: "present"])
        inputSensor2.initialize(appExecutor, [presence: "present"])
        outputSensor.initialize(appExecutor, [presence: "present"])

        when:
        inputSensor1.departed()
        inputSensor2.departed()

        then:
        inputSensor1.currentValue('presence') == "not present"
        inputSensor2.currentValue('presence') == "not present"
        outputSensor.currentValue('presence') == "not present"
    }
}
