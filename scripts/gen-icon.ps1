# Generates web/src-tauri/app-icon.png (1024x1024 source for `tauri icon`).
# Brand-matched to web/src/styles.css: bg #0b0d12, accent #e50914.
# Run:  powershell -File scripts/gen-icon.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$out = Join-Path $PSScriptRoot '..\web\src-tauri\app-icon.png'
$out = [System.IO.Path]::GetFullPath($out)
$size = 1024

$bmp = New-Object System.Drawing.Bitmap($size, $size)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

function New-RoundRectPath([int]$x, [int]$y, [int]$w, [int]$h, [int]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

try {
    # 1. Brand background.
    $bg = New-Object System.Drawing.SolidBrush(
        [System.Drawing.Color]::FromArgb(255, 0x0b, 0x0d, 0x12))
    $g.FillRectangle($bg, 0, 0, $size, $size)

    # 2. Accent rounded-square tile with a vertical gradient (crisper at 32px
    #    than a flat fill, and keeps a solid silhouette when downsampled).
    $tileRect = New-Object System.Drawing.Rectangle(112, 112, 800, 800)
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $tileRect,
        [System.Drawing.Color]::FromArgb(255, 0xe5, 0x09, 0x14),  # #e50914
        [System.Drawing.Color]::FromArgb(255, 0x8a, 0x04, 0x09),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal)
    $tilePath = New-RoundRectPath 112 112 800 800 176
    $g.FillPath($grad, $tilePath)

    # 3. Play triangle, optically centred (shifted right for visual balance).
    $pts = [System.Drawing.Point[]]@(
        (New-Object System.Drawing.Point(414, 306)),
        (New-Object System.Drawing.Point(414, 718)),
        (New-Object System.Drawing.Point(742, 512))
    )
    $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillPolygon($white, $pts)

    # 4. Subtle inner highlight along the tile top edge for depth.
    $pen = New-Object System.Drawing.Pen(
        ([System.Drawing.Color]::FromArgb(70, 255, 255, 255)), 6)
    $g.DrawPath($pen, $tilePath)

    $dir = [System.IO.Path]::GetDirectoryName($out)
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

    Write-Host "OK  $out"
    Write-Host "    $((Get-Item $out).Length) bytes, ${size}x${size}"
}
finally {
    $g.Dispose()
    $bmp.Dispose()
}
