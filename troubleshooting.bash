#!/bin/bash

echo "Java Version:"
java --version

echo "\nJavac Version:"
javac --version

echo "\nGradle Version:"
./gradlew --version

echo "\nAvailable Gradle Tasks:"
./gradlew tasks

echo "\nJPackage Check:"
jpackage --version