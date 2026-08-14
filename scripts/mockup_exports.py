#!/usr/bin/env python3
"""
Generate mockups of the improved CSV and PDF export layouts.

Run with the project Python:
    C:/Users/regan/OneDrive/Kimi/PocketRate/.tools/python/python.exe mockup_exports.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = Path("C:/Users/regan/OneDrive/Kimi/PocketRate/export_mockups")

# Colors
WHITE = (255, 255, 255)
BG = (247, 245, 242)
INK = (46, 46, 46)
INK_MUTED = (107, 107, 107)
INK_SUBTLE = (158, 154, 150)
SURFACE = (255, 255, 255)
OUTLINE = (232, 228, 224)
PRIMARY = (122, 158, 126)
PRIMARY_LIGHT = (232, 240, 233)
SAGE = (122, 158, 126)
BLUE = (107, 138, 154)
TERRACOTTA = (199, 141, 107)
MUSTARD = (212, 168, 67)
PURPLE = (156, 122, 177)

CATEGORY_COLORS = {
    "Food": SAGE,
    "Transport": BLUE,
    "Accommodation": TERRACOTTA,
}


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
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


def draw_rounded_rect(draw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def csv_mockup() -> Image.Image:
    width, height = 900, 1100
    img = Image.new("RGB", (width, height), WHITE)
    draw = ImageDraw.Draw(img)

    font_header = load_font(22, bold=True)
    font_section = load_font(16, bold=True)
    font_body = load_font(13)
    font_mono = load_font(12)

    y = 30
    x = 40

    # Title
    draw.text((x, y), "PocketRate Export — Europe Trip", font=font_header, fill=INK)
    y += 40

    # === EXPENSES ===
    draw.text((x, y), "=== EXPENSES ===", font=font_section, fill=INK)
    y += 28

    headers = ["Date", "Description", "Category", "Payer", "Amount", "Currency", "Converted", "Home", "Rate", "Alice Share", "Bob Share", "Charlie Share", "Settlement", "Buffer"]
    col_widths = [80, 110, 95, 60, 55, 50, 60, 45, 45, 65, 65, 75, 65, 45]

    # Header row
    hx = x
    for h, cw in zip(headers, col_widths):
        draw.text((hx + 4, y), h, font=font_mono, fill=INK_MUTED)
        hx += cw
    y += 20
    draw.line([(x, y), (width - x, y)], fill=OUTLINE, width=1)
    y += 8

    rows = [
        ["2024-06-12", "Dinner", "Food", "Alice", "120.00", "EUR", "120.00", "EUR", "1.0000", "40.00", "40.00", "40.00", "120.00", "0%"],
        ["2024-06-13", "Taxi", "Transport", "Bob", "60.00", "EUR", "60.00", "EUR", "1.0000", "20.00", "20.00", "20.00", "60.00", "0%"],
        ["2024-06-13", "Hotel", "Accommodation", "Alice", "300.00", "EUR", "300.00", "EUR", "1.0000", "150.00", "150.00", "—", "300.00", "0%"],
    ]

    for row in rows:
        hx = x
        for cell, cw in zip(row, col_widths):
            draw.text((hx + 4, y), cell, font=font_mono, fill=INK)
            hx += cw
        y += 20

    y += 30

    # === COMPANION SUMMARY ===
    draw.text((x, y), "=== COMPANION SUMMARY ===", font=font_section, fill=INK)
    y += 28

    summary_headers = ["Companion", "Paid", "Owed", "Net"]
    summary_widths = [120, 100, 100, 100]
    hx = x
    for h, cw in zip(summary_headers, summary_widths):
        draw.text((hx + 4, y), h, font=font_mono, fill=INK_MUTED)
        hx += cw
    y += 20
    draw.line([(x, y), (x + sum(summary_widths), y)], fill=OUTLINE, width=1)
    y += 8

    summary_rows = [
        ["Alice", "420.00 EUR", "210.00 EUR", "+210.00 EUR"],
        ["Bob", "60.00 EUR", "210.00 EUR", "-150.00 EUR"],
        ["Charlie", "0.00 EUR", "60.00 EUR", "-60.00 EUR"],
    ]

    for row in summary_rows:
        hx = x
        for cell, cw in zip(row, summary_widths):
            color = SAGE if "+" in cell else (TERRACOTTA if "-" in cell else INK)
            draw.text((hx + 4, y), cell, font=font_mono, fill=color)
            hx += cw
        y += 20

    y += 30

    # === SETTLEMENT ===
    draw.text((x, y), "=== SETTLEMENT ===", font=font_section, fill=INK)
    y += 28

    settlement_headers = ["From", "To", "Amount", "Currency"]
    settlement_widths = [120, 120, 100, 100]
    hx = x
    for h, cw in zip(settlement_headers, settlement_widths):
        draw.text((hx + 4, y), h, font=font_mono, fill=INK_MUTED)
        hx += cw
    y += 20
    draw.line([(x, y), (x + sum(settlement_widths), y)], fill=OUTLINE, width=1)
    y += 8

    settlement_rows = [
        ["Bob", "Alice", "150.00", "EUR"],
        ["Charlie", "Alice", "60.00", "EUR"],
    ]

    for row in settlement_rows:
        hx = x
        for cell, cw in zip(row, settlement_widths):
            draw.text((hx + 4, y), cell, font=font_mono, fill=INK)
            hx += cw
        y += 20

    y += 30

    # === HOW IT WAS CALCULATED ===
    draw.text((x, y), "=== HOW IT WAS CALCULATED ===", font=font_section, fill=INK)
    y += 28

    explanation = (
        "For each expense, the payer is credited for the full amount paid. Each companion is debited for their share. "
        "A companion's share is either an equal split or a custom amount defined when the expense was added. "
        "The 'Net' column is Paid minus Owed. Positive means the person is owed money; negative means they owe money. "
        "The settlement list shows the smallest number of payments needed to balance everyone's net to zero."
    )

    # Word wrap
    words = explanation.split()
    line = ""
    for word in words:
        test = line + " " + word if line else word
        bbox = draw.textbbox((0, 0), test, font=font_body)
        if bbox[2] - bbox[0] <= width - 2 * x:
            line = test
        else:
            draw.text((x, y), line, font=font_body, fill=INK_MUTED)
            y += 20
            line = word
    if line:
        draw.text((x, y), line, font=font_body, fill=INK_MUTED)

    return img


def pdf_mockup() -> Image.Image:
    width, height = 600, 900
    img = Image.new("RGB", (width, height), WHITE)
    draw = ImageDraw.Draw(img)

    font_title = load_font(26, bold=True)
    font_header = load_font(16, bold=True)
    font_section = load_font(14, bold=True)
    font_body = load_font(12)
    font_small = load_font(11)

    x = 40
    y = 40

    # Header
    draw.text((x, y), "PocketRate Trip Report", font=font_title, fill=INK)
    y += 36
    draw.text((x, y), "Trip: Europe Trip", font=font_body, fill=INK)
    y += 20
    draw.text((x, y), "Home Currency: EUR  |  Settlement Currency: EUR", font=font_body, fill=INK_MUTED)
    y += 20
    draw.text((x, y), "Total Spent: 480.00 EUR", font=font_header, fill=INK)
    y += 35

    # Expenses section
    draw.text((x, y), "Expenses", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 130], radius=12, fill=SURFACE, outline=OUTLINE)

    # Table headers
    table_y = y + 14
    headers = ["Date", "Description", "Payer", "Original", "Split"]
    col_x = [x + 10, x + 90, x + 230, x + 310, x + 400]
    for cx, h in zip(col_x, headers):
        draw.text((cx, table_y), h, font=font_small, fill=INK_MUTED)
    table_y += 22
    draw.line([(x + 10, table_y - 4), (width - x - 10, table_y - 4)], fill=OUTLINE, width=1)

    expenses = [
        ("Jun 12", "Dinner", "Food", "Alice", "120.00 EUR", "Alice 40, Bob 40, Charlie 40"),
        ("Jun 13", "Taxi", "Transport", "Bob", "60.00 EUR", "Alice 20, Bob 20, Charlie 20"),
        ("Jun 13", "Hotel", "Accommodation", "Alice", "300.00 EUR", "Alice 150, Bob 150"),
    ]

    for date, desc, category, payer, amount, split in expenses:
        draw.text((col_x[0], table_y), date, font=font_small, fill=INK)
        draw.text((col_x[1], table_y), desc, font=font_small, fill=INK)
        draw.text((col_x[2], table_y), payer, font=font_small, fill=INK)
        draw.text((col_x[3], table_y), amount, font=font_small, fill=INK)
        draw.text((col_x[4], table_y), split, font=font_small, fill=INK_MUTED)
        table_y += 24

    y += 150

    # Companion summary
    draw.text((x, y), "Companion Summary", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 110], radius=12, fill=SURFACE, outline=OUTLINE)

    summary_y = y + 14
    sum_headers = ["Companion", "Paid", "Owed", "Net"]
    sum_x = [x + 10, x + 160, x + 280, x + 400]
    for sx, h in zip(sum_x, sum_headers):
        draw.text((sx, summary_y), h, font=font_small, fill=INK_MUTED)
    summary_y += 22
    draw.line([(x + 10, summary_y - 4), (width - x - 10, summary_y - 4)], fill=OUTLINE, width=1)

    summary = [
        ("Alice", "420.00 EUR", "210.00 EUR", "+210.00 EUR"),
        ("Bob", "60.00 EUR", "210.00 EUR", "-150.00 EUR"),
        ("Charlie", "0.00 EUR", "60.00 EUR", "-60.00 EUR"),
    ]

    for name, paid, owed, net in summary:
        draw.text((sum_x[0], summary_y), name, font=font_small, fill=INK)
        draw.text((sum_x[1], summary_y), paid, font=font_small, fill=INK)
        draw.text((sum_x[2], summary_y), owed, font=font_small, fill=INK)
        color = SAGE if "+" in net else TERRACOTTA
        draw.text((sum_x[3], summary_y), net, font=font_small, fill=color)
        summary_y += 22

    y += 130

    # Settlement
    draw.text((x, y), "Settlement", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 70], radius=12, fill=PRIMARY_LIGHT, outline=OUTLINE)

    settle_y = y + 14
    settle_headers = ["From", "To", "Amount"]
    settle_x = [x + 10, x + 160, x + 310]
    for sx, h in zip(settle_x, settle_headers):
        draw.text((sx, settle_y), h, font=font_small, fill=INK_MUTED)
    settle_y += 22
    draw.line([(x + 10, settle_y - 4), (width - x - 10, settle_y - 4)], fill=OUTLINE, width=1)

    settlements = [
        ("Bob", "Alice", "150.00 EUR"),
        ("Charlie", "Alice", "60.00 EUR"),
    ]

    for from_, to_, amount in settlements:
        draw.text((settle_x[0], settle_y), from_, font=font_small, fill=INK)
        draw.text((settle_x[1], settle_y), to_, font=font_small, fill=INK)
        draw.text((settle_x[2], settle_y), amount, font=font_small, fill=INK)
        settle_y += 22

    y += 95

    # Explanation
    draw.text((x, y), "How the calculation works", font=font_section, fill=INK)
    y += 22

    explanation = (
        "For each expense, the payer is credited the full amount paid. Each companion is debited their share. "
        "A companion's share is either an equal split among everyone, or a custom amount set when the expense was added. "
        "Net = Paid minus Owed. A positive net means the person is owed money; a negative net means they owe money. "
        "The settlement list above shows the smallest number of payments needed to make every net balance zero."
    )

    words = explanation.split()
    line = ""
    for word in words:
        test = line + " " + word if line else word
        bbox = draw.textbbox((0, 0), test, font=font_small)
        if bbox[2] - bbox[0] <= width - 2 * x:
            line = test
        else:
            draw.text((x, y), line, font=font_small, fill=INK_MUTED)
            y += 18
            line = word
    if line:
        draw.text((x, y), line, font=font_small, fill=INK_MUTED)

    return img


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    csv_img = csv_mockup()
    csv_path = OUTPUT_DIR / "csv_mockup.png"
    csv_img.save(csv_path)
    print(f"Saved CSV mockup: {csv_path}")

    pdf_img = pdf_mockup()
    pdf_path = OUTPUT_DIR / "pdf_mockup.png"
    pdf_img.save(pdf_path)
    print(f"Saved PDF mockup: {pdf_path}")


if __name__ == "__main__":
    main()
