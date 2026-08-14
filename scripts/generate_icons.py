#!/usr/bin/env python3
"""
Generate Android launcher icon assets and a preview sheet for PocketRate.

Prerequisites:
    pip install Pillow

Usage:
    python generate_icons.py

Input:
    Place your source logo at:
        C:/Users/regan/Downloads/PocketRate Logo.png

Output:
    C:/Users/regan/OneDrive/Kimi/PocketRate/icons/
        preview.png                 - overview sheet
        light/                      - white background + black logo
            adaptive_foreground.png
            adaptive_background.png
            legacy/mdpi...xxxhdpi/ic_launcher.png + ic_launcher_round.png
            play_store/ic_launcher_play_store.png
        dark/                       - dark background + white logo (reversed)
            adaptive_foreground.png
            adaptive_background.png
            legacy/mdpi...xxxhdpi/ic_launcher.png + ic_launcher_round.png
            play_store/ic_launcher_play_store.png
"""

from __future__ import annotations

import math
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Tuple

from PIL import Image, ImageDraw, ImageFilter, ImageOps

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

SOURCE_LOGO = Path("C:/Users/regan/Downloads/PocketRate Logo.png")
OUTPUT_DIR = Path("C:/Users/regan/OneDrive/Kimi/PocketRate/icons")

# Android adaptive icon canvas: 108 dp @ 6x raster = 660 px
ADAPTIVE_SIZE = 660
SAFE_ZONE_SIZE = 404  # 66 dp safe zone @ 6x

# Legacy icon sizes
LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Theme variants to generate.
# Each entry: (variant_name, background_hex, foreground_hex)
# "light"  = white background + black logo
# "dark"   = dark background + white logo (reversed)
THEME_VARIANTS = [
    ("light", "#FFFFFF", "#000000"),
    ("dark", "#121212", "#FFFFFF"),
]


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class RGBA:
    r: int
    g: int
    b: int
    a: int

    @classmethod
    def from_hex(cls, hex_color: str) -> "RGBA":
        hex_color = hex_color.lstrip("#")
        if len(hex_color) == 6:
            return cls(
                int(hex_color[0:2], 16),
                int(hex_color[2:4], 16),
                int(hex_color[4:6], 16),
                255,
            )
        if len(hex_color) == 8:
            return cls(
                int(hex_color[0:2], 16),
                int(hex_color[2:4], 16),
                int(hex_color[4:6], 16),
                int(hex_color[6:8], 16),
            )
        raise ValueError(f"Invalid hex color: {hex_color}")


def hex_to_rgb_tuple(hex_color: str) -> Tuple[int, int, int]:
    c = RGBA.from_hex(hex_color)
    return (c.r, c.g, c.b)


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def make_square(img: Image.Image, bgcolor: Tuple[int, int, int, int] = (255, 255, 255, 0)) -> Image.Image:
    """Pad an image to a square using the given background color."""
    w, h = img.size
    size = max(w, h)
    canvas = Image.new("RGBA", (size, size), bgcolor)
    canvas.paste(img, ((size - w) // 2, (size - h) // 2), img)
    return canvas


def remove_white_background(img: Image.Image, threshold: int = 240) -> Image.Image:
    """Make near-white pixels transparent."""
    img = img.convert("RGBA")
    data = img.getdata()
    new_data = []
    for r, g, b, a in data:
        if r > threshold and g > threshold and b > threshold:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append((r, g, b, a))
    img.putdata(new_data)
    return img


def thicken_lines(img: Image.Image, iterations: int = 2) -> Image.Image:
    """Dilate the alpha channel to make thin lines thicker."""
    # Split alpha channel
    r, g, b, a = img.split()
    # Dilate alpha
    for _ in range(iterations):
        a = a.filter(ImageFilter.MaxFilter(size=3))
    return Image.merge("RGBA", (r, g, b, a))


def colorize_foreground(img: Image.Image, hex_color: str) -> Image.Image:
    """Recolor non-transparent pixels to the given hex color."""
    target = RGBA.from_hex(hex_color)
    data = img.getdata()
    new_data = []
    for r, g, b, a in data:
        if a == 0:
            new_data.append((0, 0, 0, 0))
        else:
            # Preserve alpha, replace color
            new_data.append((target.r, target.g, target.b, a))
    img.putdata(new_data)
    return img


def center_in_canvas(img: Image.Image, canvas_size: int, margin_ratio: float = 0.12) -> Image.Image:
    """Center the image on a transparent canvas, leaving a margin."""
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    available = int(canvas_size * (1 - 2 * margin_ratio))
    img = make_square(img)
    img = img.resize((available, available), Image.Resampling.LANCZOS)
    offset = (canvas_size - available) // 2
    canvas.paste(img, (offset, offset), img)
    return canvas


def apply_circular_mask(img: Image.Image) -> Image.Image:
    """Apply a circular mask (for round legacy icons)."""
    size = img.size[0]
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(img, (0, 0), mask)
    return result


def rounded_square_mask(size: int, radius_ratio: float = 0.2) -> Image.Image:
    """Create a rounded rectangle mask similar to Android legacy icons."""
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    radius = int(size * radius_ratio)
    draw.rounded_rectangle((0, 0, size, size), radius=radius, fill=255)
    return mask


def apply_rounded_square_mask(img: Image.Image, radius_ratio: float = 0.2) -> Image.Image:
    size = img.size[0]
    mask = rounded_square_mask(size, radius_ratio)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(img, (0, 0), mask)
    return result


def build_foreground(source: Image.Image, foreground_hex: str) -> Image.Image:
    """Create the adaptive-icon foreground from the source logo."""
    fg = remove_white_background(source)
    fg = make_square(fg)
    # Thicken a bit so it survives downscaling
    fg = thicken_lines(fg, iterations=2)
    # Apply the requested foreground color
    fg = colorize_foreground(fg, foreground_hex)
    # Center in adaptive canvas with safe-zone padding
    fg = center_in_canvas(fg, ADAPTIVE_SIZE, margin_ratio=0.18)
    return fg


def build_background(size: int, hex_color: str) -> Image.Image:
    """Create a solid-color background."""
    color = hex_to_rgb_tuple(hex_color)
    return Image.new("RGB", (size, size), color)


def composite_adaptive(foreground: Image.Image, background: Image.Image) -> Image.Image:
    """Overlay foreground on background."""
    bg = background.convert("RGBA")
    bg.paste(foreground, (0, 0), foreground)
    return bg


def generate_legacy_icon(
    foreground: Image.Image,
    background: Image.Image,
    size: int,
    round: bool = False,
) -> Image.Image:
    """Generate a legacy launcher icon at a given density."""
    fg = foreground.resize((size, size), Image.Resampling.LANCZOS)
    bg = background.resize((size, size), Image.Resampling.LANCZOS)
    icon = composite_adaptive(fg, bg)
    if round:
        icon = apply_circular_mask(icon)
    else:
        icon = apply_rounded_square_mask(icon)
    return icon


def build_preview_sheet(
    variants: list[tuple[str, Image.Image, str, str]],
) -> Image.Image:
    """Build a preview image showing each theme variant."""
    preview_size = 256
    cols = 2
    rows = math.ceil(len(variants) / cols)
    sheet_w = cols * (preview_size + 40) + 40
    sheet_h = rows * (preview_size + 80) + 40

    sheet = Image.new("RGB", (sheet_w, sheet_h), (240, 240, 240))
    draw = ImageDraw.Draw(sheet)

    def paste_preview(img: Image.Image, row: int, col: int, label: str) -> None:
        x = 40 + col * (preview_size + 40)
        y = 40 + row * (preview_size + 80)
        preview = img.resize((preview_size, preview_size), Image.Resampling.LANCZOS)
        sheet.paste(preview, (x, y))
        draw.text((x, y + preview_size + 10), label, fill=(30, 30, 30))

    for idx, (name, foreground, bg_hex, fg_hex) in enumerate(variants):
        row = idx // cols
        col = idx % cols
        bg = build_background(preview_size, bg_hex)
        icon = composite_adaptive(
            foreground.resize((preview_size, preview_size), Image.Resampling.LANCZOS),
            bg,
        )
        paste_preview(icon, row, col, f"{name} — bg {bg_hex}, fg {fg_hex}")

    return sheet


def main() -> int:
    if not SOURCE_LOGO.exists():
        print(f"Source logo not found: {SOURCE_LOGO}")
        print("Please place your logo at that path, then rerun this script.")
        return 1

    try:
        source = Image.open(SOURCE_LOGO)
    except Exception as e:
        print(f"Could not open source logo: {e}")
        return 1

    print(f"Loaded source logo: {source.size}")
    ensure_dir(OUTPUT_DIR)

    variants: list[tuple[str, Image.Image, str, str]] = []

    for variant_name, bg_hex, fg_hex in THEME_VARIANTS:
        print(f"\n--- Generating '{variant_name}' variant ---")
        variant_dir = OUTPUT_DIR / variant_name
        ensure_dir(variant_dir)

        # Build foreground for this variant
        foreground = build_foreground(source, fg_hex)
        fg_path = variant_dir / "adaptive_foreground.png"
        foreground.save(fg_path)
        print(f"Saved foreground: {fg_path}")

        # Build background
        background = build_background(ADAPTIVE_SIZE, bg_hex)
        bg_path = variant_dir / "adaptive_background.png"
        background.save(bg_path)
        print(f"Saved background: {bg_path}")

        # Legacy icons
        for density, size in LEGACY_SIZES.items():
            density_dir = variant_dir / "legacy" / density
            ensure_dir(density_dir)

            square = generate_legacy_icon(foreground, background, size, round=False)
            square.save(density_dir / "ic_launcher.png")

            round_icon = generate_legacy_icon(foreground, background, size, round=True)
            round_icon.save(density_dir / "ic_launcher_round.png")

            print(f"Saved legacy icons: {density_dir}")

        # Play Store icon
        play_store_dir = variant_dir / "play_store"
        ensure_dir(play_store_dir)
        play_store = composite_adaptive(
            foreground.resize((512, 512), Image.Resampling.LANCZOS),
            build_background(512, bg_hex),
        )
        play_store.save(play_store_dir / "ic_launcher_play_store.png")
        print(f"Saved Play Store icon: {play_store_dir / 'ic_launcher_play_store.png'}")

        variants.append((variant_name, foreground, bg_hex, fg_hex))

    # Generate preview sheet
    preview = build_preview_sheet(variants)
    preview_path = OUTPUT_DIR / "preview.png"
    preview.save(preview_path)
    print(f"\nSaved preview sheet: {preview_path}")

    print("\nDone. Review the preview.png.")
    print("Pick the 'light' or 'dark' variant and I'll apply it to the Android project.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
