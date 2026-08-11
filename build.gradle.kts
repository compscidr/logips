plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlinter)
    id("jacoco")
    alias(libs.plugins.git.version) // https://stackoverflow.com/a/71212144
    alias(libs.plugins.sonatype.maven.central)
    alias(libs.plugins.gradleup.nmcp.aggregation)
}

kotlin {
    jvmToolchain(21)
}

// Emit Java 11 bytecode for the published jar so consumers on JDK 11+ can use the
// library (https://github.com/compscidr/logips/issues/83). -Xjdk-release links against
// the JDK 11 API surface so use of newer APIs fails our build instead of consumers'.
// Tests still compile and run on the JDK 21 toolchain.
tasks.compileJava {
    options.release = 11
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        freeCompilerArgs.add("-Xjdk-release=11")
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

jacoco {
    toolVersion = "0.8.15"
}

dependencies {
    api(libs.slf4j.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.test.runtime)
}

version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
    refs {
        branch(".+") { version = "\${ref}-SNAPSHOT" }
        tag("v(?<version>.*)") { version = "\${ref.version}" }
    }
}

nmcpAggregation {
    val props = project.properties
    centralPortal {
        username = props["centralPortalToken"] as String? ?: ""
        password = props["centralPortalPassword"] as String? ?: ""
        publishingType = "AUTOMATIC"
    }
    publishAllProjectsProbablyBreakingProjectIsolation()
}

// see: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#configuring-the-pom
mavenPublishing {
    coordinates("com.jasonernst.logips", "logips", version.toString())
    pom {
        name = "logips"
        description = "A simple lib to log ip addresses in kotlin"
        inceptionYear = "2024"
        url = "https://github.com/compscidr/logips"
        licenses {
            license {
                name = "GPL-3.0"
                url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "compscidr"
                name = "Jason Ernst"
                url = "https://www.jasonernst.com"
            }
        }
        scm {
            url = "https://github.com/compscidr/logips"
            connection = "scm:git:git://github.com/compscidr/logips.git"
            developerConnection = "scm:git:ssh://git@github.com/compscidr/logips.git"
        }
    }

    signAllPublications()
}
