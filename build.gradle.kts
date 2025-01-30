import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("app.cash.sqldelight") version "2.0.2"
}

group = "pherus.merchant.center"
version = "1.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("pherus.merchant.center")
            dialect("app.cash.sqldelight:mysql-dialect:2.0.2")
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material")
    }
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)

    // PDF Generation
    implementation("org.apache.pdfbox:pdfbox:2.0.27")

    // Excel Generation
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Date and Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // Navigation desktop compose
    implementation("cafe.adriel.voyager:voyager-navigator:1.0.0-rc06")
    implementation("cafe.adriel.voyager:voyager-transitions:1.0.0-rc06")

    // Use JVM-specific Room dependencies
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2") // SQLite driver
    implementation("app.cash.sqldelight:jdbc-driver:2.0.2")  // For general JDBC access

    // Logger
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    //jvmToolchain(21) // Specify Java 21
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs += listOf(
            "-Xmx2G",
            "-Dfile.encoding=UTF-8"
        )
        nativeDistributions {
            targetFormats(
                TargetFormat.Msi, TargetFormat.Deb
            )
            includeAllModules = true
            packageName = "Merchant Center"
            packageVersion = "1.1.2"
            copyright = "© 2025 Pherus all rights reserved"
            description =
                "Merchant Center is a shop management system for data entries with sale, stocks and export "
            vendor = "Pherus"

            windows {
                installationPath = "C:\\Program Files\\Merchant Center"
                iconFile.set(File("src/main/resources/merchant.ico"))
            }

            linux {
                installationPath = "/usr/share/merchant-center"
                iconFile.set(File("src/main/resources/merchant.png")) // Change to merchant.png
                debMaintainer = "pherus@pherus.org"
            }

            windows {
                menu = true
                shortcut = true
                iconFile.set(project.file("src/main/resources/merchant.ico"))
            }

            modules("java.base", "java.desktop")
            modules("java.instrument", "java.sql", "jdk.unsupported")
            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

tasks.register("jpackage") {
    dependsOn("compileKotlin")
    doLast {
        println("Running jpackage...")
    }
}