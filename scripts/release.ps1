# AutoBookkeeper GitHub自动发布脚本
# 用法: .\release.ps1 -Version "2.1.1" -Token "your_github_token"

param(
    [Parameter(Mandatory=$true)]
    [string]$Version,
    
    [Parameter(Mandatory=$true)]
    [string]$Token,
    
    [string]$Owner = "zengqi513",
    [string]$Repo = "zengqi",
    [string]$ApkDir = "E:\AutoBookkeeper-APKs",
    [string]$ProjectDir = "E:\AutoBookkeeper"
)

$ErrorActionPreference = "Stop"

function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Cyan }
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Error { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

Set-Location $ProjectDir

# 1. 读取当前版本号
Write-Info "读取当前版本信息..."
$buildGradle = Get-Content "app\build.gradle.kts" -Raw
$currentVersionCode = [regex]::Match($buildGradle, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$currentVersionName = [regex]::Match($buildGradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
Write-Info "当前版本: $currentVersionName (build $currentVersionCode)"

# 2. 更新版本号
Write-Info "更新版本号为 $Version..."
$newVersionCode = [int]$currentVersionCode + 1
$buildGradle = $buildGradle -replace "versionCode\s*=\s*\d+", "versionCode = $newVersionCode"
$buildGradle = $buildGradle -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$Version`""
$buildGradle | Out-File -FilePath "app\build.gradle.kts" -Encoding UTF8
Write-Success "版本号已更新: $Version (build $newVersionCode)"

# 3. 构建Release APK
Write-Info "开始构建Release APK..."
.\gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Error "构建失败!"
    exit 1
}
Write-Success "构建完成"

# 4. 复制APK到发布目录
$sourceApk = "app\build\outputs\apk\release\app-release.apk"
$targetApk = "$ApkDir\AutoBookkeeper_v$Version.apk"
Copy-Item $sourceApk $targetApk -Force
Write-Success "APK已复制到: $targetApk"

# 5. 提交版本更新
Write-Info "提交版本更新到Git..."
git add app\build.gradle.kts
git commit -m "Bump version to $Version (build $newVersionCode)"
git push origin main
Write-Success "代码已推送"

# 6. 创建GitHub Release
Write-Info "创建GitHub Release v$Version..."
$releaseData = @{
    tag_name = "v$Version"
    target_commitish = "main"
    name = "AutoBookkeeper v$Version"
    body = @"
## AutoBookkeeper v$Version

### 更新内容
- 版本号: $Version
- 构建号: $newVersionCode
- 发布日期: $(Get-Date -Format "yyyy-MM-dd")

### 安装说明
1. 下载APK文件
2. 允许安装未知来源应用
3. 安装并开启通知监听权限
"@
    draft = $false
    prerelease = $false
} | ConvertTo-Json -Depth 10

$headers = @{
    Authorization = "token $Token"
    Accept = "application/vnd.github.v3+json"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$Owner/$Repo/releases" -Method Post -Headers $headers -Body $releaseData
    $releaseId = $response.id
    $uploadUrl = $response.upload_url
    Write-Success "Release创建成功! ID: $releaseId"
} catch {
    Write-Error "创建Release失败: $($_.Exception.Message)"
    exit 1
}

# 7. 上传APK
Write-Info "上传APK到Release..."
$apkBytes = [System.IO.File]::ReadAllBytes($targetApk)
$uploadUrl = "https://uploads.github.com/repos/$Owner/$Repo/releases/$releaseId/assets?name=AutoBookkeeper_v$Version.apk"

$headers["Content-Type"] = "application/vnd.android.package-archive"

try {
    $response = Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers $headers -Body $apkBytes
    Write-Success "APK上传成功!"
    Write-Info "下载链接: $($response.browser_download_url)"
} catch {
    Write-Error "上传APK失败: $($_.Exception.Message)"
    exit 1
}

Write-Success "发布完成! 🎉"
Write-Info "Release页面: https://github.com/$Owner/$Repo/releases/tag/v$Version"
