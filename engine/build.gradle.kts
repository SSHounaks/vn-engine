import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlinx.kover")
    `maven-publish`
}

group = "io.github.sshounaks"
version = "0.1.0"

repositories {
    mavenCentral()
    google()
}

// The web (wasmJs) target is opt-in: production Wasm optimization needs Binaryen,
// which cannot be fetched in every environment. Enable with -PenableWasm=true to
// compile and run the browser build (./gradlew :engine:wasmJsBrowserDevelopmentRun -PenableWasm=true).
val enableWasm = providers.gradleProperty("enableWasm").orNull?.toBoolean() ?: false

kotlin {
    jvm("desktop")

    if (enableWasm) {
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser {
                commonWebpackConfig {
                    outputFileName = "composeApp.js"
                }
                testTask {
                    enabled = false
                }
            }
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                // Runtime binding to the (user-installed) Cubism Core native library.
                implementation("net.java.dev.jna:jna:5.14.0")
            }
        }
        if (enableWasm) {
            val wasmJsMain by getting {
                // Serve the same art assets to the web target as the desktop target.
                resources.srcDir("src/desktopMain/resources")
                // Nearby sample scenes (withheld/private, gitignored) can be swapped in
                // for local previews with -PsampleStory=true; the committed default uses
                // the asset-free demoStory.
                if (providers.gradleProperty("sampleStory").orNull?.toBoolean() == true) {
                    kotlin.exclude("**/WebStory.kt")
                    kotlin.srcDir("src/wasmJsSample/kotlin")
                }
                dependencies {
                    implementation(compose.runtime)
                    implementation(compose.foundation)
                    implementation(compose.material3)
                }
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/SSHounaks/vn-engine")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                // Local-only, gitignored sample scene; not part of the published library.
                classes("com.vnengine.dsl.SampleGameKt")
            }
        }
        verify {
            rule {
                minBound(70)
            }
        }
    }
}
