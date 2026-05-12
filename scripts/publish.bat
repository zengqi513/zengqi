# AutoBookkeeper 一键发布脚本
# 双击运行，按提示输入即可

Write-Host @"
╔════════════════════════════════════════╗
║     AutoBookkeeper 一键发布工具        ║
╚════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# 读取保存的Token（如果存在）
$tokenFile = "$PSScriptRoot\.github_token"
if (Test-Path $tokenFile) {
    $savedToken = Get-Content $tokenFile -Raw
    $useSaved = Read-Host "使用保存的GitHub Token? (Y/n)"
    if ($useSaved -ne "n") {
        $Token = $savedToken
    }
}

# 输入版本号
$defaultVersion = "2.1.1"
$Version = Read-Host "输入版本号 (默认: $defaultVersion)"
if ([string]::IsNullOrWhiteSpace($Version)) {
    $Version = $defaultVersion
}

# 输入Token
if ([string]::IsNullOrWhiteSpace($Token)) {
    $Token = Read-Host "输入GitHub Personal Access Token"
    # 保存Token（可选）
    $saveToken = Read-Host "保存Token以便下次使用? (y/N)"
    if ($saveToken -eq "y") {
        $Token | Out-File $tokenFile
        Write-Host "Token已保存" -ForegroundColor Green
    }
}

# 确认发布
Write-Host ""
Write-Host "即将发布: AutoBookkeeper v$Version" -ForegroundColor Yellow
Write-Host "GitHub仓库: zengqi513/zengqi" -ForegroundColor Yellow
$confirm = Read-Host "确认发布? (y/N)"

if ($confirm -eq "y") {
    Write-Host ""
    Write-Host "开始发布流程..." -ForegroundColor Cyan
    
    # 调用主脚本
    & "$PSScriptRoot\release.ps1" -Version $Version -Token $Token
} else {
    Write-Host "发布已取消" -ForegroundColor Red
}

Write-Host ""
Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
