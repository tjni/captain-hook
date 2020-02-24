plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    kotlin("jvm") version "1.3.61"
    id("com.diffplug.gradle.spotless") version "3.27.0"
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
    val main = sourceSets.getByName("main")
    val compileClasspathConfiguration = configurations[compileClasspathConfigurationName]
    val runtimeClasspathConfiguration = configurations[runtimeClasspathConfigurationName]
    compileClasspath = main.output + compileClasspathConfiguration
    runtimeClasspath = output + main.output + runtimeClasspathConfiguration
}

gradlePlugin {
    isAutomatedPublishing = false
    plugins.create("captain-hook") {
        id = "com.github.tjni.captainhook"
        implementationClass = "com.github.tjni.captainhook.CaptainHookPlugin"
    }

    testSourceSets(functionalTestSourceSet)
}

repositories {
    mavenLocal()
    jcenter()
}

configurations {
    getByName("functionalTestImplementation").extendsFrom(configurations.testImplementation.get())
    getByName("functionalTestRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    annotationProcessor("com.google.dagger:dagger-compiler:2.25.3")
    annotationProcessor("org.immutables:value-processor:2.8.2")
    implementation("com.google.dagger:dagger:2.25.3")
    implementation("javax.inject:javax.inject:1")
    implementation("one.util:streamex:0.7.2")
    implementation("org.freemarker:freemarker:2.3.29")
    implementation("org.immutables:value:2.8.2")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("org.mockito:mockito-core:3.2.4")
    testImplementation("org.mockito:mockito-junit-jupiter:3.2.4")
    "functionalTestImplementation"("one.util:streamex:0.7.2")
    "functionalTestImplementation"("org.assertj:assertj-core:3.15.0")
    "functionalTestImplementation"("org.junit.jupiter:junit-jupiter:5.6.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    @Suppress("UnstableApiUsage")
    withJavadocJar()

    @Suppress("UnstableApiUsage")
    withSourcesJar()
}

tasks.withType(Javadoc::class) {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

val functionalTest by tasks.registering(Test::class) {
    description = "Runs the functional tests."
    group = "verification"

    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath

    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(functionalTest)
}

tasks.withType(Test::class) {
    useJUnitPlatform()
    systemProperty("java.io.tmpdir", temporaryDir)
}

spotless {
    java {
        googleJavaFormat("1.7")
    }
}

group = "com.github.tjni.captainhook"
version = "0.1.0"

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Captain Hook")
                description.set("Gradle plugin for installing Git hooks.")
                url.set("https://github.com/tjni/captain-hook")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        name.set("Theodore Ni")
                        email.set("zyloch@gmail.com")
                        url.set("https://github.com/tjni")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/tjni/captain-hook.git")
                    developerConnection.set("scm:git:ssh://github.com/tjni/captain-hook.git")
                    url.set("https://github.com/tjni/captain-hook");
                }
            }
        }
    }

    repositories {
        maven {
            url = when (version.toString().endsWith("SNAPSHOT")) {
                true -> uri("https://oss.sonatype.org/content/repositories/snapshots/")
                false -> uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            }

            credentials {
                username = properties["nexusUsername"] as String
                password = properties["nexusPassword"] as String
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
