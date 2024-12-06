#!/bin/bash
#
# Start script for extensions-api

PORT=8080
JAVA_OPTS="-Xmx2g -verbose:gc -XX:+UseG1GC"
exec java ${JAVA_OPTS} -jar -Dserver.port="${PORT}" "file-transfer-service.jar"
