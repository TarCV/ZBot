plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("pircbot:pircbot:1.5.0")
    implementation("mysql:mysql-connector-java:5.0.8")
    implementation("org.ini4j:ini4j:0.5.2")
    implementation("javax.json:javax.json-api:1.1.4")
    implementation("org.glassfish:javax.json:1.1.4")
}

application {
    mainClass.set("org.bestever.bebot.Bot")
}
