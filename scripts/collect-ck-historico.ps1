param(
    [Parameter(Mandatory=$true)][string]$CkJar,
    [string]$Java17Home = $env:JAVA17_HOME
)
$ErrorActionPreference = 'Stop'
if (-not $Java17Home) { throw 'Defina JAVA17_HOME ou -Java17Home.' }
$root = Split-Path $PSScriptRoot -Parent
$CkJar = (Resolve-Path -LiteralPath $CkJar).Path
$java = (Resolve-Path -LiteralPath (Join-Path $Java17Home 'bin/java.exe')).Path
$expected = '98893EFDFFD22FB2356560E9ED78B0528E01BBBAD032DCD63BE3863C73BFC517'
if ((Get-FileHash -LiteralPath $CkJar -Algorithm SHA256).Hash -ne $expected) { throw 'JAR CK difere do utilizado na coleta V2.' }

$components = [ordered]@{
    'monolito' = "$root/projects/monolith/src/main/java"
    'owners' = "$root/projects/owners-service/src/main/java"
    'gateway' = "$root/projects/gateway/src/main/java"
}
$work = Join-Path $root '.work'
New-Item -ItemType Directory -Force $work | Out-Null
$out = (New-Item -ItemType Directory (Join-Path $work ('ck-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')))).FullName
foreach ($entry in $components.GetEnumerator()) {
    $dest = (New-Item -ItemType Directory (Join-Path $out $entry.Key)).FullName
    & $java -jar $CkJar $entry.Value false 0 false $dest 2>&1 | ForEach-Object { "$_" } | Tee-Object -FilePath "$dest/coleta.txt"
    if ($LASTEXITCODE -ne 0) { throw "CK falhou: $($entry.Key)" }
}
@{
    baseline = 'c36452a2c34443ae26b4ecbba4f149906af14717'
    evaluatedHead = (git -C $root rev-parse HEAD)
    workingTree = (git -C $root status --porcelain | Out-String)
    ckVersion = '0.7.1-SNAPSHOT'
    ckSourceCommit = '8a1ef916d597d33ad619d748b616bb7dbc3f0c36'
    ckSha256 = $expected
    parameters = @('false','0','false')
    collectedUtc = [DateTime]::UtcNow.ToString('o')
} | ConvertTo-Json | Set-Content -Encoding utf8 "$out/proveniencia.json"
python "$PSScriptRoot/analyze-metrics.py" $out
if ($LASTEXITCODE -ne 0) { throw 'Analise falhou' }
Write-Output "Coleta CK: $out"
