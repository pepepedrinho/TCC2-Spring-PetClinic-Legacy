param(
    [string]$MavenHome = $env:MAVEN_HOME,
    [string]$Java8Home = $env:JAVA8_HOME,
    [string]$Java17Home = $env:JAVA17_HOME,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
if (-not $Java8Home -or -not $Java17Home) {
    throw 'Defina JAVA8_HOME e JAVA17_HOME ou informe -Java8Home e -Java17Home.'
}
$java8 = (Resolve-Path -LiteralPath (Join-Path $Java8Home 'bin/java.exe')).Path
$java17 = (Resolve-Path -LiteralPath (Join-Path $Java17Home 'bin/java.exe')).Path
if ($MavenHome) {
    $mvn = (Resolve-Path -LiteralPath (Join-Path $MavenHome 'bin/mvn.cmd')).Path
} else {
    $mvn = (Get-Command mvn.cmd -CommandType Application).Source
}

$projects = Join-Path (Split-Path $PSScriptRoot -Parent) 'projects'
$monolith = Join-Path $projects 'monolith'
$previousJava = $env:JAVA_HOME
$previousPath = $env:Path
$argsMaven = @('-B', 'clean', 'package')
if ($Offline) { $argsMaven += '-o' }

try {
    $env:JAVA_HOME = Split-Path (Split-Path $java8 -Parent) -Parent
    $env:Path = "$(Split-Path $java8 -Parent);$previousPath"
    Push-Location -LiteralPath $monolith
    try {
        $wrapperJar = Join-Path $monolith '.mvn/wrapper/maven-wrapper.jar'
        & $java8 -classpath $wrapperJar "-Dmaven.multiModuleProjectDirectory=$monolith" org.apache.maven.wrapper.MavenWrapperMain @argsMaven '-Dcobertura.skip=true'
        if ($LASTEXITCODE -ne 0) { throw 'Build legado falhou.' }
    } finally {
        Pop-Location
    }

    $env:JAVA_HOME = Split-Path (Split-Path $java17 -Parent) -Parent
    $env:Path = "$(Split-Path $java17 -Parent);$previousPath"
    foreach ($module in @('owners-service', 'gateway')) {
        & $mvn @argsMaven -f (Join-Path $projects "$module/pom.xml")
        if ($LASTEXITCODE -ne 0) { throw "Build $module falhou." }
    }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:Path = $previousPath
}
