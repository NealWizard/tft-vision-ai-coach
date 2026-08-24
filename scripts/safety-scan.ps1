# TFT Vision AI Coach — Safety Boundary Scanner (P0-FOUND-Safety-001)
# Blocks process-memory, injection, input simulation, and game-traffic interception patterns.
# Exit 1 on violation; exit 0 when clean.

param(
  [string]$ScanRoot
)

$ErrorActionPreference = "Stop"
if ($ScanRoot) {
  $Root = (Resolve-Path -LiteralPath $ScanRoot).Path
} else {
  $Root = Split-Path -Parent $PSScriptRoot
  if (-not (Test-Path (Join-Path $Root "pom.xml"))) {
    $Root = Get-Location
  }
}

$Include = @("*.java", "*.kt", "*.cs", "*.py", "*.js", "*.ts", "*.go", "*.rs", "*.xml", "*.gradle", "*.kts", "*.ps1", "*.bat", "*.cmd", "*.sh")
$ExcludeDirs = @(".git", ".tools", "target", "node_modules", ".idea", ".vscode", "fixtures")

# Pattern => human reason
$Rules = [ordered]@{
  "ReadProcessMemory" = "Process memory read is forbidden"
  "WriteProcessMemory" = "Process memory write is forbidden"
  "OpenProcess\s*\(" = "Opening foreign process handle is forbidden"
  "CreateRemoteThread" = "DLL/code injection primitive is forbidden"
  "LoadLibrary(A|W)?\s*\(" = "Dynamic library injection pattern is forbidden (review)"
  "SetWindowsHookEx" = "Global hooking is forbidden"
  "SendInput\s*\(" = "Input simulation is forbidden"
  "mouse_event\s*\(" = "Input simulation is forbidden"
  "keybd_event\s*\(" = "Input simulation is forbidden"
  "AutoHotkey|ahk\.dll" = "AutoHotKey automation is forbidden"
  "JNativeHook" = "Global native hook library is forbidden"
  "java\.awt\.Robot" = "AWT Robot input simulation is forbidden"
  "win32api\.|ctypes\.windll\.user32" = "Native input/process APIs are forbidden"
  "pcap|WinPcap|Npc4j|Npcng" = "Game traffic interception libraries are forbidden"
  "riotclient|lockfile.*riot|LeagueClientUx" = "Direct client lockfile/process coupling requires explicit allowlist (blocked in P0)"
}

function ShouldSkip([string]$path) {
  foreach ($d in $ExcludeDirs) {
    if ($path -match "[\\/]$d[\\/]") { return $true }
  }
  # Allow the scanner itself and intentional violation fixtures under tools/safety-fixtures
  if ($path -match "[\\/]scripts[\\/]safety-scan") { return $true }
  if ($path -match "[\\/]tools[\\/]safety-fixtures[\\/]") { return $true }
  return $false
}

$violations = @()
$files = Get-ChildItem -Path $Root -Recurse -File -Include $Include | Where-Object { -not (ShouldSkip $_.FullName) }

foreach ($file in $files) {
  $content = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction SilentlyContinue
  if ($null -eq $content) { continue }
  foreach ($pattern in $Rules.Keys) {
    if ([regex]::IsMatch($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
      $rel = $file.FullName.Substring($Root.Length).TrimStart("\", "/")
      $violations += [pscustomobject]@{
        File = $rel
        Pattern = $pattern
        Reason = $Rules[$pattern]
      }
    }
  }
}

if ($violations.Count -gt 0) {
  Write-Host "SAFETY SCAN FAILED — $($violations.Count) violation(s)" -ForegroundColor Red
  $violations | Format-Table -AutoSize | Out-String | Write-Host
  exit 1
}

Write-Host "SAFETY SCAN PASSED — scanned $($files.Count) files, 0 violations" -ForegroundColor Green
exit 0
