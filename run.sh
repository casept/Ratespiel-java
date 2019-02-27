#!/bin/sh
set -eu pipefail
./gradlew jar
java -jar build/libs/Ratespiel-java.jar