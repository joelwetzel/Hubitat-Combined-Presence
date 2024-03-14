package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.PresenceSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

class NotificationTests extends IntegrationAppSpecification {
    def wifiSensor = PresenceSensorFixtureFactory.create('sensor1')
    def gpsSensor1 = PresenceSensorFixtureFactory.create('sensor2')
    def gpsSensor2 = PresenceSensorFixtureFactory.create('sensor3')

    def output = PresenceSensorFixtureFactory.create('output')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "standardCombinedPresenceInstance.groovy",
                                    userSettingValues: [inputSensorsGps: [gpsSensor1, gpsSensor2], inputSensorsWifi: [wifiSensor], outputSensor: output, notificationDevice: null, notifyAboutStateChanges: false, notifyAboutInconsistencies: true, enableLogging: true])
        wifiSensor.initialize(appExecutor, [presence: 'not present'])
        gpsSensor1.initialize(appExecutor, [presence: 'not present'])
        gpsSensor2.initialize(appExecutor, [presence: 'not present'])
        output.initialize(appExecutor, [presence: 'not present'])

        appScript.installed()
    }

    void "Inconsistencies will be logged"() {       // TODO - test that they also get sent out through the notification device.
        when:   "Advance a little, so that it records a last consistent time"
        TimeKeeper.advanceMinutes(5)

        and:    "One of the sensors changes but the others stay the same"
        wifiSensor.arrived()

        and:    "It stays inconsistent for a half hour"
        TimeKeeper.advanceMinutes(31)

        then:
        1 * log.debug("Input sensors for output have been inconsistent for 30 minutes.  This may mean one of your presence sensors is not updating. present: sensor1, not present: sensor2,sensor3")

        when:   "But not too often"
        TimeKeeper.advanceMinutes(1)

        then:
        0 * log.debug("Input sensors for output have been inconsistent for 30 minutes.  This may mean one of your presence sensors is not updating. present: sensor1, not present: sensor2,sensor3")
    }

    void "If the other sensors arrive a minute after the wifi sensor, and then we wait 30 minutes, no inconsistency log is written"() {
        when:   "Advance a little, so that it records a last consistent time"
        TimeKeeper.advanceMinutes(5)

        and:    "One of the sensors changes but the others stay the same"
        wifiSensor.arrived()

        and:    "The other sensors arrive a minute later"
        TimeKeeper.advanceMinutes(1)
        gpsSensor1.arrived()
        gpsSensor2.arrived()

        and:    "It stays consistent for a half hour"
        TimeKeeper.advanceMinutes(31)

        then:
        0 * log.debug("Input sensors for output have been inconsistent for 30 minutes.  This may mean one of your presence sensors is not updating. present: sensor1, not present: sensor2,sensor3")
    }

}
