# Uploads every jar in release/ to the GitLab Generic Package Registry and attaches it to the release for the given tag.
# Usage: $env:GITLAB_TOKEN = "<personal access token with api scope>"; ./scripts/publish-release.ps1 v0.1.0-alpha
param([Parameter(Mandatory = $true)][string]$Tag)

if (-not $env:GITLAB_TOKEN) { throw "Set GITLAB_TOKEN first" }
$api = "https://gitlab.com/api/v4/projects/nihaolauncher%2Fnihao-minecraft-client"
$headers = @{ "PRIVATE-TOKEN" = $env:GITLAB_TOKEN }
$version = ($Tag -replace '^v', '') -replace '-.*$', ''

foreach ($jar in Get-ChildItem "$PSScriptRoot/../release/*.jar") {
    $name = $jar.Name
    $url = "$api/packages/generic/nihao/$version/$name"
    Invoke-RestMethod -Method Put -Uri $url -Headers $headers -InFile $jar.FullName | Out-Null
    $link = @{ name = $name; url = $url; direct_asset_path = "/$name"; link_type = "package" }
    Invoke-RestMethod -Method Post -Uri "$api/releases/$Tag/assets/links" -Headers $headers -Body $link | Out-Null
    Write-Host "attached $name"
}
