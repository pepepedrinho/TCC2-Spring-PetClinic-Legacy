param(
    [Parameter(Mandatory=$true)][string]$CkJar,
    [string]$Java17Home = $env:JAVA17_HOME
)
& "$PSScriptRoot/../../../scripts/collect-ck-historico.ps1" @PSBoundParameters
