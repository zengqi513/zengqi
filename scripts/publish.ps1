# AutoBookkeeper 一键发布 (PowerShell版)
param(
    [string]$Version = "2.1.1",
    [string]$Token = ""
)

if ([string]::IsNullOrWhiteSpace($Token)) {
    $tokenFile = "$PSScriptRoot\.github_token"
    if (Test-Path $tokenFile) {
        $Token = Get-Content $tokenFile -Raw
    } else {
        Write-Error "请提供GitHub Token，或创建 $tokenFile 文件"
        exit 1
    }
}

& "$PSScriptRoot\release.ps1" -Version $Version -Token $Token
