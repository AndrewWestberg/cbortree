import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    id("io.github.ben-manes.versions") version Versions.VERSIONS_PLUGIN
    id("com.vanniktech.maven.publish") version Versions.MAVEN_PUBLISH_PLUGIN
    id("signing")
}

group = "io.newm"
version = "0.5.0-NEWM-SNAPSHOT"

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:${Versions.JSON}")
    implementation("it.unimi.dsi:fastutil:${Versions.FASTUTIL}")
    implementation("org.checkerframework:checker-qual:${Versions.CHECKER_QUAL}")
    implementation("com.google.code.findbugs:jsr305:${Versions.JSR305}")
    implementation("com.google.errorprone:error_prone_annotations:${Versions.ERROR_PRONE_ANNOTATIONS}")

    testImplementation("com.google.truth:truth:${Versions.GOOGLE_TRUTH}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.JUNIT}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${Versions.JUNIT}")
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.newm", "com.google.iot.cbor", version.toString())
    pom {
        name.set("CborTree")
        description.set("A Java API for decoding, manipulating, and encoding CBOR data items.")
        url.set("https://github.com/AndrewWestberg/cbortree")
        licenses {
            license {
                name.set("Apache 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("AndrewWestberg")
                name.set("Andrew Westberg")
                email.set("andrewwestberg@gmail.com")
                organization.set("NEWM")
                organizationUrl.set("https://newm.io")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/AndrewWestberg/cbortree.git")
            developerConnection.set("scm:git:ssh://github.com/AndrewWestberg/cbortree.git")
            url.set("https://github.com/AndrewWestberg/cbortree")
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<DependencyUpdatesTask> {
    // Example 1: reject all non stable versions
    rejectVersionIf {
        isNonStable(candidate.version)
    }

    // Example 2: disallow release candidates as upgradable versions from stable versions
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }

    // Example 3: using the full syntax
    resolutionStrategy {
        componentSelection {
            all {
                if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                    reject("Release candidate")
                }
            }
        }
    }
}

tasks.withType<Test> {
    maxHeapSize = "8192m"
}
