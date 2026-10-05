# README pixel artwork

Editable adaptation of the `agentcraft-gui-assets` standalone pipeline, targeting
the Minecraft 1.21.1 addon documentation. This source generates illustrations;
it does not change the mod's in-game textures or claim to be a game screenshot.

From the repository root, with Python and Pillow 12.3.0 available:

```sh
python docs/build_readme_art.py --verify
```

The pipeline draws at 480px wide, then exports at exactly 2x using nearest-neighbor
scaling. Text uses an original hand-authored pixel alphabet. There are no system
fonts, antialiased curves or generated image dependencies. The native and enlarged
renders are checked for dimensions, color counts, alpha and byte determinism.
`asset-report.json` records the latest verified pipeline output.

## Palette and asset sources

- Paper colors are sampled from Envelope's actual letter GUI: `#EFDBAE`,
  `#FEECBA`, `#EDD7AA`, `#EBC598`. Brown frame shades are derived from that palette.
- Signature ink `#7B593D` and enchanted purple `#AA55FF` match the addon defaults.
- `inputs/paper.png`, `letter.png`, `package.png` and `letter_and_quill.png` are
  unmodified textures from the Envelope 0.7.5 dependency used by this project,
  under their original `assets/envelope/textures/gui` and `textures/item` paths.
  Envelope assets remain covered by GPL-3.0; see the repository's [license](../../../LICENSE).
- Circle geometry and the signature bitmap are read directly from
  `common/src/main/resources/assets/questlog_envelope` in this repository.
  The signature bitmap derives from the CC0 BitScript font, credited in the
  [configuration guide](../../GUIDE.md).
- AgentCraft pipeline utilities retain their original MIT [license](LICENSE).
  Upstream revision: `0be815deb4833a9ce0f070c29737bba882e98759`.

Edit `palette.json` and `gen/readme.py` to change the artwork. `build.py` writes
fresh output directories and validates all PNGs; the repository-level export
script copies only the two final illustrations and the validation report.
