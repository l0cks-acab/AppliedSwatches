# Changelog

## 0.2.1

- Re-release of 0.2.0 for CurseForge (the 0.2.0 upload didn't go through). No gameplay changes; see 0.2.0 below for what's new since 0.1.0.

## 0.2.0

- Swatch Applicator settings screen (Shift + right-click): pick the block or Random, and a placement mode.
  - Single: one block.
  - Wand: extends the clicked surface across matching blocks, like a building wand (3×3 to 9×9).
  - Line: a straight line out of the clicked face (2 to 16 long).
  - Square: a square on the clicked face (3×3 to 9×9).
- Multi-block modes show a purple outline of where blocks will go, and place straight from ME storage.
- Shift + scroll still cycles the selected block.
- Optional "Show craftable blocks" toggle on both terminals (crafting table button, off by default): also lists matching blocks you don't have stored but that have a crafting or stonecutter recipe. They're marked with a green + and skipped by Export.

## 0.1.0

First release.

- Swatch Terminal: pick a colour and scheme (single, monochrome, analogous, complementary, split, triadic), set tolerance and amount, and export matching blocks from ME storage.
- Theme Terminal: 14 built-in themes (cyberpunk, desert, underwater and more) with pinned blocks.
- Save and load swatches as .json; import and export themes as .json.
- Save the current swatch as a theme from the Swatch Terminal.
- Click a result to skip it on export.
- Swatch Applicator: handheld placer loaded from either terminal; places the selected or a random palette block straight from ME storage, with a hologram of the selected block.
- Server config for idle power draw of each terminal and applicator range.
