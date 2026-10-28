param(
    [Parameter(Mandatory=$true)][string]$SourceDb,
    [Parameter(Mandatory=$true)][string]$NewTargetDirectory,
    [string]$Java17Home = $env:JAVA17_HOME,
    [string]$MavenRepository = "$env:USERPROFILE\.m2\repository"
)
$ErrorActionPreference = 'Stop'
if (-not $Java17Home) { throw 'Defina JAVA17_HOME ou -Java17Home.' }
$repoRoot = Split-Path $PSScriptRoot -Parent
$hsql = Join-Path $MavenRepository 'org\hsqldb\hsqldb\2.3.5\hsqldb-2.3.5.jar'
$h2 = Join-Path $MavenRepository 'com\h2database\h2\2.3.232\h2-2.3.232.jar'
& "$Java17Home\bin\java.exe" --class-path "$hsql;$h2" "$PSScriptRoot\MigrateOwners.java" $SourceDb $NewTargetDirectory $repoRoot
if ($LASTEXITCODE -ne 0) { throw 'Migration failed; keep source intact and inspect the new target.' }
