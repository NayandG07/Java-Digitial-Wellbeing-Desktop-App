@echo off
echo Building standalone JAR file...

REM Compile all Java files
javac -d target/classes -cp "lib/*" src/main/java/com/wellbeing/*.java

REM Create lib directory in target
mkdir target\lib 2>nul

REM Copy dependencies to lib directory 
copy lib\*.jar target\lib\ 2>nul

REM Create manifest file
echo Manifest-Version: 1.0 > MANIFEST.MF
echo Main-Class: com.wellbeing.DigitalWellbeingApp >> MANIFEST.MF
echo Class-Path: lib/jna-5.14.0.jar lib/jna-platform-5.14.0.jar lib/jfreechart-1.5.4.jar >> MANIFEST.MF

REM Create data directories in target
mkdir target\data 2>nul
mkdir target\data\reports 2>nul
mkdir target\data\usage 2>nul

REM Create the JAR file
jar cfm DigitalWellbeing.jar MANIFEST.MF -C target/classes .

REM Create a simple launcher batch file for the JAR
echo @echo off > DigitalWellbeing.bat
echo java -jar DigitalWellbeing.jar >> DigitalWellbeing.bat

echo.
echo Build complete! 
echo Distribution files created:
echo - DigitalWellbeing.jar (Main JAR file)
echo - DigitalWellbeing.bat (Launcher script)

pause 