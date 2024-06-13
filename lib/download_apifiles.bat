:: REF: https://sdpf.ntt.com/services/docs/icms/service-descriptions/applet/sample_applet/sample_applet.html#/api-usim-apiuicc-api
::
:: download *.exp, *.jar, and JavaDoc from 3GPP and ETSI and extract them
::

@echo off
SETLOCAL

cd %~dp0

:: 3GPP TS 31.130 v13.0.0
curl -fOL https://www.3gpp.org/ftp/Specs/archive/31_series/31.130/31130-d30.zip
if ERRORLEVEL 1 exit /b


:: ETSI TS 102 241 v13.0.0
curl -fOL https://www.etsi.org/deliver/etsi_ts/102200_102299/102241/13.00.00_60/ts_102241v130000p0.zip
if ERRORLEVEL 1 exit /b


for %%F in (%~dp0\*.zip) do (
  powershell -command "Expand-Archive -Force '%%F'"
)

ENDLOCAL
