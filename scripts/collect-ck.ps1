param(
    [Parameter(Mandatory=$true)][string]$CkJar,
    [string]$Java17Home = $env:JAVA17_HOME,
    [switch]$V3
)
$ErrorActionPreference = 'Stop'
if (-not $Java17Home) { throw 'Defina JAVA17_HOME ou -Java17Home.' }

$root = Split-Path $PSScriptRoot -Parent
$CkJar = (Resolve-Path -LiteralPath $CkJar).Path
$java = (Resolve-Path -LiteralPath (Join-Path $Java17Home 'bin/java.exe')).Path
$javaVersion = (& $java -version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "17[."]') {
    throw 'A coleta CK requer Java 17.'
}

$expected = '98893EFDFFD22FB2356560E9ED78B0528E01BBBAD032DCD63BE3863C73BFC517'
if ((Get-FileHash -LiteralPath $CkJar -Algorithm SHA256).Hash -ne $expected) {
    throw 'O JAR CK difere do utilizado na coleta V2.'
}

$head = git -C $root rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível identificar HEAD.' }
$baseline = 'c36452a2c34443ae26b4ecbba4f149906af14717'
$scenarios = [ordered]@{
    'A-monolito-original' = @{ revision = $baseline; sourceRoot = 'src/main/java' }
    'B-monolito-adaptado' = @{ revision = $head; sourceRoot = 'projects/monolith/src/main/java' }
    'C-owners-service' = @{ revision = $head; sourceRoot = 'projects/owners-service/src/main/java' }
}

if ($V3) {
    $v2 = '9fc9afb839a0ecc85b7b0054e635dc618f0b150a'
    $scenarios['B-monolito-adaptado'].revision = $v2
    $scenarios['C-owners-service'].revision = $v2
    $scenarios['D-monolito-v3'] = @{ revision = $head; sourceRoot = 'projects/monolith/src/main/java' }
    $scenarios['E-owners-service-v3'] = @{ revision = $head; sourceRoot = 'projects/owners-service/src/main/java' }
    $scenarios['F-gateway-v3'] = @{ revision = $head; sourceRoot = 'projects/gateway/src/main/java' }
}

$work = Join-Path $root '.work'
New-Item -ItemType Directory -Force $work | Out-Null
$out = (New-Item -ItemType Directory (Join-Path $work ('ck-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')))).FullName
$parameters = @('false', '0', 'false')
foreach ($entry in $scenarios.GetEnumerator()) {
    $scenario = $entry.Value
    $dest = (New-Item -ItemType Directory (Join-Path $out $entry.Key)).FullName
    $snapshot = (New-Item -ItemType Directory (Join-Path $dest 'fontes')).FullName
    $archive = Join-Path $dest 'fontes.tar'
    git -C $root archive --format=tar "--output=$archive" $scenario.revision $scenario.sourceRoot
    if ($LASTEXITCODE -ne 0) { throw "Falha ao exportar os fontes de $($entry.Key)." }
    tar -xf $archive -C $snapshot
    if ($LASTEXITCODE -ne 0) { throw "Falha ao extrair os fontes de $($entry.Key)." }
    $source = Join-Path $snapshot $scenario.sourceRoot
    & $java -jar $CkJar $source @parameters $dest 2>&1 |
        ForEach-Object { "$_" } | Tee-Object -FilePath "$dest/coleta.txt"
    if ($LASTEXITCODE -ne 0) { throw "CK falhou em $($entry.Key)." }
    $scenario.types = @(Import-Csv "$dest/class.csv").Count
    $scenario.methods = @(Import-Csv "$dest/method.csv").Count
    $scenario.outputSha256 = [ordered]@{}
    foreach ($csv in @('class.csv', 'method.csv')) {
        $scenario.outputSha256[$csv] = (Get-FileHash -LiteralPath "$dest/$csv").Hash.ToLowerInvariant()
    }
}

[ordered]@{
    collectedUtc = [DateTime]::UtcNow.ToString('o')
    ckVersion = '0.7.1-SNAPSHOT'
    ckSourceCommit = '8a1ef916d597d33ad619d748b616bb7dbc3f0c36'
    ckSha256 = $expected.ToLowerInvariant()
    javaVersion = $javaVersion
    parameters = $parameters
    scenarios = $scenarios
} | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 "$out/proveniencia.json"
Write-Output "Coleta CK: $out"
