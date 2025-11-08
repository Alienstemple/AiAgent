plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    application
    alias(libs.plugins.kotlin.serialization)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

application {
    mainClass.set("com.whitemonkeys.console_agent.ConsoleAgentKt")
}

dependencies {
    implementation(libs.koog.agents)
    implementation(libs.prompt.executor.deepseek.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.dotenv.kotlin)
}
