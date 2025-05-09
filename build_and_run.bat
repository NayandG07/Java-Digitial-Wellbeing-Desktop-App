@echo off
echo Building and running Digital Wellbeing app...

REM Ensure directories exist
mkdir target\classes 2>nul
mkdir data 2>nul
mkdir data\usage 2>nul
mkdir data\reports 2>nul

REM Compile Java files
javac -d target\classes src\main\java\com\wellbeing\*.java

IF %ERRORLEVEL% NEQ 0 (
  echo Compilation failed. Please ensure you have JDK installed and the path is set correctly.
  pause
  exit /b 1
)

REM Run the application
echo Starting the application...
java -cp target\classes com.wellbeing.DigitalWellbeingApp

pause 