plugins {
    kotlin("jvm") version "1.4.32"
    application
}

application {
    mainClass.set("org.bestever.bebot.Bot")
}

dependencies {
    implementation("com.mewna:catnip:3.0.0")
    implementation("mysql:mysql-connector-java:5.0.8")
    implementation("org.ini4j:ini4j:0.5.2")
    implementation("javax.json:javax.json-api:1.1.4")
    implementation("org.glassfish:javax.json:1.1.4")
    implementation("com.google.guava:guava:30.1-jre")
    implementation(project(":vendor:natural-cli"))

    testImplementation("junit:junit:4.12")

    implementation(kotlin("stdlib-jdk8"))
}
val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "15"
}
val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "15"
}
