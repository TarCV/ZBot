plugins {
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.mewna:catnip:3.0.0")
    implementation("mysql:mysql-connector-java:5.0.8")
    implementation("org.ini4j:ini4j:0.5.2")
    implementation("javax.json:javax.json-api:1.1.4")
    implementation("org.glassfish:javax.json:1.1.4")
    implementation("com.google.guava:guava:30.1-jre")

    testImplementation("junit:junit:4.12")
}

application {
    mainClass.set("org.bestever.bebot.Bot")
}
