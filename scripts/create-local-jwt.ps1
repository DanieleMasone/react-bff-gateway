param(
    [string]$Subject = "user-123",
    [string]$Issuer = $env:BFF_JWT_ISSUER,
    [string]$Audience = $env:BFF_JWT_AUDIENCE,
    [string]$Secret = $env:BFF_JWT_SECRET,
    [int]$TtlMinutes = 60
)

if ([string]::IsNullOrWhiteSpace($Issuer)) {
    $Issuer = "react-bff-gateway-local"
}
if ([string]::IsNullOrWhiteSpace($Audience)) {
    $Audience = "react-dashboard"
}
if ([string]::IsNullOrWhiteSpace($Secret)) {
    $Secret = "local-development-secret-change-me-at-least-32-bytes"
}

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)
    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$header = @{ alg = "HS256"; typ = "JWT" } | ConvertTo-Json -Compress
$payload = @{
    sub = $Subject
    iss = $Issuer
    aud = @($Audience)
    iat = $now
    exp = $now + ($TtlMinutes * 60)
    scope = "dashboard:read"
} | ConvertTo-Json -Compress

$encodedHeader = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($header))
$encodedPayload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload))
$data = "$encodedHeader.$encodedPayload"

$hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
$signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data)))

Write-Output "$data.$signature"
