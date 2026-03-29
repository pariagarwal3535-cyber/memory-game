#!/bin/bash
echo "============================================"
echo " Memory Game - Build and Run"
echo "============================================"

# Create output directory
mkdir -p bin

# Compile all Java files
echo "Compiling..."
javac -d bin -sourcepath src src/Main.java

if [ $? -ne 0 ]; then
    echo ""
    echo "BUILD FAILED. Make sure JDK is installed."
    exit 1
fi

echo "Compilation successful!"
echo ""
echo "Launching Memory Game..."
java -cp bin Main