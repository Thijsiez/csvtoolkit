import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("org.jetbrains.compose") version "0.4.0"
}

group = "ch.icken"
version = "1.0.0"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        //This allows us to use the experimental Material API calls
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

compose.desktop {
    application {
        mainClass = "ch.icken.csvtoolkit.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "csvtoolkit"
            packageVersion = "1.0.0"
        }
    }
}