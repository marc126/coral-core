#!/bin/bash
echo [INFO] Package the war in target dir.

exec mvn clean package -Dmaven.test.skip=true
