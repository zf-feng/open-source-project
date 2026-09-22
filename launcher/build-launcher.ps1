# =====================================================================
# 重新编译一键启动器「星尘地牢.exe」
# 使用 Windows 自带的 .NET Framework 编译器（csc.exe），无需安装任何工具。
# 用法：双击 launcher\build-launcher.bat，或执行 .\build-launcher.ps1
# 说明：仅在修改了 StardustLauncher.cs 或换图标后需要重新运行。
# =====================================================================
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $here

$csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (-not (Test-Path $csc)) {
    $csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe'
}
if (-not (Test-Path $csc)) {
    Write-Host '未找到 Windows 自带的 .NET 编译器 csc.exe，无法重建启动器。'
    exit 1
}

$source = Join-Path $here 'StardustLauncher.cs'
$icon = Join-Path $here 'app.ico'
if (-not (Test-Path $source)) { Write-Host "未找到源码: $source"; exit 1 }
if (-not (Test-Path $icon)) {
    Write-Host '未找到图标 app.ico，正在自动生成...'
    & (Join-Path $here 'make-icon.ps1')
}

$outExe = Join-Path $root '星尘地牢.exe'

$arguments = @(
    '/nologo'
    '/target:winexe'
    '/optimize+'
    '/codepage:65001'
    "/out:$outExe"
    "/win32icon:$icon"
    '/r:System.Windows.Forms.dll'
    '/r:System.Drawing.dll'
    '/r:System.IO.Compression.dll'
    '/r:System.IO.Compression.FileSystem.dll'
    $source
)

& $csc $arguments
if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败（csc 退出码 $LASTEXITCODE）"
    exit $LASTEXITCODE
}

$sizeKb = [Math]::Round((Get-Item $outExe).Length / 1024, 1)
Write-Host "已生成启动器: $outExe （$sizeKb KB）"
