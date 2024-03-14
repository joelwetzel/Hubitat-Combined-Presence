package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Behavior tests for advancedCombinedPresenceInstance.groovy
*/
class AdvancedCombinerTests extends IntegrationAppSpecification {
    def sensor1 = PresenceSensorFixtureFactory.create('sensor1')
    def sensor2 = PresenceSensorFixtureFactory.create('sensor2')
    def sensor3 = PresenceSensorFixtureFactory.create('sensor3')
    def sensor4 = PresenceSensorFixtureFactory.create('sensor4')
    def sensor5 = PresenceSensorFixtureFactory.create('sensor5')
    def sensor6 = PresenceSensorFixtureFactory.create('sensor6')
    def sensor7 = PresenceSensorFixtureFactory.create('sensor7')
    def sensor8 = PresenceSensorFixtureFactory.create('sensor8')

    def output = PresenceSensorFixtureFactory.create('output')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "advancedCombinedPresenceInstance.groovy",
                                    userSettingValues:
                                        [inputSensorsArrivingOr: [sensor1, sensor2],
                                        inputSensorsArrivingAnd: [sensor3, sensor4],
                                        inputSensorsDepartingOr: [sensor5, sensor6],
                                        inputSensorsDepartingAnd: [sensor7, sensor8],
                                        outputSensor: output,
                                        enableLogging: true
                                        ])
        sensor1.initialize(appExecutor, [presence: 'not present'])
        sensor2.initialize(appExecutor, [presence: 'not present'])
        sensor3.initialize(appExecutor, [presence: 'not present'])
        sensor4.initialize(appExecutor, [presence: 'not present'])
        sensor5.initialize(appExecutor, [presence: 'not present'])
        sensor6.initialize(appExecutor, [presence: 'not present'])
        sensor7.initialize(appExecutor, [presence: 'not present'])
        sensor8.initialize(appExecutor, [presence: 'not present'])
        output.initialize(appExecutor, [presence: 'not present'])

        appScript.installed()
    }


    void "initialize() subscribes to events"() {
        when:
        appScript.initialize()

        then:
        // Expect that events are subscribed to
        1 * appExecutor.subscribe([sensor1, sensor2], 'presence', 'presenceChangedHandler')
        1 * appExecutor.subscribe([sensor3, sensor4], 'presence', 'presenceChangedHandler')
        1 * appExecutor.subscribe([sensor5, sensor6], 'presence', 'presenceChangedHandler')
        1 * appExecutor.subscribe([sensor7, sensor8], 'presence', 'presenceChangedHandler')
    }

    void "Arrival-OR sensors make a person ipresent, no matter what other sensors say"() {
        when:
        sensor1.arrived()

        then:
        1 * appExecutor.sendEvent(output, [name: 'presence', value: 'present'])
        output.currentValue('presence') == 'present'
    }

    // I won't be writing a full suite of tests, as the advanced combiner is low priority.
    // However, there are enough users, that I will keep it around.
}
