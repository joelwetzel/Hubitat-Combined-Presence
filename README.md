# Combined Presence app for Hubitat
An app for Hubitat to combine two or more presence sensors to control an output Virtual Presence Sensor.

There are 3 types of combiners:
- Standard Combiners (Use this for combining wifi and gps-based sensors for a single person.  It is optimized for that.)
- Boolean-OR Combiners (Example:  If Person1 OR Person2 is home, then the virtual SomeoneIsHome sensor should be home.)
- Advanced Combiners (All logic options.  It is a superset of what the Standard and Boolean-OR combiners can do, and you can re-create their functionality or do other boolean operations by configuring it correctly.  But almost no one will need this.)

## Use Case 1
You have several geofencing sensors on your phone (Alexa, Homekit, Life360, etc) and you also use a sensor that detects when your phone is on WiFi.  You can use the Standard Combiner for optimal logic of deciding whether you are home or away.

## Use Case 2
If you have presence sensors for two people, you can use a Boolean-OR Combiner to set a third Virtual Presence sensor for "Somebody is home", then use the departed event from it in Rule Machine to trigger events for when the last person has left the house.

## Use Case 3
I have documented the advanced bindings here: (https://community.hubitat.com/t/release-combined-presence/9186/35?u=jwetzel1492)

## Daisychaining
This app is entirely virtual and on the Hubitat hub, so it runs very fast.  This means you can daisychain multiple combinations.

An example with 3 combiners:
- "My iPhone Alexa" + "My iPhone WiFi" --> "My combined, reliable presence"
- "My wife's iPhone Alexa" + "My wife's iPhone Wifi" --> "My wife's combined, reliable presence"
- "My combined presence" + "My wife's combined presence" --> "Somebody is home"

Now I can make RM rules based on individual people arriving. And if I want a rule for when the last person leaves the house, I just trigger on "Somebody is home" changing to "not present".

It's nice because it keeps the combination logic outside of Rule Machine.

## Installation

Prerequisites:
- Input sensors already exist
- You've added a Virtual Presence sensor to be the output

The best way to install this code is by using [Hubitat Package Manager](https://community.hubitat.com/t/beta-hubitat-package-manager).

However, if you must install manually:

1. On your Hubitat hub, go to the "Apps Code" page
2. Click "+ New App"
3. Paste in the contents of combinedPresence.groovy and click "Save"
4. Go back to the "Apps Code" page
5. Click "+ New App"
6. Paste in the contents of combinedPresenceInstance.groovy and click "Save"
7. Click "+ New App"
8. Paste in the contents of advancedCombinedPresenceInstance.groovy and click "Save"
9. Click "+ New App"
10. Paste in the contents of standardCombinedPresenceInstance.groovy and click "Save"
11. Go to the "Apps" page
12. Click "+ Add User App"
13. Choose "Combined Presence"
14. Click "Done"
15. Click on "Combined Presence" in your apps list
16. Click on "Add a new standard combiner"
17. Choose your input presence sensors
18. Choose your Virtual Presence sensor as the output
19. Click "Done"

## Developer Instructions

This project includes a comprehensive integration test suite to ensure code quality and reliability when making changes.

### Testing Framework

The tests use a fork of biocomp's [Hubitat_CI](https://github.com/biocomp/hubitat_ci) project. The forked version is maintained at [https://github.com/joelwetzel/hubitat_ci](https://github.com/joelwetzel/hubitat_ci).

### Environment Variables

The Gradle build system pulls the hubitat_ci testing framework from Maven during the build process. The required environment variables are automatically configured when using GitHub Codespaces.

#### For GitHub Codespaces

Public variables are configured in `.devcontainer/devcontainer.json`. For sensitive credentials, add them as Codespaces Secrets:

1. Go to your repository → **Settings** → **Secrets and variables** → **Codespaces**
2. Add the following secret:
   - `MAVEN_GITHUB_TOKEN`: Your personal access token with `read:packages` scope ([generate here](https://github.com/settings/tokens/new?scopes=read:packages))

`MAVEN_GITHUB_ACTOR` is automatically populated from your authenticated GitHub user when the Codespace starts.

#### For Local Development

If developing locally, export these variables in your shell:

```bash
export MAVEN_GITHUB_REPOSITORY=joelwetzel/hubitat_ci
export MAVEN_GITHUB_ACTOR=your_github_username
export MAVEN_GITHUB_TOKEN=your_personal_access_token
export MAVEN_ARTIFACT_ID=hubitat_ci
```

**Note**: The `MAVEN_GITHUB_TOKEN` requires `read:packages` scope to access the Maven package repository.

### Running Tests

To run the integration tests:

```bash
./gradlew test
```

To clean the build artifacts:

```bash
./gradlew clean
```

The test reports can be found in the `build/reports/tests/test/` directory after running the tests.

