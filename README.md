# matrix-client [![GitLab CI Build Status](https://gitlab.com/mextrix/matrix-client/badges/master/pipeline.svg)](https://gitlab.com/mextrix/matrix-client/pipelines) [![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![coverage report](https://gitlab.com/mextrix/matrix-client/badges/master/coverage.svg)](https://gitlab.com/mextrix/matrix-client/commits/master)

A [matrix](https://matrix.org/) client for Java and other JVM languages written in Kotlin.

The documentation of the `master`-branch is automatically uploaded here:
https://mextrix.gitlab.io/matrix-client/

If you encounter any problems, please open an issue here:
https://gitlab.com/mextrix/matrix-client/issues

There is also a cli interface written in Java in the `cli` folder.

**NOTE:** There is nobody actively working on this project. If you are interested in working on this project, please get in touch.

## Supported Modules

| Module | Support | Branch |
|--------|:-------:|:------:|
| Instant Messaging | Partial | `master` |
| Presence | Planned | -- |
| Push Notifications | No | -- |
| Receipts | Planned | -- |
| Typing Notifications | No | -- |
| VoIP | Partial | `master` |
| Content Repository | Partial | `master` |
| Managing History Visibility | Fully | `master` |
| Server Side Search | No | -- |
| Server Administration | No | -- |
| Event Context | No | -- |
| Device Management | Fully | `master` |
| End-to-End Encryption | Partial | `e2e` |
| Third-party Invites | No | -- |
| Guest Access | No | -- |

## Gradle

After running `./gradlew install` on this project, you can use it like this:

```gradle
repositories {
	mavenLocal()
}

dependencies {
	compile "de.msrd0.matrix:matrix-client:1.0"
}
```

## Usage

To see how this client can be used, take a look at the
[Main](https://gitlab.com/mextrix/matrix-client/blob/master/cli/src/de/msrd0/matrix/client/cli/Main.java)
class in the cli project
