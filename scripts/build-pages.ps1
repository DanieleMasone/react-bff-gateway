param(
    [string]$Repository = $env:GITHUB_REPOSITORY,
    [string]$TargetDirectory = "target/pages"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Repository)) {
    $Repository = "danielemasone/react-bff-gateway"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$pagesSource = Join-Path $repoRoot ".github/pages"
$target = Join-Path $repoRoot $TargetDirectory

function Assert-PathExists {
    param(
        [string]$Path,
        [string]$Description
    )

    if (-not (Test-Path $Path)) {
        throw "$Description not found: $Path"
    }
}

function Ensure-CleanDirectory {
    param([string]$Path)

    if (Test-Path $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

function Copy-RequiredDirectory {
    param(
        [string]$Source,
        [string]$Destination,
        [string]$Description
    )

    Assert-PathExists -Path $Source -Description $Description
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Copy-Item -Path (Join-Path $Source "*") -Destination $Destination -Recurse -Force
}

function Copy-RequiredFile {
    param(
        [string]$Source,
        [string]$Destination,
        [string]$Description
    )

    Assert-PathExists -Path $Source -Description $Description
    New-Item -ItemType Directory -Force -Path (Split-Path $Destination -Parent) | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}

Assert-PathExists -Path $pagesSource -Description "Pages source directory"
Ensure-CleanDirectory -Path $target

Copy-RequiredDirectory `
    -Source (Join-Path $pagesSource "assets") `
    -Destination (Join-Path $target "assets") `
    -Description "Pages shared assets"

$htmlFiles = Get-ChildItem -Path $pagesSource -Filter "*.html" -Recurse -File
foreach ($file in $htmlFiles) {
    $relativePath = $file.FullName.Substring($pagesSource.Length).TrimStart("\", "/")
    $destination = Join-Path $target $relativePath
    New-Item -ItemType Directory -Force -Path (Split-Path $destination -Parent) | Out-Null
    (Get-Content -Raw $file.FullName).Replace("__REPOSITORY__", $Repository) |
        Set-Content -Encoding UTF8 -Path $destination
}

Copy-RequiredDirectory `
    -Source (Join-Path $repoRoot "target/site/apidocs") `
    -Destination (Join-Path $target "javadoc") `
    -Description "Generated Javadoc"

Copy-RequiredDirectory `
    -Source (Join-Path $repoRoot "target/site/jacoco") `
    -Destination (Join-Path $target "coverage") `
    -Description "Generated JaCoCo coverage report"

Copy-RequiredFile `
    -Source (Join-Path $repoRoot "target/openapi/openapi.json") `
    -Destination (Join-Path $target "api/openapi.json") `
    -Description "Generated OpenAPI JSON"

Copy-RequiredFile `
    -Source (Join-Path $repoRoot "target/openapi/openapi.yaml") `
    -Destination (Join-Path $target "api/openapi.yaml") `
    -Description "Generated OpenAPI YAML"

Copy-RequiredDirectory `
    -Source (Join-Path $repoRoot "target/swagger-ui") `
    -Destination (Join-Path $target "swagger-ui") `
    -Description "Generated Swagger UI"

$unresolvedPlaceholder = Get-ChildItem -Path $target -Filter "*.html" -Recurse -File |
    Select-String -Pattern "__REPOSITORY__"
if ($null -ne $unresolvedPlaceholder) {
    throw "Pages assembly left unresolved __REPOSITORY__ placeholders."
}

Write-Output "GitHub Pages content assembled at $target"
