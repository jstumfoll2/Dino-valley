import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: the game rules. No Android here, so the compiler keeps UI and rules apart.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// Writes every sentence the game can say, for the build to record ahead of time (decision #45).
tasks.register<JavaExec>("voiceLines") {
    group = "build"
    description = "Lists every sentence the narrator can say, one per line."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.littledungeon.engine.rpg.VoiceCatalogKt")
    args(layout.buildDirectory.file("voice/lines.txt").get().asFile.absolutePath)
}
