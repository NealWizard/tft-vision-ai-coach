# Local MkDocs preview
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

if (-not (Get-Command pip -ErrorAction SilentlyContinue)) {
    throw "pip not found. Install Python 3 first."
}

pip install -q -r requirements-docs.txt
Write-Host "Wiki preview: http://127.0.0.1:8000"
mkdocs serve
