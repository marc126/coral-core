#!/bin/bash
echo [INFO] Use maven eclipse-plugin download jars and generate eclipse project files.
echo [Info] Please add "-Declipse.workspace=<path-to-eclipse-workspace>" at end of mvn command.

exec mvn eclipse:clean eclipse:eclipse

