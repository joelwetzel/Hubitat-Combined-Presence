package joelwetzel.combined_presence.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.validation.Flags

/**
 * Basic tests for advancedCombinedPresenceInstance.groovy (Advanced Combiner)
 */
class AdvancedCombinerTests extends IntegrationAppSpecification {
    def arrivingOrSensor1 = PresenceSensorFixtureFactory.create('arrivingOr1')
    def arrivingAndSensor1 = PresenceSensorFixtureFactory.create('arrivingAnd1')
    def arrivingAndSensor2 = PresenceSensorFixtureFactory.create('arrivingAnd2')
    def departingOrSensor1 = PresenceSensorFixtureFactory.create('departingOr1')
    def outputSensor = PresenceSensorFixtureFactory.create('output')

    @Override
    def setup() {
        super.initializeEnvironment(
            appScriptFilename: "advancedCombinedPresenceInstance.groovy",
            validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
            userSettingValues: [
                inputSensorsArrivingOr: [arrivingOrSensor1],
                inputSensorsArrivingAnd: [arrivingAndSensor1, arrivingAndSensor2],
                inputSensorsDepartingOr: [departingOrSensor1],
                inputSensorsDepartingAnd: [],
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

    void "initialize() subscribes to all sensor groups"() {
        when:
        appScript.initialize()

        then:
        1 * appExecutor.subscribe([arrivingOrSensor1], 'presence', 'presenceChangedHandler')
        1 * appExecutor.subscribe([arrivingAndSensor1, arrivingAndSensor2], 'presence', 'presenceChangedHandler')
        1 * appExecutor.subscribe([departingOrSensor1], 'presence', 'presenceChangedHandler')
        1 * appExecutor.subscribe([], 'presence', 'presenceChangedHandler')
    }

    void "when any ArrivingOr sensor is present, output becomes present"() {
        given:
        appScript.initialize()
        outputSensor.presence = 'not present'
        arrivingOrSensor1.presence = 'present'

        when:
        appScript.presenceChangedHandler(makeEvent(arrivingOrSensor1, 'presence', 'present'))

        then:
        1 * outputSensor.arrived()
    }

    void "when all ArrivingAnd sensors are present, output becomes present"() {
        given:
        appScript.initialize()
        outputSensor.presence = 'not present'
        arrivingAndSensor1.presence = 'present'
        arrivingAndSensor2.presence = 'present'

        when:
        appScript.presenceChangedHandler(makeEvent(arrivingAndSensor1, 'presence', 'present'))

        then:
        1 * outputSensor.arrived()
    }

    void "when any DepartingOr sensor departs, output becomes not present"() {
        given:
        appScript.initialize()
        outputSensor.presence = 'present'
        departingOrSensor1.presence = 'not present'

        when:
        appScript.presenceChangedHandler(makeEvent(departingOrSensor1, 'presence', 'not present'))

        then:
        1 * outputSensor.departed()
    }
}
