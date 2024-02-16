package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Behavior tests for standardCombinedPresenceInstance.groovy
*/
class StandardCombinerTests extends IntegrationAppSpecification {
    def wifiSensor = PresenceSensorFixtureFactory.create('sensor1')
    def gpsSensor1 = PresenceSensorFixtureFactory.create('sensor2')
    def gpsSensor2 = PresenceSensorFixtureFactory.create('sensor3')

    def output = PresenceSensorFixtureFactory.create('output')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "standardCombinedPresenceInstance.groovy",
                                    userSettingValues: [inputSensorsGps: [gpsSensor1, gpsSensor2], inputSensorsWifi: [wifiSensor], outputSensor: output, notificationDevice: null, notifyAboutStateChanges: false, notifyAboutInconsistencies: false, enableLogging: true])
        wifiSensor.initialize(appExecutor, [presence: 'not present'])
        gpsSensor1.initialize(appExecutor, [presence: 'not present'])
        gpsSensor2.initialize(appExecutor, [presence: 'not present'])
        output.initialize(appExecutor, [presence: 'not present'])

        appScript.installed()
    }


    void "initialize() subscribes to events"() {
        when:
        appScript.initialize()

        then:
        // Expect that events are subscribed to
        1 * appExecutor.subscribe([wifiSensor], 'presence.present', 'arrivedHandler')
        1 * appExecutor.subscribe([gpsSensor1, gpsSensor2], 'presence.present', 'arrivedHandler')
        1 * appExecutor.subscribe([gpsSensor1, gpsSensor2], 'presence.not present', 'departedHandler')
    }

    void "If wifi detects arrival, then the person is present, no matter what other sensors say"() {
        when:
        wifiSensor.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'
    }

    void "If a GPS sensor detects arrival, then the person is present, no matter what other sensors say"() {
        when:
        gpsSensor1.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'
    }

    void "A wifi arrival after a GPS arrival does not trigger a second output arrival"() {
        when:
        gpsSensor1.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'

        when:
        wifiSensor.arrived()

        then:
        0 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'
    }

    void "A gps arrival after a wifi arrival does not trigger a second output arrival"() {
        when:
        wifiSensor.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'

        when:
        gpsSensor1.arrived()

        then:
        0 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'
    }

    void "After arrival, the wifi detecting departure doesn't change anything.  It might just be a wifi sleep on the phone."() {
        when:
        wifiSensor.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'

        when:
        wifiSensor.departed()

        then:
        0 * appExecutor.sendEvent(output, [name: 'presence', value: 'not present'])
        output.currentValue('presence') == 'present'
    }

    void "After arrival, a gps departure will cause the person to be considered not present"() {
        when:
        gpsSensor1.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'

        when:
        gpsSensor1.departed()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'not present'])
        output.currentValue('presence') == 'not present'
    }

    void "On initialize, output sensor gets correct Boolean-OR value based on the inputs. Case: None present"() {
        given:
            wifiSensor.initialize(appExecutor, [presence: 'not present'])
            gpsSensor1.initialize(appExecutor, [presence: 'not present'])
            gpsSensor2.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'not present'
    }


    void "On initialize, output sensor gets correct Boolean-OR value based on the inputs. Case: One wifi sensor present"() {
        given:
            wifiSensor.initialize(appExecutor, [presence: 'present'])
            gpsSensor1.initialize(appExecutor, [presence: 'not present'])
            gpsSensor2.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'not present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'present'
    }

    void "On initialize, output sensor gets correct Boolean-OR value based on the inputs. Case: One GPS sensor present"() {
        given:
            wifiSensor.initialize(appExecutor, [presence: 'not present'])
            gpsSensor1.initialize(appExecutor, [presence: 'present'])
            gpsSensor2.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'not present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'present'
    }

    void "On initialize, output sensor gets correct Boolean-OR value based on the inputs. Case: All inputs present"() {
        given:
            wifiSensor.initialize(appExecutor, [presence: 'present'])
            gpsSensor1.initialize(appExecutor, [presence: 'present'])
            gpsSensor2.initialize(appExecutor, [presence: 'present'])
            output.initialize(appExecutor, [presence: 'not present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'present'
    }
}
