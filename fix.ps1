# Fix HorizontalDivider in MainScreen.kt
$mainScreen = "C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\MainScreen.kt"
$content = [IO.File]::ReadAllText($mainScreen, [Text.Encoding]::UTF8)
if ($content.Contains("HorizontalDivider")) {
    $content = $content -replace "HorizontalDivider\(\)", "Divider()"
    [IO.File]::WriteAllText($mainScreen, $content, [Text.Encoding]::UTF8)
    Write-Host "Fixed MainScreen.kt HorizontalDivider"
}

# Fix HorizontalDivider in ReportScreen.kt
$reportScreen = "C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\ReportScreen.kt"
$content2 = [IO.File]::ReadAllText($reportScreen, [Text.Encoding]::UTF8)
if ($content2.Contains("HorizontalDivider")) {
    $content2 = $content2 -replace "HorizontalDivider\(\)", "Divider()"
    [IO.File]::WriteAllText($reportScreen, $content2, [Text.Encoding]::UTF8)
    Write-Host "Fixed ReportScreen.kt HorizontalDivider"
}

# Fix TransactionViewModel line 501 - replace the addCategory call with correct one
$vmFile = "C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\viewmodel\TransactionViewModel.kt"
$vmContent = [IO.File]::ReadAllText($vmFile, [Text.Encoding]::UTF8)

# The issue: CategoryData(name, icon, type, parentName, isCustom = true)
# Need to check what CategoryData constructor looks like
# parentName is a String? parameter

# Find and fix the addCategory body
$oldBlock = 'categoryDao.insert(CategoryData(name, icon, type, parentName, isCustom = true))'
$newBlock = 'categoryDao.insert(CategoryData(name = name, icon = icon, type = type, parentName = parentName, isCustom = true))'

if ($vmContent.Contains($oldBlock)) {
    $vmContent = $vmContent.Replace($oldBlock, $newBlock)
    [IO.File]::WriteAllText($vmFile, $vmContent, [Text.Encoding]::UTF8)
    Write-Host "Fixed TransactionViewModel.kt addCategory"
} else {
    Write-Host "Pattern not found, trying alternate"
    # The file might have different encoding, look for the actual text
    if ($vmContent -match 'categoryDao\.insert\(CategoryData\(name') {
        Write-Host "Found addCategory pattern with named args"
    }
}

# Show last 10 lines of VM file to debug
$lines = [IO.File]::ReadAllLines($vmFile, [Text.Encoding]::UTF8)
Write-Host "Total lines: $($lines.Count)"
$lines[-15..-1] | ForEach-Object { Write-Host "L: $_" }

Write-Host "Done"