@echo off
echo ============================================
echo  Memory Game - Build and Run
echo ============================================

:: Create output directory
if not exist bin mkdir bin

:: Compile all Java files
echo Compiling...
javac -d bin -sourcepath src src\Main.java

if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED. Make sure JDK is installed and in PATH.
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Launching Memory Game...
java -cp bin Main
pause
