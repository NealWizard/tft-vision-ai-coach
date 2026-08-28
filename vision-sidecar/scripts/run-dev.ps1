$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:PYTHONPATH = $root
python -m uvicorn app.main:app --host 127.0.0.1 --port 19090
