# Log Gazer (with JSON Log Support)

![GitHub](https://img.shields.io/github/license/osslabz/lnd-rest-client)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/log-gazer/build-on-push.yml?branch=main)

Log Gazer is a simple log file viewer that can highlight log lines based on certain keywords (FATAL, ERROR, WARN, INFO, DEBUG, TRACE) that are commonly used from various log systems.
In addition, it can also format JSON logs in case one needs to be parse them .

## Features
- Opens regular files, ZIP-Files (*.zip), TAR files (*.tar) and GZ files (*.gz, *.tar.gz) directly
- Detects various log levels (TRACE, DEBUG, INFO, WARN, ERROR, FATAL) and highlights log lines accordingly for easy visual parsing
- Can process JSON log files, even if the whole file is not valid JSON (but the log line is).

## Limitations
- The while file is loaded and kept in memory. This viewer is not (yet) optimized for very large files.