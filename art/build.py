"""Build storybook art: SVG sources -> per-layer SVGs -> PNG (Chromium) -> WebP (ffmpeg).

Run: python3 art/build.py   (needs node + playwright and ffmpeg; outputs go to app/src/main/res/drawable-nodpi)
"""
import json, os, re, subprocess, sys, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "src"))
import characters, scenes  # noqa: E402

OUT = os.path.join(HERE, "out")
RES = os.path.join(HERE, "..", "app", "src", "main", "res", "drawable-nodpi")
CHAR_SCALE = 3  # sprites render at 3x their viewBox for crisp phone screens


def only(svg: str, keep: str, layers: list) -> str:
    hide = [l for l in layers if l != keep]
    style = "<style>" + ",".join(f"#{l}" for l in hide) + "{display:none}</style>"
    return re.sub(r"(<svg[^>]*>)", r"\1" + style, svg, count=1)


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)
    jobs = []
    for name, (svg, layers) in characters.sprites().items():
        w, h = map(float, re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', svg).groups())
        size = [int(w * CHAR_SCALE), int(h * CHAR_SCALE)]
        if layers is None:
            outs = [(name, svg)]
        else:
            outs = [(f"{name}_{l.replace('-', '_')}", only(svg, l, layers)) for l in layers]
        for out, s in outs:
            open(os.path.join(OUT, out + ".svg"), "w").write(s)
            jobs.append({"name": out, "w": size[0], "h": size[1], "transparent": True})
    for name, svg in scenes.scenes().items():
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
