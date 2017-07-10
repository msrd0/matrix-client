# matrix-client [![GitLab CI Build Status](https://gitlab.com/msrd0/matrix-client/badges/master/build.svg)](https://gitlab.com/msrd0/matrix-client/pipelines)

A [matrix](https://matrix.org/) client for Java and other JVM languages written in Kotlin.

There is also a cli interface written in Java in the cli folder.

## Gradle

```gradle
repositories {
	maven { url "https://msrd0.duckdns.org/artifactory/gradle" }
}

dependencies {
	compile "msrd0.matrix:matrix-client:+"
}
```

## Usage

To see how this client can be used, take a look at the
[Main](https://gitlab.com/msrd0/matrix-client/blob/master/cli/src/msrd0/matrix/client/cli/Main.java)
class in the cli project
