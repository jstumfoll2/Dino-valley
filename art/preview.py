#!/usr/bin/env python3
"""Build a few pieces of art to look at before they go into the game.

    python3 art/preview.py items item_mountain_key item_glow_silk
    python3 art/preview.py dungeon_scenes scene_belfry
    python3 art/preview.py --list minigames

The pieces are named as `sprites()` and `scenes()` name them in art/src/<module>.py, without the `art_` prefix.
The WebP files land in art/out/preview (not in the app), so they can be opened and checked first. To put the same
pieces into the app once they look right, point ART_RES at the resource folder:

    ART_RES=app/src/main/res/drawable-nodpi python3 art/preview.py items item_mountain_key

Needs node with playwright, and ffmpeg, like build.py (see docs/art-plan.md).
"""
import importlib
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
os.environ.setdefault("ART_RES", os.path.join(HERE, "out", "preview"))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "src"))

import build  # noqa: E402  (reads ART_RES when it is imported)


def pieces(module):
    mod = importlib.import_module(module)
    sprites = mod.sprites() if hasattr(mod, "sprites") else {}
    scenes = mod.scenes() if hasattr(mod, "scenes") else {}
    return sprites, scenes


def main():
    args = sys.argv[1:]
    if len(args) >= 2 and args[0] == "--list":
        sprites, scenes = pieces(args[1])
        print("\n".join(sorted(list(sprites) + list(scenes))))
        return
    if len(args) < 2:
        sys.exit(__doc__)
    module, names = args[0], args[1:]
    sprites, scenes = pieces(module)
    unknown = [n for n in names if n not in sprites and n not in scenes]
    if unknown:
        sys.exit(f"not in {module}: {', '.join(unknown)} (try --list {module})")
    out = os.path.join(HERE, "out", "preview-work")
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    build.build({k: v for k, v in sprites.items() if k in names}, {k: v for k, v in scenes.items() if k in names}, out)
    print("written to", build.RES)


if __name__ == "__main__":
    main()
