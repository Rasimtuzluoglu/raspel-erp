# Marka gorsellerini logo/ klasorunden uretir ve frontend/public'e yazar.
# Harici bagimlilik gerektirmez; Windows System.Drawing kullanir.
#
# Kullanim: powershell -ExecutionPolicy Bypass -File scripts/generate-brand-assets.ps1

param(
    [string]$LogoDir = (Join-Path $PSScriptRoot '..\logo'),
    [string]$OutDir = (Join-Path $PSScriptRoot '..\frontend\public')
)

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$interp = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

function Get-ContentBounds {
    param([string]$Path)

    $bmp = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
        $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $bytes = New-Object byte[] ($data.Stride * $bmp.Height)
            [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
        } finally {
            $bmp.UnlockBits($data)
        }

        $w = $bmp.Width
        $h = $bmp.Height
        $stride = $data.Stride
        $minX = $w; $minY = $h; $maxX = -1; $maxY = -1

        for ($y = 0; $y -lt $h; $y++) {
            $row = $y * $stride
            for ($x = 0; $x -lt $w; $x++) {
                if ($bytes[$row + ($x * 4) + 3] -gt 8) {
                    if ($x -lt $minX) { $minX = $x }
                    if ($x -gt $maxX) { $maxX = $x }
                    if ($y -lt $minY) { $minY = $y }
                    if ($y -gt $maxY) { $maxY = $y }
                }
            }
        }

        if ($maxX -lt 0) {
            return New-Object System.Drawing.Rectangle(0, 0, $w, $h)
        }
        return New-Object System.Drawing.Rectangle($minX, $minY, ($maxX - $minX + 1), ($maxY - $minY + 1))
    } finally {
        $bmp.Dispose()
    }
}

function New-SquarePadded {
    param(
        [System.Drawing.Image]$Source,
        [System.Drawing.Rectangle]$Bounds,
        [double]$PadRatio = 0.08
    )

    $side = [int][Math]::Ceiling([Math]::Max($Bounds.Width, $Bounds.Height) * (1 + 2 * $PadRatio))
    $canvas = New-Object System.Drawing.Bitmap($side, $side, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    try {
        $g.InterpolationMode = $interp
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $dx = [int](($side - $Bounds.Width) / 2)
        $dy = [int](($side - $Bounds.Height) / 2)
        $dest = New-Object System.Drawing.Rectangle($dx, $dy, $Bounds.Width, $Bounds.Height)
        $g.DrawImage($Source, $dest, $Bounds, [System.Drawing.GraphicsUnit]::Pixel)
    } finally {
        $g.Dispose()
    }
    return $canvas
}

function Save-PngSquare {
    param([System.Drawing.Image]$Source, [int]$Size, [string]$Path)

    $bmp = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.InterpolationMode = $interp
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($Source, 0, 0, $Size, $Size)
    } finally {
        $g.Dispose()
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Save-PngWidth {
    param([System.Drawing.Image]$Source, [int]$Width, [string]$Path)

    $height = [int][Math]::Round($Source.Height * $Width / $Source.Width)
    $bmp = New-Object System.Drawing.Bitmap($Width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.InterpolationMode = $interp
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($Source, 0, 0, $Width, $height)
    } finally {
        $g.Dispose()
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Save-IcoFromSquare {
    param([System.Drawing.Image]$Source, [int]$Size, [string]$Path)

    $bmp = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.InterpolationMode = $interp
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.DrawImage($Source, 0, 0, $Size, $Size)
    } finally {
        $g.Dispose()
    }

    $hicon = $bmp.GetHicon()
    try {
        $icon = [System.Drawing.Icon]::FromHandle($hicon)
        $fs = [System.IO.File]::Create($Path)
        try { $icon.Save($fs) } finally { $fs.Dispose() }
        $icon.Dispose()
    } finally {
        $bmp.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}

# --- Ikonyon (amblem) ---
$iconPath = Join-Path $LogoDir 'raspelicon-logo.png'
$iconBounds = Get-ContentBounds -Path $iconPath
$iconSrc = [System.Drawing.Image]::FromFile($iconPath)
$iconSquare = New-SquarePadded -Source $iconSrc -Bounds $iconBounds
$iconSrc.Dispose()

Save-PngSquare -Source $iconSquare -Size 512 -Path (Join-Path $OutDir 'icon-512.png')
Save-PngSquare -Source $iconSquare -Size 192 -Path (Join-Path $OutDir 'icon-192.png')
Save-PngSquare -Source $iconSquare -Size 256 -Path (Join-Path $OutDir 'logo-icon.png')
Save-PngSquare -Source $iconSquare -Size 32  -Path (Join-Path $OutDir 'favicon.png')
Save-IcoFromSquare -Source $iconSquare -Size 32 -Path (Join-Path $OutDir 'favicon.ico')
$iconSquare.Dispose()

# --- Tam logo (amblem + yazi) ---
$fullPath = Join-Path $LogoDir 'raspel-logo.png'
$fullBounds = Get-ContentBounds -Path $fullPath
$fullSrc = [System.Drawing.Image]::FromFile($fullPath)
$fullSquare = New-SquarePadded -Source $fullSrc -Bounds $fullBounds -PadRatio 0.04
$fullSrc.Dispose()
Save-PngWidth -Source $fullSquare -Width 640 -Path (Join-Path $OutDir 'logo-full.png')
$fullSquare.Dispose()

Write-Host "Marka gorselleri uretildi -> $OutDir"
Get-ChildItem -LiteralPath $OutDir -Filter '*.png' | Where-Object { $_.Name -match 'icon-|logo-|favicon' } | ForEach-Object {
    Write-Host ("  {0}  {1:N0} B" -f $_.Name, $_.Length)
}
