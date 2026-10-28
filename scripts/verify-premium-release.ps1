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
$allJavascript = (Get-ChildItem -LiteralPath $assets -Filter '*.js' -File | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"
foreach ($endpoint in @('/api/premium/access', '/api/premium/list', '/api/premium/admin/listeners', '/api/premium/admin/users')) {
    if (-not $allJavascript.Contains($endpoint)) { throw "Audience API missing from frontend build: $endpoint" }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $BackendJar).Path)
try {
    foreach ($class in @('controller/PremiumAudioController', 'entity/PremiumAudio', 'repository/PremiumAudioRepository', 'controller/PremiumAudienceController', 'service/PremiumAudienceService', 'entity/PremiumListener', 'repository/PremiumListenerRepository')) {
        $name = "BOOT-INF/classes/com/kaede/portfoliobackend/$class.class"
        if (-not $jar.GetEntry($name)) { throw "Backend class missing from JAR: $class" }
    }
    $security = $jar.GetEntry('BOOT-INF/classes/com/kaede/portfoliobackend/config/SecurityConfig.class')
    $stream = $security.Open()
    $buffer = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($buffer)
        $constants = [System.Text.Encoding]::ASCII.GetString($buffer.ToArray())
        foreach ($rule in @('/api/premium/play/**', '/api/premium/admin/**', '/api/premium/access', '/api/premium/list')) {
            if (-not $constants.Contains($rule)) { throw "Premium security rule missing from JAR: $rule" }
        }
    } finally { $stream.Dispose(); $buffer.Dispose() }
    if (-not $jar.GetEntry('BOOT-INF/classes/application-prod.properties')) { throw 'Production configuration is missing from JAR.' }
    if (-not $jar.GetEntry('BOOT-INF/classes/db/manual/premium-listeners-2026-10-28.sql')) { throw 'Listener migration SQL is missing from JAR.' }
} finally { $jar.Dispose() }
Write-Output 'Release verified: audio pages, routes, audience APIs, backend classes, security rules, listener SQL and production configuration are present.'
