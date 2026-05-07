param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path "out")) {
    New-Item -ItemType Directory -Path "out" | Out-Null
}

$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d out $javaFiles
java -cp out com.rechargeapp.Main $Port
