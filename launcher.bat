@echo off
echo Digital Wellbeing Desktop App Installer
echo ======================================
echo.

REM Create shortcut on desktop
echo Creating desktop shortcut...
powershell "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%USERPROFILE%\Desktop\Digital Wellbeing.lnk'); $Shortcut.TargetPath = '%~dp0DigitalWellbeing.bat'; $Shortcut.WorkingDirectory = '%~dp0'; $Shortcut.Description = 'Digital Wellbeing Desktop Application'; $Shortcut.Save()"

REM Create Start Menu shortcut
echo Creating Start Menu shortcut...
powershell "$StartMenuPath = [System.Environment]::GetFolderPath('StartMenu') + '\Programs'; if(!(Test-Path -Path \"$StartMenuPath\Digital Wellbeing\")) { New-Item -Path \"$StartMenuPath\Digital Wellbeing\" -ItemType Directory }; $WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut(\"$StartMenuPath\Digital Wellbeing\Digital Wellbeing.lnk\"); $Shortcut.TargetPath = '%~dp0DigitalWellbeing.bat'; $Shortcut.WorkingDirectory = '%~dp0'; $Shortcut.Description = 'Digital Wellbeing Desktop Application'; $Shortcut.Save()"

echo.
echo Installation complete!
echo.
echo The application will now be available:
echo  - On your desktop
echo  - In the Start Menu under "Digital Wellbeing"
echo.
echo Press any key to launch the application...
pause > nul

start "" "%~dp0DigitalWellbeing.bat" 