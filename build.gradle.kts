import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.0.1"
    kotlin("plugin.serialization") version "1.6.10"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.2.0")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.7.4")
    implementation("com.charleskorn.kaml:kaml:0.38.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "15"
        //This allows us to use the experimental Material API calls
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

compose.desktop {
    application {
        mainClass = "ch.icken.csvtoolkit.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "csvtoolkit"
            packageVersion = "1.0.0"
            description = "Manipulate CSV files as if they are tables in a relational database :)"

            val iconsRoot = project.file("./src/main/resources")
            macOS {
                iconFile.set(iconsRoot.resolve("icon-macos.icns"))
                bundleID = "ch.icken.csvtoolkit"
            }
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
                upgradeUuid = "DDE4599E-E34C-4206-B7F2-74DBEFFFAA10"
                menu = true
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon.png"))
            }
        }
    }
}