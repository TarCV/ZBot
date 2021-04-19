plugins {
    id("org.sonarqube").version("3.1.1")
}

subprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "TarCV_ZBot")
        property("sonar.organization", "tarcv")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
