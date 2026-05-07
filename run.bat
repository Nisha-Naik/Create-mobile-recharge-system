@echo off
setlocal
set PORT=%1
if "%PORT%"=="" set PORT=8080
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out com.rechargeapp.Main %PORT%
endlocal
