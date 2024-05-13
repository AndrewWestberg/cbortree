import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.hibernate.build.publish.auth.maven.MavenRepoAuthPlugin

plugins {
    java
    id("com.github.ben-manes.versions") version Versions.VERSIONS_PLUGIN
    id("maven-publish")
    id("signing")
    id("org.hibernate.build.maven-repo-auth") version Versions.MAVEN_REPO_AUTH_PLUGIN apply false
}

if (!project.hasProperty("isGithubActions")) {
    // only use this plugin if we're running locally, not on github.
    apply<MavenRepoAuthPlugin>()
}

group = "io.newm"
version = "0.2.2-NEWM-SNAPSHOT"

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
}

tasks {
    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        dependsOn("classes")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        dependsOn("javadoc")
        from("${layout.buildDirectory}/javadoc")
    }

    artifacts {
        archives(javadocJar)
        archives(sourcesJar)
    }

    assemble {
        dependsOn("sourcesJar", "javadocJar")
    }
}

publishing {
    repositories {
        maven {
            name = "ossrh"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            if (project.hasProperty("release")) {
                setUrl(releasesRepoUrl)
            } else {
                setUrl(snapshotsRepoUrl)
            }
        }
    }
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                groupId = "io.newm"
                artifactId = "com.google.iot.cbor"

                name.set("CborTree")
                description.set(">A Java API for decoding, manipulating, and encoding CBOR data items.")
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
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenKotlin"])
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
