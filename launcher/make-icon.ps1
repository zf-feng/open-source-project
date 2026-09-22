# =====================================================================
# 生成启动器图标 launcher\app.ico（256x256，PNG-in-ICO）。
# 源图优先使用项目根目录的「星尘地牢.png」；没有时使用
# src\main\resources\sprites\ui\game_icon.png（256x256 版）。
# 用法：更换图标时，把新图放到项目根目录并命名为「星尘地牢.png」，
# 双击运行本脚本，再执行 build-launcher.bat 重建 exe。
# 说明：app.ico 已随仓库提交，正常无需重新生成。
# =====================================================================
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $here
$sourcePng = Join-Path $root '星尘地牢.png'
if (-not (Test-Path $sourcePng)) {
    $sourcePng = Join-Path $root 'src\main\resources\sprites\ui\game_icon.png'
}
if (-not (Test-Path $sourcePng)) { Write-Host '未找到游戏图标素材（星尘地牢.png），图标生成失败。'; exit 1 }

Add-Type -AssemblyName System.Drawing

$size = 256
$image = [System.Drawing.Image]::FromFile($sourcePng)
try {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($image, 0, 0, $size, $size)
    } finally {
        $g.Dispose()
    }

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngBytes = $ms.ToArray()
    $ms.Dispose()
    $bmp.Dispose()
} finally {
    $image.Dispose()
}

# 组装 ICO 文件（单张 256x256 PNG 图像，Windows Vista 及以上原生支持）
$icoPath = Join-Path $here 'app.ico'
$stream = [System.IO.File]::Create($icoPath)
$writer = New-Object System.IO.BinaryWriter($stream)
try {
    $writer.Write([UInt16]0)                  # reserved
    $writer.Write([UInt16]1)                  # type = icon
    $writer.Write([UInt16]1)                  # image count
    $writer.Write([Byte]0)                    # width 0 = 256
    $writer.Write([Byte]0)                    # height 0 = 256
    $writer.Write([Byte]0)                    # palette count
    $writer.Write([Byte]0)                    # reserved
    $writer.Write([UInt16]1)                  # color planes
    $writer.Write([UInt16]32)                 # bits per pixel
    $writer.Write([UInt32]$pngBytes.Length)   # image data size
    $writer.Write([UInt32]22)                 # image data offset
    $writer.Write($pngBytes)
} finally {
    $writer.Close()
    $stream.Close()
}
Write-Host "已生成图标: $icoPath （PNG 数据 $($pngBytes.Length) 字节）"
