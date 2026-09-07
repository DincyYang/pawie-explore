#!/usr/bin/env bash
# Compiles the reference implementations and runs the JUnit suite.
# No build tool: a JDK and the two vendored jars are the whole toolchain.
set -euo pipefail
cd "$(dirname "$0")"

CP="lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar"
rm -rf out test-out
mkdir -p out test-out

javac -Xlint:all -d out $(find src -name '*.java')
javac -cp "out:$CP" -d test-out $(find test -name '*.java')

TESTS=$(cd test && find . -name '*Test.java' | sed 's|^\./||; s|\.java$||; s|/|.|g')
java -cp "out:test-out:$CP" org.junit.runner.JUnitCore $TESTS
