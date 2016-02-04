#!/bin/bash
echo [INFO] Package the war in target dir.

exec mvn clean deploy
