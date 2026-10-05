"""Build the game's art: SVG sources -> per-layer SVGs -> PNG (Chromium) -> WebP (ffmpeg).

Run: python3 art/build.py [module ...]
Needs node + playwright and ffmpeg. Outputs go to app/src/main/res/drawable-nodpi as
art_<name>[_<layer>].webp. Each module in art/src may define sprites() -> {name: (svg, layers or None)}
and/or scenes() -> {name: svg} (1920x1080 backgrounds). With no arguments every module is built.
"""
import importlib, json, os, re, subprocess, sys, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "src"))

MODULES = ["heroes", "props", "dungeon_scenes", "cast", "creatures", "gear", "items", "world", "minigames"]
# ART_RES redirects the WebP output (to look at new art without touching the app).
RES = os.environ.get("ART_RES") or os.path.join(HERE, "..", "app", "src", "main", "res", "drawable-nodpi")
CHAR_SCALE = 3  # sprites render at 3x their viewBox for crisp phone screens


def only(svg: str, keep: str, layers: list) -> str:
    hide = [l for l in layers if l != keep]
    style = "<style>" + ",".join(f"#{l}" for l in hide) + "{display:none}</style>"
    return re.sub(r"(<svg[^>]*>)", r"\1" + style, svg, count=1)


def main():
    wanted = sys.argv[1:] or MODULES
    out = os.path.join(HERE, "out", "-".join(wanted))
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    sprites, backgrounds = {}, {}
    for m in wanted:
        mod = importlib.import_module(m)
        if hasattr(mod, "sprites"):
            sprites.update(mod.sprites())
        if hasattr(mod, "scenes"):
            backgrounds.update(mod.scenes())
    build(sprites, backgrounds, out)


def build(sprites, backgrounds, OUT):
    jobs = []
    for name, (svg, layers) in sprites.items():
        w, h = map(float, re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', svg).groups())
        size = [int(w * CHAR_SCALE), int(h * CHAR_SCALE)]
        if layers is None:
            outs = [(name, svg)]
        else:
            outs = [(f"{name}_{l.replace('-', '_')}", only(svg, l, layers)) for l in layers]
        for out, s in outs:
            open(os.path.join(OUT, out + ".svg"), "w").write(s)
            jobs.append({"name": out, "w": size[0], "h": size[1], "transparent": True})
    for name, svg in backgrounds.items():
        open(os.path.join(OUT, name + ".svg"), "w").write(svg)
        jobs.append({"name": name, "w": 1920, "h": 1080, "transparent": False})
    json.dump(jobs, open(os.path.join(OUT, "jobs.json"), "w"))
    subprocess.run(["node", os.path.join(HERE, "render.cjs"), OUT], check=True)
    os.makedirs(RES, exist_ok=True)
    for j in jobs:
        q = "88" if j["transparent"] else "82"
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", os.path.join(OUT, j["name"] + ".png"),
                        "-c:v", "libwebp", "-quality", q, "-compression_level", "6",
                        os.path.join(RES, "art_" + j["name"] + ".webp")], check=True)
    print(f"built {len(jobs)} images")


if __name__ == "__main__":
    main()
