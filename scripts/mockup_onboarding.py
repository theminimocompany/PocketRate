#!/usr/bin/env python3
"""
Generate a mockup of the current PocketRate onboarding screen.

Run with the project Python:
    C:/Users/regan/OneDrive/Kimi/PocketRate/.tools/python/python.exe mockup_onboarding.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUTPUT = Path("C:/Users/regan/OneDrive/Kimi/PocketRate/onboarding_mockup.png")
WIDTH, HEIGHT = 400, 800

# Colors (light mode, approximating Material 3)
BG = (247, 245, 242)       # Paper / background
SURFACE = (255, 255, 255)  # Card background
OUTLINE = (232, 228, 224)  # Card border
PRIMARY = (122, 158, 126)  # Sage primary
ON_BG = (46, 46, 46)       # Ink
ON_SURFACE_VAR = (107, 107, 107)
PRIMARY_CONTAINER = (232, 240, 233)
ON_PRIMARY_CONTAINER = (46, 80, 50)


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    """Try to load a system font; fall back to default bitmap font."""
    candidates = [
        ("C:/Windows/Fonts/segoeuib.ttf", True),
        ("C:/Windows/Fonts/segoeui.ttf", False),
        ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", True),
        ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", False),
    ]
    for path, is_bold in candidates:
        if Path(path).exists() and bold == is_bold:
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                pass
    return ImageFont.load_default()


def rounded_rectangle(draw: ImageDraw.ImageDraw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_icon(draw: ImageDraw.ImageDraw, cx: int, cy: int, name: str) -> None:
    """Draw a simple line icon centered at (cx, cy)."""
    size = 24
    x, y = cx - size // 2, cy - size // 2
    color = ON_PRIMARY_CONTAINER
    if name == "convert":
        # Two arrows crossing
        draw.line([(x, y + size // 2), (x + size, y + size // 2)], fill=color, width=2)
        draw.polygon([(x + size - 6, y + size // 2 - 4), (x + size, y + size // 2), (x + size - 6, y + size // 2 + 4)], fill=color)
        draw.polygon([(x + 6, y + size // 2 - 4), (x, y + size // 2), (x + 6, y + size // 2 + 4)], fill=color)
    elif name == "luggage":
        # Suitcase outline
        draw.rounded_rectangle([x + 4, y + 4, x + size - 4, y + size - 4], radius=3, outline=color, width=2)
        draw.line([(x + size // 2, y), (x + size // 2, y + 4)], fill=color, width=2)
    elif name == "info":
        # Lowercase i in circle
        r = size // 2
        draw.ellipse([x, y, x + size, y + size], outline=color, width=2)
        draw.line([(cx, cy - 2), (cx, cy + 6)], fill=color, width=2)
        draw.ellipse([cx - 1, cy - 6, cx + 1, cy - 4], fill=color)


def main() -> None:
    img = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(img)

    font_title = load_font(32, bold=True)
    font_card_title = load_font(16, bold=True)
    font_card_body = load_font(14)
    font_button = load_font(14, bold=True)

    # App title
    draw.text((WIDTH // 2, 80), "PocketRate", font=font_title, fill=ON_BG, anchor="mm")

    # Cards
    cards = [
        ("convert", "Free Currency Converter", "Convert 160+ currencies with cached offline rates."),
        ("luggage", "Trip Expenses", "Track spending in any currency per trip and split costs with friends."),
        ("info", "Ad-Supported", "PocketRate is free forever. Watch a quick ad to remove banners for 24 hours."),
    ]

    card_w = WIDTH - 40
    card_h = 110
    card_x = 20
    start_y = 160
    gap = 16

    for i, (icon_name, title, desc) in enumerate(cards):
        y = start_y + i * (card_h + gap)

        # Card background with border
        rounded_rectangle(draw, [card_x, y, card_x + card_w, y + card_h], radius=20, fill=SURFACE, outline=OUTLINE, width=1)

        # Icon container
        icon_cx = card_x + 42
        icon_cy = y + card_h // 2
        draw.rounded_rectangle([icon_cx - 24, icon_cy - 24, icon_cx + 24, icon_cy + 24], radius=12, fill=PRIMARY_CONTAINER)
        draw_icon(draw, icon_cx, icon_cy, icon_name)

        # Title
        draw.text((card_x + 84, y + 26), title, font=font_card_title, fill=ON_BG)

        # Description (wrap manually into two lines)
        words = desc.split()
        lines = []
        line = ""
        for word in words:
            test = line + " " + word if line else word
            bbox = draw.textbbox((0, 0), test, font=font_card_body)
            if bbox[2] - bbox[0] <= card_w - 110:
                line = test
            else:
                if line:
                    lines.append(line)
                line = word
        if line:
            lines.append(line)

        for j, line in enumerate(lines[:2]):
            draw.text((card_x + 84, y + 54 + j * 20), line, font=font_card_body, fill=ON_SURFACE_VAR)

    # Button
    btn_h = 48
    btn_y = HEIGHT - 110
    draw.rounded_rectangle([20, btn_y, WIDTH - 20, btn_y + btn_h], radius=12, fill=PRIMARY)
    draw.text((WIDTH // 2, btn_y + btn_h // 2), "GET STARTED", font=font_button, fill=(255, 255, 255), anchor="mm")

    img.save(OUTPUT)
    print(f"Saved onboarding mockup to {OUTPUT}")


if __name__ == "__main__":
    main()
