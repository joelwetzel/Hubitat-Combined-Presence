package joelwetzel.combined_presence.tests

import me.biocomp.hubitat_ci.device.helpers.PresenceSensorHelper
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.validation.Flags

/**
 * Basic tests for standardCombinedPresenceInstance.groovy (Standard Combiner)
 */
class StandardCombinerTests extends IntegrationAppSpecification {
    def gpsInputSensor1 = PresenceSensorHelper.getDeviceMock('gps1')
    def gpsInputSensor2 = PresenceSensorHelper.getDeviceMock('gps2')
    def wifiInputSensor = PresenceSensorHelper.getDeviceMock('wifi1')
    def outputSensor = PresenceSensorHelper.getDeviceMock('output')
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
    }

    void "installed() logs the settings"() {
        when:
        appScript.installed()

        then:
        1 * log.info(_)
    }

    void "initialize() subscribes to GPS sensors for both arrival and departure"() {
        when:
        appScript.initialize()

        then:
        1 * appExecutor.subscribe(inputSensorsGps, 'presence.present', 'arrivedHandler')
        1 * appExecutor.subscribe(inputSensorsGps, 'presence.not present', 'departedHandler')
    }

    void "initialize() subscribes to WiFi sensors only for arrival"() {
        when:
        appScript.initialize()

        then:
        1 * appExecutor.subscribe(inputSensorsWifi, 'presence.present', 'arrivedHandler')
    }

    void "arrivedHandler() sets output sensor to present"() {
        given:
        appScript.initialize()
        outputSensor.presence = 'not present'

        when:
        appScript.arrivedHandler(makeEvent(gpsInputSensor1, 'presence', 'present'))

        then:
        1 * outputSensor.arrived()
    }

    void "departedHandler() sets output sensor to not present"() {
        given:
        appScript.initialize()
        outputSensor.presence = 'present'

        when:
        appScript.departedHandler(makeEvent(gpsInputSensor1, 'presence', 'not present'))

        then:
        1 * outputSensor.departed()
    }

    void "WiFi sensor arrival triggers arrivedHandler"() {
        given:
        appScript.initialize()
        outputSensor.presence = 'not present'

        when:
        appScript.arrivedHandler(makeEvent(wifiInputSensor, 'presence', 'present'))

        then:
        1 * outputSensor.arrived()
    }
}
