@echo off
echo Creating Digital Wellbeing App Installer package...

REM Ensure target directory exists
mkdir DigitalWellbeingInstaller 2>nul

REM Copy necessary files
echo Copying application files...
mkdir DigitalWellbeingInstaller\target\classes 2>nul
mkdir DigitalWellbeingInstaller\data 2>nul
mkdir DigitalWellbeingInstaller\data\usage 2>nul 
mkdir DigitalWellbeingInstaller\data\reports 2>nul

xcopy /E /Y target\classes DigitalWellbeingInstaller\target\classes\ >nul
copy build_and_run.bat DigitalWellbeingInstaller\DigitalWellbeing.bat >nul

REM Create the launcher script
echo Creating launcher...
echo @echo off > DigitalWellbeingInstaller\launcher.bat
echo echo Digital Wellbeing Desktop App Installer >> DigitalWellbeingInstaller\launcher.bat
echo echo ====================================== >> DigitalWellbeingInstaller\launcher.bat
echo echo. >> DigitalWellbeingInstaller\launcher.bat
echo echo Creating desktop shortcut... >> DigitalWellbeingInstaller\launcher.bat
echo powershell "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%%USERPROFILE%%\Desktop\Digital Wellbeing.lnk'); $Shortcut.TargetPath = '%%~dp0DigitalWellbeing.bat'; $Shortcut.WorkingDirectory = '%%~dp0'; $Shortcut.Description = 'Digital Wellbeing Desktop Application'; $Shortcut.Save()" >> DigitalWellbeingInstaller\launcher.bat
echo echo Creating Start Menu shortcut... >> DigitalWellbeingInstaller\launcher.bat
echo powershell "$StartMenuPath = [System.Environment]::GetFolderPath('StartMenu') + '\Programs'; if(!(Test-Path -Path \"$StartMenuPath\Digital Wellbeing\")) { New-Item -Path \"$StartMenuPath\Digital Wellbeing\" -ItemType Directory }; $WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut(\"$StartMenuPath\Digital Wellbeing\Digital Wellbeing.lnk\"); $Shortcut.TargetPath = '%%~dp0DigitalWellbeing.bat'; $Shortcut.WorkingDirectory = '%%~dp0'; $Shortcut.Description = 'Digital Wellbeing Desktop Application'; $Shortcut.Save()" >> DigitalWellbeingInstaller\launcher.bat
echo echo. >> DigitalWellbeingInstaller\launcher.bat
echo echo Installation complete! >> DigitalWellbeingInstaller\launcher.bat
echo echo. >> DigitalWellbeingInstaller\launcher.bat
echo echo The application will now be available: >> DigitalWellbeingInstaller\launcher.bat
echo echo  - On your desktop >> DigitalWellbeingInstaller\launcher.bat
echo echo  - In the Start Menu under "Digital Wellbeing" >> DigitalWellbeingInstaller\launcher.bat
echo echo. >> DigitalWellbeingInstaller\launcher.bat
echo echo Press any key to launch the application... >> DigitalWellbeingInstaller\launcher.bat
echo pause ^> nul >> DigitalWellbeingInstaller\launcher.bat
echo start "" "%%~dp0DigitalWellbeing.bat" >> DigitalWellbeingInstaller\launcher.bat

REM Create a README file
echo Creating README file...
echo Digital Wellbeing Desktop Application > DigitalWellbeingInstaller\README.txt
echo ================================= >> DigitalWellbeingInstaller\README.txt
echo. >> DigitalWellbeingInstaller\README.txt
echo Installation Instructions: >> DigitalWellbeingInstaller\README.txt
echo 1. Run "launcher.bat" to create shortcuts and start the application >> DigitalWellbeingInstaller\README.txt
echo 2. You will need Java JDK or JRE 11 or higher installed on your system >> DigitalWellbeingInstaller\README.txt
echo. >> DigitalWellbeingInstaller\README.txt
echo Features: >> DigitalWellbeingInstaller\README.txt
echo - Track and monitor your computer usage >> DigitalWellbeingInstaller\README.txt
echo - Set up focus mode to block distracting applications >> DigitalWellbeingInstaller\README.txt
echo - Set usage goals and get alerts when you exceed them >> DigitalWellbeingInstaller\README.txt
echo - Configure break reminders to avoid prolonged screen time >> DigitalWellbeingInstaller\README.txt
echo - Generate detailed reports of your application usage >> DigitalWellbeingInstaller\README.txt

echo.
echo Installer package created successfully in the "DigitalWellbeingInstaller" folder
echo.
echo To distribute, zip the "DigitalWellbeingInstaller" folder and share it with your friends

pause 