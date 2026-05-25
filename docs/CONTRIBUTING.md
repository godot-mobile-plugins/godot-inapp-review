![Android CI](https://github.com/godot-mobile-plugins/godot-inapp-review/actions/workflows/android-ci.yml/badge.svg)
![iOS CI](https://github.com/godot-mobile-plugins/godot-inapp-review/actions/workflows/ios-ci.yml/badge.svg)

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="28"> Contributing

Thank you for your interest in contributing to the Godot InappReview Plugin! This guide will help you understand the project structure, build processes, and development workflows.

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Table of Contents

- [Project Structure](#-project-structure)
- [Build System Architecture](#-build-system-architecture)
- [Prerequisites](#-prerequisites)
- [Configuration](#-configuration)
- [Development Workflow](#-development-workflow)
- [Building](#-building)
- [Code Formatting](#-code-formatting)
- [Testing](#-testing)
- [Creating Releases](#-creating-releases)
- [Installation](#-installation)
- [Troubleshooting](#-troubleshooting)
- [Contributing Guidelines](#-contributing-guidelines)

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Project structure

```text
.
├-- addon/                                 # GDScript interface module
│   ├-- addon-build.gradle.kts               # Gradle build configuration for addon module
│   ├-- ?.gradle.kts                         # Any extra addon-specific Gradle configuration (configured in
│   │                                        # addon/config/addon-build.properties) for the plugin goes here
│   ├-- build/
│   │   └-- output/                          # Generated GDScript code
│   │
│   ├-- config/
│   │   └-- addon-build.properties           # Gradle build customization for addon module
│   │
│   └-- src/
│       ├-- main                             # Main GDScript templates
│       └-- shared                           # GDScript templates in common with other plugins, if any
│
├-- android/                               # Android platform module
│   ├-- android-build.gradle.kts             # Android build configuration
│   ├-- ?.gradle.kts                         # Any extra Android-specific Gradle configuration (configured in
│   │                                        # android/config/android-build.properties) for the plugin goes here
│   │
│   ├-- build/
│   │   └-- outputs/                         # Generated Android AAR files
│   │
│   ├-- config/
│   │   └-- android-build.properties         # Gradle build customization for android module
│   │
│   ├-- libs/                                # Godot library for Android (default location; configurable via local.properties)
│   └-- src/
│       ├-- main                             # Android source code
│       └-- test                             # Android test code
│
├-- common/                                # Gradle root - shared build configuration
│   ├-- build.gradle.kts                     # Root build configuration
│   ├-- ?.gradle.kts                         # Any extra Gradle configuration (configured in
│   │                                        # common/config/build.properties) for the plugin goes here
│   │
│   ├-- gradle.properties                    # Gradle properties
│   ├-- local.properties                     # Local machine config (gitignored)
│   ├-- settings.gradle.kts                  # Gradle settings
│   ├-- build/
│   │   ├-- archive/                         # Generated archives
│   │   ├-- plugin/                          # Built plugin files
│   │   └-- reports/                         # Build reports
│   │
│   ├-- build-logic/                       # Convention plugin (precompiled script plugins)
│   │   ├-- build.gradle.kts
│   │   ├-- settings.gradle.kts
│   │   └-- src/main/java/
│   │       ├-- base-conventions.gradle.kts  # Core convention plugin - applied by every module
│   │       ├-- BuildConfig.kt               # Reads build.properties + per-module *-build.properties
│   │       ├-- GodotConfig.kt               # Reads godot.properties
│   │       ├-- IosConfig.kt                 # Reads ios/config/ios.properties
│   │       ├-- PluginConfig.kt              # Reads plugin.properties
│   │       ├-- ProjectExtensions.kt         # loadPluginConfig(), loadGodotConfig(), loadIosConfig(), loadBuildConfig()
│   │       └-- SpmDependency.kt             # Data class for spm_dependencies.json entries
│   │
│   ├-- config/
│   │   ├-- build.properties                 # Build-related property configuration & customization
│   │   ├-- godot.properties                 # Godot version configuration
│   │   └-- plugin.properties                # Plugin configuration
│   │
│   └-- gradle/                              # Gradle wrapper and version catalogs
│       └-- libs.versions.toml               # Dependencies and versions
│
├-- demo/                                  # Demo application
│   ├-- addons/                              # Installed plugin files
│   ├-- ios/                                 # iOS-specific demo files
│   └-- *.gd                                 # Demo app scripts
│
├-- ios/                                   # iOS platform module
│   ├-- ios-build.gradle.kts                 # iOS build configuration
│   ├-- ?.gradle.kts                         # Any extra iOS-specific Gradle configuration (configured in
│   │                                        # ios/config/ios-build.properties) for the plugin goes here
│   │
│   ├-- src/                                 # iOS platform code
│   ├-- test/                                # iOS test code
│   ├-- plugin.xcodeproj/                    # Xcode project
│   ├-- build/                               # iOS build outputs
│   │
│   ├-- config/
│   │   ├-- ios.properties                   # iOS configuration
│   │   ├-- ios-build.properties             # Gradle build customization for ios module
│   │   ├-- spm_dependencies.json            # SPM dependency configuration
│   │   └-- *.gdip                           # Godot iOS plugin config
│   │
│   └-- godot/                               # Downloaded Godot source (default location; configurable via local.properties)
│
├-- script/                                # Build and utility scripts
│   ├-- build.sh                             # Main build script
│   ├-- build_android.sh                     # Android build script
│   ├-- build_ios.sh                         # iOS build script
│   ├-- install.sh                           # Plugin installation script
│   ├-- run_gradle_task.sh                   # Gradle task runner
│   ├-- get_config_property.sh               # Configuration reader
│   └-- spm_manager.rb                       # Ruby script for managing SPM dependencies in Xcode project
│
├-- docs/                                  # Documentation
│
└-- release/                               # Final release archives
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Build System Architecture

The build system is centred on a **convention plugin** living in `common/build-logic/`. This is an [included build](https://docs.gradle.org/current/userguide/composite_builds.html) whose compiled output is available on the classpath of every module build script that declares `plugins { id("base-conventions") }`.

### Config Data Classes

All plugin, Godot, iOS, and build settings are loaded once into typed immutable data classes. Project build scripts access them through `Project` extension functions - the same call pattern used throughout the Kotlin ecosystem:

| Extension function   | Data class    | Source file                                    |
|----------------------|---------------|------------------------------------------------|
| `loadPluginConfig()` | `PluginConfig` | `common/config/plugin.properties`             |
| `loadGodotConfig()`  | `GodotConfig`  | `common/config/godot.properties`              |
| `loadIosConfig()`    | `IosConfig`    | `ios/config/ios.properties` + `ios/config/spm_dependencies.json` |
| `loadBuildConfig()`  | `BuildConfig`  | `common/config/build.properties` + all four `*-build.properties` |

Usage in any module build script:

```kotlin
plugins { id("base-conventions") }

val pluginConfig = loadPluginConfig()
val godotConfig  = loadGodotConfig()
val iosConfig    = loadIosConfig()

println(pluginConfig.pluginName)        // "*Plugin"
println(godotConfig.godotAarUrl)        // full GitHub download URL
println(iosConfig.frameworks)           // List<String> - already parsed
```

### `base-conventions` Convention Plugin

Applying `id("base-conventions")` in a module build script:

1. Loads all four config data classes.
2. Bridges every scalar config value onto `project.extra` (for compatibility with `apply(from = …)` scripts that cannot reference build-logic types directly).
3. Sets shared directory-layout extras (`pluginDir`, `repositoryRootDir`, `archiveDir`, `demoDir`).
4. Applies the per-module user-defined extra properties and extra Gradle scripts from `BuildConfig`, scoped by `project.path` - so `:android` only receives `BuildConfig.androidExtraProperties` / `androidExtraGradle`, `:ios` only receives the iOS equivalents, and so on.

### `IosConfig` List Fields

The `frameworks`, `embeddedFrameworks`, and `linkerFlags` fields on `IosConfig` are `List<String>`. The comma-separated values in `ios/config/ios.properties` are split and trimmed at load time, so consumers never need to parse delimiters:

```kotlin
val iosConfig = loadIosConfig()
iosConfig.frameworks         // ["Foundation.framework", "Network.framework"]
iosConfig.embeddedFrameworks // [] when empty
iosConfig.linkerFlags        // ["-ObjC"]
```

### `IosConfig` SPM Dependencies

`IosConfig` also exposes a `spmDependencies: List<SpmDependency>` field, decoded at load time from `ios/config/spm_dependencies.json`. Each `SpmDependency` entry carries three fields:

| Field      | Type           | Description                                      |
|------------|----------------|--------------------------------------------------|
| `url`      | `String`       | Git repository URL of the Swift package          |
| `version`  | `String`       | Minimum version requirement                      |
| `products` | `List<String>` | SPM product names to link against                |

```kotlin
val iosConfig = loadIosConfig()
iosConfig.spmDependencies   // [SpmDependency(url="https://...", version="1.2.3", products=["ProductA"])]
```

`base-conventions` bridges this list onto `project.extra["iosSpmDependencies"]` so it is accessible from any task lambda that cannot reference `IosConfig` by type directly.

The `addon-build.gradle.kts` `generateGDScript` and `generateSharedGDScript` tasks expose the list via the `@spmDependencies@` token. Each dependency is rendered as a GDScript dictionary literal using [StringName](https://docs.godotengine.org/en/stable/classes/class_stringname.html) key syntax (`&"key"`), and multiple entries are joined with `, ` — without outer brackets, because they are supplied by the surrounding GDScript constant:

```gdscript
# Template source:
const SPM_DEPENDENCIES: Array = [ @spmDependencies@ ]

# After token replacement (two dependencies):
const SPM_DEPENDENCIES: Array = [ {&"url": "https://github.com/owner/repo", &"version": "1.2.3", &"products": ["ProductA", "ProductB"]}, {&"url": "https://github.com/other/pkg", &"version": "2.0.0", &"products": ["ProductC"]} ]
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Prerequisites

### General Requirements
- **Git** - For version control
- **Bash** - For running build scripts (macOS/Linux native, Windows via WSL or Git Bash)

### Android Development
- **Java Development Kit (JDK)** - Version 17 or higher
- **Android SDK** - With the following components:
  - Android SDK Platform Tools
  - Android SDK Build Tools (version specified in gradle)
  - Android SDK Platform (API level specified in gradle)
  - Android NDK (if building native code)

Your Android SDK directory should contain:

```text
android-sdk/
├-- build-tools/
├-- cmdline-tools/
├-- licenses/
├-- ndk/
├-- platform-tools/
├-- platforms/
└-- tools/
```

- Create `local.properties` file inside `./common` directory that locates the Android SDK installation directory

Sample `local.properties` on Windows:
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

Sample `local.properties` on Unix-like command-line:
```properties
sdk.dir=/usr/lib/android-sdk
```

Optionally, set `godot.dir` to use a Godot source tree at a custom location instead of the default `ios/godot/`:
```properties
godot.dir=/path/to/your/shared/godot
```

### iOS Development (macOS only)
- **Xcode** - Latest stable version recommended
- **Xcode Command Line Tools** - Install via: `xcode-select --install`
- **Ruby** - Required for SPM dependency management via `spm_manager.rb` (macOS system Ruby is sufficient)
- **xcodeproj gem** - Installed automatically by the build system if missing, or manually via: `gem install xcodeproj --user-install`

### Developer Tools (Optional - required for format checking)

These tools are needed when running `checkFormat` or `applyFormat` tasks:

- **ktlint** - Kotlin/KTS formatter: `brew install ktlint`
- **shellcheck** - Shell script linter: `brew install shellcheck`
- **editorconfig-checker** - EditorConfig compliance: `brew install editorconfig-checker`
- **clang-format** - ObjC/C++ formatter: `brew install clang-format` (iOS only)
- **swiftlint** - Swift linter/formatter: `brew install swiftlint` (iOS only)
- **gdformat** - GDScript formatter: install via the Godot toolchain

### Verifying Prerequisites

```bash
# Check Java version
java -version

# macOS/iOS only
xcodebuild -version
ruby --version
gem list xcodeproj
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Configuration

The build files are static and shared across all GMP plugins. Any plugin-specific build customization is handled through the following configuration files:

```text
.
├-- addon/
│   ├-- ?.gradle.kts                       # Any extra addon-specific Gradle configuration (configured in
│   │                                      # addon/config/addon-build.properties) for the plugin goes here
│   └-- config/
│       └-- addon-build.properties         # Gradle build customization for addon module
│
├-- android/
│   ├-- android-build.gradle.kts           # Android build configuration
│   ├-- ?.gradle.kts                       # Any extra Android-specific Gradle configuration (configured in
│   │                                      # android/config/android-build.properties) for the plugin goes here
│   └-- config/
│       └-- android-build.properties       # Gradle build customization for android module
│
├-- common/
│   ├-- config/
│   │   ├-- build.properties               # Build-related property configuration & customization
│   │   ├-- godot.properties               # Godot version configuration
│   │   └-- plugin.properties              # Plugin configuration
│   │
│   └-- gradle/
│       └-- libs.versions.toml             # Android dependencies and versions
│
└-- ios/
    └-- config/
        ├-- ios.properties                 # iOS configuration
        ├-- ios-build.properties           # Gradle build customization for ios module
        └-- spm_dependencies.json          # SPM dependency configuration
```

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Common Configuration

The `common/config/plugin.properties` file contains core plugin settings:

```properties
# Plugin identification
pluginNodeName=...                # Name of the plugin node in Godot (e.g. MyPlugin)
pluginModuleName=...              # Snake-case module name for native symbols (e.g. my_plugin)
pluginPackage=...                 # Fully-qualified Java/Kotlin package (e.g. org.godotengine.plugin.myplugin)
pluginVersion=1.0                 # Plugin version
```

The `common/config/godot.properties` file contains core Godot version settings:

```properties
# Godot configuration
godotVersion=4.6                  # Target Godot version
godotReleaseType=stable           # Release type: stable, dev6, beta3, rc1, etc.
```

The `common/config/build.properties` file contains Gradle build-related property settings. The `gradleProjectName` key is required. Extra properties and Gradle scripts that apply only to the **root project** use a `root.` prefix:

```properties
gradleProjectName=godot-*-plugin

# Extra properties set on the root project only
root.extra.anotherProperty=property value

# Extra Gradle scripts applied to the root project only
root.gradle.another=another.gradle.kts
```

Per-module extra properties and scripts are configured in each module's own `*-build.properties` file (see [Build Customization](#-build-customization) below).

**Key Properties:**
- `pluginNodeName` - The name of the main plugin node used in Godot
- `pluginVersion` - Semantic version for releases
- `godotVersion` - Must match your target Godot version
- `godotReleaseType` - Determines which Godot binary to download

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Build Customization

Plugin-specific build customizations can be configured in the following files.

`common/config/build.properties` for root-project customizations. The `root.` prefix scopes each entry to the root project only:

```properties
# Set plugin-specific extra properties on the root project
#root.extra.myProperty=value

# Configure plugin-specific Gradle scripts for the root project
#root.gradle.extraGradle=extra.gradle.kts
```

`addon/config/addon-build.properties` for addon-module build customizations:

```properties
# Set plugin-specific extra properties for addon module
#extra.myProperty=value

# Configure plugin-specific Gradle scripts for addon module
#gradle.extraGradle=extra.gradle.kts
```

`android/config/android-build.properties` for android-module build customizations:

```properties
# Set plugin-specific extra properties for android module
#extra.myProperty=value

# Configure plugin-specific Gradle scripts for android module
#gradle.extraGradle=extra.gradle.kts
```

`ios/config/ios-build.properties` for ios-module build customizations:

```properties
# Set plugin-specific extra properties for ios module
#extra.myProperty=value

# Configure plugin-specific Gradle scripts for ios module
#gradle.extraGradle=extra.gradle.kts
```

Each `extra.*` key sets a Gradle extra property on the corresponding module's project. Each `gradle.*` key applies the named Gradle script file to that module via `project.apply(from = …)`. Extra scripts are resolved relative to the repository root.

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Local Configuration

Create `common/local.properties` to configure machine-specific paths. This file is gitignored and must be created locally.

#### Android SDK Location

```properties
# Windows
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

# macOS/Linux
sdk.dir=/Users/YourUsername/Library/Android/sdk

# Linux (alternate)
sdk.dir=/usr/lib/android-sdk
```

#### Godot Directory (iOS - optional)

By default, the iOS build scripts download and use the Godot source from `ios/godot/` inside the project. If you want to use a Godot source tree located elsewhere on your machine (e.g. to share it across multiple plugin projects), set `godot.dir` in `local.properties`:

```properties
# Use a shared Godot source directory outside the project
godot.dir=/path/to/your/shared/godot
```

When `godot.dir` is not set, the build uses the `ios/godot/` directory. The path supports `~` and environment variable expansion.

#### Godot Android Library (AAR - optional)

By default, the Godot Android AAR libary file is expected to be placed inside `android/libs/` directory inside the project. If you want to use a location elsewhere on your machine (e.g. to share it across multiple plugin projects), set `lib.dir` in `local.properties`:

```properties
# Use a shared Godot AAR library directory outside the project
lib.dir=/path/to/your/shared/aar
```

When `lib.dir` is not set, the build uses the `android/libs/` directory. The path supports `~` and environment variable expansion.

**Note:** The Godot headers directory must contain a `GODOT_VERSION` file whose content matches the `godotVersion` property in `common/config/godot.properties`. The `downloadGodotHeaders` Gradle task creates this file automatically when it downloads the headers. If the directory already exists but contains a different version, the build will fail with a clear error message - run `./script/build_ios.sh -gG` to remove the old directory and re-download the correct version.

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> iOS Configuration

The `ios/config/ios.properties` file contains iOS-specific settings:

```properties
# iOS deployment target
platform_version=14.3

# Swift language version (required - must match your Xcode project)
swift_version=5.9

# iOS system framework dependencies (comma-separated)
frameworks=Foundation.framework,...

# Embedded iOS external framework dependencies (comma-separated; may be empty)
# Use this for vendored or prebuilt xcframeworks that are NOT managed by SPM.
# SPM packages should be declared in spm_dependencies.json instead.
embedded_frameworks=res://ios/framework/*.xcframework,...

# Linker flags (comma-separated; may be empty)
flags=-ObjC,-Wl,...

# Files to include in the app bundle
bundle_files=res://assets/VisionPlugin/face_landmarker.task
```

The `frameworks`, `embedded_frameworks`, `flags`, and `bundle_files` values are comma-separated lists. The build system parses them into typed lists at configuration time (`IosConfig.kt`) - blank entries are ignored. Values are used as-is for token replacement in GDScript templates and passed directly to `xcodebuild`.

GDScript templates may reference the following tokens for iOS values set in `ios.properties` and `spm_dependencies.json`:

| Token                    | Source                        | GDScript type  |
|--------------------------|-------------------------------|----------------|
| `@iosFrameworks@`        | `frameworks` (ios.properties) | quoted strings |
| `@iosEmbeddedFrameworks@`| `embedded_frameworks`         | quoted strings |
| `@iosLinkerFlags@`       | `flags`                       | quoted strings |
| `@iosBundleFiles@`       | `bundle_files`                | quoted strings |
| `@spmDependencies@`      | `spm_dependencies.json`       | GDScript dicts |

The `@spmDependencies@` token produces GDScript dictionary literals with StringName keys and no outer brackets (see [`IosConfig` SPM Dependencies](#iosconfig-spm-dependencies) for the exact format).

SPM dependencies are configured in the `ios/config/spm_dependencies.json` file in the following format:

```json
[
  {
    "url": "https://github.com/Alamofire/Alamofire",
    "version": "5.8.1",
    "products": [
      "Alamofire",
      "AlamofireImage"
    ]
  },
  {
    "url": "https://github.com/kishikawakatsumi/KeychainAccess",
    "version": "4.2.2",
    "products": [
      "KeychainAccess"
    ]
  }
]
```

If the plugin has no SPM dependencies:

```json
[

]
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Development Workflow

### Initial Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/godot-mobile-plugins/godot-inapp-review.git
   cd godot-inapp-review
   ```

2. **Configure Android SDK:**
   ```bash
   echo "sdk.dir=/path/to/your/android-sdk" > common/local.properties
   ```

3. **First build:**
   ```bash
   # Android only
   ./script/build.sh -a -- -b

   # iOS only (macOS) - downloads Godot automatically
   ./script/build.sh -i -- -A
   ```

### Making Changes

1. **Edit source code:**
   - Android: `android/src/main/`
   - iOS: `ios/src/`
   - GDScript templates: `addon/src/`

2. **Build and test:**
   ```bash
   # Quick Android build
   ./script/build.sh -a -- -b

   # Run unit tests (Android + iOS)
   ./script/build.sh -t

   # Install to demo app
   ./script/build.sh -D

   # Run demo in Godot to test
   cd demo
   godot project.godot
   ```

3. **Iterate:**
   - Make changes
   - Rebuild with `./script/build.sh -a -- -cb` or  `./script/build.sh -i -- -cb`
   - Test in demo app
   - Repeat until tests pass

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Building

There are three main build scripts located in the `script` directory.

- `build.sh` - the main build script
- `build_android.sh` - build script for Android platform
- `build_ios.sh` - build script for iOS platform

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Cross-Platform Builds

Cross-platform builds with the `build.sh` script.

#### Build Options

| Option | Description |
|--------|-------------|
| `-a` | Build plugin for Android platform (`-a -- -h` for all options) |
| `-i` | Build plugin for iOS platform (`-i -- -h` for all options) |
| `-c` | Remove existing builds |
| `-C` | Remove existing builds and archives |
| `-d` | Uninstall plugin from demo app |
| `-D` | Install plugin to demo app |
| `-f` | Fix source code format issues |
| `-A` | Create Android release archive |
| `-I` | Create iOS release archive |
| `-M` | Create multi-platform release archive |
| `-R` | Create all release archives |
| `-v` | Verify source code format compliance |

#### Output Locations

- **GDScript code:** `addon/build/output/`
- **Debug AAR:** `android/build/outputs/aar/*-debug.aar`
- **Release AAR:** `android/build/outputs/aar/*-release.aar`
- **Built plugin:** `common/build/plugin/`
- **Release archive:** `release/InappReviewPlugin-*-v*.zip`

---

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Android Builds

#### Quick Reference

```bash
# Clean and build Android debug
./script/build.sh -a -- -cb

**Note:** Options after `--` are passed to `build_android.sh`

# Clean and build Android release
./script/build.sh -a -- -cbr

# Install Android plugin to demo app
./script/build_android.sh -D

# Uninstall Android plugin from demo app
./script/build_android.sh -d

# Create Android release archive
./script/build_android.sh -R
```

#### Build Options

| Option | Description |
|--------|-------------|
| `-b` | Build plugin for Android platform (debug build variant by default) |
| `-c` | Clean Android build |
| `-d` | Uninstall Android plugin from demo app |
| `-D` | Install Android plugin to demo app |
| `-h` | Display script usage information |
| `-r` | Build Android plugin with release build variant |
| `-R` | Create Android release archive |
| `-t` | Run Android unit tests (prints per-suite pass/fail table and code coverage) |

#### Android Studio

If using Android Studio, make sure to open the root Gradle project from the `common` directory.

---

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> iOS Builds

#### Quick Reference

```bash
# Clean and run iOS debug build
./script/build.sh -i -- -cb

**Note:** Options after `--` are passed to `build_ios.sh`

# Full build (first time - downloads Godot headers automatically)
./script/build_ios.sh -A

# Clean and rebuild (reuses existing Godot headers)
./script/build_ios.sh -ca

# Full clean rebuild (removes Godot headers directory first)
./script/build_ios.sh -cgA

# Clean, build and create archive
./script/build_ios.sh -cR

# Debug build for simulator
./script/build_ios.sh -bs

# Release build for simulator
./script/build_ios.sh -Bs

# Install iOS plugin to demo app
./script/build_ios.sh -D

# Uninstall iOS plugin from demo app
./script/build_ios.sh -d

# Resolve SPM dependencies only
./script/build_ios.sh -r
```

#### Build Options

| Option | Description |
|--------|-------------|
| `-a` | Update SPM packages and build both debug and release variants |
| `-A` | Download Godot headers, update SPM packages, and build both debug and release variants |
| `-b` | Run debug build (device); combine with `-s` for simulator |
| `-B` | Run release build (device); combine with `-s` for simulator |
| `-c` | Clean existing build |
| `-d` | Uninstall iOS plugin from demo app |
| `-D` | Install iOS plugin to demo app |
| `-g` | Remove Godot headers directory |
| `-G` | Download Godot headers |
| `-h` | Display help |
| `-p` | Remove SPM packages and build artifacts |
| `-P` | Add SPM packages from configuration |
| `-r` | Resolve SPM dependencies |
| `-R` | Create release archive |
| `-s` | Simulator build; use with `-b` for simulator debug, `-B` for simulator release |
| `-t` | Run iOS unit tests (requires macOS and Xcode; Godot headers must be present) |

#### Build Process Explained

The iOS build process involves several steps that are orchestrated automatically:

1. **Download Godot Headers** (if needed):
   - Downloads a pre-built Godot headers archive from `github.com/godot-mobile-plugins/godot-headers`
   - Version is determined by `godotVersion` and `godotReleaseType` in `godot.properties`
   - Extracted to `ios/godot/` by default, or to the path set by `godot.dir` in `common/local.properties`
   - The download is skipped if the correct version is already present (checked via a `GODOT_VERSION` file)
   - If the directory exists but contains a different version, the build fails with a clear error - run `./script/build_ios.sh -gG` to switch versions

2. **Validate Swift Version**:
   - Reads `swift_version` from `ios/config/ios.properties`
   - Fails early with a clear error if the property is missing or blank
   - Syncs the version into `plugin.xcodeproj/project.pbxproj` automatically

3. **Validate Godot Version**:
   - Confirms the `GODOT_VERSION` file in the Godot headers directory matches `godotVersion` in `godot.properties`

4. **Update & Resolve SPM Packages**:
   - Reads dependency definitions from `ios/config/spm_dependencies.json`
   - Injects package references into the Xcode project via `script/spm_manager.rb` (requires Ruby and the `xcodeproj` gem)
   - Resolves the packages with `xcodebuild -resolvePackageDependencies`

5. **Build XCFrameworks**:
   - Builds up to four variants via `xcodebuild archive`:
     - `buildiOSDebug` - device (arm64), debug
     - `buildiOSRelease` - device (arm64), release
     - `buildiOSDebugSimulator` - simulator (arm64/x86_64), debug
     - `buildiOSReleaseSimulator` - simulator (arm64/x86_64), release
   - The `-s` flag selects simulator variants; without it, device variants are built
   - Archives are created as `.xcarchive` bundles under `ios/build/lib/`
   - XCFrameworks combining device and simulator slices are assembled in `ios/build/framework/`
   - **Only the plugin's own xcframeworks** (`PluginName.debug.xcframework`, `PluginName.release.xcframework`) are copied into the plugin directory and included in release archives
   - SPM dependency xcframeworks produced in `ios/build/DerivedData/` are **not** bundled in the archive; they are resolved by Xcode at Godot iOS export time using the `Package.resolved` file that is committed alongside the Xcode project

#### Output Locations

- **Godot headers:** `ios/godot/` (default) or path set by `godot.dir` in `common/local.properties`
- **Build artifacts:** `ios/build/`
- **xcarchives:** `ios/build/lib/ios_debug.xcarchive`, `ios_release.xcarchive`, `sim_debug.xcarchive`, `sim_release.xcarchive`
- **Plugin XCFrameworks:** `ios/build/framework/InappReviewPlugin.debug.xcframework`, `InappReviewPlugin.release.xcframework`
- **Release archive:** `release/InappReviewPlugin-iOS-v*.zip`

> **Note:** Release archives (iOS and Multi) contain only the plugin's own xcframeworks. SPM dependency xcframeworks are intentionally excluded — they are fetched and linked by Xcode at Godot iOS export time using the `Package.resolved` committed with the Xcode project.

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Code Formatting

The project enforces consistent formatting across all source languages. Two aggregate tasks are available via the main build script:

```bash
# Verify all source code format compliance
./script/build.sh -v

# Fix all source code format issues
./script/build.sh -f
```

These delegate to the following per-language Gradle sub-tasks:

| Check task | Fix task | Language | Tool | Module |
|------------|----------|----------|------|--------|
| `checkGdscriptFormat` | `formatGdscriptSource` | GDScript | gdformat | addon |
| `checkJavaFormat` | `rewriteRun` | Java | Checkstyle / OpenRewrite | android |
| `checkXmlFormat` | `formatXml` | XML | Prettier | android |
| `checkObjCFormat` | `formatObjCSource` | ObjC / C++ | clang-format | ios |
| `checkSwiftFormat` | `formatSwiftSource` | Swift | swiftlint | ios |
| `checkKtsFormat` | `formatKtsSource` | Gradle KTS | ktlint | common |
| `checkBashScriptFormat` | `applyBashScriptFormat` | Bash | shellcheck | common |
| `checkEditorConfig` | _(n/a)_ | All files | editorconfig-checker | common |

Sub-tasks can also be run individually. For example, to check only GDScript formatting:

```bash
cd common
./gradlew :addon:checkGdscriptFormat
```

Sub-tasks that require external tools (`ktlint`, `shellcheck`, `editorconfig-checker`, `clang-format`, `swiftlint`, `gdformat`) will fail with a clear error if the tool is not found on `PATH`. See [Prerequisites](#-prerequisites) for installation instructions.

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Testing

### Running All Tests

Run unit tests for both platforms with a single command from the repository root:

```bash
./script/build.sh -t
```

This delegates to `:android:printTestSummary` and `:ios:testiOS` in sequence. The command exits non-zero if any test on either platform fails.

In order to clean test results and rerun tests, use the following options:

```bash
./script/build.sh -ct
```

---

### Android Unit Tests

Android tests are written with **JUnit 5** and **Mockito** and live under `android/src/test/`.

#### Running Android tests

```bash
# Run all Android tests, print per-suite summary, and show coverage
./script/build_android.sh -t

# Or via Gradle directly
cd common
./gradlew :android:printTestSummary
```

#### What the pipeline does

The `printTestSummary` Gradle task orchestrates three steps automatically:

| Step | Gradle Task | Description |
|------|-------------|-------------|
| 1 | `testDebugUnitTest` | Runs all JUnit 5 tests. `ignoreFailures=true` is set so coverage always runs even if tests fail. |
| 2 | `createDebugUnitTestCoverageReport` | Generates the JaCoCo XML and HTML coverage report. |
| 3 | `printTestSummary` | Prints a per-suite pass/fail table and overall coverage summary to the console, then exits non-zero if any tests failed. |

#### Test dependencies

Test library versions are managed in `common/gradle/libs.versions.toml`. The version catalog uses a naming convention to assign dependencies to the correct configurations automatically:

| Alias prefix | Gradle configuration |
|---|---|
| `test-` | `testImplementation` |
| `test-runtime-` | `testRuntimeOnly` |

Current test dependencies:

| Library | Version | Purpose |
|---------|---------|---------|
| `org.junit.jupiter:junit-jupiter` | 6.0.3 | JUnit 6 test runner |
| `org.mockito:mockito-core` | 5.23.0 | Mocking framework |
| `org.mockito:mockito-junit-jupiter` | 5.23.0 | JUnit Mockito integration |
| `org.junit.platform:junit-platform-launcher` | 6.0.3 | Required test runtime launcher |

#### Output

- **HTML coverage report:** `android/build/reports/coverage/test/debug/index.html`
- **XML coverage report:** `android/build/reports/coverage/test/debug/report.xml`
- **Test results XML:** `android/build/test-results/testDebugUnitTest/`

#### Writing new Android tests

Place new test files under `android/src/test/java/` (or `kotlin/`) mirroring your main source package. JUnit 5 and Mockito are available without any additional configuration.

---

### iOS Unit Tests

iOS tests are written with **XCTest** and live under `ios/test/unit/`. There are two test files:

| File | Language | Coverage |
|------|----------|---------|
| `InappReviewTests.swift` | Swift | `InappReview` class — init, `isActiveKey`, `onThisHappened` callback, ObjC bridging, concurrency |
| `InappReviewLoggerTests.mm` | Objective-C++ | `inapp_review_log` global — initialization, all log levels, format string handling |

The tests are compiled into the `inapp_review_plugin_tests` Xcode test bundle target, which declares a dependency on the main `inapp_review_plugin` static library target. The Xcode scheme used by the build system is `inapp_review_plugin_tests`.

#### Running iOS tests

```bash
# Run all iOS tests and print a pass/fail summary
./script/build_ios.sh -t

# Or via Gradle directly
cd common
./gradlew :ios:testiOS
```

> **Note:** iOS tests require macOS and Xcode. The Godot headers must already be present in `ios/godot/` (or the path configured by `godot.dir`). If they are missing, run `./script/build_ios.sh -G` first.

#### What the pipeline does

The `testiOS` Gradle task runs:

```
xcodebuild test
  -project ios/plugin.xcodeproj
  -scheme inapp_review_plugin_tests
  -destination 'platform=iOS Simulator,...'
```

The task exits non-zero if any test fails and prints `xcodebuild`'s test output to the console.

#### Xcode project wiring

The test target is wired into the Xcode project as follows:

| Concern | Detail |
|---------|--------|
| Target name | `inapp_review_plugin_tests` |
| Product type | `com.apple.product-type.bundle.unit-test` |
| Scheme file | `ios/plugin.xcodeproj/xcshareddata/xcschemes/inapp_review_plugin_tests.xcscheme` |
| Linked library | `libinapp_review_plugin.a` (the main plugin target) |
| Target dependency | `inapp_review_plugin` (built before tests run) |
| Header search paths | `$(SRCROOT)/src`, `$(CONFIGURATION_TEMP_DIR)/inapp_review_plugin.build/DerivedSources` |

#### Writing new iOS tests

Swift tests: add a new `XCTestCase` subclass to `ios/test/unit/` and register the file in the `inapp_review_plugin_tests` target's Sources build phase in `project.pbxproj`.

Objective-C++ tests: add a `.mm` file to `ios/test/unit/`. Import `inapp_review_logger.h` for logger access or `inapp_review_plugin.h` for the plugin class. Add the file to the Sources build phase the same way.

> **Note on Godot engine classes:** `InappReviewPlugin` (the `Object` subclass), `inapp_review_plugin_init()`, and `inapp_review_plugin_deinit()` all depend on `memnew`/`memdelete` and `Engine::get_singleton()`, which are Godot engine internals unavailable in a plain XCTest host. Test those paths using GDScript integration tests via the Godot editor or the [GUT](https://github.com/bitwes/Gut) framework instead.

---

### Testing in the Demo App

Manual end-to-end testing via the demo Godot project:

1. **Install plugin to demo:**
   ```bash
   ./script/build.sh -D
   ```

2. **Open demo project:**
   ```bash
   cd demo
   godot project.godot
   ```

3. **Android — install and inspect logs:**

Run the demo project on an Android device or simulator directly from Godot editor or export to .apk and...

   ```bash
   # Export Android build from Godot editor, then:
   adb install demo/export/android/demo.apk
   adb logcat | grep -i InappReview
   ```

4. **iOS — run from Xcode:**

Run the demo project on an iOS device or simulator directly from Godot editor or...

- export Xcode project
- open exported project with Xcode
- run on selected device or simulator

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Creating Releases

### Full Multi-Platform Release

```bash
# Create all release archives
./script/build.sh -R
```

This creates:
- `release/InappReviewPlugin-Android-v*.zip`
- `release/InappReviewPlugin-iOS-v*.zip`
- `release/InappReviewPlugin-Multi-v*.zip` (combined)

### Platform-Specific Releases

```bash
# Create all release archives
./script/build.sh -R

# Create only Android release archive
./script/build.sh -A

# Create only iOS release archive
./script/build.sh -I

# Create only multi-platform release archive
./script/build.sh -M
```

### Release Checklist

- [ ] Update version in `common/config/plugin.properties` (`pluginVersion`)
- [ ] Update versions in issue templates (`.github/ISSUE_TEMPLATE`)
- [ ] Run unit tests on both platforms (`./script/build.sh -t`)
- [ ] Test on both platforms (manual demo app verification)
- [ ] Build release archives
- [ ] Create GitHub release
- [ ] Upload archives to release & publish
- [ ] Close GitHub milestone
- [ ] Post GitHub announcement
- [ ] Update Asset Library listing
- [ ] Update Asset Store listing

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> GitHub Actions Workflows

This document describes each GitHub Actions workflow in `.github/workflows/`, covering what it does, when it runs, and which tools it relies on.

---

### Android Build (`android-build.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `common/config/**`
- `common/*.gradle.kts`
- `android/src/**`
- `android/*.gradle.kts`

**Runner:** `ubuntu-22.04`

**Purpose:**

Check that the Android plugin build completes successfully after each PR submission.

**What it does:**

Builds a debug Android AAR by invoking the `buildAndroidDebug` Gradle task from the `common` project directory. Before running the build, the workflow sets up the full toolchain: Node.js 22 (via `actions/setup-node`, or a manual tarball download when run locally with [act](https://github.com/nektos/act)), JDK 17 (Temurin distribution), and the Android SDK.

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install Node (act) | Manual Node.js 22 install for local `act` runs |
| Setup Node (GitHub) | Standard Node.js 22 setup for GitHub-hosted runners |
| Set up JDK | Installs Temurin JDK 17 |
| Setup Android SDK | Installs the Android SDK via `android-actions/setup-android` |
| Build Android debug | Runs `./gradlew buildAndroidDebug` from `common/` |

---

### Android Code Style (`android-style.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `android/src/**/*.java`
- `android/src/**/*.kt`
- `android/src/**/*.xml`

**Runner:** `ubuntu-22.04`

**What it does:**

Enforces code style on all Android source files. Four independent checks are run in sequence:

1. **ktlint** — validates Kotlin source files against the project's ktlint rules.
2. **Checkstyle** — validates Java source files using the project's Checkstyle configuration at `.github/config/checkstyle.xml`. Checkstyle is downloaded directly from its GitHub releases at version 13.3.0.
3. **Prettier + XML plugin** — validates Android XML resource files using Prettier with the `@prettier/plugin-xml` plugin, configured by `.github/config/prettier.xml.json`.
4. **editorconfig-checker** — verifies that all `.java`, `.kt`, and `.xml` files comply with the repository's EditorConfig rules (final newlines, no trailing whitespace).

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Set up Java | Installs Temurin JDK 21 |
| Install ktlint | Uses the shared `.github/actions/install-ktlint` composite action |
| Run ktlint | Lints `android/src/**/*.kt` |
| Run Checkstyle | Downloads and runs Checkstyle 13.3.0 against `android/src` |
| Install Node | Uses the shared `.github/actions/install-node` composite action |
| Install Prettier and XML plugin | Runs `npm install --save-dev prettier @prettier/plugin-xml` |
| Check XML formatting | Runs `npx prettier --check` on `android/src/**/*.xml` |
| Install editorconfig-checker | Uses the shared `.github/actions/install-ec` composite action |
| Verify EditorConfig | Runs `editorconfig-checker` on all `.java`, `.kt`, and `.xml` files under `android/src` |

---

### Documentation (`docs.yml`)

**Trigger:** Pushes to `main` that touch any of the following paths:

- `docs/content/**`
- `.github/workflows/docs.yml`
- `docs/zensical.toml`

**Runner:** `ubuntu-22.04`

**Permissions:** `contents: read`, `pages: write`, `id-token: write`

**Concurrency:** Only one deployment runs at a time (`group: pages`); any in-progress run is cancelled when a new one starts.

**What it does:**

Builds and deploys the project's documentation site to GitHub Pages using the [Zensical](https://pypi.org/project/zensical/) static site generator. The built site is written to `docs/site/`, uploaded as a Pages artifact, and then deployed.

**Steps summary:**

| Step | Description |
|------|-------------|
| Configure Pages | Configures the GitHub Pages environment |
| Checkout | Checks out the repository |
| Setup Python | Installs Python 3.x |
| Install Dependencies | Upgrades pip and installs `zensical` |
| Build Documentation | Runs `zensical build --clean --config-file docs/zensical.toml` |
| Upload Pages | Uploads `docs/site` as a Pages artifact |
| Deploy Pages | Deploys the uploaded artifact to GitHub Pages |

---

### GDScript Code Style (`gdscript-style.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `addon/src/**/*.gd`
- `demo/**/*.gd`

**Runner:** `ubuntu-22.04`

**What it does:**

Validates GDScript formatting across the addon and demo directories using two tools:

1. **gdformat** (from [gdtoolkit](https://github.com/Scony/godot-gdscript-toolkit)) — checks formatting compliance with `--check` (dry run). The `.gdformatrc` configuration file is copied from `.github/config/` to the working directory before checking. Certain auto-generated files are excluded: `AdmobPlugin.gd` and `MediationNetwork.gd` in the addon, and `Main.gd` in the demo. Files under `demo/addons/` are also excluded.
2. **editorconfig-checker** — verifies final newlines and no trailing whitespace on all `.gd` files (same exclusion for `demo/addons/`).

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install gdtoolkit | Creates a Python venv at `/opt/gdtoolkit`, installs `gdtoolkit`, adds it to `$GITHUB_PATH` |
| Verify Formatting | Copies `.gdformatrc`, runs `gdformat --check` on addon and demo `.gd` files |
| Install editorconfig-checker | Uses the shared `.github/actions/install-ec` composite action |
| Verify EditorConfig | Runs `editorconfig-checker` on all `.gd` files in addon and demo |

---

### Gradle Kotlin DSL Style (`gradle-kts-style.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `addon/*.gradle.kts`
- `android/*.gradle.kts`
- `common/*.gradle.kts`

**Runner:** `ubuntu-22.04`

**What it does:**

Validates formatting of all Gradle Kotlin DSL build files using two tools:

1. **ktlint 1.8.0** — checks all `*.gradle.kts` files at the root of `addon/`, `android/`, and `common/`.
2. **editorconfig-checker 3.6.1** — verifies final newlines and no trailing whitespace on the same set of files.

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install Node | Uses the shared `.github/actions/install-node` composite action |
| Set up JDK | Installs Temurin JDK 17 |
| Install ktlint | Uses the shared `.github/actions/install-ktlint` composite action with version `1.8.0` |
| Run ktlint | Runs `ktlint` on all `*.gradle.kts` files in `addon/`, `android/`, and `common/` |
| Install editorconfig-checker | Uses the shared `.github/actions/install-ec` composite action with version `3.6.1` |
| Verify EditorConfig | Runs `editorconfig-checker` on the same set of files |

---

### iOS Build (`ios-build.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `common/config/**`
- `common/*.gradle.kts`
- `ios/src/**`
- `ios/config/**`
- `ios/*.gradle.kts`
- `ios/*.xcodeproj/**`

**Runner:** `macos-latest`

**Purpose:**

Check that the iOS plugin build completes successfully after each PR submission.

**What it does:**

Builds a debug iOS xcframework. The workflow first downloads the Godot engine sources and generates the Godot header files (both via dedicated Gradle tasks that delegate to `script/build_ios.sh`), then runs the full `buildiOSDebug` Gradle task. This task depends on GDScript generation, iOS config generation, SPM dependency management, and the Xcode build itself.

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install Node | Uses the shared `.github/actions/install-node` composite action |
| Set up JDK | Installs Temurin JDK 17 |
| Download Godot | Runs `./gradlew downloadGodot` from `common/` |
| Generate Godot Headers | Runs `./gradlew generateGodotHeaders` from `common/` |
| Build iOS debug | Runs `./gradlew buildiOSDebug` from `common/` |

---

### iOS Code Style (`ios-style.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `ios/src/**/*.m`
- `ios/src/**/*.mm`
- `ios/src/**/*.h`
- `ios/src/**/*.swift`

**Runner:** `macos-latest`

**What it does:**

Validates code style on all iOS source files. Three independent checks are run:

1. **SwiftLint** — lints Swift files under `ios/src/` using the configuration at `.github/config/.swiftlint.yml`. Output is formatted for GitHub Actions annotations.
2. **clang-format** — performs a dry-run check (`--dry-run --Werror`) on all Objective-C and Objective-C++ files (`.m`, `.mm`, `.h`) using the style rules in `.github/config/.clang-format`.
3. **editorconfig-checker** — verifies final newlines and no trailing whitespace on all `.m`, `.mm`, `.h`, and `.swift` files (installed via Homebrew).

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install SwiftLint | Runs `brew install swiftlint` |
| Install clang-format | Runs `brew install clang-format` |
| Run SwiftLint | Runs `swiftlint lint` on all `*.swift` files under `ios/src` |
| Run clang-format check | Runs `clang-format --dry-run --Werror` on all `.m`, `.mm`, `.h` files under `ios/src` |
| Install editorconfig-checker | Runs `brew install editorconfig-checker` |
| Verify EditorConfig | Runs `editorconfig-checker` on all iOS source files |

---

### Properties File Style (`properties-style.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `common/config/*.properties`
- `ios/config/*.properties`

**Runner:** `ubuntu-22.04`

**What it does:**

Validates that all `.properties` configuration files comply with the repository's EditorConfig rules (final newlines and no trailing whitespace) using **editorconfig-checker**.

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install editorconfig-checker | Uses the shared `.github/actions/install-ec` composite action |
| Validate properties formatting | Runs `editorconfig-checker` on `common/config/*.properties` and `ios/config/*.properties` |

---

### Script Code Style (`script-style.yml`)

**Trigger:** Pull requests targeting `main` that touch any of the following paths:

- `script/**/*.sh`
- `script/**/*.rb`

**Runner:** `ubuntu-22.04`

**What it does:**

Validates coding style for all shell and Ruby scripts under `script/`. Three tools are run in sequence:

1. **ShellCheck** (v0.10.0) — static analysis for shell scripts (`*.sh`). Downloaded as a pre-built binary.
2. **RuboCop** (1.85.0) — lints Ruby scripts using the configuration at `.github/config/.rubocop.yml`.
3. **editorconfig-checker** — verifies final newlines and no trailing whitespace on all `.sh` and `.rb` files.

**Steps summary:**

| Step | Description |
|------|-------------|
| Checkout | Checks out the repository |
| Install Node | Uses the shared `.github/actions/install-node` composite action |
| Install ShellCheck | Downloads ShellCheck v0.10.0 binary and adds it to `$GITHUB_PATH` |
| Run ShellCheck | Runs `shellcheck` on all `*.sh` files under `script/` |
| Set up Ruby | Installs Ruby 3.2 via `ruby/setup-ruby` with bundler cache |
| Install RuboCop | Runs `gem install rubocop -v 1.85.0` |
| Run RuboCop | Runs `rubocop --config .github/config/.rubocop.yml script` |
| Install editorconfig-checker | Uses the shared `.github/actions/install-ec` composite action |
| Verify EditorConfig | Runs `editorconfig-checker` on all `.sh` and `.rb` files under `script/` |

---

### Shared Composite Actions

Several workflows reference local composite actions under `.github/actions/` rather than repeating installation steps inline. The following shared actions are used across multiple workflows:

| Action | Used by |
|--------|---------|
| `install-node` | `android-style`, `gradle-kts-style`, `ios-build`, `script-style` |
| `install-ktlint` | `android-style`, `gradle-kts-style` |
| `install-ec` | `android-style`, `gdscript-style`, `gradle-kts-style`, `properties-style`, `script-style` |

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Installation

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Installing to Demo App

```bash
# Install both platforms
./script/build.sh -D

# Uninstall
./script/build.sh -d
```

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="20"> Installing to Your Project

```bash
# Using install script
./script/install.sh -t /path/to/your/project -z /path/to/InappReviewPlugin-*.zip

# Example
./script/install.sh -t ~/MyGame -z release/InappReviewPlugin-Multi-v6.0.zip
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Troubleshooting

### Common Build Issues

#### Android

**Problem:** Gradle version mismatch
```bash
# Solution: Use Gradle wrapper
cd common
./gradlew --version
./gradlew clean build
```

**Problem:** Dependency resolution failures
```bash
# Solution: Clear Gradle cache
rm -rf ~/.gradle/caches/
./gradlew clean build --refresh-dependencies
```

#### iOS

**Problem:** SPM package resolution fails
```bash
# Solution: Clear SPM cache and re-resolve
./script/build_ios.sh -pP
```

**Problem:** Xcode build fails
```bash
# Solution: Clean derived data and rebuild
rm -rf ios/build/DerivedData
./script/build_ios.sh -cb
```

**Problem:** Godot version mismatch (headers directory contains the wrong version)
```bash
# The GODOT_VERSION file in the headers directory must match
# the godotVersion property in common/config/godot.properties.
# Solution: remove the existing headers directory and re-download
./script/build_ios.sh -gG
```

**Problem:** Build cannot find Godot headers after setting a custom `godot.dir`
```bash
# Verify the path is set correctly in common/local.properties:
#   godot.dir=/your/custom/path
# Then download the headers into that directory:
./script/build_ios.sh -G
```

**Problem:** Build fails with "swift_version not configured"
```bash
# Solution: add swift_version to ios/config/ios.properties, e.g.:
#   swift_version=5.9
# Then retry the build.
```

**Problem:** "No such module" errors
```bash
# Solution: Ensure packages are added and resolved
./script/build_ios.sh -pP
```

**Problem:** `xcodeproj` gem missing (Ruby gem required for SPM management)
```bash
# Solution: install the gem manually
gem install xcodeproj --user-install
# The build system will also install it automatically if Ruby is available.
```

#### Tests

**Problem:** Android tests fail with `ClassNotFoundException` for JUnit 5
```bash
# Solution: ensure the test runtime launcher is on the classpath.
# Check that common/gradle/libs.versions.toml includes:
#   test-runtime-junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher", ... }
# and that android-build.gradle.kts adds it as testRuntimeOnly.
```

**Problem:** Android coverage report is empty after test run
```bash
# Solution: testDebugUnitTest must run before createDebugUnitTestCoverageReport.
# Use printTestSummary (which sequences them correctly) rather than running
# createDebugUnitTestCoverageReport in isolation.
```

**Problem:** iOS tests fail with "scheme not found" error
```bash
# The scheme file must exist at:
#   ios/plugin.xcodeproj/xcshareddata/xcschemes/inapp_review_plugin_tests.xcscheme
# and the test target must be listed in the targets array of project.pbxproj.
# Verify both are present:
find ios/plugin.xcodeproj -name "inapp_review_plugin_tests.xcscheme"
grep "inapp_review_plugin_tests" ios/plugin.xcodeproj/project.pbxproj
```

**Problem:** iOS tests fail with "module not found" or linker errors
```bash
# The test target links against libinapp_review_plugin.a.
# Ensure the main target was built before running tests:
./script/build_ios.sh -b   # build first
./script/build_ios.sh -t   # then test
```

**Problem:** iOS tests fail with Godot engine symbol errors (memnew, Engine::get_singleton)
```bash
# Tests that exercise InappReviewPlugin (the Godot Object subclass) or the
# bootstrap init/deinit functions require the Godot engine, which is not
# available in the XCTest host. Move those tests to GDScript integration tests.
```

### Getting Help

- Check existing [GitHub Issues](https://github.com/godot-mobile-plugins/godot-inapp-review/issues)
- Check exısting [GitHub Discussions](https://github.com/godot-mobile-plugins/godot-inapp-review/discussions)
- Review [Godot documentation](https://docs.godotengine.org/)

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Contributing Guidelines

### Code Style

- **GDScript:** Follow [GDScript style guide](https://docs.godotengine.org/en/stable/tutorials/scripting/gdscript/gdscript_styleguide.html)
- **Java:** Follow [Google Java style guide](https://google.github.io/styleguide/javaguide.html)
- **Kotlin:** Follow [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- **Objective-C:** Follow [Google Objective-C style guide](https://google.github.io/styleguide/objcguide.html)
- **Swift:** Follow [Swift style guide](https://www.swift.org/documentation/api-design-guidelines/)
- **Ruby:** Follow [Ruby style guide](https://rubystyle.guide/) (used in `script/spm_manager.rb`)
- **Shell:** Follow [Google Shell style guide](https://google.github.io/styleguide/shellguide.html); all scripts are checked with `shellcheck`

### Commit Messages

Use conventional commits format:

```
type(scope): subject

body

footer
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
```
feat(android): add support for native ads
fix(ios): resolve banner positioning issue
docs: update installation instructions
```

### Pull Request Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test on both platforms
5. Commit with descriptive messages
6. Push to your fork
7. Open a Pull Request with:
   - Clear description of changes
   - Related issue numbers
   - Testing performed
   - Screenshots (if UI changes)

### Reporting Issues

Include:
- Plugin version
- Godot version
- Platform (Android/iOS)
- Device/OS version
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/main/icon.png" width="24"> Additional Resources

- [Godot Engine Documentation](https://docs.godotengine.org/)
- [Android Developer Documentation](https://developer.android.com/)
- [iOS Developer Documentation](https://developer.apple.com/documentation/)
- [Gradle Documentation](https://docs.gradle.org/)

---
