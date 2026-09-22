# =====================================================================
# 生成干净的“源码版”发布压缩包（含一键启动器），排除开发与运行时目录。
# 用法：在项目根目录双击 打包发布.bat，或执行本脚本。
# 产物：项目根目录「星尘地牢-源码版.zip」（可直接发给朋友）
# =====================================================================
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $here
$zipPath = Join-Path $root '星尘地牢-源码版.zip'

if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$archive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $level = [System.IO.Compression.CompressionLevel]::Optimal

    # 源码与启动器目录（整目录打包）
    foreach ($dirName in @('src', 'launcher')) {
        $dirPath = Join-Path $root $dirName
        if (-not (Test-Path $dirPath)) { continue }
        Get-ChildItem $dirPath -Recurse -File | ForEach-Object {
            $entryName = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
            [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $entryName, $level)
        }
    }

    # 根目录说明与配置文件
    foreach ($fileName in @('README.md', 'pom.xml', '.gitignore', '总类图源码.mermaid', '总类图（已根据实际情况修改）.png')) {
        $filePath = Join-Path $root $fileName
        if (Test-Path $filePath) {
            [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $filePath, $fileName, $level)
        }
    }

    # 一键启动器 exe
    Get-ChildItem $root -Filter '*.exe' -File | ForEach-Object {
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $_.Name, $level)
    }
} finally {
    $archive.Dispose()
}

$sizeKb = [Math]::Round((Get-Item $zipPath).Length / 1024, 1)
Write-Host "已生成: $zipPath （$sizeKb KB）"
