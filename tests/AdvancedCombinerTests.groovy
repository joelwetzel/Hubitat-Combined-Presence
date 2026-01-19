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
        appScript.installed()
    }
    

    void "ArrivingOr arrival sets the output to present"() {
        given:
        arrivingOrSensor1.initialize(appExecutor, [presence: "not present"])
        arrivingAndSensor1.initialize(appExecutor, [presence: "not present"])
        arrivingAndSensor2.initialize(appExecutor, [presence: "not present"])
        departingOrSensor1.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "not present"])
        appScript.initialize()

        when:
        arrivingOrSensor1.arrived()

        then:
        arrivingOrSensor1.currentValue('presence') == "present"
        outputSensor.currentValue('presence') == "present"
    }

    void "ArrivingAnd sensors arriving together set the output to present"() {
        given:
        arrivingOrSensor1.initialize(appExecutor, [presence: "not present"])
        arrivingAndSensor1.initialize(appExecutor, [presence: "not present"])
        arrivingAndSensor2.initialize(appExecutor, [presence: "not present"])
        departingOrSensor1.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "not present"])

        when:
        arrivingAndSensor1.arrived()
        arrivingAndSensor2.arrived()

        then:
        arrivingAndSensor1.currentValue('presence') == "present"
        arrivingAndSensor2.currentValue('presence') == "present"
        outputSensor.currentValue('presence') == "present"
    }

    void "DepartingOr departure sets the output to not present"() {
        given:
        arrivingOrSensor1.initialize(appExecutor, [presence: "present"])
        arrivingAndSensor1.initialize(appExecutor, [presence: "present"])
        arrivingAndSensor2.initialize(appExecutor, [presence: "present"])
        departingOrSensor1.initialize(appExecutor, [presence: "present"])
        outputSensor.initialize(appExecutor, [presence: "present"])

        when:
        departingOrSensor1.departed()

        then:
        departingOrSensor1.currentValue('presence') == "not present"
        outputSensor.currentValue('presence') == "not present"
    }
}
