plugins {
    kotlin("multiplatform") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}
group = "me.regob"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

//dependencies {
//    testImplementation(kotlin("test-js"))
//    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
//    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
//    //implementation(npm("bootstrap", "5.2.2"))
//    //implementation(devNpm("html-webpack-plugin", "5.3.1"))
//}

kotlin {
    js ("worker", IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName="worker.js"
            }

            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    js ("js", IR) {
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



    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
            }
        }
        val workerMain by getting {
            dependsOn(commonMain)
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val workerTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}



//task("jsBrowserDevelopmentRun") {
//    dependsOn()
//}

//
//
//kotlin {
//    js(IR) {
//        binaries.executable()
//        browser {
//            commonWebpackConfig {
//                outputFileName = "client.js"
//                cssSupport.enabled = true
//            }
//        }
//    }
//    js("webworker", IR) {
//        binaries.executable()
//        browser{
//            commonWebpackConfig {
//                outputFileName = "worker.js"
//            }
//        }
//    }
//    sourceSets {
//        val commonMain by getting {
//            dependencies {
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
//            }
//        }
//        val jsMain by getting {
//            dependencies {
//                implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.3")
//            }
//        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
//        val webworkerMain by getting {
//            dependencies {
//                //worker has separate deps from main
//                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.0")
//            }
//        }
//    }
//}
//
////rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
////    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().versions.webpackCli.version = "4.9.0"
////}
//
//
///*This task just copies files from build/distributions to /docs
//    to make it easier to update gh-pages  */
//task("copyDistToDocs", Copy::class) {
//    from("$buildDir/distributions/")
//    into("/docs")
//}
//tasks.named("copyDistToDocs"){ dependsOn("build") }