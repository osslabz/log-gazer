# Build Configuration

This document explains the Maven build configuration for log-gazer, detailing the plugins used and how they interact during the build lifecycle.

## Prerequisites

- **JDK 25** (Temurin or Liberica NIK for native builds)
- **Maven 3.9+**

## Build Extensions

### os-maven-plugin
**Version**: 1.7.1
**Purpose**: Detects the operating system and architecture at build time, setting the `os.detected.classifier` property.
**Usage**: The detected classifier is used by platform-specific assembly profiles to name distribution packages.

## Core Build Plugins

The following plugins are configured in the main build section and run during standard Maven lifecycle phases.

### maven-compiler-plugin
**Version**: 3.13.0
**Lifecycle Phase**: `compile`
**Purpose**: Compiles Java source code to bytecode using Java 25.
**Configuration**: Uses the `maven.compiler.release` property set to Java 25.

### maven-jar-plugin
**Version**: 3.4.2
**Lifecycle Phase**: `package`
**Purpose**: Creates the project JAR file and configures its manifest.
**Key Configuration**:
- Sets `Main-Class` to `net.osslabz.loggazer.LogGazerApp`
- Adds default implementation entries (version, vendor, etc.)

### maven-shade-plugin
**Version**: 3.6.0
**Lifecycle Phase**: `package`
**Purpose**: Creates an uber/fat JAR containing all dependencies bundled together.
**Key Configuration**:
- Transforms the manifest to include the main class
- Produces a single executable JAR with all dependencies

### javafx-maven-plugin
**Version**: 0.0.8
**Purpose**: Provides JavaFX-specific build and run capabilities.
**Configuration**: Configured with the main class for running the JavaFX application.

## Build Flow and Plugin Interactions

The plugins interact during the Maven build lifecycle in the following sequence:

```
1. COMPILE PHASE
   └─> maven-compiler-plugin
       └─> Compiles Java sources (Java 25)

2. PACKAGE PHASE
   ├─> maven-jar-plugin
   │   └─> Creates JAR with manifest (Main-Class, Implementation entries)
   │
   └─> maven-shade-plugin
       └─> Creates uber JAR with all dependencies bundled
           └─> References Main-Class from manifest

3. NATIVE IMAGE (native-graalvm-default-liberica-nik profile)
   └─> native-maven-plugin (GraalVM)
       └─> Compiles uber JAR to native binary during package phase

4. PLATFORM PACKAGING (auto-activated by native profile)
   └─> os-maven-plugin (extension)
       └─> Detects OS/architecture → sets classifier
           └─> maven-assembly-plugin
               └─> Creates platform-specific archive
                   └─> Packages native binary
```

## Profiles

### osslabz-release

#### maven-release-plugin
**Version**: 3.1.1
**Purpose**: Manages the release process with conventional commits versioning.
**Key Features**:
- Uses `ConventionalCommitsVersionPolicy` for semantic versioning
- Automatically determines version bumps from commit messages
- Tags releases with version numbers
- Executes deployment on release

**Dependencies**:
- `conventional-commits-version-policy`: 1.0.7

### native-graalvm-default-liberica-nik

**Purpose**: Builds a GraalVM native image of the application using Liberica NIK.

#### native-maven-plugin
**Version**: 0.11.3
**Lifecycle Phase**: `package`
**Purpose**: Compiles the application to a native executable using GraalVM's ahead-of-time compilation.
**Key Configuration**:
- Uses `compile-no-fork` goal
- Fallback mode disabled (fails if native compilation fails)
- Sets `-Djava.awt.headless=false` for JavaFX support

Activating this profile also sets the `create-os-specific-archive` property, which auto-activates one of the platform-specific archive profiles below.

### Platform-specific Archive Profiles

These profiles are **not activated manually** — they are auto-activated when the `create-os-specific-archive` property is set (by the native profile) combined with OS detection.

#### os-specific-archive-windows
**Activation**: `create-os-specific-archive` property + Windows OS

#### maven-assembly-plugin
**Version**: 3.7.1
- Creates `.zip` archives
- Packages `log-gazer*.exe` native binary

#### create-os-specific-mac
**Activation**: `create-os-specific-archive` property + non-Windows OS (Linux, macOS)

#### maven-assembly-plugin
**Version**: 3.7.1
- Creates `.tar.gz` archives
- Packages `log-gazer*` native binary

**Naming Convention**: `${project.artifactId}-${project.version}-${os.detected.classifier}`

## Common Build Commands

### Standard Build (uber JAR)
```bash
mvn clean package
```
Compiles the code and creates both the regular JAR and uber JAR.

### Native Image Build
```bash
mvn clean package -Pnative-graalvm-default-liberica-nik
```
Builds a native executable using GraalVM (requires Liberica NIK). The appropriate platform-specific archive (`.zip` on Windows, `.tar.gz` on Linux/macOS) is created automatically.

### Run with JavaFX Plugin
```bash
mvn javafx:run
```
Runs the application using the JavaFX Maven plugin.

### Release Process
```bash
mvn release:prepare -Posslabz-release
mvn release:perform -Posslabz-release
```
Prepares and performs a release using conventional commits for versioning.

## Key Properties

- `osslabz.java.version`: 25
- `mainClass`: net.osslabz.loggazer.LogGazerApp
- `project.build.outputTimestamp`: 2024-12-02T20:20:06Z (for reproducible builds)