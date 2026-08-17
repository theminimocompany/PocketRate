#!/usr/bin/env python3
"""
Apply the new PocketRate logo (full-color squircle design) to the project.

Source: a presentation JPEG containing the icon (navy squircle with
orange/white exchange-loop arcs around a euro sign). This script:
  1. Locates the navy squircle and samples its color.
  2. Extracts the arcs + euro as a transparent adaptive-icon foreground.
  3. Writes adaptive layers + legacy density icons + Play Store icon.

Usage: python scripts/apply_new_logo.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SOURCE = Path("C:/Users/user/.kimi-work/logo/1.jpeg")
PROJECT = Path("C:/Users/user/OneDrive/Kimi/PocketRate")
RES = PROJECT / "app/src/main/res"
PREVIEW_OUT = Path("C:/Users/user/.kimi-work/logo_preview.png")

ADAPTIVE_SIZE = 432          # adaptive icon layer canvas
SAFE_CONTENT = 300           # foreground content box (inside the 66dp safe circle)
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

NAVY_TOLERANCE = 45          # color distance for "is squircle navy"


def color_dist(px, ref):
    return abs(px[0] - ref[0]) + abs(px[1] - ref[1]) + abs(px[2] - ref[2])


def main() -> int:
    img = Image.open(SOURCE).convert("RGB")
    w, h = img.size
    print(f"source: {img.size}")

    # 1. Sample navy inside the squircle (upper-left interior, off the artwork)
    navy = img.getpixel((int(w * 0.27), int(h * 0.22)))
    print(f"sampled navy: {navy}")

    # 2. Bounding box of the squircle = pixels close to navy
    min_x, min_y, max_x, max_y = w, h, 0, 0
    px = img.load()
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            if color_dist(px[x, y], navy) < NAVY_TOLERANCE:
                if x < min_x: min_x = x
                if x > max_x: max_x = x
                if y < min_y: min_y = y
                if y > max_y: max_y = y
    if max_x == 0:
        print("ERROR: squircle not found")
        return 1
    bbox = (min_x, min_y, max_x + 2, max_y + 2)
    print(f"squircle bbox: {bbox}, size {max_x - min_x}x{max_y - min_y}")
    squircle = img.crop(bbox)

    # 3. Foreground mask: orange arcs + white arc/euro, inside the squircle's
    # inscribed circle (the rounded corners of the crop contain page-white
    # background pixels that must not leak into the foreground).
    fg_full = Image.new("RGBA", squircle.size, (0, 0, 0, 0))
    spx = squircle.load()
    fpx = fg_full.load()
    cx, cy = squircle.size[0] / 2, squircle.size[1] / 2
    inner_r = min(squircle.size) / 2 * 0.98
    for y in range(squircle.size[1]):
        for x in range(squircle.size[0]):
            if (x - cx) ** 2 + (y - cy) ** 2 > inner_r ** 2:
                continue
            r, g, b = spx[x, y]
            is_orange = r > 170 and 50 < g < 150 and b < 90
            is_white = r > 200 and g > 200 and b > 200
            if is_orange or is_white:
                fpx[x, y] = (r, g, b, 255)
    # Close JPEG speckle, then keep only the largest blobs' region
    alpha = fg_full.split()[3].filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
    fg_full.putalpha(alpha)

    # Crop foreground to content and center on the adaptive canvas
    fbbox = fg_full.getbbox()
    fg_content = fg_full.crop(fbbox)
    scale = SAFE_CONTENT / max(fg_content.size)
    fg_scaled = fg_content.resize(
        (int(fg_content.size[0] * scale), int(fg_content.size[1] * scale)),
        Image.Resampling.LANCZOS,
    )
    foreground = Image.new("RGBA", (ADAPTIVE_SIZE, ADAPTIVE_SIZE), (0, 0, 0, 0))
    off = ((ADAPTIVE_SIZE - fg_scaled.size[0]) // 2, (ADAPTIVE_SIZE - fg_scaled.size[1]) // 2)
    foreground.paste(fg_scaled, off, fg_scaled)

    # 4. Background: solid sampled navy
    background = Image.new("RGB", (ADAPTIVE_SIZE, ADAPTIVE_SIZE), navy)

    # 5. Write adaptive layers
    (RES / "drawable").mkdir(parents=True, exist_ok=True)
    foreground.save(RES / "drawable/ic_launcher_foreground.png")
    background.save(RES / "drawable/ic_launcher_background.png")
    print("wrote drawable/ic_launcher_foreground.png + ic_launcher_background.png")

    # 6. Legacy icons: the squircle itself, per density; round = circular mask
    for density, size in LEGACY_SIZES.items():
        icon = squircle.resize((size, size), Image.Resampling.LANCZOS).convert("RGBA")
        out = RES / f"mipmap-{density}"
        out.mkdir(parents=True, exist_ok=True)
        icon.save(out / "ic_launcher.png")

        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_icon.paste(icon, (0, 0), mask)
        round_icon.save(out / "ic_launcher_round.png")
    print("wrote legacy mipmap icons (5 densities, square + round)")

    # 7. Play Store icon 512x512 (square, no mask, per Play requirements)
    play = squircle.resize((512, 512), Image.Resampling.LANCZOS)
    play_dir = PROJECT / "icons/play_store"
    play_dir.mkdir(parents=True, exist_ok=True)
    play.convert("RGB").save(play_dir / "ic_launcher_play_store.png")
    print("wrote icons/play_store/ic_launcher_play_store.png")

    # 8. Preview: adaptive as circle + squircle mask, plus a legacy size
    sheet = Image.new("RGB", (3 * 220 + 80, 300), (238, 238, 238))
    composite = background.convert("RGBA")
    composite.paste(foreground, (0, 0), foreground)
    for i, (label, masked) in enumerate([
        ("adaptive / circle", "circle"),
        ("adaptive / squircle", "squircle"),
        ("legacy 192px", None),
    ]):
        prev = composite.resize((192, 192), Image.Resampling.LANCZOS)
        if masked == "circle":
            m = Image.new("L", (192, 192), 0)
            ImageDraw.Draw(m).ellipse((0, 0, 192, 192), fill=255)
            out = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
            out.paste(prev, (0, 0), m)
            prev = out
        elif masked == "squircle":
            m = Image.new("L", (192, 192), 0)
            ImageDraw.Draw(m).rounded_rectangle((0, 0, 192, 192), radius=44, fill=255)
            out = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
            out.paste(prev, (0, 0), m)
            prev = out
        else:
            prev = squircle.resize((192, 192), Image.Resampling.LANCZOS).convert("RGBA")
        x = 40 + i * 220
        sheet.paste(prev, (x, 40), prev)
        ImageDraw.Draw(sheet).text((x, 250), label, fill=(30, 30, 30))
    sheet.save(PREVIEW_OUT)
    print(f"preview: {PREVIEW_OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
