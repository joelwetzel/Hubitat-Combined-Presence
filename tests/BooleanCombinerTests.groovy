package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Behavior tests for combinedPresenceInstance.groovy
*/
class BooleanCombinerTests extends IntegrationAppSpecification {
    def sensor1 = PresenceSensorFixtureFactory.create('sensor1')
    def sensor2 = PresenceSensorFixtureFactory.create('sensor2')
    def sensor3 = PresenceSensorFixtureFactory.create('sensor3')
    def sensors = [sensor1, sensor2, sensor3]

    def output = PresenceSensorFixtureFactory.create('output')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "combinedPresenceInstance.groovy",
                                    userSettingValues: [inputSensors: sensors, outputSensor: output, notificationDevice: null, notifyAboutStateChanges: false, enableLogging: true])
        appScript.installed()
    }


    void "initialize() subscribes to events"() {
        when:
        appScript.initialize()

        then:
        // Expect that events are subscribe to
        1 * appExecutor.subscribe(sensors, 'presence', 'presenceChangedHandler')
    }

    void "On initialize, output sensor gets correct value based on the inputs. Case: All present"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'present'])
            sensor2.initialize(appExecutor, [presence: 'present'])
            sensor3.initialize(appExecutor, [presence: 'present'])
            output.initialize(appExecutor, [presence: 'not present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'present'
    }

    void "On initialize, output sensor gets correct value based on the inputs. Case: None present"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'not present'])
            sensor2.initialize(appExecutor, [presence: 'not present'])
            sensor3.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'not present'
    }

    void "On initialize, output sensor gets correct value based on the inputs. Case: Single present"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'not present'])
            sensor2.initialize(appExecutor, [presence: 'present'])
            sensor3.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'not present'])

        when:
            appScript.initialize()

        then:
            output.currentValue('presence') == 'present'
    }

    void "A single sensor triggers arrival"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'not present'])
            sensor2.initialize(appExecutor, [presence: 'not present'])
            sensor3.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'not present'])
            appScript.initialize()

        when:
            sensor2.arrived()

        then:
            1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
            output.currentValue('presence') == 'present'
    }

    void "A second arrival doesn't change anything"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'not present'])
            sensor2.initialize(appExecutor, [presence: 'not present'])
            sensor3.initialize(appExecutor, [presence: 'not present'])
            output.initialize(appExecutor, [presence: 'not present'])
            appScript.initialize()

        when:
            sensor2.arrived()

        then:
            1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
            output.currentValue('presence') == 'present'

        when:
            sensor3.arrived()

        then:
            0 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
            output.currentValue('presence') == 'present'
    }

    void "A single sensor does not trigger departure"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'present'])
            sensor2.initialize(appExecutor, [presence: 'present'])
            sensor3.initialize(appExecutor, [presence: 'present'])
            output.initialize(appExecutor, [presence: 'present'])
            appScript.initialize()

        when:
            sensor2.departed()

        then:
            0 * appExecutor.sendEvent(output, [name: 'presence', value: 'not present'])
            output.currentValue('presence') == 'present'
    }

    void "All sensors must depart to trigger departure"() {
        given:
            sensor1.initialize(appExecutor, [presence: 'present'])
            sensor2.initialize(appExecutor, [presence: 'present'])
            sensor3.initialize(appExecutor, [presence: 'present'])
            output.initialize(appExecutor, [presence: 'present'])
            appScript.initialize()

        when:
            sensor1.departed()

        then:
            0 * appExecutor.sendEvent(output, [name: 'presence', value: 'not present'])
            output.currentValue('presence') == 'present'

        when:
            sensor2.departed()

        then:
            0 * appExecutor.sendEvent(output, [name: 'presence', value: 'not present'])
            output.currentValue('presence') == 'present'

        when:
            sensor3.departed()

        then:
            1 * appExecutor.sendEvent(output, [name: 'presence', value: 'not present'])
            output.currentValue('presence') == 'not present'
    }

}
