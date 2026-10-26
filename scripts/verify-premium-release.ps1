param(
    [Parameter(Mandatory = $true)][string]$FrontendDist,
    [Parameter(Mandatory = $true)][string]$BackendJar
)
$ErrorActionPreference = 'Stop'

# Check the built files, rather than assuming local source files were packaged.
$indexPath = Join-Path $FrontendDist 'index.html'
if (-not (Test-Path -LiteralPath $indexPath)) { throw 'Frontend index.html is missing.' }
$assets = Join-Path $FrontendDist 'assets'
$entryFiles = @(Get-ChildItem -LiteralPath $assets -Filter 'index-*.js' -File)
$entryContent = ($entryFiles | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"
foreach ($route in @('/premium-admin', '/exclusive-player/:uuid')) {
    if (-not $entryContent.Contains($route)) { throw "Frontend route missing from build: $route" }
}
foreach ($view in @('PremiumAdminView', 'ExclusivePlayerView')) {
    $chunks = @(Get-ChildItem -LiteralPath $assets -Filter "$view-*.js" -File)
    if ($chunks.Count -ne 1) { throw "Expected exactly one built chunk for $view." }
    if (-not $entryContent.Contains($chunks[0].Name)) { throw "Built route does not reference $view." }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $BackendJar).Path)
try {
    foreach ($class in @('controller/PremiumAudioController', 'entity/PremiumAudio', 'repository/PremiumAudioRepository')) {
        $name = "BOOT-INF/classes/com/kaede/portfoliobackend/$class.class"
        if (-not $jar.GetEntry($name)) { throw "Backend class missing from JAR: $class" }
    }
    $security = $jar.GetEntry('BOOT-INF/classes/com/kaede/portfoliobackend/config/SecurityConfig.class')
    $stream = $security.Open()
    $buffer = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($buffer)
        $constants = [System.Text.Encoding]::ASCII.GetString($buffer.ToArray())
        foreach ($rule in @('/api/premium/play/**', '/api/premium/admin/**')) {
            if (-not $constants.Contains($rule)) { throw "Premium security rule missing from JAR: $rule" }
        }
    } finally { $stream.Dispose(); $buffer.Dispose() }
    if (-not $jar.GetEntry('BOOT-INF/classes/application-prod.properties')) { throw 'Production configuration is missing from JAR.' }
} finally { $jar.Dispose() }
Write-Output 'Release verified: both audio pages, both routes, all three backend classes, premium security rules and production configuration are present.'
