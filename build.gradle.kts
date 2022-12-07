plugins {
    kotlin("js") version "1.7.10"
}

group = "me.regob"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

dependencies {
    testImplementation(kotlin("test-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    //implementation(npm("bootstrap", "5.2.2"))
    //implementation(devNpm("html-webpack-plugin", "5.3.1"))
}

kotlin {
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName="backgammon-kotlinjs.js"
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    //useFirefox()
                    useChromeHeadless()
                }
            }
        }
    }

}