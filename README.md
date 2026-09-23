# Log Gazer (with JSON Log Support)

![GitHub](https://img.shields.io/github/license/osslabz/log-gazer)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/log-gazer/build-on-push.yml?branch=dev&label=build&logo=git)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/log-gazer/release.yml?branch=dev&label=release&logo=semanticrelease)
[![GitHub Release](https://img.shields.io/github/v/release/osslabz/log-gazer)](https://github.com/osslabz/log-gazer/releases/latest)
![GitHub Downloads](https://img.shields.io/github/downloads/osslabz/log-gazer/total)

Log Gazer is a simple log file viewer that can highlight log lines based on certain keywords (FATAL, ERROR, WARN, INFO, DEBUG, TRACE) that are commonly used from various log systems.
In addition, it can also format JSON logs in case one needs to be parse them.

Tagged nine times since September 2024, with binaries published for five of those; 1.3.0 landed on 2026-09-16 after a week of bug fixes. The 39 tests cover highlighting, JSON formatting, file loading and window state, and assert real output.

## Features
- Opens regular files, ZIP-Files (*.zip), TAR files (*.tar) and GZ files (*.gz, *.tar.gz, *.tgz) directly
- Detects various log levels (TRACE, DEBUG, INFO, WARN, ERROR, FATAL) and highlights log lines accordingly for easy visual parsing
- Can process JSON log files, even if the whole file is not valid JSON (but the log line is).

## Install

Each release ships one native binary per platform, built with GraalVM native-image. There is no Maven artifact and no JDK is needed to run it.

| Platform       | Archive                                        |
|----------------|------------------------------------------------|
| Linux x86_64   | `log-gazer-<version>-linux-x86_64.tar.gz`      |
| macOS arm64    | `log-gazer-<version>-osx-aarch_64.tar.gz`      |
| macOS x86_64   | `log-gazer-<version>-osx-x86_64.tar.gz`        |
| Windows x86_64 | `log-gazer-<version>-windows-x86_64.zip`       |

Download the archive for your platform from the [latest release](https://github.com/osslabz/log-gazer/releases/latest), unpack it and run the binary it contains:

```bash
tar xzf log-gazer-1.3.0-osx-aarch_64.tar.gz
./log-gazer-1.3.0-osx-aarch_64/log-gazer
```

On Windows the zip holds `log-gazer.exe` in the same layout. `log-gazer --version` prints the version and exits.

## Limitations
- The while file is loaded and kept in memory. This viewer is not (yet) optimized for very large files.