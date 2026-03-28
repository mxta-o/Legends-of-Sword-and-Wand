#!/usr/bin/env bash
set -euo pipefail

# Copies dependencies and runs the Swing demo with dependencies on the classpath.
# Use the JVM flag to avoid the native-access warning when running with newer JDKs.

mvn -DskipTests=true dependency:copy-dependencies package
mvn -DskipTests=true package

java --enable-native-access=ALL-UNNAMED -cp 'target/classes:target/dependency/*' app.PlayDemoSwing
