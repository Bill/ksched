import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.41"
}

group = "com.thoughtpropulsion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M2")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.0-M2-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
