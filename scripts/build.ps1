param(
    [string]$MavenHome = 'C:\TCC2\ferramentas\apache-maven-3.9.11',
    [string]$Java8Home = 'C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot',
    [string]$Java17Home = 'C:\Program Files\Java\jdk-17',
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$mvn = Join-Path $MavenHome 'bin/mvn.cmd'
$previousJava = $env:JAVA_HOME
try {
    $argsMaven = @('-B','clean','package')
    if ($Offline) { $argsMaven += '-o' }
    $env:JAVA_HOME = $Java8Home
    & $mvn @argsMaven '-Dcobertura.skip=true'
    if ($LASTEXITCODE -ne 0) { throw 'Build legado falhou.' }
    $env:JAVA_HOME = $Java17Home
    foreach ($module in @('owners-service','gateway')) {
        & $mvn @argsMaven -f "$module/pom.xml"
        if ($LASTEXITCODE -ne 0) { throw "Build $module falhou." }
    }
} finally { $env:JAVA_HOME = $previousJava }
