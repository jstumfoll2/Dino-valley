"""Whisperwood: the kingdom map painting, road backdrops and one scene per place.

Same house style as dungeon_scenes (Doc builder, paper grain, vignette) and the same overlay zones:
bottom-left open for the hero, top centre quiet for the caption, centre-right calm for pictures.
Names: `scene_world_map`, `scene_road_<terrain>`, `scene_<location id>` (camp is `scene_camp`,
already painted in dungeon_scenes).
"""
import math
from dungeon_scenes import (Doc, W, H, INK, smooth, blob_pts, glow, sparkle, flame, torch, tree, pine,
                            lantern, mushroom, grass_blades, motes, stalactites, stone_blocks, barrel,
                            crate, sack, apple, cattails, lily_pad, banner, arch_path, crystal_cluster)

SKIES = {
    "day": [(0, "#8fc9ef"), (0.6, "#cfe9f5"), (1, "#fff2cf")],
    "dusk": [(0, "#9c8fd0"), (0.45, "#f2b6a0"), (1, "#ffe2a8")],
    "gloom": [(0, "#5d5a86"), (0.6, "#8c88ac"), (1, "#c3bad0")],
    "night": [(0, "#1d2350"), (0.6, "#4a4a86"), (1, "#8a7aa8")],
    "storm": [(0, "#6a7a95"), (0.6, "#a3b0c2"), (1, "#d6dce2")],
}


def sky(d, kind="day", sun=None):
    d.add(f'<rect width="{W}" height="{H}" fill="{d.lin(SKIES[kind])}"/>')
    if kind == "night":
        for _ in range(70):
            d.add(f'<circle cx="{d.r.uniform(0, W):.0f}" cy="{d.r.uniform(0, 420):.0f}" r="{d.r.uniform(1, 3):.1f}" fill="#fffbe6" opacity="0.8"/>')
        d.add(glow(d, 1500, 160, 220, "#f4f0ff", 0.6))
        d.add('<circle cx="1500" cy="160" r="52" fill="#fbf8ff"/>')
    elif sun:
        d.add(glow(d, sun[0], sun[1], 420, "#fff1b8", 0.7))
        d.add(f'<circle cx="{sun[0]}" cy="{sun[1]}" r="64" fill="#fff0a8" stroke="#ffd36a" stroke-width="5"/>')
    for cx, cy, s in [(240, 150, 1.0), (1650, 130, 1.2), (1000, 330, 0.7)]:
        col = "#ffffff" if kind != "gloom" else "#aaa6c4"
        for k in range(4):
            d.add(f'<ellipse cx="{cx + k * 60 * s - 90 * s:.0f}" cy="{cy - (k % 2) * 16 * s:.0f}" rx="{(90 - k * 6) * s:.0f}" ry="{24 * s:.0f}" fill="{col}" opacity="0.55"/>')


def hills(d, y, col_a, col_b, amp=70, seed=0, step=240):
    r = d.r
    pts = [(x, y + r.uniform(-amp, amp)) for x in range(-100, W + 200, step)]
    g = d.lin([(0, col_a), (1, col_b)])
    d.add(f'<path d="{smooth(pts, closed=False)} L{W + 200} {H} L-100 {H} Z" fill="{g}"/>')


def ground(d, y, a="#a9cf6c", b="#5e9440", wave=30):
    g = d.lin([(0, a), (0.5, (a if False else "#88b955")), (1, b)])
    d.add(f'<path d="M0 {y} Q480 {y - wave} 960 {y + 8} Q1440 {y + wave} 1920 {y - 6} L1920 {H} L0 {H} Z" fill="{g}"/>')
    d.add(grass_blades(d.r, 0, W, y, H, 380, ["#7fb04c", "#6ea443", "#98c75e"]))


def frame_trees(d, left=True, right=True, pal=("#4f7d3e", "#66974b", "#86b25c")):
    if left:
        d.add(tree(d, d.r, -20, 1000, 1.8, pal))
    if right:
        d.add(tree(d, d.r, 1960, 980, 1.7, pal))


def flowers(d, n=34, y0=820):
    for _ in range(n):
        x, y = d.r.uniform(0, W), d.r.uniform(y0, 1070)
        if 720 < x < 1880 and y < 1000:
            continue
        col = d.r.choice(["#ffffff", "#ffd36a", "#ff9fb8", "#c7a6ff"])
        d.add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="7" fill="{col}" stroke="{INK}" stroke-width="1.5" stroke-opacity="0.4"/><circle cx="{x:.0f}" cy="{y:.0f}" r="2.5" fill="#e09a2a"/>')


def mountain(d, cx, base, w, h, col=("#b49acb", "#8a72a6"), snow=True):
    g = d.lin([(0, col[0]), (1, col[1])])
    d.add(f'<path d="M{cx - w / 2} {base} L{cx - w * 0.06} {base - h} L{cx + w * 0.08} {base - h * 0.95} L{cx + w / 2} {base} Z" fill="{g}" stroke="{INK}" stroke-width="4" stroke-opacity="0.25" stroke-linejoin="round"/>')
    d.add(f'<path d="M{cx + w * 0.02} {base - h * 0.97} L{cx + w / 2} {base} L{cx + w * 0.12} {base} Z" fill="#000" opacity="0.13"/>')
    if snow:
        d.add(f'<path d="M{cx - w * 0.16} {base - h * 0.72} L{cx - w * 0.06} {base - h} L{cx + w * 0.08} {base - h * 0.95} L{cx + w * 0.17} {base - h * 0.7} L{cx + w * 0.08} {base - h * 0.76} L{cx} {base - h * 0.66} L{cx - w * 0.07} {base - h * 0.76} Z" fill="#f6eef8"/>')


def house(d, x, base, w, h, wall="#f2dcb6", roof="#cf6a58", door="#7a4e2e", win=True, lit=False):
    d.add(f'<ellipse cx="{x + w / 2}" cy="{base + 4}" rx="{w * 0.62}" ry="12" fill="#2f4a22" opacity="0.28"/>')
    d.add(f'<rect x="{x}" y="{base - h}" width="{w}" height="{h}" fill="{wall}" stroke="{INK}" stroke-width="4" stroke-opacity="0.55"/>')
    d.add(f'<rect x="{x}" y="{base - h}" width="{w * 0.3}" height="{h}" fill="#b89a74" opacity="0.28"/>')
    d.add(f'<path d="M{x - 14} {base - h} L{x + w / 2} {base - h - w * 0.55} L{x + w + 14} {base - h} Z" fill="{roof}" stroke="{INK}" stroke-width="4" stroke-opacity="0.6" stroke-linejoin="round"/>')
    d.add(f'<path d="M{x + w / 2} {base - h - w * 0.55} L{x + w + 14} {base - h} L{x + w * 0.7} {base - h} Z" fill="#000" opacity="0.14"/>')
    dw = w * 0.24
    d.add(f'<path d="M{x + w * 0.22} {base} v{-h * 0.5} a{dw / 2} {dw / 2} 0 0 1 {dw} 0 v{h * 0.5} Z" fill="{door}" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
    if win:
        wc = "#ffe08a" if lit else "#bfe3f2"
        d.add(f'<rect x="{x + w * 0.6}" y="{base - h * 0.68}" width="{w * 0.22}" height="{w * 0.22}" rx="4" fill="{wc}" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
        if lit:
            d.add(glow(d, x + w * 0.71, base - h * 0.57, 60, "#ffd36a", 0.5))


def path_ribbon(d, pts, wid, col="#e6c98b", edge="#b8975e"):
    d.add(f'<path d="{smooth(pts, closed=False)}" fill="none" stroke="{edge}" stroke-width="{wid + 8}" stroke-linecap="round"/>')
    d.add(f'<path d="{smooth(pts, closed=False)}" fill="none" stroke="{col}" stroke-width="{wid}" stroke-linecap="round"/>')


def river(d, pts, wid, col="#6fc3e6", hi="#bfeaf7"):
    d.add(f'<path d="{smooth(pts, closed=False)}" fill="none" stroke="#3f8fb8" stroke-width="{wid + 8}" stroke-linecap="round"/>')
    d.add(f'<path d="{smooth(pts, closed=False)}" fill="none" stroke="{col}" stroke-width="{wid}" stroke-linecap="round"/>')
    d.add(f'<path d="{smooth(pts, closed=False)}" fill="none" stroke="{hi}" stroke-width="{max(3, wid // 8)}" stroke-linecap="round" stroke-dasharray="26 40" opacity="0.8"/>')


def finish(d, v=0.22):
    return d.svg(vignette=v)


def belfry_tower(d, x, base, w=62, h=150):
    """An old stone bell tower with a pointed roof and an open arch holding a gold bell: the Bat King's lair on the map."""
    stone = d.lin([(0, "#c4bacb"), (1, "#8a8098")], 0, 0, 1, 0)
    d.add(f'<ellipse cx="{x + w / 2}" cy="{base + 4}" rx="{w * 0.75}" ry="12" fill="#2f4a22" opacity="0.28"/>')
    d.add(f'<path d="M{x} {base} L{x + 4} {base - h} L{x + w - 4} {base - h} L{x + w} {base} Z" fill="{stone}" stroke="{INK}" stroke-width="4" stroke-opacity="0.55" stroke-linejoin="round"/>')
    for k in range(1, 5):
        yy = base - k * h / 5
        d.add(f'<path d="M{x + 2 + k * 0.8:.0f} {yy:.0f} L{x + w - 2 - k * 0.8:.0f} {yy:.0f}" stroke="{INK}" stroke-width="2.5" opacity="0.22"/>')
    d.add(f'<path d="M{x + w * 0.62} {base - h + 6} L{x + w - 4} {base - h + 6} L{x + w} {base} L{x + w * 0.62} {base} Z" fill="#000" opacity="0.14"/>')
    # the open belfry arch with its bell
    ay = base - h + 22
    aw = w * 0.5
    d.add(f'<path d="{arch_path(x + w / 2 - aw / 2, x + w / 2 + aw / 2, ay + aw / 2, ay + aw * 1.5)}" fill="#3a2e5e" stroke="{INK}" stroke-width="3" stroke-opacity="0.7"/>')
    bx, by = x + w / 2, ay + aw * 0.7
    d.add(f'<path d="M{bx - 8} {by + 10} C{bx - 8} {by - 6} {bx + 8} {by - 6} {bx + 8} {by + 10} L{bx + 11} {by + 14} L{bx - 11} {by + 14} Z" fill="#f0c26a" stroke="{INK}" stroke-width="2.5"/>')
    # a door at the foot
    d.add(f'<path d="M{x + w * 0.38} {base} v-24 a{w * 0.12} {w * 0.12} 0 0 1 {w * 0.24} 0 v24 Z" fill="#7a4e2e" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
    # a pointed roof, with a flag
    d.add(f'<path d="M{x - 8} {base - h} L{x + w / 2} {base - h - 62} L{x + w + 8} {base - h} Z" fill="#5a4a7a" stroke="{INK}" stroke-width="4" stroke-opacity="0.6" stroke-linejoin="round"/>')
    d.add(f'<path d="M{x + w / 2} {base - h - 62} L{x + w + 8} {base - h} L{x + w * 0.72} {base - h} Z" fill="#000" opacity="0.16"/>')
    d.add(f'<path d="M{x + w / 2} {base - h - 62} v-22" stroke="{INK}" stroke-width="3"/><path d="M{x + w / 2} {base - h - 84} l20 6 l-20 7 Z" fill="#c4473f" stroke="{INK}" stroke-width="2"/>')
    # a few bats around the top
    for bx2, by2 in ((x - 26, base - h - 10), (x + w + 24, base - h - 30), (x + w + 4, base - h - 64)):
        d.add(f'<path d="M{bx2 - 12} {by2} q6 -10 12 0 q6 -10 12 0 q-6 4 -12 10 q-6 -6 -12 -10 Z" fill="#3a2e4e"/>')


def door_arch(d, x, base, w=70, h=110):
    """A stone archway standing alone, with a pair of wooden doors that do not open: the Hall of Doors on the map."""
    stone = d.lin([(0, "#c4bacb"), (1, "#8a8098")], 0, 0, 1, 0)
    d.add(f'<ellipse cx="{x + w / 2}" cy="{base + 4}" rx="{w * 0.8}" ry="12" fill="#2f4a22" opacity="0.28"/>')
    d.add(f'<path d="M{x - 10} {base} L{x - 10} {base - h + 30} A{w / 2 + 10} {w / 2 + 10} 0 0 1 {x + w + 10} {base - h + 30} L{x + w + 10} {base} Z" fill="{stone}" stroke="{INK}" stroke-width="4" stroke-opacity="0.55" stroke-linejoin="round"/>')
    d.add(f'<path d="{arch_path(x + 4, x + w - 4, base - h + 40, base)}" fill="#7a4e2e" stroke="{INK}" stroke-width="3" stroke-opacity="0.7"/>')
    d.add(f'<path d="M{x + w / 2} {base - h + 24} L{x + w / 2} {base}" stroke="{INK}" stroke-width="3" stroke-opacity="0.7"/>')
    d.add(f'<circle cx="{x + w / 2 - 9}" cy="{base - h * 0.34}" r="5" fill="#f0c44a" stroke="{INK}" stroke-width="2"/><circle cx="{x + w / 2 + 9}" cy="{base - h * 0.34}" r="5" fill="#f0c44a" stroke="{INK}" stroke-width="2"/>')
    d.add(f'<path d="M{x + w / 2 - 12} {base - h - 6} L{x + w / 2} {base - h - 22} L{x + w / 2 + 12} {base - h - 6} Z" fill="#6a5a8a" stroke="{INK}" stroke-width="3" stroke-linejoin="round"/>')
    # a few loose doors lying about in the grass
    for dx, dy, rot in ((-70, 6, -14), (w + 54, 10, 10)):
        d.add(f'<g transform="rotate({rot} {x + dx} {base + dy})"><rect x="{x + dx - 14}" y="{base + dy - 30}" width="28" height="46" rx="12" fill="#a8693c" stroke="{INK}" stroke-width="3" stroke-opacity="0.7"/><circle cx="{x + dx + 6}" cy="{base + dy - 6}" r="3" fill="#f0c44a"/></g>')


# ------------------------------------------------------------------------------ world map
def scene_world_map():
    d = Doc(900)
    r = d.r
    sea = d.lin([(0, "#7fc0de"), (1, "#5fa5cc")])
    d.add(f'<rect width="{W}" height="{H}" fill="{sea}"/>')
    land = [(70, 130), (300, 40), (760, 70), (1200, 30), (1650, 60), (1880, 200), (1900, 600), (1860, 960),
            (1500, 1040), (1000, 1030), (560, 1040), (200, 980), (50, 760), (30, 420)]
    d.add(f'<path d="{smooth(land)}" fill="#e9dca8" stroke="#c9b57a" stroke-width="20"/>')
    inner = [(x + (960 - x) * 0.025, y + (540 - y) * 0.04) for x, y in land]
    lg = d.lin([(0, "#b9d67a"), (0.55, "#9cc462"), (1, "#86b055")])
    d.add(f'<path d="{smooth(inner)}" fill="{lg}"/>')
    # soft grass patches
    for _ in range(26):
        x, y = r.uniform(120, 1800), r.uniform(100, 980)
        d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(60, 160):.0f}" ry="{r.uniform(30, 70):.0f}" fill="#c4e08a" opacity="0.35"/>')
    # lake bottom right
    lake = blob_pts(r, 1400, 940, 330, 120, 12, 0.1)
    d.add(f'<path d="{smooth(lake)}" fill="#3f8fb8"/>')
    d.add(f'<path d="{smooth([(x * 0.97 + 1400 * 0.03, y * 0.95 + 940 * 0.05) for x, y in lake])}" fill="#6fc3e6"/>')
    for k in range(4):
        d.add(f'<path d="M{1200 + k * 120} {930 + (k % 2) * 30} q30 -12 60 0" stroke="#bfeaf7" stroke-width="4" fill="none" stroke-linecap="round"/>')
    # rivers
    river(d, [(300, 40), (330, 250), (520, 420), (580, 630), (700, 760), (780, 960)], 34)
    river(d, [(1200, 30), (1230, 300), (1280, 560), (1330, 780), (1380, 900)], 28)
    # forests
    for cx, cy, rx, ry, n in [(300, 880, 280, 90, 26), (240, 560, 160, 220, 18), (700, 330, 150, 80, 12), (1130, 140, 220, 70, 14), (1000, 800, 160, 70, 12)]:
        for _ in range(n):
            x, y = cx + r.uniform(-rx, rx), cy + r.uniform(-ry, ry)
            d.add(pine(x, y, r.uniform(0.35, 0.55), "#3f7a4a", "#2f6040"))
    # swamp
    for _ in range(14):
        x, y = 1010 + r.uniform(-140, 170), 600 + r.uniform(-70, 70)
        d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(24, 60):.0f}" ry="{r.uniform(10, 22):.0f}" fill="#6a8a4a" opacity="0.8"/>')
        d.add(f'<ellipse cx="{x + 4:.0f}" cy="{y:.0f}" rx="{r.uniform(10, 24):.0f}" ry="5" fill="#4f7a58"/>')
    # hills
    for cx, cy in [(780, 240), (180, 330), (860, 900), (560, 690)]:
        for k in range(3):
            d.add(f'<path d="M{cx - 70 + k * 50} {cy + 20} q30 -60 70 0 Z" fill="#a9c76a" stroke="#7a9a48" stroke-width="3" opacity="0.9"/>')
    # mountains (right side) with the dragon's peak
    for cx, base, w, h in [(1500, 330, 260, 190), (1600, 560, 300, 220), (1720, 360, 280, 230), (1560, 190, 240, 170), (1800, 560, 260, 200), (1700, 150, 220, 160), (1480, 560, 220, 150)]:
        mountain(d, cx, base, w, h)
    mountain(d, 1790, 540, 330, 330, ("#9a82b8", "#715a8e"))
    # glowing cave
    d.add(glow(d, 1790, 470, 70, "#ffc777", 0.5))
    # little northern mountains
    for cx in (1000, 1060):
        mountain(d, cx, 260, 180, 130, ("#c3b2d8", "#9d8cb8"))
    # a dark manor on a crooked hill, right below
    d.add(f'<path d="M1650 800 Q1770 700 1880 790 Z" fill="#6b7a50"/>')
    house(d, 1732, 790, 70, 90, "#a8a0b8", "#4a4a6a")
    # an old bell tower on a windy ridge, between the northern mountains: the Bat King's belfry
    d.add(f'<path d="M1256 256 Q1344 196 1432 256 Z" fill="#7f9a5a"/>')
    belfry_tower(d, 1314, 250)
    # a stone archway of doors in the field south of the river: the Door Warden's hall
    d.add(f'<path d="M1030 790 Q1094 730 1160 790 Z" fill="#7f9a5a"/>')
    door_arch(d, 1060, 780)
    # compass
    cx, cy = 150, 910
    d.add(f'<circle cx="{cx}" cy="{cy}" r="70" fill="#f6ead0" stroke="{INK}" stroke-width="5" opacity="0.92"/>')
    for a, c in [(0, "#c4473f"), (90, "#5a4838"), (180, "#5a4838"), (270, "#5a4838")]:
        d.add(f'<path d="M0 -58 L10 0 L-10 0 Z" fill="{c}" stroke="{INK}" stroke-width="2" transform="translate({cx} {cy}) rotate({a})"/>')
    # sea waves & a little boat
    for _ in range(18):
        x, y = r.uniform(0, W), r.uniform(0, H)
        if 80 < x < 1840 and 60 < y < 1000:
            continue
        d.add(f'<path d="M{x:.0f} {y:.0f} q18 -10 36 0 q18 10 36 0" stroke="#d6f0fa" stroke-width="4" fill="none" stroke-linecap="round" opacity="0.8"/>')
    return d.svg(grain=0.12, vignette=0.2)


# ------------------------------------------------------------------------------ road backdrops
def scene_road(kind):
    d = Doc(910 + len(kind))
    r = d.r
    if kind == "road":
        sky(d, "day", (1500, 220))
        hills(d, 560, "#bcd27a", "#9ab85e", 50, 1)
        hills(d, 660, "#a8cc6a", "#86b055", 40, 2)
        ground(d, 740)
        path_ribbon(d, [(-60, 1020), (400, 900), (900, 800), (1400, 760), (1990, 740)], 150)
        for x in (1100, 1250, 1500):
            house(d, x, 700, 60, 50, "#f2dcb6", "#cf6a58", win=False)
        frame_trees(d)
        flowers(d)
    elif kind == "forest":
        sky(d, "dusk")
        for i, (cx, s) in enumerate([(100, 1.4), (400, 1.1), (800, 0.9), (1300, 1.0), (1700, 1.3), (1000, 0.7), (600, 0.7)]):
            d.add(pine(cx, 640 + i * 6, s * 2.2, "#4a7f55", "#35634a"))
        ground(d, 740, "#7aa850", "#3f7040")
        path_ribbon(d, [(-60, 1000), (500, 880), (1000, 800), (1500, 770), (1990, 760)], 130, "#c9ac76", "#8f7448")
        for x, y in [(300, 900), (1550, 880), (1700, 960)]:
            d.add(mushroom(d, x, y, 1.0, "#e8604a", "#f6efe0", 0.0))
        for k in range(5):
            x = 760 + k * 120
            d.add(f'<path d="M{x} 0 L{x - 60} 640 L{x + 20} 640 Z" fill="#fff3b0" opacity="0.14"/>')
        d.add(tree(d, r, -20, 1000, 2.1, ("#3f6e3a", "#55873f", "#7aa44f")))
        d.add(tree(d, r, 1960, 990, 2.0, ("#3f6e3a", "#55873f", "#7aa44f")))
        d.add(motes(r, 24, 0, W, 300, 900, "#fff2a0", 2, 4, avoid=(700, 300, 1880, 1000)))
    elif kind == "mountain":
        sky(d, "gloom")
        mountain(d, 1400, 680, 900, 520, ("#a9a0c4", "#7a7098"))
        mountain(d, 500, 700, 800, 420, ("#b8b0d0", "#8a80a8"))
        mountain(d, 1000, 740, 700, 300, ("#9d94b8", "#6f6690"))
        ground(d, 760, "#a8a58a", "#6f7060", 20)
        path_ribbon(d, [(-60, 1020), (300, 930), (800, 850), (1300, 810), (1990, 780)], 120, "#cdbfa0", "#9a8a68")
        for _ in range(26):
            x, y = r.uniform(0, W), r.uniform(790, 1060)
            if 700 < x < 1880 and y < 1000:
                continue
            d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(20, 50):.0f}" ry="{r.uniform(12, 26):.0f}" fill="#8f8a9a" stroke="{INK}" stroke-width="3" stroke-opacity="0.4"/>')
        d.add(pine(120, 1000, 2.4, "#4a6a58", "#385046"))
        d.add(pine(1820, 980, 2.0, "#4a6a58", "#385046"))
    elif kind == "river":
        sky(d, "day", (1550, 200))
        hills(d, 580, "#bcd27a", "#9ab85e", 50, 1)
        ground(d, 740)
        river(d, [(-60, 900), (500, 820), (1000, 790), (1500, 830), (1990, 790)], 150)
        d.add(f'<path d="M820 740 Q960 650 1100 740 L1100 790 L820 790 Z" fill="#b8b2a0" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
        for x in (900, 1000, 1080):
            d.add(f'<ellipse cx="{x}" cy="{930 if x != 1000 else 960}" rx="46" ry="20" fill="#9a9aa4" stroke="{INK}" stroke-width="3" stroke-opacity="0.5"/>')
        d.add(cattails(d.r, 120, 1000, 6) if False else "")
        frame_trees(d)
        flowers(d)
    else:  # swamp
        sky(d, "gloom")
        for cx, s in [(200, 1.3), (900, 0.9), (1500, 1.2), (1750, 1.0)]:
            d.add(tree(d, r, cx, 700, s * 1.4, ("#4a6a45", "#5f7f4f", "#7a9a5f"), "#4a3a2a"))
        g = d.lin([(0, "#6e8a55"), (1, "#3f5a40")])
        d.add(f'<path d="M0 720 Q480 700 960 730 Q1440 750 1920 710 L1920 {H} L0 {H} Z" fill="{g}"/>')
        for _ in range(6):
            x, y = r.uniform(200, 1700), r.uniform(800, 1040)
            d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(120, 240):.0f}" ry="{r.uniform(30, 50):.0f}" fill="#5f8f78" stroke="#3f6a5a" stroke-width="4"/>')
            d.add(lily_pad(d, x - 40, y, 22, False))
        for x in (100, 400, 1300, 1800):
            d.add(cattails(r, x, 1000, 5))
        d.add(motes(r, 22, 0, W, 400, 950, "#d8f8a8", 2, 4, avoid=(700, 300, 1880, 1000)))
        d.add(f'<rect width="{W}" height="{H}" fill="#cfe8c0" opacity="0.10"/>')
    return finish(d)


# ------------------------------------------------------------------------------ places
def scene_meadow():
    d = Doc(920); r = d.r
    sky(d, "day", (400, 220))
    hills(d, 520, "#c4dc84", "#a2c468", 50, 1)
    hills(d, 640, "#b0d070", "#8fb85a", 40, 2)
    ground(d, 740)
    for _ in range(40):
        x, y = r.uniform(0, W), r.uniform(780, 1070)
        if 720 < x < 1880 and y < 1000:
            continue
        col = r.choice(["#ff9fb8", "#ffd36a", "#c7a6ff", "#ffffff"])
        d.add(f'<path d="M{x:.0f} {y + 28:.0f} v-26" stroke="#5f9a45" stroke-width="4"/><circle cx="{x:.0f}" cy="{y:.0f}" r="10" fill="{col}" stroke="{INK}" stroke-width="2" stroke-opacity="0.4"/><circle cx="{x:.0f}" cy="{y:.0f}" r="3.5" fill="#e09a2a"/>')
    for bx, by in [(1200, 360), (1500, 300), (1700, 420)]:
        d.add(f'<ellipse cx="{bx}" cy="{by}" rx="12" ry="8" fill="#ffd23a" stroke="{INK}" stroke-width="2"/><path d="M{bx - 8} {by - 6} q-6 -14 6 -12 M{bx + 4} {by - 8} q6 -14 -4 -12" stroke="#cfe9f5" stroke-width="5" fill="none"/>')
    d.add(tree(d, r, 1960, 980, 1.9)); d.add(tree(d, r, -30, 990, 1.6))
    d.add(motes(r, 14, 0, W, 300, 900, "#fff2a0", 2, 4, avoid=(700, 300, 1880, 1000)))
    return finish(d)


def scene_fairy_ring():
    d = Doc(921); r = d.r
    sky(d, "night")
    for i, cx in enumerate([80, 380, 1500, 1820, 900]):
        d.add(pine(cx, 640 + i * 5, 2.2 + (i % 2) * 0.3, "#2f5a5a", "#234848"))
    ground(d, 760, "#4f8a68", "#2f5a48")
    d.add(glow(d, 1300, 800, 520, "#9ffff0", 0.28, ry=160))
    for k in range(14):
        a = 2 * math.pi * k / 14
        d.add(mushroom(d, 1300 + math.cos(a) * 330, 830 + math.sin(a) * 90, 1.0 + math.sin(a) * 0.15, "#7ff0e0", "#d8f0ee", 0.45))
    for x, y, s in [(220, 960, 1.6), (520, 1020, 1.2), (1700, 1010, 1.4)]:
        d.add(mushroom(d, x, y, s, "#ff9fd6", "#f6e8f0", 0.4))
    d.add(motes(r, 40, 0, W, 300, 1000, "#d8fff0", 2, 5, avoid=(700, 300, 1880, 1000)))
    return finish(d, 0.35)


def scene_mossbrook():
    d = Doc(922); r = d.r
    sky(d, "day", (360, 220))
    hills(d, 560, "#bcd27a", "#9ab85e", 50, 1)
    ground(d, 740)
    river(d, [(-60, 990), (500, 930), (1000, 960), (1500, 1000), (1990, 980)], 90)
    for x, h, c in [(1000, 110, "#f2dcb6"), (1190, 130, "#efd0a8"), (1380, 100, "#f6e6c4"), (1560, 125, "#f2dcb6"), (1740, 95, "#efd0a8")]:
        house(d, x, 770 + (x % 3) * 10, 110, h, c, r.choice(["#cf6a58", "#7aa04a", "#c4883a"]), lit=False)
    path_ribbon(d, [(600, 1000), (1000, 860), (1400, 820), (1900, 800)], 80)
    d.add(f'<path d="M520 960 Q620 880 720 960" fill="none" stroke="#a89a82" stroke-width="30"/>')
    frame_trees(d); flowers(d)
    return finish(d)


def scene_old_bridge():
    d = Doc(923); r = d.r
    sky(d, "day", (1500, 200))
    hills(d, 600, "#bcd27a", "#9ab85e", 50, 1)
    ground(d, 760)
    river(d, [(-60, 860), (500, 840), (1000, 850), (1500, 840), (1990, 860)], 240)
    # bridge
    d.add(f'<path d="M560 780 Q960 600 1360 780 L1360 840 L560 840 Z" fill="#b8b2a0" stroke="{INK}" stroke-width="5" stroke-opacity="0.55"/>')
    d.add(f'<path d="M640 840 Q960 720 1280 840 Z" fill="#6fc3e6"/>')
    d.add(stone_blocks(r, 560, 650, 1360, 800, 70, 34, "#b8b2a0", "#d2ccba", "#8f8a78", 0.5))
    for x in range(580, 1360, 90):
        d.add(f'<ellipse cx="{x}" cy="{720 + abs(x - 960) * 0.2:.0f}" rx="22" ry="12" fill="#6f9a45" opacity="0.7"/>')
    for x in (600, 1320):
        d.add(f'<rect x="{x}" y="640" width="40" height="150" fill="#a8a290" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    d.add(cattails(r, 160, 1000, 6)); d.add(cattails(r, 1760, 1010, 6))
    frame_trees(d)
    return finish(d)


def scene_sunken_crypt():
    d = Doc(924); r = d.r
    sky(d, "gloom")
    hills(d, 560, "#8a9a88", "#6a7a70", 50, 1)
    ground(d, 740, "#7a8a68", "#4a5a48")
    # stone entrance in a mound
    d.add(f'<path d="M1100 780 Q1300 560 1500 540 Q1700 560 1900 780 Z" fill="#6b7a60" stroke="{INK}" stroke-width="4" stroke-opacity="0.4"/>')
    d.add(f'<path d="{arch_path(1360, 1640, 700, 780)}" fill="#1f2230" stroke="{INK}" stroke-width="6"/>')
    d.add(glow(d, 1500, 740, 180, "#7fffd8", 0.35))
    for k in range(5):
        d.add(f'<path d="M{1340 + k * 15} {800 + k * 26} h{320 - k * 30} v26 h{-(320 - k * 30)} Z" fill="#8a8fa0" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    for x, y in [(1100, 800), (1700, 810), (1860, 820)]:
        d.add(f'<path d="M{x} {y + 50} v-56 a22 22 0 0 1 44 0 v56 Z" fill="#a8aab8" stroke="{INK}" stroke-width="4" stroke-opacity="0.55"/><path d="M{x + 22} {y - 30} v30 M{x + 10} {y - 14} h24" stroke="#6f7284" stroke-width="5"/>')
    d.add(pine(120, 1000, 2.2, "#3a5a4a", "#2a4a3a")); d.add(motes(r, 18, 0, W, 400, 950, "#c8fff0", 2, 4, avoid=(700, 300, 1880, 1000)))
    return finish(d, 0.35)


def scene_hermit_hill():
    d = Doc(925); r = d.r
    sky(d, "day", (400, 200))
    hills(d, 640, "#bcd27a", "#9ab85e", 40, 1)
    d.add(f'<path d="M900 1000 Q1300 400 1700 460 Q1900 520 1990 1000 Z" fill="{d.lin([(0, "#a8d070"), (1, "#7fae52")])}" stroke="{INK}" stroke-width="4" stroke-opacity="0.3"/>')
    d.add(f'<circle cx="1480" cy="760" r="70" fill="#7a4e2e" stroke="{INK}" stroke-width="6"/><circle cx="1480" cy="760" r="52" fill="#a8693c"/><circle cx="1500" cy="765" r="7" fill="#f6d36a"/>')
    d.add(f'<path d="M1480 820 Q1300 900 1000 1000" fill="none" stroke="#e6c98b" stroke-width="60" stroke-linecap="round"/>')
    d.add(f'<path d="M1585 560 v-60 h26 v60 Z" fill="#8a7a6a" stroke="{INK}" stroke-width="3"/>')
    for k in range(4):
        d.add(f'<circle cx="{1600 - k * 6}" cy="{480 - k * 40}" r="{14 + k * 5}" fill="#fff" opacity="{0.8 - k * 0.15:.2f}"/>')
    ground(d, 900, "#a9cf6c", "#5e9440", 10)
    for x in (200, 440, 640):
        d.add(f'<path d="M{x} 1000 q-6 -90 10 -160" stroke="#5f9a45" stroke-width="5" fill="none"/>')
    d.add(tree(d, r, -20, 1000, 1.8)); flowers(d, 20, 900)
    return finish(d)


def scene_pennywhistle():
    d = Doc(926); r = d.r
    sky(d, "day", (420, 220))
    hills(d, 580, "#bcd27a", "#9ab85e", 50, 1)
    ground(d, 760, "#c8d890", "#8fb060", 14)
    path_ribbon(d, [(-60, 1000), (700, 900), (1300, 860), (1990, 840)], 160, "#e6d3a8", "#b8a070")
    cols = ["#e2574c", "#4fa3d9", "#ffcf4a", "#7fcf6a"]
    for i, x in enumerate([980, 1220, 1460, 1700]):
        d.add(f'<rect x="{x}" y="660" width="170" height="120" fill="#e9d2a8" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
        for k in range(6):
            c = cols[i % 4] if k % 2 == 0 else "#fff3dc"
            d.add(f'<path d="M{x - 10 + k * 32} 640 h32 l-6 54 h-20 Z" fill="{c}" stroke="{INK}" stroke-width="2" stroke-opacity="0.4"/>')
        d.add(f'<rect x="{x + 20}" y="720" width="130" height="60" fill="#8a5a33" stroke="{INK}" stroke-width="3" stroke-opacity="0.5"/>')
        d.add(apple(x + 40, 715) + apple(x + 80, 712, 1.0, "#f2b43a") + apple(x + 120, 715))
    for k in range(10):
        d.add(f'<path d="M{760 + k * 120} 520 q50 40 100 0" fill="none" stroke="#6b4a32" stroke-width="3"/>')
        d.add(f'<path d="M{790 + k * 120} 546 l12 26 l12 -26 Z" fill="{cols[k % 4]}"/>')
    d.add(barrel(120, 960, 120, 150)); d.add(crate(300, 1000, 120, 100))
    frame_trees(d, left=False)
    return finish(d)


def scene_gloomwood_mine():
    d = Doc(927); r = d.r
    sky(d, "gloom")
    mountain(d, 1300, 760, 1400, 560, ("#9a90ac", "#6f6684"), snow=False)
    ground(d, 790, "#8a8a6a", "#5a5a48", 20)
    d.add(f'<path d="M1180 810 L1180 640 Q1300 560 1420 640 L1420 810 Z" fill="#1c1a24" stroke="#6b4a32" stroke-width="16"/>')
    d.add(f'<path d="M1160 810 L1160 630 L1440 630 L1440 810" fill="none" stroke="#7a5230" stroke-width="22"/>')
    d.add(glow(d, 1300, 740, 140, "#ffd27a", 0.4))
    for x, y in [(1305, 770), (1215, 790)]:
        d.add(f'<path d="M{x - 40} {y + 30} h110" stroke="#5a4a3a" stroke-width="6"/>')
    d.add(f'<path d="M700 1000 Q1000 900 1250 810" fill="none" stroke="#a89a82" stroke-width="10"/><path d="M760 1040 Q1050 940 1320 830" fill="none" stroke="#a89a82" stroke-width="10"/>')
    d.add(f'<path d="M520 960 l40 -30 h70 l30 30 Z" fill="#6a5a4a" stroke="{INK}" stroke-width="4"/><circle cx="560" cy="965" r="18" fill="#4a4a54"/><circle cx="640" cy="965" r="18" fill="#4a4a54"/>')
    for x in (1500, 1600, 1760):
        d.add(crystal_cluster(d, r, x, 800, 0.5, [("#9fe0ff", "#e8f8ff", "#4a7aa8")], n=3, spread=40, glow_op=0.3) if False else "")
    d.add(pine(120, 1000, 2.0, "#4a6a58", "#385046"))
    return finish(d, 0.32)


def scene_spider_caves():
    d = Doc(928); r = d.r
    sky(d, "gloom")
    d.add(f'<path d="M1000 800 Q1250 360 1560 340 Q1820 380 1990 800 Z" fill="{d.lin([(0, "#6a6080"), (1, "#4a4260")])}" stroke="{INK}" stroke-width="4" stroke-opacity="0.4"/>')
    d.add(f'<path d="M1280 800 Q1300 560 1560 540 Q1780 580 1820 800 Z" fill="#17141f"/>')
    ground(d, 790, "#6f7a58", "#434a38", 14)
    for k in range(9):
        d.add(f'<path d="M1560 540 L{1280 + k * 66} 800" stroke="#f4f4ff" stroke-width="2" opacity="0.75"/>')
    for rr in (60, 120, 190, 260):
        d.add(f'<path d="M{1560 - rr * 1.0} {800 - rr * 0.1} Q1560 {540 + rr * 0.3} {1560 + rr} {800 - rr * 0.1}" fill="none" stroke="#f4f4ff" stroke-width="2" opacity="0.6"/>')
    d.add(f'<path d="M1500 0 V420" stroke="#f4f4ff" stroke-width="2"/><ellipse cx="1500" cy="440" rx="22" ry="26" fill="#2a2030"/>')
    d.add(motes(r, 16, 0, W, 300, 950, "#e8e8ff", 2, 3, avoid=(700, 300, 1880, 1000)))
    d.add(pine(100, 1000, 2.3, "#3f5a4a", "#2f483a"))
    return finish(d, 0.35)


def scene_lantern_hollow():
    d = Doc(929); r = d.r
    sky(d, "dusk")
    hills(d, 560, "#9a8ab8", "#7a6a98", 50, 1)
    ground(d, 750, "#88a860", "#4f7040", 14)
    path_ribbon(d, [(-60, 1000), (600, 900), (1200, 840), (1990, 820)], 150, "#d8bd8a", "#a88c5a")
    for x, h in [(1000, 130), (1230, 150), (1480, 120), (1700, 140)]:
        house(d, x, 790, 130, h, r.choice(["#f2dcb6", "#e4cfa6"]), r.choice(["#8a5a9a", "#5a7aa0", "#b46a58"]), lit=True)
    for i, x in enumerate(range(160, 1900, 150)):
        y = 330 + 50 * math.sin(i * 0.9)
        col = r.choice(["#ffd77a", "#ffb08a", "#9fe0ff", "#ffe9a0"])
        d.add(f'<path d="M{x} 0 V{y - 30}" stroke="#6b4a32" stroke-width="3"/>')
        d.add(glow(d, x, y, 90, col, 0.45) + f'<path d="M{x - 18} {y} h36 l-6 40 h-24 Z" fill="{col}" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
    d.add(tree(d, r, -20, 1000, 1.7, ("#4f6a6a", "#5f8a7a", "#7aa88a")))
    return finish(d, 0.28)


def scene_whispering_falls():
    d = Doc(930); r = d.r
    sky(d, "day", (400, 180))
    d.add(f'<path d="M1100 760 L1180 120 Q1500 60 1820 140 L1920 760 Z" fill="{d.lin([(0, "#8a9a88"), (1, "#5f705f")])}" stroke="{INK}" stroke-width="4" stroke-opacity="0.35"/>')
    g = d.lin([(0, "#e8f8ff"), (1, "#9fd8f0")])
    d.add(f'<path d="M1420 100 L1560 100 Q1600 420 1620 760 L1380 760 Q1400 420 1420 100 Z" fill="{g}" stroke="#7ab8d8" stroke-width="5"/>')
    for k in range(6):
        d.add(f'<path d="M{1440 + k * 22} 130 Q{1450 + k * 24} 420 {1420 + k * 36} 740" stroke="#fff" stroke-width="4" fill="none" opacity="0.8"/>')
    d.add(glow(d, 1500, 760, 220, "#ffffff", 0.7, ry=60))
    ground(d, 790, "#8fc060", "#4f8a45", 10)
    pool = blob_pts(r, 1500, 880, 420, 90, 12, 0.08)
    d.add(f'<path d="{smooth(pool)}" fill="#3f8fb8"/>')
    d.add(f'<path d="{smooth([(x * 0.95 + 75, y * 0.92 + 70) for x, y in pool])}" fill="#7acbe8"/>')
    for x, y, rx in [(1250, 900, 36), (1740, 880, 30), (1500, 960, 40)]:
        d.add(lily_pad(d, x, y, rx, False))
    d.add(f'<path d="M1500 770 q-40 -50 -10 -100" stroke="#fff" stroke-width="3" fill="none" opacity="0.6"/>')
    d.add(tree(d, r, -20, 1000, 1.8)); flowers(d, 22, 900)
    return finish(d)


def scene_fishers_dock():
    d = Doc(931); r = d.r
    sky(d, "dusk", (420, 480))
    sea = d.lin([(0, "#7ac4e8"), (1, "#3f86b0")])
    d.add(f'<rect x="0" y="560" width="{W}" height="{H - 560}" fill="{sea}"/>')
    for k in range(18):
        x, y = r.uniform(0, W), r.uniform(600, 1050)
        d.add(f'<path d="M{x:.0f} {y:.0f} q18 -10 36 0 q18 10 36 0" stroke="#d6f0fa" stroke-width="4" fill="none" stroke-linecap="round" opacity="0.7"/>')
    d.add(glow(d, 420, 600, 600, "#ffe2a0", 0.4, ry=70))
    d.add(f'<path d="M0 600 L{W} 600" stroke="#fff" stroke-width="3" opacity="0.4"/>')
    # dock planks
    d.add(f'<path d="M-40 1080 L360 760 L1900 760 L1990 1080 Z" fill="#b8854a" stroke="{INK}" stroke-width="5" stroke-opacity="0.55"/>')
    for k in range(9):
        t = k / 9
        d.add(f'<path d="M{-40 + 400 * t} {1080 - 320 * t} L{1990 - 90 * (1 - t)} {1080 - 320 * t}" stroke="#7a4e2a" stroke-width="3" opacity="0.6"/>')
    for x in (560, 960, 1360, 1760):
        d.add(f'<rect x="{x}" y="700" width="26" height="130" fill="#7a4e2a" stroke="{INK}" stroke-width="3" stroke-opacity="0.5"/>')
    # boat
    d.add(f'<path d="M1150 640 Q1400 700 1650 640 L1580 700 Q1400 730 1240 700 Z" fill="#c4473f" stroke="{INK}" stroke-width="4"/>')
    d.add(f'<path d="M1400 640 V400 M1400 420 L1560 600 H1400 Z" stroke="{INK}" stroke-width="4" fill="#fbe9cf"/>')
    d.add(barrel(1740, 980, 110, 140)); d.add(crate(1580, 1000, 110, 90))
    d.add(f'<path d="M300 0 V300" stroke="{INK}" stroke-width="3" opacity="0"/>')
    for x, y in [(160, 700), (260, 660)]:
        d.add(f'<path d="M{x} {y} q18 -22 36 -6 q18 -18 36 6" fill="none" stroke="#ffffff" stroke-width="5" stroke-linecap="round"/>')
    return finish(d)


def scene_windy_pass():
    d = Doc(932); r = d.r
    sky(d, "storm")
    mountain(d, 300, 820, 900, 640, ("#a09ab8", "#6f6890"))
    mountain(d, 1650, 820, 1000, 700, ("#9a94b2", "#68608a"))
    ground(d, 800, "#a8a58a", "#6f7060", 10)
    path_ribbon(d, [(600, 1060), (900, 900), (960, 800), (960, 700)], 220, "#cdbfa0", "#9a8a68")
    for k in range(12):
        y = 160 + k * 60
        d.add(f'<path d="M{200 + (k * 137) % 1500} {y} q80 -26 160 0 q40 10 80 -6" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.6"/>')
    d.add(f'<path d="M180 1000 l60 -20 v-60 l-30 -40 l-30 40 Z" fill="#6a5a4a"/>')
    d.add(pine(1800, 1000, 2.2, "#4a6a58", "#385046"))
    return finish(d, 0.3)


def scene_inkwell_cellars():
    d = Doc(933); r = d.r
    sky(d, "gloom")
    ground(d, 790, "#6a5a7a", "#3f3550", 10)
    d.add(f'<rect x="900" y="480" width="900" height="320" fill="#6a6078" stroke="{INK}" stroke-width="5" stroke-opacity="0.5"/>')
    d.add(stone_blocks(r, 900, 480, 1800, 800, 90, 44, "#6a6078", "#857a96", "#4a4258", 0.7))
    d.add(f'<path d="{arch_path(1260, 1440, 640, 800)}" fill="#1a1428" stroke="{INK}" stroke-width="6"/>')
    d.add(glow(d, 1350, 760, 130, "#9a7aff", 0.4))
    for x in (1020, 1620):
        d.add(barrel(x, 800, 130, 160, "#4a3a5a", "#2f2540", "#7a6a9a"))
    d.add(f'<path d="M1080 800 Q1150 840 1230 800" fill="#1a1230"/>')
    d.add(f'<path d="M300 1060 Q500 960 700 980 Q900 1000 1000 900" fill="none" stroke="#1a1230" stroke-width="42" stroke-linecap="round"/>')
    for x, y in [(600, 960), (400, 1030)]:
        d.add(f'<ellipse cx="{x}" cy="{y}" rx="60" ry="16" fill="#2a2050"/><circle cx="{x - 20}" cy="{y - 4}" r="8" fill="#6a5aa8" opacity="0.7"/>')
    d.add(torch(d, 860, 600, 1.0)); d.add(torch(d, 1840, 600, 1.0))
    return finish(d, 0.35)


def scene_lair_peak():
    d = Doc(934); r = d.r
    sky(d, "dusk", (260, 460))
    mountain(d, 1420, 900, 1500, 860, ("#9a82b8", "#6a5288"))
    mountain(d, 360, 900, 900, 400, ("#b8a0cc", "#8a72a6"))
    d.add(f'<path d="M1360 330 Q1420 270 1480 330 L1500 400 Q1420 370 1340 400 Z" fill="#17101f"/>')
    d.add(glow(d, 1420, 380, 150, "#ff9a4a", 0.55))
    for k in range(5):
        d.add(f'<circle cx="{1440 + k * 26}" cy="{250 - k * 50}" r="{18 + k * 8}" fill="#6a5a6a" opacity="{0.6 - k * 0.1:.2f}"/>')
    ground(d, 840, "#8a7f78", "#5a524f", 16)
    path_ribbon(d, [(300, 1070), (800, 950), (1200, 760), (1380, 560), (1420, 420)], 90, "#cdbfa0", "#8a7a5a")
    for x, y in [(170, 960), (1700, 940), (1800, 1020)]:
        d.add(f'<ellipse cx="{x}" cy="{y}" rx="46" ry="26" fill="#8f8a9a" stroke="{INK}" stroke-width="3" stroke-opacity="0.4"/>')
    d.add(f'<path d="M1100 700 q8 -30 -2 -60 m24 70 q8 -26 0 -50" stroke="#e8e0c8" stroke-width="8" fill="none" stroke-linecap="round"/>')
    return finish(d, 0.3)


def scene_lair_manor():
    d = Doc(935); r = d.r
    sky(d, "night")
    for i, cx in enumerate([80, 300, 1700, 1860]):
        d.add(tree(d, r, cx, 780 + i * 6, 1.6, ("#2f3a50", "#3f4a66", "#566280"), "#2a2230"))
    ground(d, 800, "#4a5a50", "#2a3a38", 12)
    path_ribbon(d, [(500, 1070), (900, 950), (1250, 860), (1280, 800)], 130, "#7a7488", "#4a4660")
    x, base = 1050, 810
    d.add(f'<rect x="{x}" y="{base - 330}" width="470" height="330" fill="#4a4660" stroke="{INK}" stroke-width="5"/>')
    d.add(f'<path d="M{x - 30} {base - 330} L{x + 235} {base - 560} L{x + 500} {base - 330} Z" fill="#2a2640" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>')
    d.add(f'<rect x="{x + 330}" y="{base - 470}" width="60" height="150" fill="#3a3650" stroke="{INK}" stroke-width="4"/>')
    for wx, wy, lit in [(1090, 600, False), (1260, 600, True), (1400, 600, False), (1090, 720, False), (1400, 720, False)]:
        c = "#ffe08a" if lit else "#1a1830"
        d.add(f'<rect x="{wx}" y="{wy}" width="56" height="76" rx="6" fill="{c}" stroke="{INK}" stroke-width="3"/>')
        if lit:
            d.add(glow(d, wx + 28, wy + 38, 110, "#ffd36a", 0.55))
    d.add(f'<path d="M1240 {base} v-90 a40 40 0 0 1 80 0 v90 Z" fill="#2a1e20" stroke="{INK}" stroke-width="4"/>')
    d.add(motes(r, 16, 0, W, 300, 900, "#d8d8ff", 2, 3, avoid=(700, 300, 1880, 1000)))
    return finish(d, 0.4)


def scenes() -> dict:
    out = {"scene_world_map": scene_world_map()}
    for k in ("road", "forest", "mountain", "river", "swamp"):
        out[f"scene_road_{k}"] = scene_road(k)
    for n in ("meadow fairy_ring mossbrook old_bridge sunken_crypt hermit_hill pennywhistle gloomwood_mine "
              "spider_caves lantern_hollow whispering_falls fishers_dock windy_pass inkwell_cellars "
              "lair_peak lair_manor").split():
        out[f"scene_{n}"] = globals()[f"scene_{n}"]()
    return out
