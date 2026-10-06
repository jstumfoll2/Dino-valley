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

// Plays simulated children through many journeys and prints what the game is like to play: how long it
// takes, which skills come up, whether levels drift, whether answers can be guessed by position.
tasks.register<JavaExec>("balanceReport") {
    group = "verification"
    description = "Prints a balance report from simulated play (also written to build/balance/report.md)."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.littledungeon.engine.rpg.balance.BalanceSimKt")
    args(layout.buildDirectory.file("balance/report.md").get().asFile.absolutePath)
}
