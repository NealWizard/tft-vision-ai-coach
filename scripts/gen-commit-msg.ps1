# Generate commit message from git changes (output only, no commit)
param(
    [string[]]$Tasks,
    [string]$Type,
    [string]$Scope,
    [string]$Subject,
    [string]$RepoRoot
)

$ErrorActionPreference = "Stop"
if (-not $RepoRoot) { $RepoRoot = Split-Path $PSScriptRoot -Parent }
if (-not $Type) { $Type = "docs" }
Set-Location $RepoRoot

$changed = git status --porcelain
if (-not $changed) {
    Write-Host "No changes to commit."
    exit 0
}

if (-not $Subject) {
    $files = ($changed | ForEach-Object { $_.Substring(3).Trim() }) -join "`n"
    if ($files -match 'TFT_Vision_AI_Coach_Complete_Roadmap_V3_1') {
        $Type = "docs"
        $Scope = "roadmap"
        if ($Tasks -and $Tasks.Count -gt 0) {
            $Subject = "mark $($Tasks -join ', ') DONE in Roadmap V3.1"
        } else {
            $Subject = "update Roadmap V3.1 task status"
        }
    }
    elseif ($files -match '\.github/workflows') {
        $Type = "ci"
        $Scope = "github"
        $Subject = "update GitHub Actions workflow"
    }
    elseif ($files -match 'readme|docs/') {
        $Type = "docs"
        $Subject = "update project documentation"
    }
    elseif ($files -match 'schemas/') {
        $Type = "feat"
        $Scope = "contracts"
        $Subject = "update canonical schemas"
    }
    else {
        $Subject = "update project files"
    }
}

$scopePart = ""
if ($Scope) { $scopePart = "($Scope)" }
$header = "${Type}${scopePart}: $Subject"

Write-Host "========== Suggested commit message =========="
Write-Host $header
Write-Host ""
if ($Tasks -and $Tasks.Count -gt 0) {
    Write-Host "Tasks: $($Tasks -join ', ')"
}
Write-Host "Changed files:"
$changed | ForEach-Object { Write-Host "- $($_.Substring(3))" }
Write-Host "=============================================="
Write-Host ""
Write-Host "Copy and run:"
Write-Host "  git add -A"
Write-Host ('  git commit -m "' + $header + '"')
