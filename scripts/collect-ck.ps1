param(
    [Parameter(Mandatory=$true)][string]$CkJar,
    [string]$Java17Home = $env:JAVA17_HOME
)
$ErrorActionPreference = 'Stop'
if (-not $Java17Home) { throw 'Defina JAVA17_HOME ou -Java17Home.' }
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$out = Join-Path $root 'docs/metricas/v2'
$archive = Join-Path $root '.work/ck-baseline.zip'
$baseline = Join-Path $root '.work/ck-baseline'
New-Item -ItemType Directory -Force $baseline | Out-Null
$expected = '98893EFDFFD22FB2356560E9ED78B0528E01BBBAD032DCD63BE3863C73BFC517'
if ((Get-FileHash -LiteralPath $CkJar -Algorithm SHA256).Hash -ne $expected) { throw 'JAR CK difere do utilizado na coleta V2.' }
git archive --format=zip "--output=$archive" c36452a2c34443ae26b4ecbba4f149906af14717 src/main/java
if ($LASTEXITCODE -ne 0) { throw 'git archive falhou' }
Expand-Archive -LiteralPath $archive -DestinationPath $baseline -Force
$components = [ordered]@{
    'baseline-recoletada' = "$baseline/src/main/java"
    'monolito' = "$root/src/main/java"
    'owners' = "$root/owners-service/src/main/java"
    'gateway' = "$root/gateway/src/main/java"
}
foreach ($entry in $components.GetEnumerator()) {
    $dest = Join-Path $out $entry.Key
    New-Item -ItemType Directory -Force $dest | Out-Null
    & "$Java17Home/bin/java.exe" -jar $CkJar $entry.Value false 0 false $dest 2>&1 | Tee-Object -FilePath "$dest/coleta.txt"
    if ($LASTEXITCODE -ne 0) { throw "CK falhou: $($entry.Key)" }
}
@{
    baseline = 'c36452a2c34443ae26b4ecbba4f149906af14717'
    evaluatedHead = (git rev-parse HEAD)
    workingTree = (git status --porcelain | Out-String)
    ckVersion = '0.7.1-SNAPSHOT'
    ckSourceCommit = '8a1ef916d597d33ad619d748b616bb7dbc3f0c36'
    ckSha256 = $expected
    parameters = @('false','0','false')
    collectedUtc = [DateTime]::UtcNow.ToString('o')
} | ConvertTo-Json | Set-Content -Encoding utf8 "$out/proveniencia.json"
python "$PSScriptRoot/analyze-metrics.py"
if ($LASTEXITCODE -ne 0) { throw 'Analise falhou' }
