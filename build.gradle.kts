import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
version = "1.5-SNAPSHOT"

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
                TargetFormat.Msi, 
                TargetFormat.Deb
            )
            includeAllModules = true
            packageName = "Merchant Center"
            packageVersion = "1.1.6"
            copyright = "© 2025 Pherus all rights reserved"
            description =
                "Merchant Center is a shop management system for data entries with sale, stocks and export "
            vendor = "Pherus"
            
            // Enable automatic updates
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/main/resources"))
            
            // Ensure previous version uninstallation
            windows {
                menu = true
                shortcut = true
                installationPath = "C:\\Program Files\\Merchant Center"
                iconFile.set(project.file("src/main/resources/merchant.ico"))
                // Use consistent upgradeUuid to properly handle upgrades
                upgradeUuid = "1b1a9c9a-475d-4591-8e2d-756f04374565"
                msiPackageVersion = "1.1.6"
                exePackageVersion = "1.1.6"
                // Add uninstaller logic
                dirChooser = true
                perUserInstall = true
                console = false
                // Enable auto-update support
                menuGroup = "Merchant Center"
            }

            linux {
                installationPath = "/opt/merchant-center"
                iconFile.set(project.file("src/main/resources/merchant.png"))
                debMaintainer = "pherus@pherus.org"
                // Add package management info for better updates
                packageName = "merchant-center"
                appCategory = "Office;Finance"
                rpmLicenseType = "Commercial"
            }

            macOS {
                // Add macOS support
                iconFile.set(project.file("src/main/resources/merchant.icns"))
                bundleID = "org.pherus.merchantcenter"
                appCategory = "public.app-category.business"
                signing {
                    sign.set(false) // Set to true when certificates are ready
                }
            }

            // Required Java modules
            modules("java.base", "java.desktop", "java.instrument", "java.sql", "jdk.unsupported")
            
            // Optimize build for release
            buildTypes.release {
                proguard {
                    configurationFiles.from("proguard-rules.pro")
                    obfuscate.set(true)
                    isEnabled.set(false) // Enable for production releases
                }
            }
        }

        sourceSets {
            main {
                resources {
                    exclude("**/unused/**")
                    // Include update-related resources
                    include("**/updates/**")
                }
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.set(
            listOf(
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions",
                "-Xskip-metadata-version-check",
                "-Xinline-classes",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
                "-opt-in=kotlin.Experimental",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
            )
        )
    }
}

tasks.withType<Jar> {
    exclude("**/*.md")
    exclude("**/*.txt")
    exclude("**/*.xml")
}

tasks.register("jpackage") {
    dependsOn("compileKotlin")
    doLast {
        println("Running jpackage...")
    }
}