#!/bin/bash
#
# Start script for file-transfer-service

PORT=8080
exec java -jar -Dserver.port="${PORT}" "file-transfer-service.jar"
