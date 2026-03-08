import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.example.idleprogrammergame"
version = "1.0.0"

application {
    mainClass.set("com.example.server.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.logback)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// ShadowJar configuration to build a single executable JAR with all dependencies
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.server.MainKt"
        attributes["Implementation-Title"] = "IdleProgrammerServer"
        attributes["Implementation-Version"] = project.version
    }
    
    // Naming the output file clearly
    archiveFileName.set("server-${project.version}.jar")

    // This creates a "fat" jar by including all runtime dependencies
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
