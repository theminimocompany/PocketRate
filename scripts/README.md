# PocketRate Icon Generator

This script generates all the Android launcher icon assets and a preview sheet from your source logo.

## What it produces

- `icons/preview.png` — overview of the light and dark variants
- `icons/light/` — white background + black logo
  - `adaptive_foreground.png` — 660×660 px foreground
  - `adaptive_background.png` — 660×660 px background
  - `legacy/mdpi...xxxhdpi/ic_launcher.png` + `ic_launcher_round.png`
  - `play_store/ic_launcher_play_store.png` — 512×512
- `icons/dark/` — dark background + white logo (reversed)
  - Same files as the light variant

## Requirements

1. Python 3.9+ installed from <https://python.org> with "Add Python to PATH" checked.
2. Pillow installed:

```bash
pip install Pillow
```

## Run

```bash
cd "C:/Users/regan/OneDrive/Kimi/PocketRate/scripts"
python generate_icons.py
```

The script reads `C:/Users/regan/Downloads/PocketRate Logo.png` and writes outputs to `C:/Users/regan/OneDrive/Kimi/PocketRate/icons/`.

## Customizing

Open `generate_icons.py` and edit this section:

```python
THEME_VARIANTS = [
    ("light", "#FFFFFF", "#000000"),
    ("dark", "#121212", "#FFFFFF"),
]
```

Each tuple is `(variant_name, background_hex, foreground_hex)`. Change the colors or add more variants as needed.

## After you choose a design

Once you've picked the `light` or `dark` variant from the preview, tell Kimi and it can apply the assets to the Android project by:

1. Copying the chosen `adaptive_foreground.png` and `adaptive_background.png` into the project and referencing them in `mipmap-anydpi-v26/ic_launcher.xml`.
2. Copying the legacy PNGs into `res/mipmap-*/`.
3. Updating `res/values/colors.xml` with the chosen background color.
4. Replacing `ic_launcher_foreground.xml` with a proper vector or PNG reference.
