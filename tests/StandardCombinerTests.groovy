package joelwetzel.combined_presence.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.validation.Flags

/**
 * Basic tests for standardCombinedPresenceInstance.groovy (Standard Combiner)
 */
class StandardCombinerTests extends IntegrationAppSpecification {
    def gpsInputSensor1 = PresenceSensorFixtureFactory.create('gps1')
    def gpsInputSensor2 = PresenceSensorFixtureFactory.create('gps2')
    def wifiInputSensor = PresenceSensorFixtureFactory.create('wifi1')
    def outputSensor = PresenceSensorFixtureFactory.create('output')
    def inputSensorsGps = [gpsInputSensor1, gpsInputSensor2]
    def inputSensorsWifi = [wifiInputSensor]

    @Override
    def setup() {
        super.initializeEnvironment(
            appScriptFilename: "standardCombinedPresenceInstance.groovy",
            validationFlags: [Flags.AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe],
            userSettingValues: [
                inputSensorsGps: inputSensorsGps,
                inputSensorsWifi: inputSensorsWifi,
                outputSensor: outputSensor,
                enableLogging: true
            ]
        )
        appScript.installed()
    }

    void "GPS arrival sets the output to present"() {
        given:
        gpsInputSensor1.initialize(appExecutor, [presence: "not present"])
        gpsInputSensor2.initialize(appExecutor, [presence: "not present"])
        wifiInputSensor.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "not present"])

        when:
        gpsInputSensor1.arrived()

        then:
        gpsInputSensor1.currentValue('presence') == "present"
        gpsInputSensor2.currentValue('presence') == "not present"
        outputSensor.currentValue('presence') == "present"
    }

    void "WiFi arrival sets the output to present"() {
        given:
        gpsInputSensor1.initialize(appExecutor, [presence: "not present"])
        gpsInputSensor2.initialize(appExecutor, [presence: "not present"])
        wifiInputSensor.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "not present"])

        when:
        wifiInputSensor.arrived()

        then:
        wifiInputSensor.currentValue('presence') == "present"
        outputSensor.currentValue('presence') == "present"
    }

    void "GPS departure sets the output to not present"() {
        given:
        gpsInputSensor1.initialize(appExecutor, [presence: "present"])
        gpsInputSensor2.initialize(appExecutor, [presence: "present"])
        wifiInputSensor.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "present"])

        when:
        gpsInputSensor1.departed()

        then:
        gpsInputSensor1.currentValue('presence') == "not present"
        outputSensor.currentValue('presence') == "not present"
    }

    void "WiFi departure does not set the output to not present"() {
        given:
        gpsInputSensor1.initialize(appExecutor, [presence: "present"])
        gpsInputSensor2.initialize(appExecutor, [presence: "present"])
        wifiInputSensor.initialize(appExecutor, [presence: "not present"])
        outputSensor.initialize(appExecutor, [presence: "present"])

        when:
        wifiInputSensor.departed()

        then:
        wifiInputSensor.currentValue('presence') == "not present"
        outputSensor.currentValue('presence') == "present"
    }
}
