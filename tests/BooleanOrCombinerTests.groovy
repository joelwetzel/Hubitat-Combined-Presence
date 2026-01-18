package joelwetzel.combined_presence.tests

import me.biocomp.hubitat_ci.device.helpers.PresenceSensorHelper
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.validation.Flags

/**
 * Basic tests for combinedPresenceInstance.groovy (Boolean-OR Combiner)
 */
class BooleanOrCombinerTests extends IntegrationAppSpecification {
    def inputSensor1 = PresenceSensorHelper.getDeviceMock('sensor1')
    def inputSensor2 = PresenceSensorHelper.getDeviceMock('sensor2')
    def outputSensor = PresenceSensorHelper.getDeviceMock('output')
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
    }

    void "installed() logs the settings"() {
        when:
        appScript.installed()

        then:
        1 * log.info(_)
    }

    void "initialize() subscribes to presence events"() {
        when:
        appScript.initialize()

        then:
        1 * appExecutor.subscribe(inputSensors, 'presence', 'presenceChangedHandler')
    }

    void "Boolean-OR: when all sensors are not present, output is not present"() {
        given:
        appScript.initialize()
        inputSensor1.presence = 'not present'
        inputSensor2.presence = 'not present'

        when:
        appScript.presenceChangedHandler(makeEvent(inputSensor1, 'presence', 'not present'))

        then:
        1 * outputSensor.departed()
    }

    void "Boolean-OR: when at least one sensor is present, output is present"() {
        given:
        appScript.initialize()
        inputSensor1.presence = 'present'
        inputSensor2.presence = 'not present'

        when:
        appScript.presenceChangedHandler(makeEvent(inputSensor1, 'presence', 'present'))

        then:
        1 * outputSensor.arrived()
    }

    void "Boolean-OR: when both sensors are present, output is present"() {
        given:
        appScript.initialize()
        inputSensor1.presence = 'present'
        inputSensor2.presence = 'present'

        when:
        appScript.presenceChangedHandler(makeEvent(inputSensor1, 'presence', 'present'))

        then:
        1 * outputSensor.arrived()
    }
}
