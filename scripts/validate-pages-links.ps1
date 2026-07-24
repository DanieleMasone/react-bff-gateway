param(
    [string]$PagesDirectory = "target/pages"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path $PagesDirectory
$errors = New-Object System.Collections.Generic.List[string]

function Is-ExternalReference {
    param([string]$Reference)

    return $Reference -match "^(https?:|mailto:|tel:|javascript:|data:)"
}

function Resolve-LinkTarget {
    param(
        [string]$BaseDirectory,
        [string]$ReferencePath
    )

    $combined = Join-Path $BaseDirectory $ReferencePath
    if ($ReferencePath.EndsWith("/")) {
        return [System.IO.Path]::GetFullPath((Join-Path $combined "index.html"))
    }
    return [System.IO.Path]::GetFullPath($combined)
}

function Get-Ids {
    param([string]$Path)

    $ids = @{}
    $content = Get-Content -Raw $Path
    [regex]::Matches($content, "id=""([^""]+)""") | ForEach-Object {
        $ids[$_.Groups[1].Value] = $true
    }
    return $ids
}

$requiredRoutes = @(
    "index.html",
    "user-guide/index.html",
    "user-guide/getting-started.html",
    "user-guide/api-and-security.html",
    "user-guide/quality-and-troubleshooting.html",
    "javadoc/index.html",
    "coverage/index.html",
    "swagger-ui/index.html",
    "api/openapi.json",
    "api/openapi.yaml"
)

foreach ($route in $requiredRoutes) {
    $path = Join-Path $root $route
    if (-not (Test-Path $path)) {
        $errors.Add("Missing required Pages route: $route")
    }
}

$htmlFiles = @()
$htmlFiles += Get-Item (Join-Path $root "index.html")
$htmlFiles += Get-ChildItem -Path (Join-Path $root "user-guide") -Filter "*.html" -File
foreach ($file in $htmlFiles) {
    $content = Get-Content -Raw $file.FullName
    $currentIds = Get-Ids -Path $file.FullName
    $references = [regex]::Matches($content, "(?:href|src)=""([^""]+)""")

    foreach ($match in $references) {
        $reference = $match.Groups[1].Value
        if ([string]::IsNullOrWhiteSpace($reference) -or (Is-ExternalReference -Reference $reference)) {
            continue
        }

        $parts = $reference.Split("#", 2)
        $pathPart = $parts[0]
        $fragment = if ($parts.Length -eq 2) { $parts[1] } else { "" }

        if ($pathPart -eq "") {
            if ($fragment -ne "" -and -not $currentIds.ContainsKey($fragment)) {
                $errors.Add("Missing anchor $reference in $($file.FullName)")
            }
            continue
        }

        $targetPath = Resolve-LinkTarget -BaseDirectory $file.DirectoryName -ReferencePath $pathPart
        if (-not (Test-Path $targetPath)) {
            $errors.Add("Missing target for $reference in $($file.FullName)")
            continue
        }

        if ($fragment -ne "" -and $targetPath.EndsWith(".html")) {
            $targetIds = Get-Ids -Path $targetPath
            if (-not $targetIds.ContainsKey($fragment)) {
                $errors.Add("Missing anchor $reference in $targetPath")
            }
        }
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Pages links validated under $root"
