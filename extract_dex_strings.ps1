# Extract UTF-8 strings from DEX file - focuses on strings data section
param(
    [string]$DexPath = "C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\build\intermediates\project_dex_archive\debug\dexBuilderDebug\out\com\autobookkeeper\service\PaymentNotificationListener.dex"
)

$data = [System.IO.File]::ReadAllBytes($DexPath)

# DEX header parsing
$stringIdsOff = [System.BitConverter]::ToUInt32($data, 0x0C)  # string_ids_off
$stringIdsSize = [System.BitConverter]::ToUInt32($data, 8)    # string_ids_size
$stringDataOff = [System.BitConverter]::ToUInt32($data, 0x10)  # data_off (approx)

Write-Host "DEX header: string_ids_off=$stringIdsOff string_ids_size=$stringIdsSize"

# Read all string data offsets
Write-Host "Reading $stringIdsSize string offsets..."
$stringOffsets = @()
for ($i = 0; $i -lt $stringIdsSize; $i++) {
    $offset = [System.BitConverter]::ToUInt32($data, [int]$stringIdsOff + $i * 4)
    $stringOffsets += $offset
}

# Read strings
$strings = @()
foreach ($off in $stringOffsets) {
    # ULEB128 encoded length
    $pos = $off
    $len = 0
    $shift = 0
    do {
        $byte = $data[$pos]
        $len = $len -bor (($byte -band 0x7F) -shl $shift)
        $shift += 7
        $pos++
    } while ($byte -band 0x80)
    
    # Try UTF-8 decoding
    if ($len -gt 0 -and $pos + $len -le $data.Length) {
        $utf8Bytes = $data[$pos..($pos+$len-1)]
        $str = [System.Text.Encoding]::UTF8.GetString($utf8Bytes)
        $strings += $str
    }
}

# Write all strings sorted, one per line
$outputPath = [System.IO.Path]::ChangeExtension($DexPath, "strings.txt")
$strings | Sort-Object -Unique | Out-File -FilePath $outputPath -Encoding UTF8
Write-Host "Extracted $($strings.Count) strings to $outputPath"

# Show interesting strings (non-empty)
$interesting = $strings | Where-Object { $_.Length -gt 0 }
$interesting[0..100]
