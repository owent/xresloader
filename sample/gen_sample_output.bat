chcp 65001

where pwsh

if %ERRORLEVEL% 0 (
    goto USE_POWERSHELL_CORE
)

:USE_POWERSHELL_CORE

@pwsh.exe -NoProfile -InputFormat None -ExecutionPolicy Bypass -File gen_sample_output.ps1

exit %ERRORLEVEL%

:USE_POWERSHELL

@"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -InputFormat None -ExecutionPolicy Bypass -File gen_sample_output.ps1