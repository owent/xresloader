
$PSDefaultParameterValues['*:Encoding'] = 'UTF-8'

$OutputEncoding = [System.Text.UTF8Encoding]::new()

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition

Set-Location $SCRIPT_DIR

mvn package

# $P4XResloaderDir = "C:/workspace/pracing/BR_PRacing_Dev/Common/Excel/xresloader/target"
# $P4XResloaderDir = "C:\workspace\pracing\SGCommon\MainCommon\Excel\xresloader\target"
$P4XResloaderDir = "C:\workspace\pracing\SGCommon_Dev\MainCommon\Excel\xresloader\target"
$XRESLOADER = Get-ChildItem -Path "./target" -Filter "xresloader-*-pracing.jar" -Name | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Copy-Item ./target/$XRESLOADER $P4XResloaderDir
