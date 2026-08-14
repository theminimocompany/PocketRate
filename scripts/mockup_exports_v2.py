#!/usr/bin/env python3
"""
Improved export mockups with better spacing and alignment.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = Path("C:/Users/regan/OneDrive/Kimi/PocketRate/export_mockups")

WHITE = (255, 255, 255)
INK = (46, 46, 46)
INK_MUTED = (107, 107, 107)
INK_SUBTLE = (158, 154, 150)
SURFACE = (250, 250, 250)
OUTLINE = (220, 220, 220)
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
    "Activities": MUSTARD,
    "Shopping": PURPLE,
}


def load_font(size: int, bold: bool = False):
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


def text_width(draw, text, font):
    return draw.textbbox((0, 0), text, font=font)[2]


def text_height(draw, text, font):
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[3] - bbox[1]


def draw_right_aligned_text(draw, x, y, text, font, fill, right_edge):
    w = text_width(draw, text, font)
    draw.text((right_edge - w, y), text, font=font, fill=fill)


def wrap_text(draw, text, font, max_width):
    words = text.split()
    lines = []
    line = ""
    for word in words:
        test = line + " " + word if line else word
        if text_width(draw, test, font) <= max_width:
            line = test
        else:
            if line:
                lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines


def csv_mockup_v2():
    width, height = 1100, 1200
    img = Image.new("RGB", (width, height), WHITE)
    draw = ImageDraw.Draw(img)

    font_title = load_font(24, bold=True)
    font_section = load_font(15, bold=True)
    font_body = load_font(12)
    font_mono = load_font(11)

    x = 40
    y = 30

    # Title
    draw.text((x, y), "PocketRate Export — Europe Trip", font=font_title, fill=INK)
    y += 45

    # Trip info
    draw.text((x, y), "Home Currency: EUR  |  Settlement Currency: EUR  |  Companions: Alice, Bob, Charlie", font=font_body, fill=INK_MUTED)
    y += 35

    # EXPENSES section
    draw.text((x, y), "EXPENSES", font=font_section, fill=INK)
    y += 25

    headers = ["Date", "Description", "Category", "Payer", "Amount", "Curr.", "Conv.", "Home", "Rate", "Split Details", "Settle", "Rate", "Buf"]
    col_widths = [70, 110, 90, 60, 65, 45, 65, 45, 55, 260, 60, 65, 35]

    # Draw header background
    draw.rectangle([x, y, x + sum(col_widths), y + 24], fill=(245, 245, 245))
    hx = x
    for h, cw in zip(headers, col_widths):
        draw.text((hx + 4, y + 4), h, font=font_mono, fill=INK_MUTED)
        hx += cw
    y += 24
    draw.line([(x, y), (x + sum(col_widths), y)], fill=OUTLINE, width=1)
    y += 6

    expenses = [
        ("2024-06-12", "Dinner", "Food", "Alice", "300.00", "EUR", "300.00", "EUR", "1.000000", "Alice 100.00, Bob 100.00, Charlie 100.00", "300.00", "1.000000", "0%"),
        ("2024-06-13", "Taxi", "Transport", "Bob", "90.00", "EUR", "90.00", "EUR", "1.000000", "Alice 30.00, Bob 30.00, Charlie 30.00", "90.00", "1.000000", "0%"),
        ("2024-06-13", "Hotel", "Accommodation", "Alice", "600.00", "EUR", "600.00", "EUR", "1.000000", "Alice 300.00, Bob 300.00", "600.00", "1.000000", "0%"),
        ("2024-06-14", "Museum", "Activities", "Charlie", "150.00", "EUR", "150.00", "EUR", "1.000000", "Alice 50.00, Bob 50.00, Charlie 50.00", "150.00", "1.000000", "0%"),
        ("2024-06-14", "Lunch", "Food", "Alice", "180.00", "EUR", "180.00", "EUR", "1.000000", "Alice 60.00, Bob 60.00, Charlie 60.00", "180.00", "1.000000", "0%"),
        ("2024-06-15", "Train", "Transport", "Bob", "240.00", "EUR", "240.00", "EUR", "1.000000", "Bob 120.00, Charlie 120.00", "240.00", "1.000000", "0%"),
        ("2024-06-15", "Drinks", "Food", "Charlie", "120.00", "EUR", "120.00", "EUR", "1.000000", "Alice 60.00, Charlie 60.00", "120.00", "1.000000", "0%"),
        ("2024-06-16", "Shopping", "Shopping", "Alice", "90.00", "EUR", "90.00", "EUR", "1.000000", "Alice 30.00, Bob 30.00, Charlie 30.00", "90.00", "1.000000", "0%"),
        ("2024-06-16", "Taxi", "Transport", "Bob", "60.00", "EUR", "60.00", "EUR", "1.000000", "Alice 20.00, Bob 20.00, Charlie 20.00", "60.00", "1.000000", "0%"),
        ("2024-06-17", "Dinner", "Food", "Charlie", "270.00", "EUR", "270.00", "EUR", "1.000000", "Alice 90.00, Bob 90.00, Charlie 90.00", "270.00", "1.000000", "0%"),
    ]

    for i, row in enumerate(expenses):
        row_y = y + i * 22
        if i % 2 == 1:
            draw.rectangle([x, row_y, x + sum(col_widths), row_y + 22], fill=(252, 252, 252))
        hx = x
        for cell, cw in zip(row, col_widths):
            draw.text((hx + 4, row_y + 3), cell, font=font_mono, fill=INK)
            hx += cw

    y += len(expenses) * 22 + 30

    # COMPANION SUMMARY
    draw.text((x, y), "COMPANION SUMMARY", font=font_section, fill=INK)
    y += 25

    sum_headers = ["Companion", "Paid", "Owed", "Net"]
    sum_widths = [150, 130, 130, 130]
    draw.rectangle([x, y, x + sum(sum_widths), y + 24], fill=(245, 245, 245))
    hx = x
    for h, cw in zip(sum_headers, sum_widths):
        draw.text((hx + 4, y + 4), h, font=font_mono, fill=INK_MUTED)
        hx += cw
    y += 24
    draw.line([(x, y), (x + sum(sum_widths), y)], fill=OUTLINE, width=1)
    y += 6

    summary = [
        ("Alice", "1,170.00 EUR", "740.00 EUR", "+430.00 EUR"),
        ("Bob", "390.00 EUR", "800.00 EUR", "-410.00 EUR"),
        ("Charlie", "540.00 EUR", "560.00 EUR", "-20.00 EUR"),
    ]

    for i, row in enumerate(summary):
        row_y = y + i * 22
        if i % 2 == 1:
            draw.rectangle([x, row_y, x + sum(sum_widths), row_y + 22], fill=(252, 252, 252))
        hx = x
        for j, (cell, cw) in enumerate(zip(row, sum_widths)):
            color = SAGE if "+" in cell else (TERRACOTTA if "-" in cell else INK)
            if j == 0:
                draw.text((hx + 4, row_y + 3), cell, font=font_mono, fill=color)
            else:
                draw_right_aligned_text(draw, hx, row_y + 3, cell, font_mono, color, hx + cw - 4)
            hx += cw

    y += len(summary) * 22 + 30

    # SETTLEMENT
    draw.text((x, y), "SETTLEMENT", font=font_section, fill=INK)
    y += 25

    settle_headers = ["From", "To", "Amount", "Currency"]
    settle_widths = [150, 150, 130, 100]
    draw.rectangle([x, y, x + sum(settle_widths), y + 24], fill=(245, 245, 245))
    hx = x
    for h, cw in zip(settle_headers, settle_widths):
        draw.text((hx + 4, y + 4), h, font=font_mono, fill=INK_MUTED)
        hx += cw
    y += 24
    draw.line([(x, y), (x + sum(settle_widths), y)], fill=OUTLINE, width=1)
    y += 6

    settlements = [
        ("Bob", "Alice", "410.00", "EUR"),
        ("Charlie", "Alice", "20.00", "EUR"),
    ]

    for i, row in enumerate(settlements):
        row_y = y + i * 22
        if i % 2 == 1:
            draw.rectangle([x, row_y, x + sum(settle_widths), row_y + 22], fill=(252, 252, 252))
        hx = x
        for j, (cell, cw) in enumerate(zip(row, settle_widths)):
            if j < 2:
                draw.text((hx + 4, row_y + 3), cell, font=font_mono, fill=INK)
            else:
                draw_right_aligned_text(draw, hx, row_y + 3, cell, font_mono, INK, hx + cw - 4)
            hx += cw

    y += len(settlements) * 22 + 35

    # EXPLANATION
    draw.text((x, y), "HOW IT WAS CALCULATED", font=font_section, fill=INK)
    y += 25

    explanation = (
        "For each expense, the payer is credited the full amount paid. Each companion is debited their share. "
        "A companion's share is either an equal split among all companions, or a custom amount set when the expense was added. "
        "Net = Paid minus Owed. A positive net means the person is owed money; a negative net means they owe money. "
        "The settlement list shows the smallest number of payments needed to make every net balance zero."
    )

    for line in wrap_text(draw, explanation, font_body, width - 2 * x):
        draw.text((x, y), line, font=font_body, fill=INK_MUTED)
        y += 18

    return img


def pdf_mockup_v2():
    width, height = 750, 1300
    img = Image.new("RGB", (width, height), WHITE)
    draw = ImageDraw.Draw(img)

    font_title = load_font(28, bold=True)
    font_header = load_font(16, bold=True)
    font_section = load_font(15, bold=True)
    font_body = load_font(12)
    font_small = load_font(11)
    font_tiny = load_font(10)

    x = 50
    y = 50

    # Header
    draw.text((x, y), "PocketRate Trip Report", font=font_title, fill=INK)
    y += 40
    draw.text((x, y), "Trip: Europe Trip", font=font_body, fill=INK)
    y += 20
    draw.text((x, y), "Home Currency: EUR  |  Settlement Currency: EUR", font=font_body, fill=INK_MUTED)
    y += 20
    draw.text((x, y), "Total Spent: 2,100.00 EUR", font=font_header, fill=INK)
    y += 45

    # Expenses
    draw.text((x, y), "Expenses", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 330], radius=12, fill=SURFACE, outline=OUTLINE)

    table_y = y + 12
    headers = ["Date", "Description", "Category", "Payer", "Original", "Split"]
    col_widths = [55, 110, 100, 70, 90, 245]
    col_x = [x + 12]
    for cw in col_widths[:-1]:
        col_x.append(col_x[-1] + cw)

    # Header row background
    draw.rectangle([x + 1, table_y, width - x - 1, table_y + 24], fill=(240, 240, 240))
    for cx, h, cw in zip(col_x, headers, col_widths):
        if h in ["Original"]:
            draw_right_aligned_text(draw, cx, table_y + 4, h, font_small, INK_MUTED, cx + cw - 8)
        else:
            draw.text((cx + 4, table_y + 4), h, font=font_small, fill=INK_MUTED)
    table_y += 24
    draw.line([(x + 12, table_y - 2), (width - x - 12, table_y - 2)], fill=OUTLINE, width=1)

    expenses = [
        ("Jun 12", "Dinner", "Food", "Alice", "300.00 EUR", "Alice 100, Bob 100, Charlie 100"),
        ("Jun 13", "Taxi", "Transport", "Bob", "90.00 EUR", "Alice 30, Bob 30, Charlie 30"),
        ("Jun 13", "Hotel", "Accommodation", "Alice", "600.00 EUR", "Alice 300, Bob 300"),
        ("Jun 14", "Museum", "Activities", "Charlie", "150.00 EUR", "Alice 50, Bob 50, Charlie 50"),
        ("Jun 14", "Lunch", "Food", "Alice", "180.00 EUR", "Alice 60, Bob 60, Charlie 60"),
        ("Jun 15", "Train", "Transport", "Bob", "240.00 EUR", "Bob 120, Charlie 120"),
        ("Jun 15", "Drinks", "Food", "Charlie", "120.00 EUR", "Alice 60, Charlie 60"),
        ("Jun 16", "Shopping", "Shopping", "Alice", "90.00 EUR", "Alice 30, Bob 30, Charlie 30"),
        ("Jun 16", "Taxi", "Transport", "Bob", "60.00 EUR", "Alice 20, Bob 20, Charlie 20"),
        ("Jun 17", "Dinner", "Food", "Charlie", "270.00 EUR", "Alice 90, Bob 90, Charlie 90"),
    ]

    for i, (date, desc, category, payer, amount, split) in enumerate(expenses):
        row_y = table_y + i * 28
        if i % 2 == 1:
            draw.rectangle([x + 1, row_y, width - x - 1, row_y + 28], fill=(245, 245, 245))

        draw.text((col_x[0] + 4, row_y + 6), date, font=font_small, fill=INK)
        draw.text((col_x[1] + 4, row_y + 6), desc, font=font_small, fill=INK)
        draw.text((col_x[2] + 4, row_y + 6), category, font=font_small, fill=CATEGORY_COLORS.get(category, INK))
        draw.text((col_x[3] + 4, row_y + 6), payer, font=font_small, fill=INK)
        draw_right_aligned_text(draw, col_x[4], row_y + 6, amount, font_small, INK, col_x[4] + col_widths[4] - 8)
        draw.text((col_x[5] + 4, row_y + 6), split, font=font_tiny, fill=INK_MUTED)

    y += 350

    # Companion Summary
    draw.text((x, y), "Companion Summary", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 120], radius=12, fill=SURFACE, outline=OUTLINE)

    summary_y = y + 12
    sum_headers = ["Companion", "Paid", "Owed", "Net"]
    sum_widths = [170, 140, 140, 140]
    sum_x = [x + 12]
    for cw in sum_widths[:-1]:
        sum_x.append(sum_x[-1] + cw)

    draw.rectangle([x + 1, summary_y, width - x - 1, summary_y + 24], fill=(240, 240, 240))
    for sx, h, cw in zip(sum_x, sum_headers, sum_widths):
        if h == "Companion":
            draw.text((sx + 4, summary_y + 4), h, font=font_small, fill=INK_MUTED)
        else:
            draw_right_aligned_text(draw, sx, summary_y + 4, h, font_small, INK_MUTED, sx + cw - 8)
    summary_y += 24
    draw.line([(x + 12, summary_y - 2), (width - x - 12, summary_y - 2)], fill=OUTLINE, width=1)

    summary = [
        ("Alice", "1,170.00 EUR", "740.00 EUR", "+430.00 EUR"),
        ("Bob", "390.00 EUR", "800.00 EUR", "-410.00 EUR"),
        ("Charlie", "540.00 EUR", "560.00 EUR", "-20.00 EUR"),
    ]

    for i, (name, paid, owed, net) in enumerate(summary):
        row_y = summary_y + i * 28
        if i % 2 == 1:
            draw.rectangle([x + 1, row_y, width - x - 1, row_y + 28], fill=(245, 245, 245))

        draw.text((sum_x[0] + 4, row_y + 6), name, font=font_small, fill=INK)
        draw_right_aligned_text(draw, sum_x[1], row_y + 6, paid, font_small, INK, sum_x[1] + sum_widths[1] - 8)
        draw_right_aligned_text(draw, sum_x[2], row_y + 6, owed, font_small, INK, sum_x[2] + sum_widths[2] - 8)
        color = SAGE if "+" in net else TERRACOTTA
        draw_right_aligned_text(draw, sum_x[3], row_y + 6, net, font_small, color, sum_x[3] + sum_widths[3] - 8)

    y += 140

    # Settlement
    draw.text((x, y), "Settlement", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 85], radius=12, fill=PRIMARY_LIGHT, outline=OUTLINE)

    settle_y = y + 12
    settle_headers = ["From", "To", "Amount"]
    settle_widths = [210, 210, 210]
    settle_x = [x + 12]
    for cw in settle_widths[:-1]:
        settle_x.append(settle_x[-1] + cw)

    draw.rectangle([x + 1, settle_y, width - x - 1, settle_y + 24], fill=(222, 232, 224))
    for sx, h, cw in zip(settle_x, settle_headers, settle_widths):
        if h == "Amount":
            draw_right_aligned_text(draw, sx, settle_y + 4, h, font_small, INK_MUTED, sx + cw - 8)
        else:
            draw.text((sx + 4, settle_y + 4), h, font=font_small, fill=INK_MUTED)
    settle_y += 24
    draw.line([(x + 12, settle_y - 2), (width - x - 12, settle_y - 2)], fill=OUTLINE, width=1)

    settlements = [
        ("Bob", "Alice", "410.00 EUR"),
        ("Charlie", "Alice", "20.00 EUR"),
    ]

    for i, (from_, to_, amount) in enumerate(settlements):
        row_y = settle_y + i * 28
        draw.text((settle_x[0] + 4, row_y + 6), from_, font=font_small, fill=INK)
        draw.text((settle_x[1] + 4, row_y + 6), to_, font=font_small, fill=INK)
        draw_right_aligned_text(draw, settle_x[2], row_y + 6, amount, font_small, INK, settle_x[2] + settle_widths[2] - 8)

    y += 105

    # Explanation
    draw.text((x, y), "How the calculation works", font=font_section, fill=INK)
    y += 22

    explanation = (
        "For each expense, the payer is credited the full amount paid. Each companion is debited their share. "
        "A companion's share is either an equal split among all companions, or a custom amount set when the expense was added. "
        "Net = Paid minus Owed. A positive net means the person is owed money; a negative net means they owe money. "
        "The settlement list above shows the smallest number of payments needed to make every net balance zero."
    )

    for line in wrap_text(draw, explanation, font_body, width - 2 * x):
        draw.text((x, y), line, font=font_body, fill=INK_MUTED)
        y += 18

    return img


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    csv_img = csv_mockup_v2()
    csv_path = OUTPUT_DIR / "csv_mockup_v2.png"
    csv_img.save(csv_path)
    print(f"Saved improved CSV mockup: {csv_path}")

    pdf_img = pdf_mockup_v2()
    pdf_path = OUTPUT_DIR / "pdf_mockup_v2.png"
    pdf_img.save(pdf_path)
    print(f"Saved improved PDF mockup: {pdf_path}")


if __name__ == "__main__":
    main()
