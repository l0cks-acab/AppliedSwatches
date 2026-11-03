# Changelog

## AppliedSwatches 0.2.3

**Swatch Applicator redesign**
- New model in the AE2 machine palette with the terminals' rainbow strip: upright grip, wide head with the screen tilted back toward you.
- The block preview and hologram now sit on the tilted screen.

**Creative Swatch Cell** (creative only, for testing)
- Goes in an ME Drive and supplies an endless amount of every block in the game, modded ones included.
- Blocks put back into it are voided.

## AppliedSwatches 0.2.2

**Autocrafting from the terminals**
- Blocks your ME network has patterns for now show up in the Swatch and Theme Terminals even when none are stored, with a purple + badge.
- Right-click any autocraftable block to start an AE2 crafting job for it (amount = the Each setting).
- New **Craft** button: starts jobs for every matching autocraftable block the network doesn't hold yet, a few per palette colour.
- Chat reports how many jobs started and how many couldn't (missing ingredients, no free crafting CPU).

**Themes**
- Your own themes (imported or saved) now sit at the top of the Theme Terminal list, newest first, above the built-in ones.

## AppliedSwatches 0.2.1

**Swatch Applicator: multi-block placement**
- New settings screen (Shift + right-click): pick the block or Random, and a placement mode.
  - **Single**: one block.
  - **Wand**: extends the clicked surface across matching blocks, like a building wand (3×3 to 9×9).
  - **Line**: a straight line out of the clicked face (2 to 16 long).
  - **Square**: a square on the clicked face (3×3 to 9×9).
- Multi-block modes show a purple outline of where blocks will go, and place straight from your ME storage.
- Shift + scroll still cycles the selected block.

**Terminals: show craftable blocks**
- New crafting table toggle on the Swatch and Theme Terminals, off by default.
- When on, the results also include matching blocks you don't have stored but that have a crafting or stonecutter recipe. They're marked with a green + and Export skips them.
- It's off by default so big modpacks don't flood the results.

Requires Applied Energistics 2 (19.0.27 or newer) on NeoForge 1.21.1.

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
