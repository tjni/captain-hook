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
    apply(from = "dependencies.gradle.kts")
    val dependencies: List<String> by project.extra
    val map = dependencies.associateBy {
        it.substringBeforeLast(':')
    }

    annotationProcessor(map.getValue("com.google.dagger:dagger-compiler"))
    annotationProcessor(map.getValue("org.immutables:value-processor"))
    implementation(map.getValue("com.google.dagger:dagger"))
    implementation(map.getValue("javax.inject:javax.inject"))
    implementation(map.getValue("one.util:streamex"))
    implementation(map.getValue("org.freemarker:freemarker"))
    implementation(map.getValue("org.immutables:value"))
    testImplementation(map.getValue("org.assertj:assertj-core"))
    testImplementation(map.getValue("org.junit.jupiter:junit-jupiter"))
    testImplementation(map.getValue("org.mockito:mockito-core"))
    testImplementation(map.getValue("org.mockito:mockito-junit-jupiter"))
    "functionalTestImplementation"(map.getValue("one.util:streamex"))
    "functionalTestImplementation"(map.getValue("org.assertj:assertj-core"))
    "functionalTestImplementation"(map.getValue("org.junit.jupiter:junit-jupiter"))
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
                username = properties["nexusUsername"] as String?
                password = properties["nexusPassword"] as String?
            }
        }
    }
}

signing {
    @Suppress("UnstableApiUsage")
    useInMemoryPgpKeys(properties["signingKey"] as String?, properties["signingPassword"] as String?)

    sign(publishing.publications["mavenJava"])
}
