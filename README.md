# matrix-client [![GitLab CI Build Status](https://gitlab.com/mextrix/matrix-client/badges/master/build.svg)](https://gitlab.com/mextrix/matrix-client/pipelines) [![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![coverage report](https://gitlab.com/mextrix/matrix-client/badges/master/coverage.svg)](https://gitlab.com/mextrix/matrix-client/commits/master)

A [matrix](https://matrix.org/) client for Java and other JVM languages written in Kotlin.

There is also a cli interface written in Java in the `cli` folder.

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

```gradle
repositories {
	maven { url "https://maven.devpi.de/repository/default/" }
}

dependencies {
	compile "msrd0.matrix:matrix-client:+"
}
```

## Usage

To see how this client can be used, take a look at the
[Main](https://gitlab.com/mextrix/matrix-client/blob/master/cli/src/msrd0/matrix/client/cli/Main.java)
class in the cli project
