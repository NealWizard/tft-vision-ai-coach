# Update task status in Roadmap V3.1 TASKS JSON (source file, not localStorage)
param(
    [Parameter(Mandatory = $true)][string[]]$TaskIds,
    [string]$Status,
    [string]$Owner,
    [string]$CompletionDate,
    [string]$HtmlPath
)

$ErrorActionPreference = "Stop"
$TaskIds = @($TaskIds |
    ForEach-Object { $_ -split "," } |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ })
if (-not $Status) { $Status = "DONE" }
if (-not $Owner) { $Owner = "NealWizard" }
if (-not $CompletionDate) { $CompletionDate = Get-Date -Format "yyyy-MM-dd" }
if (-not $HtmlPath) {
    $HtmlPath = Join-Path (Split-Path $PSScriptRoot -Parent) "TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html"
}
if (-not (Test-Path $HtmlPath)) { throw "HTML not found: $HtmlPath" }

$html = Get-Content -LiteralPath $HtmlPath -Raw -Encoding UTF8
$m = [regex]::Match($html, 'const TASKS = (\[.*?\]);', [System.Text.RegularExpressions.RegexOptions]::Singleline)
if (-not $m.Success) { throw "TASKS JSON not found in HTML" }

$tasks = $m.Groups[1].Value | ConvertFrom-Json
$updated = @()
foreach ($task in $tasks) {
    if ($TaskIds -contains $task.id) {
        $task.status = $Status
        $task.owner = $Owner
        if ($Status -eq "DONE") {
            $task.actual_completion = $CompletionDate
        }
        $updated += $task.id
    }
}

$missing = @($TaskIds | Where-Object { $_ -notin $updated })
if ($missing.Count -gt 0) { throw "Task IDs not found: $($missing -join ', ')" }

$json = ($tasks | ConvertTo-Json -Compress -Depth 30)
$json = $json -replace '\\u0026', '&'

$newHtml = $html.Substring(0, $m.Groups[1].Index) + $json + $html.Substring($m.Groups[1].Index + $m.Groups[1].Length)
[System.IO.File]::WriteAllText($HtmlPath, $newHtml, [System.Text.UTF8Encoding]::new($false))

Write-Host "Updated $($updated.Count) task(s): $($updated -join ', ')"
Write-Host "  status=$Status owner=$Owner completion=$CompletionDate"
