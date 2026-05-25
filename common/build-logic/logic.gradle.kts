//
// © 2026-present https://github.com/cengiz-pz
//

// Compiles all convention plugins (precompiled script plugins) in
// src/main/java/*.gradle.kts and makes them - together with their
// runtime dependencies - available to every subproject that applies them.
//
// Versions are kept in sync with gradle/libs.versions.toml via the
// version catalog re-export in settings.gradle.kts:
//   kotlin-android-plugin  ->  kotlin("plugin.serialization")
//   kotlinx-serialization  ->  kotlinx-serialization-json runtime
//

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

val buildLogicDependencies =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .run {
            libraryAliases
                .filter { it.startsWith("build.logic.") }
                .map { findLibrary(it).get().get() }
        }

dependencies {
    println("DEBUG: BUILD LOGIC IMPLEMENTATION Dependencies")
    buildLogicDependencies.forEach {
        println("DEBUG: Adding to runtime: $it")
        implementation(it)
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
    }
}

kotlin {
    jvmToolchain(17)

    sourceSets {
        getByName("main") {
            kotlin.srcDir("src/main/java")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = "17"
    sourceCompatibility = "17"
}
