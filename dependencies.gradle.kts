// This file should only contain dependencies. It is used by the Github build
// workflow to cache downloaded dependencies between builds.

val platforms = mapOf(
    "dagger" to "2.28.3",
    "immutables" to "2.8.8",
    "mockito" to "3.3.3"
)

extra["dependencies"] = listOf(
    "com.google.dagger:dagger:${platforms["dagger"]}",
    "com.google.dagger:dagger-compiler:${platforms["dagger"]}",
    "javax.inject:javax.inject:1",
    "org.junit.jupiter:junit-jupiter:5.6.2",
    "one.util:streamex:0.7.3",
    "org.assertj:assertj-core:3.15.0",
    "org.freemarker:freemarker:2.3.29",
    "org.immutables:value:${platforms["immutables"]}",
    "org.immutables:value-processor:${platforms["immutables"]}",
    "org.mockito:mockito-core:${platforms["mockito"]}",
    "org.mockito:mockito-junit-jupiter:${platforms["mockito"]}"
)
