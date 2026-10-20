# Generates legacy launcher PNGs (API 24/25) from web/src-tauri/app-icon.png.
# API 26+ uses the adaptive vector XML (mipmap-anydpi-v26); these PNGs are the
# fallback for minSdk 24-25 devices that predate adaptive icons.
#
# Run:  powershell -File scripts/gen-android-icons.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$srcPath = Join-Path $root 'web\src-tauri\app-icon.png'
if (-not (Test-Path $srcPath)) {
    throw "Source icon not found at $srcPath - run scripts\gen-icon.ps1 first."
}
# NOTE: the Android module lives under android/app, not app/ at the repo root.
$resDir = Join-Path $root 'android\app\src\main\res'
if (-not (Test-Path $resDir)) {
    throw "Android res dir not found at $resDir - check the project layout."
}

# density -> px size for a 48dp launcher icon
$densities = [ordered]@{
    'mipmap-mdpi'    = 48
    'mipmap-hdpi'    = 72
    'mipmap-xhdpi'   = 96
    'mipmap-xxhdpi'  = 144
    'mipmap-xxxhdpi' = 192
}

$src = New-Object System.Drawing.Bitmap($srcPath)

try {
    foreach ($d in $densities.Keys) {
        $px = $densities[$d]
        $dir = Join-Path $resDir $d
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

        # --- Square icon ---
        $bmp = New-Object System.Drawing.Bitmap($px, $px)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($src, 0, 0, $px, $px)
        $g.Dispose()
        $bmp.Save((Join-Path $dir 'ic_launcher.png'), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()

        # --- Round icon: circular mask over the same artwork ---
        $r = New-Object System.Drawing.Bitmap($px, $px)
        $rg = [System.Drawing.Graphics]::FromImage($r)
        $rg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $rg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $rg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $rg.Clear([System.Drawing.Color]::Transparent)

        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $px, $px)
        $rg.SetClip($path)
        $rg.DrawImage($src, 0, 0, $px, $px)
        $rg.Dispose()
        $r.Save((Join-Path $dir 'ic_launcher_round.png'), [System.Drawing.Imaging.ImageFormat]::Png)
        $r.Dispose()

        Write-Host ("  {0,-18} {1,3}px  square + round" -f $d, $px)
    }
    Write-Host "OK  legacy launcher icons written to $resDir"
}
finally {
    $src.Dispose()
}
