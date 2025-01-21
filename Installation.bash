#!/bin/bash

# Remove existing Java installations
sudo apt remove openjdk*

# Install Temurin JDK 21
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg

echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print $2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update
sudo apt install temurin-21-jdk

# Verify installation
java --version
javac --version

# Set as default Java
sudo update-alternatives --config java
sudo update-alternatives --config javac