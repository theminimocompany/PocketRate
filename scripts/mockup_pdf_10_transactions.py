#!/usr/bin/env python3
"""
Generate a 10-transaction PDF export mockup and print a text preview.

Run with the project Python:
    C:/Users/regan/OneDrive/Kimi/PocketRate/.tools/python/python.exe mockup_pdf_10_transactions.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUTPUT = Path("C:/Users/regan/OneDrive/Kimi/PocketRate/export_mockups/pdf_10_transactions_mockup.png")

WHITE = (255, 255, 255)
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


def wrap_text(draw, text, font, max_width):
    words = text.split()
    lines = []
    line = ""
    for word in words:
        test = line + " " + word if line else word
        bbox = draw.textbbox((0, 0), test, font=font)
        if bbox[2] - bbox[0] <= max_width:
            line = test
        else:
            if line:
                lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines


def generate_image():
    width, height = 600, 1200
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
    draw.text((x, y), "Total Spent: 2,100.00 EUR", font=font_header, fill=INK)
    y += 35

    # Expenses
    draw.text((x, y), "Expenses", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 280], radius=12, fill=SURFACE, outline=OUTLINE)

    table_y = y + 14
    headers = ["Date", "Description", "Category", "Payer", "Original", "Split"]
    col_x = [x + 10, x + 70, x + 170, x + 250, x + 320, x + 400]
    for cx, h in zip(col_x, headers):
        draw.text((cx, table_y), h, font=font_small, fill=INK_MUTED)
    table_y += 22
    draw.line([(x + 10, table_y - 4), (width - x - 10, table_y - 4)], fill=OUTLINE, width=1)

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

    for date, desc, category, payer, amount, split in expenses:
        draw.text((col_x[0], table_y), date, font=font_small, fill=INK)
        draw.text((col_x[1], table_y), desc, font=font_small, fill=INK)
        draw.text((col_x[2], table_y), category, font=font_small, fill=CATEGORY_COLORS.get(category, INK))
        draw.text((col_x[3], table_y), payer, font=font_small, fill=INK)
        draw.text((col_x[4], table_y), amount, font=font_small, fill=INK)
        draw.text((col_x[5], table_y), split, font=font_small, fill=INK_MUTED)
        table_y += 24

    y += 300

    # Companion summary
    draw.text((x, y), "Companion Summary", font=font_section, fill=INK)
    y += 22

    draw_rounded_rect(draw, [x, y, width - x, y + 100], radius=12, fill=SURFACE, outline=OUTLINE)

    summary_y = y + 14
    sum_headers = ["Companion", "Paid", "Owed", "Net"]
    sum_x = [x + 10, x + 160, x + 280, x + 400]
    for sx, h in zip(sum_x, sum_headers):
        draw.text((sx, summary_y), h, font=font_small, fill=INK_MUTED)
    summary_y += 22
    draw.line([(x + 10, summary_y - 4), (width - x - 10, summary_y - 4)], fill=OUTLINE, width=1)

    summary = [
        ("Alice", "1,170.00 EUR", "740.00 EUR", "+430.00 EUR"),
        ("Bob", "390.00 EUR", "800.00 EUR", "-410.00 EUR"),
        ("Charlie", "540.00 EUR", "560.00 EUR", "-20.00 EUR"),
    ]

    for name, paid, owed, net in summary:
        draw.text((sum_x[0], summary_y), name, font=font_small, fill=INK)
        draw.text((sum_x[1], summary_y), paid, font=font_small, fill=INK)
        draw.text((sum_x[2], summary_y), owed, font=font_small, fill=INK)
        color = SAGE if "+" in net else TERRACOTTA
        draw.text((sum_x[3], summary_y), net, font=font_small, fill=color)
        summary_y += 22

    y += 120

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
        ("Bob", "Alice", "410.00 EUR"),
        ("Charlie", "Alice", "20.00 EUR"),
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

    for line in wrap_text(draw, explanation, font_small, width - 2 * x):
        draw.text((x, y), line, font=font_small, fill=INK_MUTED)
        y += 18

    img.save(OUTPUT)
    print(f"Saved 10-transaction PDF mockup: {OUTPUT}")


def print_terminal_preview():
    print("=" * 80)
    print("POCKETRATE TRIP REPORT - Europe Trip")
    print("Home Currency: EUR  |  Settlement Currency: EUR")
    print("Total Spent: 2,100.00 EUR")
    print("=" * 80)
    print()
    print("EXPENSES")
    print("-" * 80)
    print(f"{'Date':<8} {'Description':<12} {'Category':<14} {'Payer':<8} {'Original':<12} {'Split'}")
    print("-" * 80)
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
    for date, desc, category, payer, amount, split in expenses:
        print(f"{date:<8} {desc:<12} {category:<14} {payer:<8} {amount:<12} {split}")
    print()
    print("COMPANION SUMMARY")
    print("-" * 50)
    print(f"{'Companion':<12} {'Paid':<16} {'Owed':<16} {'Net'}")
    print("-" * 50)
    print(f"{'Alice':<12} {'1,170.00 EUR':<16} {'740.00 EUR':<16} {'+430.00 EUR'}")
    print(f"{'Bob':<12} {'390.00 EUR':<16} {'800.00 EUR':<16} {'-410.00 EUR'}")
    print(f"{'Charlie':<12} {'540.00 EUR':<16} {'560.00 EUR':<16} {'-20.00 EUR'}")
    print()
    print("SETTLEMENT")
    print("-" * 40)
    print(f"{'From':<12} {'To':<12} {'Amount'}")
    print("-" * 40)
    print(f"{'Bob':<12} {'Alice':<12} {'410.00 EUR'}")
    print(f"{'Charlie':<12} {'Alice':<12} {'20.00 EUR'}")
    print()
    print("HOW IT WAS CALCULATED")
    print("-" * 80)
    print("For each expense, the payer is credited the full amount paid. Each companion")
    print("is debited their share. A companion's share is either an equal split among")
    print("everyone, or a custom amount set when the expense was added. Net = Paid minus")
    print("Owed. Positive net = owed money. Negative net = owes money. The settlement list")
    print("shows the smallest number of payments needed to balance every net to zero.")
    print("=" * 80)


if __name__ == "__main__":
    generate_image()
    print()
    print_terminal_preview()
