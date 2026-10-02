"""Prop and icon art for The Little Dungeon: gems, coins, keys, doors, chest, cauldron,
potions, ingredients, book, lantern, treasure and the picture-button choice icons.

Same contract as characters.py: sprites() -> {name: (svg, layers or None)}. Layered sprites
share one viewBox so their per-layer images line up when the app stacks them.
House style: chunky shapes, thick warm outline, gradient shading, white highlight, soft shadow.
"""
import math

INK = "#3d2a1c"


# ---------------------------------------------------------------- helpers

def _rgb(c):
    c = c.lstrip("#")
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mix(a, b, t):
    """Blend colour a toward colour b by t (0..1)."""
    x, y = _rgb(a), _rgb(b)
    return "#" + "".join(f"{round(p + (q - p) * t):02x}" for p, q in zip(x, y))


def light(c, t):
    return mix(c, "#ffffff", t)


def dark(c, t):
    return mix(c, "#000000", t)


def ink(c, t=0.62):
    """Outline colour: the object's colour pushed toward the warm dark brown."""
    return mix(c, "#24150b", t)


def doc(w, h, defs, body):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">\n'
            f'<defs>{defs}</defs>\n{body}\n</svg>')


def rg(gid, stops, cx=0.38, cy=0.3, r=0.85, extra=""):
    s = "".join(f'<stop offset="{o}" stop-color="{c}"{f" stop-opacity={chr(34)}{a}{chr(34)}" if a is not None else ""}/>'
                for o, c, a in [(st + (None,))[:3] for st in stops])
    return f'<radialGradient id="{gid}" cx="{cx}" cy="{cy}" r="{r}"{extra}>{s}</radialGradient>'


def lg(gid, stops, x1=0, y1=0, x2=0, y2=1):
    s = "".join(f'<stop offset="{o}" stop-color="{c}"{f" stop-opacity={chr(34)}{a}{chr(34)}" if a is not None else ""}/>'
                for o, c, a in [(st + (None,))[:3] for st in stops])
    return f'<linearGradient id="{gid}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{s}</linearGradient>'


def shadow(cx, cy, rx, ry, op=0.2):
    return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="#000" opacity="{op}"/>'


def outlined(shapes, fill, stroke, w=5):
    """Draw a union of plain shapes (no fill attrs) with one clean outer outline of width w."""
    return (f'<g fill="{stroke}" stroke="{stroke}" stroke-width="{2 * w}" stroke-linejoin="round" '
            f'stroke-linecap="round">{shapes}</g><g fill="{fill}">{shapes}</g>')


def star_pts(cx, cy, R, r, n=5, rot=-90):
    pts = []
    for i in range(2 * n):
        a = math.radians(rot + i * 180 / n)
        rad = R if i % 2 == 0 else r
        pts.append(f"{cx + rad * math.cos(a):.1f},{cy + rad * math.sin(a):.1f}")
    return " ".join(pts)


def star(cx, cy, R, r=None, n=5, rot=-90, attrs=""):
    r = r if r is not None else R * 0.5
    return f'<polygon points="{star_pts(cx, cy, R, r, n, rot)}" stroke-linejoin="round" {attrs}/>'


def sparkle(cx, cy, s, fill="#fff", op=1.0):
    return (f'<path d="M{cx} {cy - s} Q{cx} {cy} {cx + s} {cy} Q{cx} {cy} {cx} {cy + s} '
            f'Q{cx} {cy} {cx - s} {cy} Q{cx} {cy} {cx} {cy - s} Z" fill="{fill}" opacity="{op}"/>')


def heart_d(cx, cy, s):
    """Heart path centred near (cx, cy), roughly 2s wide."""
    p = lambda x, y: f"{cx + x * s:.1f} {cy + y * s:.1f}"
    return (f"M{p(0, 0.95)} C{p(-1.05, 0.3)} {p(-1.05, -0.75)} {p(-0.5, -0.8)} "
            f"C{p(-0.22, -0.83)} {p(-0.03, -0.62)} {p(0, -0.42)} "
            f"C{p(0.03, -0.62)} {p(0.22, -0.83)} {p(0.5, -0.8)} "
            f"C{p(1.05, -0.75)} {p(1.05, 0.3)} {p(0, 0.95)} Z")


GOLD = [(0, "#fff6c2"), (0.45, "#f8cf3a"), (1, "#c98a12")]
GOLD_INK = "#6b420e"


# ---------------------------------------------------------------- gems

GEM_COLORS = {"red": "#e23b3b", "blue": "#2f7de1", "green": "#36b24a",
              "yellow": "#f5c518", "purple": "#8e4ad8", "orange": "#f07f1a"}


def gem_body(c, prefix="g"):
    """A faceted gem in a 160x180 frame (girdle at y=72, tip at y=160). Returns (defs, body)."""
    o = ink(c, 0.6)
    L, l, d, D = light(c, 0.6), light(c, 0.3), dark(c, 0.15), dark(c, 0.32)
    facets = [
        ("18,72 52,36 56,72", l), ("52,36 80,72 56,72", L), ("52,36 108,36 80,72", light(c, 0.45)),
        ("108,36 104,72 80,72", c), ("108,36 142,72 104,72", d),
        ("18,72 56,72 80,160", c), ("56,72 80,72 80,160", l), ("80,72 104,72 80,160", d),
        ("104,72 142,72 80,160", D),
    ]
    shape = "52,36 108,36 142,72 80,160 18,72"
    defs = rg(f"{prefix}Shine", [(0, "#fff", 0.55), (1, "#fff", 0)], cx=0.3, cy=0.25, r=0.6)
    body = (
        f'<polygon points="{shape}" fill="{o}" stroke="{o}" stroke-width="10" stroke-linejoin="round"/>'
        + "".join(f'<polygon points="{p}" fill="{f}"/>' for p, f in facets)
        + f'<polygon points="{shape}" fill="url(#{prefix}Shine)"/>'
        + f'<g stroke="{o}" stroke-width="2.5" stroke-opacity="0.45" fill="none" stroke-linejoin="round">'
          f'<path d="M18 72 L142 72 M52 36 L56 72 L80 160 L80 72 L52 36 M108 36 L104 72 L80 160 M108 36 L80 72 L56 72 M104 72 L80 72"/></g>'
        + f'<polygon points="58,42 76,42 62,62" fill="#fff" opacity="0.85"/>'
        + f'<path d="M30 80 L50 80 L66 128" stroke="#fff" stroke-width="5" stroke-linecap="round" fill="none" opacity="0.55"/>'
    )
    return defs, body


def gem(c):
    defs, body = gem_body(c)
    return doc(160, 180, defs, f'<g id="all">{shadow(80, 166, 46, 7)}{body}'
               f'{sparkle(130, 34, 13)}{sparkle(144, 54, 6)}</g>')


# ---------------------------------------------------------------- coin, stone, key

def coin():
    defs = rg("coinFace", GOLD, cx=0.35, cy=0.3, r=0.85) + lg("coinEdge", [(0, "#e2a523"), (1, "#a8700c")])
    body = (
        shadow(60, 108, 36, 6)
        + f'<circle cx="60" cy="62" r="46" fill="url(#coinEdge)" stroke="{GOLD_INK}" stroke-width="5"/>'
        + f'<circle cx="60" cy="56" r="46" fill="url(#coinFace)" stroke="{GOLD_INK}" stroke-width="5"/>'
        + f'<circle cx="60" cy="56" r="35" fill="none" stroke="#c98a12" stroke-width="3.5" stroke-dasharray="1 6.2" stroke-linecap="round"/>'
        + star(61, 60, 24, 11, attrs=f'fill="#b97a0e" opacity="0.6"')
        + star(60, 58, 24, 11, attrs=f'fill="#ffe680" stroke="{GOLD_INK}" stroke-width="3.5"')
        + f'<path d="M30 40 A34 34 0 0 1 56 22" stroke="#fff" stroke-width="6" stroke-linecap="round" fill="none" opacity="0.8"/>'
        + sparkle(96, 22, 10)
    )
    return doc(120, 120, defs, f'<g id="all">{body}</g>')


def stone():
    top = "M24 62 C20 34 68 18 112 18 C162 18 200 30 198 60 C196 88 156 98 108 98 C62 98 28 88 24 62 Z"
    side = "M24 62 C22 92 60 116 108 116 C158 116 198 100 198 60 C196 88 156 98 108 98 C62 98 28 88 24 62 Z"
    o = "#3d2f25"
    defs = (rg("stoneTop", [(0, "#d8cdbb"), (0.6, "#ab9d88"), (1, "#7f705e")], cx=0.4, cy=0.3, r=0.8)
            + lg("stoneSide", [(0, "#7a6b5a"), (1, "#54473a")]))
    body = (
        shadow(110, 122, 96, 13)
        + outlined(f'<path d="{top}"/><path d="{side}"/>', "url(#stoneSide)", o)
        + f'<path d="{top}" fill="url(#stoneTop)"/>'
        + f'<path d="{top}" fill="none" stroke="{o}" stroke-width="3" stroke-opacity="0.55"/>'
        + '<path d="M60 108 L62 100 M150 106 L148 98" stroke="#3d2f25" stroke-width="2.5" opacity="0.5" stroke-linecap="round"/>'
        + '<path d="M138 40 L128 54 L136 66" stroke="#5e5143" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round" opacity="0.6"/>'
        + '<ellipse cx="84" cy="38" rx="36" ry="9" fill="#fff" opacity="0.35"/>'
        + "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="#6b5d4d" opacity="0.45"/>'
                  for x, y, r in [(160, 74, 5), (70, 76, 4), (108, 70, 3), (176, 48, 3), (48, 58, 3)])
        + '<path d="M28 66 C36 80 52 88 66 90 C58 82 50 80 44 72 C38 74 32 70 28 66 Z" fill="#6aa84f" opacity="0.85"/>'
        + '<path d="M184 74 C178 84 168 90 158 92 C164 86 170 84 174 78 Z" fill="#6aa84f" opacity="0.8"/>'
    )
    return doc(220, 140, defs, f'<g id="all">{body}</g>')


def key():
    defs = lg("keyGold", GOLD, 0, 0, 0, 1) + rg("keyGem", [(0, "#c9f1ff"), (0.5, "#4fb0f0"), (1, "#1d5fb0")], cx=0.35, cy=0.3)
    shapes = (
        '<circle cx="50" cy="60" r="25"/><circle cx="50" cy="30" r="12"/><circle cx="20" cy="60" r="12"/>'
        '<circle cx="50" cy="90" r="12"/><rect x="70" y="53" width="110" height="14" rx="7"/>'
        '<rect x="78" y="44" width="12" height="32" rx="5"/><rect x="134" y="58" width="14" height="34" rx="4"/>'
        '<rect x="158" y="58" width="14" height="28" rx="4"/><rect x="146" y="74" width="14" height="10"/>'
        '<circle cx="180" cy="60" r="9"/>'
    )
    body = (
        shadow(104, 110, 76, 6, 0.14)
        + outlined(shapes, "url(#keyGold)", GOLD_INK)
        + f'<circle cx="50" cy="60" r="14" fill="url(#keyGem)" stroke="{GOLD_INK}" stroke-width="3.5"/>'
        + '<circle cx="45" cy="55" r="4" fill="#fff" opacity="0.9"/>'
        + f'<g fill="#c98a12" opacity="0.8"><circle cx="50" cy="30" r="4"/><circle cx="20" cy="60" r="4"/><circle cx="50" cy="90" r="4"/></g>'
        + '<path d="M96 56 L174 56" stroke="#fff" stroke-width="3.5" stroke-linecap="round" opacity="0.7"/>'
        + '<path d="M34 42 A22 22 0 0 1 48 36" stroke="#fff" stroke-width="4" stroke-linecap="round" fill="none" opacity="0.8"/>'
        + sparkle(112, 30, 11, "#fff3a0") + sparkle(178, 30, 8, "#fff") + sparkle(96, 92, 7, "#fff3a0")
    )
    return doc(200, 120, defs, f'<g id="all">{body}</g>')


# ---------------------------------------------------------------- rune door

RUNE_SOCKETS = [(90 + 70 * i, 350) for i in range(7)]


def door_rune():
    o = "#2e2a2a"
    defs = (
        rg("frameStone", [(0, "#b3a796"), (0.6, "#8d8273"), (1, "#665c51")], cx=0.5, cy=0.35, r=0.75)
        + lg("slabStone", [(0, "#97a3b2"), (0.55, "#7a8697"), (1, "#5b6575")], 0, 0, 1, 1)
        + lg("bandStone", [(0, "#4f5867"), (1, "#3f4755")])
        + rg("socketHole", [(0, "#14141c"), (0.75, "#262a36"), (1, "#3c4250")], cx=0.5, cy=0.42, r=0.6)
        + '<clipPath id="clipLeft"><rect x="0" y="0" width="300" height="700"/></clipPath>'
        + '<clipPath id="clipRight"><rect x="300" y="0" width="300" height="700"/></clipPath>'
        + '<g id="runeBand">'
        + f'<rect x="40" y="316" width="520" height="68" fill="url(#bandStone)"/>'
        + '<path d="M40 316 L560 316" stroke="#e0b75a" stroke-width="5"/><path d="M40 384 L560 384" stroke="#e0b75a" stroke-width="5"/>'
        + f'<path d="M40 312 L560 312 M40 388 L560 388" stroke="{o}" stroke-width="3"/>'
        + "".join(
            f'<circle cx="{x}" cy="{y}" r="27" fill="#6b7586" stroke="{o}" stroke-width="4"/>'
            f'<circle cx="{x}" cy="{y}" r="21" fill="url(#socketHole)"/>'
            f'<path d="M{x - 19} {y + 8} A21 21 0 0 0 {x + 19} {y + 8}" stroke="#a9b4c4" stroke-width="3" fill="none" opacity="0.7"/>'
            for x, y in RUNE_SOCKETS)
        + '</g>'
    )
    # frame: arch + pillars + threshold, hole where the doors go
    frame_d = ("M10 690 L10 310 A290 290 0 0 1 590 310 L590 690 Z "
               "M50 655 L550 655 L550 310 A250 250 0 0 0 50 310 Z")
    joints = []
    for i in range(1, 12):
        a = math.radians(180 + i * 15)
        if 84 < (i * 15) < 96:
            continue
        x1, y1 = 300 + 250 * math.cos(a), 310 + 250 * math.sin(a)
        x2, y2 = 300 + 290 * math.cos(a), 310 + 290 * math.sin(a)
        joints.append(f"M{x1:.1f} {y1:.1f} L{x2:.1f} {y2:.1f}")
    for k, y in enumerate(range(370, 655, 60)):
        joints.append(f"M10 {y} L50 {y} M550 {y} L590 {y}")
        xj = 30 if k % 2 else 0
        if xj:
            joints.append(f"M{xj} {y} L{xj} {y + 60} M{600 - xj} {y} L{600 - xj} {y + 60}")
    joints.append("M140 655 L140 690 M300 655 L300 690 M460 655 L460 690")
    key_d = "M270 20 L330 20 L322 72 L278 72 Z"
    moss = ('<g fill="#6aa84f" opacity="0.9">'
            '<path d="M10 600 C24 596 34 610 50 606 L50 690 L10 690 Z"/>'
            '<path d="M590 640 C576 630 566 646 550 642 L550 690 L590 690 Z"/>'
            '<path d="M60 210 C70 196 86 196 92 186 C96 200 84 214 70 220 Z"/>'
            '<path d="M40 660 C80 650 110 664 140 656 L140 690 L40 690 Z" opacity="0.7"/></g>')
    frame = (
        f'<g id="frame">{shadow(300, 692, 290, 8, 0.25)}'
        f'<path d="{frame_d}" fill="url(#frameStone)" fill-rule="evenodd" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<path d="{" ".join(joints)}" stroke="{o}" stroke-width="3" opacity="0.55" fill="none"/>'
        f'<path d="M30 310 A270 270 0 0 1 300 40" stroke="#fff" stroke-width="5" opacity="0.25" fill="none" stroke-linecap="round"/>'
        f'<path d="{key_d}" fill="#a39886" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'{star(300, 44, 14, 6, attrs="fill=" + chr(34) + "#e0b75a" + chr(34) + " stroke=" + chr(34) + o + chr(34) + " stroke-width=" + chr(34) + "3" + chr(34))}'
        f'<path d="M50 655 L550 655" stroke="{o}" stroke-width="4"/>'
        f'{moss}'
        f'<g fill="none" stroke="{o}" stroke-width="2.5" opacity="0.5" stroke-linecap="round">'
        f'<path d="M24 430 L34 446 L28 460"/><path d="M572 500 L562 518 L570 530"/><path d="M420 40 L430 58"/></g>'
        f'</g>'
    )

    def half(side):
        if side == "left":
            d = "M50 655 L50 310 A250 250 0 0 1 300 60 L300 655 Z"
            inner = "M74 632 L74 312 A226 226 0 0 1 282 85 L282 632 Z"
            ring_x, clip = 274, "clipLeft"
            panel = "M90 430 L262 430 L262 610 L90 610 Z"
            emblem = (f'<circle cx="190" cy="210" r="46" fill="none" stroke="{o}" stroke-width="5" opacity="0.6"/>'
                      f'<path d="M190 176 A34 34 0 1 0 224 210 A22 22 0 1 1 190 176" fill="none" stroke="{o}" stroke-width="5" opacity="0.6" stroke-linecap="round"/>')
        else:
            d = "M550 655 L550 310 A250 250 0 0 0 300 60 L300 655 Z"
            inner = "M526 632 L526 312 A226 226 0 0 0 318 85 L318 632 Z"
            ring_x, clip = 326, "clipRight"
            panel = "M338 430 L510 430 L510 610 L338 610 Z"
            emblem = (f'<circle cx="410" cy="210" r="46" fill="none" stroke="{o}" stroke-width="5" opacity="0.6"/>'
                      f'{star(410, 212, 30, 13, attrs="fill=" + chr(34) + "none" + chr(34) + " stroke=" + chr(34) + o + chr(34) + " stroke-width=" + chr(34) + "5" + chr(34) + " opacity=" + chr(34) + "0.6" + chr(34))}')
        hl = '<path d="M64 600 L64 320" stroke="#fff" stroke-width="5" opacity="0.25" stroke-linecap="round"/>' if side == "left" else \
             '<path d="M312 600 L312 120" stroke="#fff" stroke-width="5" opacity="0.2" stroke-linecap="round"/>'
        cracks = ('<path d="M120 520 L136 540 L130 560" stroke="#2e2a2a" stroke-width="2.5" fill="none" opacity="0.4"/>' if side == "left"
                  else '<path d="M470 470 L456 488 L462 506" stroke="#2e2a2a" stroke-width="2.5" fill="none" opacity="0.4"/>')
        return (
            f'<g id="{side}">'
            f'<path d="{d}" fill="url(#slabStone)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
            f'<path d="{inner}" fill="none" stroke="#c5ced9" stroke-width="3" opacity="0.6" transform="translate(2 2)"/>'
            f'<path d="{inner}" fill="none" stroke="{o}" stroke-width="5" opacity="0.55" stroke-linejoin="round"/>'
            f'<path d="{panel}" fill="#000" opacity="0.08" stroke="{o}" stroke-width="4" stroke-opacity="0.45"/>'
            f'{emblem}{hl}{cracks}'
            f'<g clip-path="url(#{clip})"><use href="#runeBand"/></g>'
            f'<circle cx="{ring_x}" cy="480" r="9" fill="#5a5048" stroke="{o}" stroke-width="4"/>'
            f'<circle cx="{ring_x}" cy="508" r="22" fill="none" stroke="{o}" stroke-width="12"/>'
            f'<circle cx="{ring_x}" cy="508" r="22" fill="none" stroke="#c9a14a" stroke-width="6"/>'
            f'<path d="M{ring_x - 16} 496 A20 20 0 0 1 {ring_x - 4} 488" stroke="#fff3c4" stroke-width="3" fill="none" stroke-linecap="round"/>'
            f'</g>'
        )

    return doc(600, 700, defs, frame + half("left") + half("right"))


# ---------------------------------------------------------------- coloured doors

DOOR_COLORS = GEM_COLORS


def door(c):
    o = INK
    paint_o = ink(c, 0.62)
    door_d = "M40 260 L40 128 A60 60 0 0 1 160 128 L160 260 Z"
    defs = (
        lg("paint", [(0, light(c, 0.25)), (0.5, c), (1, dark(c, 0.22))], 0, 0, 1, 0)
        + rg("paintShine", [(0, "#fff", 0.35), (1, "#fff", 0)], cx=0.35, cy=0.25, r=0.6)
        + rg("archStone", [(0, "#c3b8a6"), (0.7, "#958a7a"), (1, "#6f6558")], cx=0.5, cy=0.4, r=0.7)
        + rg("lampGlow", [(0, "#ffe9a0", 0.85), (1, "#ffd25a", 0)], cx=0.5, cy=0.5, r=0.5)
        + lg("iron", [(0, "#6b6670"), (1, "#3b3740")])
        + f'<clipPath id="doorClip"><path d="{door_d}"/></clipPath>'
    )
    joints = []
    for deg in (200, 230, 310, 340):
        a = math.radians(deg)
        joints.append(f"M{100 + 60 * math.cos(a):.1f} {128 + 60 * math.sin(a):.1f} L{100 + 78 * math.cos(a):.1f} {128 + 78 * math.sin(a):.1f}")
    joints += ["M22 170 L40 170", "M22 214 L40 214", "M160 170 L178 170", "M160 214 L178 214"]
    studs = "".join(
        f'<circle cx="{x}" cy="{y}" r="4.5" fill="#55505a" stroke="#24212a" stroke-width="2"/><circle cx="{x - 1.5}" cy="{y - 1.5}" r="1.5" fill="#d8d4dc"/>'
        for y in (150, 226) for x in (54, 77, 100, 123, 146))
    body = (
        shadow(100, 270, 92, 7)
        + f'<path d="M22 262 L22 128 A78 78 0 0 1 178 128 L178 262 Z" fill="url(#archStone)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        + f'<path d="{" ".join(joints)}" stroke="{o}" stroke-width="3" opacity="0.5"/>'
        + f'<path d="M86 48 L114 48 L110 72 L90 72 Z" fill="#b0a593" stroke="{o}" stroke-width="4" stroke-linejoin="round"/>'
        + f'<rect x="12" y="256" width="176" height="14" rx="5" fill="#8f8474" stroke="{o}" stroke-width="4"/>'
        + f'<path d="{door_d}" fill="url(#paint)"/>'
        + f'<g clip-path="url(#doorClip)">'
          f'<path d="M71 60 L71 262 M100 60 L100 262 M129 60 L129 262" stroke="{paint_o}" stroke-width="3" opacity="0.55"/>'
          f'<path d="M58 180 C60 196 58 204 60 214 M114 94 C116 110 114 118 116 128 M144 160 C142 176 146 184 144 196" stroke="{paint_o}" stroke-width="2" opacity="0.35" fill="none"/>'
          f'<rect x="30" y="143" width="140" height="15" fill="url(#iron)"/><rect x="30" y="219" width="140" height="15" fill="url(#iron)"/>'
          f'<path d="M30 143 L170 143 M30 158 L170 158 M30 219 L170 219 M30 234 L170 234" stroke="#24212a" stroke-width="2.5"/>'
          f'<path d="{door_d}" fill="url(#paintShine)"/>'
          f'</g>'
        + studs
        + f'<path d="{door_d}" fill="none" stroke="{paint_o}" stroke-width="5" stroke-linejoin="round"/>'
        # little round window with bars
        + f'<circle cx="100" cy="106" r="17" fill="#3a2a2e" stroke="{paint_o}" stroke-width="4"/>'
        + '<circle cx="100" cy="106" r="13" fill="#ffd77a" opacity="0.45"/>'
        + '<path d="M100 90 L100 122 M84 106 L116 106" stroke="#3b3740" stroke-width="4"/>'
        # ring handle
        + '<circle cx="138" cy="186" r="6" fill="#55505a" stroke="#24212a" stroke-width="2.5"/>'
        + '<circle cx="138" cy="200" r="11" fill="none" stroke="#24212a" stroke-width="8"/>'
        + '<circle cx="138" cy="200" r="11" fill="none" stroke="#9b97a3" stroke-width="3.5"/>'
        # lantern above the keystone
        + '<circle cx="100" cy="26" r="30" fill="url(#lampGlow)"/>'
        + f'<path d="M100 48 L100 40" stroke="{o}" stroke-width="4"/>'
        + f'<path d="M100 4 L100 10" stroke="{o}" stroke-width="3"/>'
        + f'<circle cx="100" cy="7" r="4" fill="none" stroke="{o}" stroke-width="3"/>'
        + f'<path d="M86 20 L100 10 L114 20 Z" fill="#c99a3c" stroke="{o}" stroke-width="3.5" stroke-linejoin="round"/>'
        + f'<rect x="89" y="20" width="22" height="20" rx="3" fill="#ffd96a" stroke="{o}" stroke-width="3.5"/>'
        + '<path d="M100 36 C95 32 97 27 100 23 C103 27 105 32 100 36 Z" fill="#fff6c8"/>'
        + f'<rect x="85" y="39" width="30" height="6" rx="2" fill="#c99a3c" stroke="{o}" stroke-width="3"/>'
    )
    return doc(200, 280, defs, f'<g id="all">{body}</g>')


# ---------------------------------------------------------------- chest

def chest():
    o = "#3a2210"
    wood = [(0, "#c7843f"), (0.6, "#9c5d27"), (1, "#6e3d17")]
    defs = (
        lg("woodFront", wood, 0, 0, 0, 1)
        + lg("woodLid", [(0, "#d6944d"), (1, "#8c5222")], 0, 0, 0, 1)
        + lg("woodInside", [(0, "#5a3115"), (1, "#3a1e0c")], 0, 0, 0, 1)
        + lg("chestGold", GOLD, 0, 0, 0, 1)
        + rg("glowFill", [(0, "#fff7c8", 0.95), (0.45, "#ffd75a", 0.6), (1, "#ffc93a", 0)], cx=0.5, cy=0.6, r=0.55)
        + lg("rayFill", [(0, "#fff3b0", 0), (1, "#fff3b0", 0.75)], 0, 0, 0, 1)
    )
    # glow (behind)
    rays = "".join(
        f'<polygon points="130,104 {130 + 150 * math.cos(math.radians(a - 5)):.0f},{104 + 150 * math.sin(math.radians(a - 5)):.0f} '
        f'{130 + 150 * math.cos(math.radians(a + 5)):.0f},{104 + 150 * math.sin(math.radians(a + 5)):.0f}" fill="url(#rayFill)"/>'
        for a in (-160, -130, -105, -75, -50, -20))
    glow = (f'<g id="glow"><ellipse cx="130" cy="100" rx="128" ry="98" fill="url(#glowFill)"/>{rays}'
            f'{sparkle(54, 40, 10, "#fff6c0")}{sparkle(206, 30, 12, "#fff6c0")}{sparkle(160, 14, 7, "#fff")}{sparkle(96, 16, 7, "#fff")}</g>')
    # lid open: inside of the lid standing up behind the box
    lid_open_d = "M50 106 L38 30 C74 16 186 16 222 30 L210 106 Z"
    lid_open = (
        f'<g id="lid-open">'
        f'<path d="{lid_open_d}" fill="url(#woodLid)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<path d="M62 100 L52 40 C84 30 176 30 208 40 L198 100 Z" fill="url(#woodInside)" stroke="{o}" stroke-width="3" stroke-linejoin="round"/>'
        f'<path d="M64 98 L54 44 C86 34 174 34 206 44" stroke="#c0392b" stroke-width="0" fill="none"/>'
        f'<g fill="url(#chestGold)" stroke="{o}" stroke-width="3.5">'
        f'<path d="M58 106 L47 28 L61 25 L72 106 Z"/><path d="M188 106 L199 25 L213 28 L202 106 Z"/></g>'
        f'<path d="M60 92 L52 40" stroke="#fff" stroke-width="3" opacity="0.35" stroke-linecap="round"/>'
        f'</g>'
    )
    coins_in = "".join(
        f'<ellipse cx="{x}" cy="{y}" rx="11" ry="5" fill="url(#chestGold)" stroke="{GOLD_INK}" stroke-width="2"/>'
        for x, y in [(80, 106), (100, 103), (122, 106), (144, 102), (166, 106), (186, 104), (110, 108), (154, 109)])
    base = (
        f'<g id="base">{shadow(130, 200, 108, 10, 0.25)}'
        f'<path d="M44 114 L58 98 L202 98 L216 114 Z" fill="#2a160a" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'{coins_in}'
        f'{sparkle(98, 96, 7)}{sparkle(170, 94, 6)}'
        f'<rect x="40" y="112" width="180" height="84" rx="6" fill="url(#woodFront)" stroke="{o}" stroke-width="5"/>'
        f'<path d="M44 140 L216 140 M44 168 L216 168" stroke="{o}" stroke-width="2.5" opacity="0.4"/>'
        f'<g fill="url(#chestGold)" stroke="{o}" stroke-width="3.5">'
        f'<rect x="56" y="112" width="16" height="84"/><rect x="188" y="112" width="16" height="84"/>'
        f'<rect x="40" y="112" width="180" height="10"/>'
        f'<path d="M112 114 L148 114 L148 140 C148 152 112 152 112 140 Z"/></g>'
        f'<path d="M130 126 m-5 0 a5 5 0 1 1 10 0 a5 5 0 0 1 -2.5 4.3 L134 140 L126 140 L127.5 130.3 A5 5 0 0 1 125 126 Z" fill="{o}"/>'
        f'<g fill="#ffe680" stroke="{o}" stroke-width="1.5">'
        + "".join(f'<circle cx="{x}" cy="{y}" r="3"/>' for x in (64, 196) for y in (134, 158, 182))
        + f'</g><path d="M50 128 L50 186" stroke="#fff" stroke-width="4" opacity="0.25" stroke-linecap="round"/>'
        f'</g>'
    )
    lid_closed_d = "M36 120 L36 92 C36 62 80 54 130 54 C180 54 224 62 224 92 L224 120 Z"
    lid_closed = (
        f'<g id="lid-closed">'
        f'<path d="{lid_closed_d}" fill="url(#woodLid)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<path d="M40 100 C80 94 180 94 220 100" stroke="{o}" stroke-width="2.5" opacity="0.35" fill="none"/>'
        f'<g fill="url(#chestGold)" stroke="{o}" stroke-width="3.5" stroke-linejoin="round">'
        f'<path d="M56 120 L56 64 C61 61 66 60 72 59 L72 120 Z"/><path d="M188 59 C194 60 199 61 204 64 L204 120 L188 120 Z"/>'
        f'<rect x="36" y="110" width="188" height="10"/>'
        f'<path d="M120 104 L140 104 L140 126 L130 134 L120 126 Z"/></g>'
        f'<circle cx="130" cy="116" r="3.5" fill="{o}"/>'
        f'<path d="M58 72 C80 62 110 60 130 60" stroke="#fff" stroke-width="5" opacity="0.4" fill="none" stroke-linecap="round"/>'
        f'</g>'
    )
    return doc(260, 220, defs, lid_open + glow + base + lid_closed)


# ---------------------------------------------------------------- cauldron, spoon

def cauldron():
    o = "#1f1d26"
    defs = (
        rg("potBody", [(0, "#7d8296"), (0.5, "#4b4f62"), (1, "#2a2c38")], cx=0.32, cy=0.25, r=0.85)
        + lg("potRim", [(0, "#8d93a8"), (1, "#4a4e60")], 0, 0, 0, 1)
        + lg("potInside", [(0, "#14121a"), (1, "#3a3646")], 0, 0, 0, 1)
        + rg("brewFill", [(0, "#ffffff"), (0.7, "#ececec"), (1, "#c9c9c9")], cx=0.45, cy=0.35, r=0.7)
        + rg("fireFill", [(0, "#fff3a0"), (0.5, "#ffb030"), (1, "#f0541a")], cx=0.5, cy=0.8, r=0.8)
        + lg("logWood", [(0, "#9a6232"), (1, "#5e3716")])
    )
    back = (
        f'<g id="back">'
        f'<ellipse cx="180" cy="120" rx="140" ry="36" fill="url(#potRim)" stroke="{o}" stroke-width="5"/>'
        f'<ellipse cx="180" cy="120" rx="118" ry="26" fill="url(#potInside)" stroke="{o}" stroke-width="3"/>'
        f'</g>'
    )
    brew = (
        f'<g id="brew">'
        f'<ellipse cx="180" cy="127" rx="112" ry="21" fill="url(#brewFill)" stroke="#8f8f8f" stroke-width="3"/>'
        f'<path d="M120 122 C150 112 200 114 236 124" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.9"/>'
        f'<g fill="#ffffff" stroke="#9a9a9a" stroke-width="2.5"><circle cx="150" cy="114" r="8"/><circle cx="214" cy="112" r="6"/><circle cx="188" cy="104" r="10"/><circle cx="244" cy="120" r="4"/></g>'
        f'<g fill="#fff"><circle cx="185" cy="100" r="3"/><circle cx="147" cy="111" r="2.4"/></g>'
        f'</g>'
    )
    flames = (
        '<path d="M120 300 C108 276 124 262 128 240 C140 258 150 270 146 300 Z" fill="url(#fireFill)"/>'
        '<path d="M150 302 C138 270 160 252 166 222 C182 250 198 268 188 302 Z" fill="url(#fireFill)"/>'
        '<path d="M190 302 C182 274 200 258 204 236 C220 258 232 276 222 302 Z" fill="url(#fireFill)"/>'
        '<path d="M222 300 C218 282 230 270 234 254 C244 270 250 286 244 300 Z" fill="url(#fireFill)"/>'
    )
    front = (
        f'<g id="front">{shadow(180, 304, 146, 12, 0.25)}'
        f'<g stroke="#a33a12" stroke-width="4" stroke-linejoin="round">{flames}</g>'
        f'<g fill="url(#logWood)" stroke="{o}" stroke-width="4">'
        f'<rect x="96" y="290" width="168" height="16" rx="8" transform="rotate(-6 180 298)"/>'
        f'<rect x="96" y="290" width="168" height="16" rx="8" transform="rotate(6 180 298)"/></g>'
        f'<g fill="url(#potBody)" stroke="{o}" stroke-width="5" stroke-linejoin="round">'
        f'<path d="M86 236 L74 290 C76 298 100 298 104 290 L108 250 Z"/><path d="M274 236 L286 290 C284 298 260 298 256 290 L252 250 Z"/></g>'
        f'<path d="M40 120 C28 196 80 276 180 276 C280 276 332 196 320 120 A140 36 0 0 1 40 120 Z" fill="url(#potBody)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<path d="M66 172 C76 222 110 250 150 260" stroke="#fff" stroke-width="7" opacity="0.22" fill="none" stroke-linecap="round"/>'
        f'<ellipse cx="96" cy="190" rx="10" ry="16" fill="#fff" opacity="0.18" transform="rotate(-25 96 190)"/>'
        f'<path d="M40 120 A140 36 0 0 0 320 120 L298 120 A118 26 0 0 1 62 120 Z" fill="url(#potRim)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<path d="M70 140 C110 156 150 158 190 158" stroke="#fff" stroke-width="4" opacity="0.4" fill="none" stroke-linecap="round"/>'
        f'<g fill="url(#potRim)" stroke="{o}" stroke-width="5"><circle cx="34" cy="150" r="13"/><circle cx="326" cy="150" r="13"/></g>'
        f'<g fill="none" stroke="{o}" stroke-width="5"><circle cx="34" cy="150" r="5"/><circle cx="326" cy="150" r="5"/></g>'
        f'</g>'
    )
    return doc(360, 320, defs, back + brew + front)


def spoon():
    o = "#4a2a12"
    defs = lg("spoonWood", [(0, "#e9b979"), (0.5, "#c98a4a"), (1, "#94592a")], 0, 0, 1, 0) + \
        rg("spoonBowl", [(0, "#a0652f"), (1, "#6e4019")], cx=0.5, cy=0.6, r=0.6)
    shapes = ('<rect x="40" y="12" width="20" height="170" rx="10"/>'
              '<ellipse cx="50" cy="208" rx="32" ry="42"/><circle cx="50" cy="20" r="13"/>')
    body = (
        outlined(shapes, "url(#spoonWood)", o)
        + '<ellipse cx="50" cy="212" rx="22" ry="31" fill="url(#spoonBowl)"/>'
        + '<path d="M45 40 L45 160 M54 70 L54 120" stroke="#8a5222" stroke-width="2" opacity="0.5" stroke-linecap="round"/>'
        + '<circle cx="50" cy="20" r="5" fill="none" stroke="#8a5222" stroke-width="2.5" opacity="0.6"/>'
        + '<path d="M46 30 L46 168" stroke="#fff" stroke-width="3" opacity="0.35" stroke-linecap="round"/>'
        + '<path d="M28 196 C30 182 36 176 42 172" stroke="#fff" stroke-width="4" opacity="0.45" fill="none" stroke-linecap="round"/>'
        + '<ellipse cx="42" cy="200" rx="5" ry="9" fill="#fff" opacity="0.25"/>'
    )
    return doc(100, 260, defs, f'<g id="all">{body}</g>')


# ---------------------------------------------------------------- potions

POTIONS = {
    "strength": "#e23b3b",
    "glow": "#f5c518",
    "bubble": "#2f7de1",
    "friendship": "#f06aa8",
}


def potion(kind):
    c = POTIONS[kind]
    o = ink(c, 0.68)
    defs = (
        rg("liquid", [(0, light(c, 0.45)), (0.55, c), (1, dark(c, 0.3))], cx=0.4, cy=0.35, r=0.75)
        + rg("glass", [(0, "#ffffff", 0.75), (1, light(c, 0.7), 0.85)], cx=0.4, cy=0.3, r=0.8)
        + lg("cork", [(0, "#d6a46a"), (1, "#8f5e2c")], 0, 0, 1, 0)
        + rg("halo", [(0, "#fff3a0", 0.9), (1, "#ffd84a", 0)], cx=0.5, cy=0.5, r=0.5)
        + '<clipPath id="bottleIn"><circle cx="80" cy="130" r="46"/></clipPath>'
    )
    shapes = '<circle cx="80" cy="130" r="52"/><rect x="64" y="40" width="32" height="50"/>'
    halo = '<circle cx="80" cy="118" r="80" fill="url(#halo)"/>' if kind == "glow" else ""
    # label symbol
    if kind == "strength":
        arm = "M55 152 L88 152 C97 152 101 145 99 136 L96 121 L83 123 L85 138 C80 126 63 125 57 136 Z"
        sym = (f'<path d="{arm}" fill="#f4b183" stroke="{o}" stroke-width="3.5" stroke-linejoin="round"/>'
               f'<circle cx="89" cy="116" r="9" fill="#f4b183" stroke="{o}" stroke-width="3.5"/>'
               f'<path d="M85 112 L93 112" stroke="{o}" stroke-width="2" stroke-linecap="round" opacity="0.6"/>'
               f'<path d="M63 136 C67 131 73 130 77 132" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.8"/>')
    elif kind == "glow":
        sym = star(80, 135, 19, 9, attrs=f'fill="#ffe14a" stroke="{o}" stroke-width="3.5"') + \
            '<circle cx="74" cy="131" r="2.5" fill="#fff"/>'
    elif kind == "bubble":
        sym = "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="#bfe4ff" stroke="{o}" stroke-width="3"/>'
                      f'<circle cx="{x - r * 0.35:.1f}" cy="{y - r * 0.35:.1f}" r="{r * 0.28:.1f}" fill="#fff"/>'
                      for x, y, r in [(74, 140, 10), (90, 128, 7), (88, 146, 5)])
    else:
        sym = f'<path d="{heart_d(80, 136, 18)}" fill="#ff4f8b" stroke="{o}" stroke-width="3.5" stroke-linejoin="round"/>' \
              '<ellipse cx="72" cy="129" rx="4" ry="3" fill="#fff" opacity="0.85"/>'
    extras = ""
    if kind == "bubble":
        extras = "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="#d6eeff" fill-opacity="0.7" stroke="{o}" stroke-width="2.5"/>'
                         f'<circle cx="{x - r * 0.35:.1f}" cy="{y - r * 0.35:.1f}" r="{r * 0.3:.1f}" fill="#fff"/>'
                         for x, y, r in [(118, 30, 9), (134, 12, 6), (106, 10, 5)])
    elif kind == "glow":
        extras = sparkle(128, 40, 11, "#fff8c0") + sparkle(28, 70, 8, "#fff8c0") + sparkle(136, 168, 7, "#fff")
    elif kind == "friendship":
        extras = f'<path d="{heart_d(126, 34, 10)}" fill="#ff8fbd" stroke="{o}" stroke-width="2.5"/>' \
                 f'<path d="{heart_d(140, 60, 6)}" fill="#ff8fbd" stroke="{o}" stroke-width="2"/>'
    else:
        extras = sparkle(128, 44, 9, "#fff") + sparkle(30, 76, 7, "#fff")
    bubbles_in = "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="#fff" opacity="0.45"/>' for x, y, r in [(58, 150, 4), (104, 156, 3), (66, 112, 3)])
    body = (
        halo
        + shadow(80, 188, 50, 7)
        + outlined(shapes, "url(#glass)", o)
        + f'<g clip-path="url(#bottleIn)"><path d="M20 104 C40 96 60 112 80 104 C100 96 120 112 140 104 L140 190 L20 190 Z" fill="url(#liquid)"/>'
          f'<path d="M20 104 C40 96 60 112 80 104 C100 96 120 112 140 104" stroke="{light(c, 0.55)}" stroke-width="4" fill="none"/>{bubbles_in}</g>'
        + f'<circle cx="80" cy="136" r="27" fill="#fbf1d8" stroke="{o}" stroke-width="3.5"/>'
        + sym
        + f'<rect x="58" y="36" width="44" height="13" rx="6" fill="url(#glass)" stroke="{o}" stroke-width="4.5"/>'
        + f'<path d="M66 38 L64 16 C64 10 96 10 96 16 L94 38 Z" fill="url(#cork)" stroke="{o}" stroke-width="4.5" stroke-linejoin="round"/>'
        + '<path d="M70 18 L70 32" stroke="#fff" stroke-width="3" opacity="0.4" stroke-linecap="round"/>'
        + '<path d="M44 108 C38 120 38 140 44 154" stroke="#fff" stroke-width="6" fill="none" stroke-linecap="round" opacity="0.8"/>'
        + '<path d="M70 56 L70 82" stroke="#fff" stroke-width="4" stroke-linecap="round" opacity="0.7"/>'
        + extras
    )
    return doc(160, 200, defs, f'<g id="all">{body}</g>')


# ---------------------------------------------------------------- ingredients

def ingredient(kind):
    if kind == "leaf":
        o = "#1f4d1c"
        defs = lg("leafFill", [(0, "#9ee07a"), (0.5, "#4fae45"), (1, "#2f7a2b")], 0, 0, 1, 1)
        leaf = "M30 112 C22 66 52 26 116 22 C120 84 88 118 30 112 Z"
        body = (
            shadow(72, 128, 46, 6)
            + f'<path d="M30 112 L16 126" stroke="{o}" stroke-width="11" stroke-linecap="round"/>'
            + f'<path d="M30 112 L16 126" stroke="#5a9a3a" stroke-width="5" stroke-linecap="round"/>'
            + f'<path d="{leaf}" fill="url(#leafFill)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
            + f'<path d="M32 110 C56 84 84 54 112 26" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round" opacity="0.7"/>'
            + f'<path d="M52 88 L44 64 M68 72 L62 46 M86 54 L84 34 M52 88 L80 92 M68 72 L96 76 M86 54 L106 56" stroke="{o}" stroke-width="2.5" fill="none" stroke-linecap="round" opacity="0.5"/>'
            + '<path d="M40 84 C42 62 58 42 80 34" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.45"/>'
        )
    elif kind == "berry":
        o = "#5a1414"
        defs = rg("berryFill", [(0, "#ff9a8a"), (0.5, "#e23b3b"), (1, "#9c1e24")], cx=0.35, cy=0.3, r=0.8)
        berries = [(48, 90, 24), (92, 90, 24), (70, 62, 24), (70, 108, 20)]
        body = (
            shadow(70, 128, 48, 6)
            + f'<path d="M70 40 C70 30 74 22 82 16" stroke="{o}" stroke-width="9" fill="none" stroke-linecap="round"/>'
            + '<path d="M70 40 C70 30 74 22 82 16" stroke="#6b8e23" stroke-width="4" fill="none" stroke-linecap="round"/>'
            + f'<path d="M74 30 C86 16 106 16 116 24 C104 36 88 38 74 30 Z" fill="#5fbf4a" stroke="#1f4d1c" stroke-width="4" stroke-linejoin="round"/>'
            + '<path d="M78 29 C90 24 100 23 110 24" stroke="#1f4d1c" stroke-width="2" fill="none" opacity="0.6"/>'
            + "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="url(#berryFill)" stroke="{o}" stroke-width="5"/>'
                      f'<ellipse cx="{x - r * 0.35:.0f}" cy="{y - r * 0.38:.0f}" rx="{r * 0.28:.0f}" ry="{r * 0.2:.0f}" fill="#fff" opacity="0.85"/>'
                      for x, y, r in berries)
        )
    elif kind == "crystal":
        o = "#123a6e"
        defs = rg("crysShine", [(0, "#fff", 0.6), (1, "#fff", 0)], cx=0.3, cy=0.2, r=0.6)

        def shard(pts, faces):
            return (f'<polygon points="{pts}" fill="{o}" stroke="{o}" stroke-width="10" stroke-linejoin="round"/>'
                    + "".join(f'<polygon points="{p}" fill="{f}"/>' for p, f in faces)
                    + f'<polygon points="{pts}" fill="url(#crysShine)"/>')
        big = shard("72,12 96,40 92,116 52,116 48,40",
                    [("72,12 48,40 64,46", "#bfe6ff"), ("72,12 64,46 80,46", "#8cc8ff"), ("72,12 80,46 96,40", "#3f8fe8"),
                     ("48,40 64,46 62,116 52,116", "#6fb4f5"), ("64,46 80,46 80,116 62,116", "#4f9df0"),
                     ("80,46 96,40 92,116 80,116", "#2a6fc9")])
        small = shard("106,58 122,78 118,118 96,118 94,76",
                      [("106,58 94,76 106,82", "#bfe6ff"), ("106,58 106,82 122,78", "#3f8fe8"),
                       ("94,76 106,82 106,118 96,118", "#6fb4f5"), ("106,82 122,78 118,118 106,118", "#2a6fc9")])
        tiny = shard("32,84 42,96 40,118 26,118 24,94",
                     [("32,84 24,94 32,98", "#bfe6ff"), ("32,84 32,98 42,96", "#3f8fe8"),
                      ("24,94 32,98 32,118 26,118", "#6fb4f5"), ("32,98 42,96 40,118 32,118", "#2a6fc9")])
        body = (shadow(72, 124, 54, 7) + small + tiny + big
                + '<path d="M56 50 L56 104" stroke="#fff" stroke-width="5" stroke-linecap="round" opacity="0.7"/>'
                + sparkle(110, 24, 11) + sparkle(24, 52, 7))
    elif kind == "flower":
        o = "#7a4a08"
        defs = (rg("petal", [(0, "#fff7b0"), (0.6, "#f8d332"), (1, "#e0a612")], cx=0.5, cy=0.9, r=0.95)
                + rg("flowerMid", [(0, "#ffc06a"), (1, "#d9661a")], cx=0.4, cy=0.35, r=0.7))
        petals = "".join(f'<ellipse cx="70" cy="40" rx="17" ry="25" transform="rotate({a} 70 64)"/>' for a in range(0, 360, 60))
        body = (
            shadow(70, 128, 44, 6)
            + '<path d="M70 90 C70 104 68 114 66 122" stroke="#1f4d1c" stroke-width="11" fill="none" stroke-linecap="round"/>'
            + '<path d="M70 90 C70 104 68 114 66 122" stroke="#5a9a3a" stroke-width="5" fill="none" stroke-linecap="round"/>'
            + '<path d="M68 112 C52 100 34 104 26 114 C40 124 58 122 68 112 Z" fill="#5fbf4a" stroke="#1f4d1c" stroke-width="4" stroke-linejoin="round"/>'
            + '<path d="M70 108 C84 96 102 98 110 106 C98 118 80 116 70 108 Z" fill="#5fbf4a" stroke="#1f4d1c" stroke-width="4" stroke-linejoin="round"/>'
            + outlined(petals, "url(#petal)", o)
            + f'<circle cx="70" cy="64" r="16" fill="url(#flowerMid)" stroke="{o}" stroke-width="4"/>'
            + '<g fill="#8a3a0a" opacity="0.6"><circle cx="65" cy="66" r="2"/><circle cx="74" cy="62" r="2"/><circle cx="72" cy="71" r="2"/></g>'
            + '<circle cx="64" cy="58" r="4" fill="#fff" opacity="0.8"/>'
            + '<ellipse cx="62" cy="28" rx="5" ry="8" fill="#fff" opacity="0.6" transform="rotate(-10 62 28)"/>'
        )
    else:  # mushroom
        o = "#3b1a5c"
        defs = (rg("capFill", [(0, "#d8a8ff"), (0.55, "#8e4ad8"), (1, "#5c2a99")], cx=0.38, cy=0.25, r=0.85)
                + lg("stemFill", [(0, "#fff8ea"), (1, "#e4d3b0")], 0, 0, 1, 0))
        body = (
            shadow(70, 126, 44, 7)
            + f'<path d="M54 76 C52 98 50 110 46 122 L94 122 C90 110 88 98 86 76 Z" fill="url(#stemFill)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
            + f'<path d="M14 80 C14 38 42 16 70 16 C98 16 126 38 126 80 C108 90 32 90 14 80 Z" fill="url(#capFill)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
            + "".join(f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 0.85:.1f}" fill="#fff6ff" stroke="{o}" stroke-width="2" stroke-opacity="0.4"/>'
                      for x, y, r in [(48, 46, 9), (84, 34, 7), (104, 60, 8), (70, 66, 6), (30, 70, 5)])
            + '<path d="M30 52 C36 36 50 26 62 24" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.5"/>'
            + '<path d="M60 92 C58 104 57 110 56 116" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.8"/>'
        )
    return doc(140, 140, defs, f'<g id="all">{body}</g>')


# ---------------------------------------------------------------- book, lantern, treasure

def book():
    o = "#3a1a14"
    defs = (
        lg("cover", [(0, "#b0413a"), (1, "#6e1f1d")], 0, 0, 0, 1)
        + rg("pageL", [(0, "#fffbea"), (0.7, "#fbedc4"), (1, "#e8cf92")], cx=0.85, cy=0.45, r=0.9)
        + rg("pageR", [(0, "#fffbea"), (0.7, "#fbedc4"), (1, "#e8cf92")], cx=0.15, cy=0.45, r=0.9)
        + rg("bookGlow", [(0, "#fff6c4", 0.9), (1, "#ffe07a", 0)], cx=0.5, cy=0.5, r=0.5)
    )
    left = "M150 44 C120 28 70 26 30 36 L30 182 C70 172 120 174 150 190 Z"
    right = "M150 44 C180 28 230 26 270 36 L270 182 C230 172 180 174 150 190 Z"
    body = (
        shadow(150, 210, 136, 8, 0.22)
        + '<ellipse cx="150" cy="104" rx="150" ry="104" fill="url(#bookGlow)"/>'
        + f'<path d="M150 58 C110 42 60 40 14 50 L14 200 C60 190 110 192 150 206 C190 192 240 190 286 200 L286 50 C240 40 190 42 150 58 Z" fill="url(#cover)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        + f'<g fill="#f3e2b4" stroke="{o}" stroke-width="2.5">'
        + f'<path d="M150 52 C120 36 66 34 22 44 L22 192 C66 182 120 184 150 198 Z"/>'
        + f'<path d="M150 52 C180 36 234 34 278 44 L278 192 C234 182 180 184 150 198 Z"/></g>'
        + f'<path d="{left}" fill="url(#pageL)" stroke="{o}" stroke-width="4" stroke-linejoin="round"/>'
        + f'<path d="{right}" fill="url(#pageR)" stroke="{o}" stroke-width="4" stroke-linejoin="round"/>'
        + f'<path d="M150 44 L150 190" stroke="{o}" stroke-width="3" opacity="0.6"/>'
        + '<path d="M150 50 L150 186" stroke="#c9a768" stroke-width="10" opacity="0.25"/>'
        + f'<path d="M150 190 L146 216 L152 210 L158 216 L154 190 Z" fill="#e0b13a" stroke="{o}" stroke-width="3" stroke-linejoin="round"/>'
        + f'<g fill="#e0b13a" stroke="{o}" stroke-width="3"><path d="M14 50 L34 46 L34 66 L14 70 Z"/><path d="M286 50 L266 46 L266 66 L286 70 Z"/>'
          f'<path d="M14 200 L34 196 L34 176 L14 180 Z"/><path d="M286 200 L266 196 L266 176 L286 180 Z"/></g>'
        + sparkle(28, 18, 10, "#fff6c0") + sparkle(272, 16, 9, "#fff6c0") + sparkle(240, 206, 6, "#fff")
    )
    return doc(300, 220, defs, f'<g id="all">{body}</g>')


def lantern_art(cx=60, top=8, scale=1.0, orb=False, prefix="lan"):
    """Brass lantern drawn in a 120x170 box around cx; returns (defs, body)."""
    o = "#4a2c0c"
    defs = (
        lg(f"{prefix}Brass", [(0, "#ffe08a"), (0.5, "#d9a336"), (1, "#8f5e14")], 0, 0, 1, 0)
        + rg(f"{prefix}Light", [(0, "#fffbe0"), (0.5, "#ffe27a"), (1, "#ffb72e")], cx=0.5, cy=0.55, r=0.6)
        + rg(f"{prefix}Halo", [(0, "#fff0a0", 0.85), (1, "#ffd24a", 0)], cx=0.5, cy=0.5, r=0.5)
    )
    inner = ""
    if orb:
        rays = "".join(
            f'<path d="M{60 + 15 * math.cos(math.radians(a)):.1f} {94 + 15 * math.sin(math.radians(a)):.1f} L{60 + 24 * math.cos(math.radians(a)):.1f} {94 + 24 * math.sin(math.radians(a)):.1f}"/>'
            for a in range(0, 360, 45))
        inner = (f'<g stroke="#ff9d1a" stroke-width="4" stroke-linecap="round">{rays}</g>'
                 f'<circle cx="60" cy="94" r="13" fill="#fff6b0" stroke="#ff9d1a" stroke-width="3.5"/>'
                 f'<circle cx="56" cy="90" r="4" fill="#fff"/>')
    else:
        inner = ('<path d="M60 116 C44 106 50 88 60 70 C70 88 76 106 60 116 Z" fill="#ffb12e" stroke="#e0701a" stroke-width="3"/>'
                 '<path d="M60 112 C52 104 55 94 60 84 C65 94 68 104 60 112 Z" fill="#fff6c8"/>')
    body = (
        f'<circle cx="60" cy="94" r="62" fill="url(#{prefix}Halo)"/>'
        f'<path d="M44 34 C44 8 76 8 76 34" fill="none" stroke="{o}" stroke-width="11" stroke-linecap="round"/>'
        f'<path d="M44 34 C44 8 76 8 76 34" fill="none" stroke="#d9a336" stroke-width="5" stroke-linecap="round"/>'
        f'<path d="M34 50 L60 28 L86 50 Z" fill="url(#{prefix}Brass)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<rect x="34" y="50" width="52" height="84" rx="8" fill="url(#{prefix}Light)" stroke="{o}" stroke-width="5"/>'
        f'{inner}'
        f'<path d="M34 92 L86 92" stroke="{o}" stroke-width="0"/>'
        f'<g fill="url(#{prefix}Brass)" stroke="{o}" stroke-width="4">'
        f'<rect x="30" y="46" width="60" height="10" rx="4"/><rect x="28" y="130" width="64" height="12" rx="5"/>'
        f'<rect x="33" y="52" width="7" height="80"/><rect x="80" y="52" width="7" height="80"/></g>'
        f'<path d="M46 62 L46 120" stroke="#fff" stroke-width="4" stroke-linecap="round" opacity="0.6"/>'
        f'<circle cx="60" cy="27" r="5" fill="url(#{prefix}Brass)" stroke="{o}" stroke-width="3"/>'
    )
    return defs, body


def lantern():
    defs, body = lantern_art()
    return doc(120, 180, defs, f'<g id="all">{shadow(60, 160, 38, 6)}<g transform="translate(0 10)">{body}</g></g>')


def treasure():
    o = "#5a3606"
    defs = (rg("pile", [(0, "#fff2a6"), (0.5, "#f5c518"), (1, "#b9800f")], cx=0.45, cy=0.2, r=0.9)
            + rg("crownGold", GOLD, cx=0.35, cy=0.3, r=0.9)
            + rg("tGemR", [(0, "#ffb0b0"), (0.5, "#e23b3b"), (1, "#8f1c1c")], cx=0.35, cy=0.3)
            + rg("tGemB", [(0, "#bfe0ff"), (0.5, "#2f7de1"), (1, "#18468f")], cx=0.35, cy=0.3)
            + rg("tGemG", [(0, "#c4f5c8"), (0.5, "#36b24a"), (1, "#1b6e2a")], cx=0.35, cy=0.3)
            + rg("tGemP", [(0, "#e2c6ff"), (0.5, "#8e4ad8"), (1, "#4f2384")], cx=0.35, cy=0.3)
            + rg("tCoin", GOLD, cx=0.35, cy=0.3, r=0.8))
    mound = "M16 178 C30 130 78 102 130 100 C182 102 230 130 244 178 Z"

    def coin(x, y, r=13, tilt=0):
        return (f'<g transform="rotate({tilt} {x} {y})"><ellipse cx="{x}" cy="{y + 3}" rx="{r}" ry="{r * 0.62:.1f}" fill="#b07a10" stroke="{o}" stroke-width="3"/>'
                f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 0.62:.1f}" fill="url(#tCoin)" stroke="{o}" stroke-width="3"/>'
                f'<ellipse cx="{x}" cy="{y}" rx="{r * 0.55:.1f}" ry="{r * 0.32:.1f}" fill="none" stroke="#c98a12" stroke-width="2"/></g>')

    def g(x, y, s, gid):
        return (f'<polygon points="{x - s},{y} {x - s * 0.5},{y - s * 0.7} {x + s * 0.5},{y - s * 0.7} {x + s},{y} {x},{y + s}" '
                f'fill="url(#{gid})" stroke="{o}" stroke-width="3" stroke-linejoin="round"/>'
                f'<polygon points="{x - s * 0.45},{y - s * 0.55} {x},{y - s * 0.55} {x - s * 0.3},{y - s * 0.1}" fill="#fff" opacity="0.8"/>')

    coins = "".join(coin(x, y, r, t) for x, y, r, t in [
        (40, 168, 13, -10), (66, 150, 13, 8), (96, 132, 12, -6), (164, 132, 12, 8), (196, 150, 13, -8),
        (220, 168, 13, 10), (76, 176, 14, 0), (130, 172, 14, 4), (184, 178, 14, -4), (110, 156, 12, 10), (152, 156, 12, -10)])
    crown_d = "M86 104 L78 50 L104 74 L130 36 L156 74 L182 50 L174 104 Z"
    crown = (
        f'<path d="{crown_d}" fill="url(#crownGold)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<rect x="82" y="92" width="96" height="16" rx="5" fill="url(#crownGold)" stroke="{o}" stroke-width="4"/>'
        f'<g fill="#fff6c8" stroke="{o}" stroke-width="3"><circle cx="78" cy="48" r="6"/><circle cx="130" cy="32" r="7"/><circle cx="182" cy="48" r="6"/></g>'
        f'{g(130, 72, 11, "tGemR")}'
        f'<circle cx="104" cy="100" r="5" fill="url(#tGemB)" stroke="{o}" stroke-width="2.5"/>'
        f'<circle cx="130" cy="100" r="5" fill="url(#tGemG)" stroke="{o}" stroke-width="2.5"/>'
        f'<circle cx="156" cy="100" r="5" fill="url(#tGemB)" stroke="{o}" stroke-width="2.5"/>'
        f'<path d="M92 92 L88 60" stroke="#fff" stroke-width="4" stroke-linecap="round" opacity="0.6"/>'
    )
    body = (
        shadow(130, 182, 122, 10, 0.22)
        + f'<path d="{mound}" fill="url(#pile)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        + coins
        + g(52, 132, 12, "tGemB") + g(212, 138, 12, "tGemG") + g(150, 186, 9, "tGemP") + g(100, 186, 9, "tGemR")
        + crown
        + sparkle(212, 92, 11) + sparkle(44, 100, 8) + sparkle(118, 140, 6)
    )
    return doc(260, 200, defs, f'<g id="all">{body}</g>')


# ---------------------------------------------------------------- choice icons

def badge(defs_extra, icon):
    defs = rg("badgeFill", [(0, "#fff8e4"), (0.7, "#f3e1b6"), (1, "#e2c48c")], cx=0.4, cy=0.35, r=0.75) + defs_extra
    body = (
        f'<circle cx="100" cy="103" r="92" fill="#000" opacity="0.15"/>'
        f'<circle cx="100" cy="100" r="92" fill="url(#badgeFill)" stroke="{INK}" stroke-width="5"/>'
        f'<circle cx="100" cy="100" r="80" fill="none" stroke="#c9a568" stroke-width="3" stroke-dasharray="2 9" stroke-linecap="round"/>'
        f'{icon}'
    )
    return doc(200, 200, defs, f'<g id="all">{body}</g>')


def choice_snack():
    o = "#5a300e"
    cx, cy, R = 98, 104, 54
    # cookie outline with two bites out of the top right
    a1, a2 = math.radians(-78), math.radians(-6)
    p1 = (cx + R * math.cos(a1), cy + R * math.sin(a1))
    p2 = (cx + R * math.cos(a2), cy + R * math.sin(a2))
    am = math.radians(-42)
    pm = (cx + R * 0.86 * math.cos(am), cy + R * 0.86 * math.sin(am))
    d = (f"M{p1[0]:.1f} {p1[1]:.1f} A{R} {R} 0 1 0 {p2[0]:.1f} {p2[1]:.1f} "
         f"A20 20 0 0 0 {pm[0]:.1f} {pm[1]:.1f} A20 20 0 0 0 {p1[0]:.1f} {p1[1]:.1f} Z")
    defs = rg("cookieFill", [(0, "#f6c983"), (0.6, "#d9984a"), (1, "#a8662a")], cx=0.4, cy=0.35, r=0.75)
    chips = "".join(f'<path d="M{x} {y - 6} C{x + 7} {y - 4} {x + 7} {y + 5} {x} {y + 6} C{x - 7} {y + 4} {x - 7} {y - 4} {x} {y - 6} Z" fill="#4a2410"/>'
                    for x, y in [(74, 84), (104, 92), (80, 124), (116, 128), (98, 148), (60, 108), (132, 104)])
    crumbs = '<g fill="#d9984a" stroke="#5a300e" stroke-width="2"><circle cx="150" cy="58" r="4"/><circle cx="160" cy="74" r="3"/><circle cx="142" cy="44" r="2.5"/></g>'
    icon = (f'<path d="{d}" fill="url(#cookieFill)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
            f'<path d="M58 88 C62 74 72 64 84 58" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.5"/>'
            f'{chips}{crumbs}')
    return badge(defs, icon)


def choice_song():
    o = "#3d2a1c"
    defs = (rg("luteBody", [(0, "#f2b56a"), (0.6, "#c9772e"), (1, "#8a4a18")], cx=0.4, cy=0.35, r=0.8)
            + lg("noteFill", [(0, "#8e4ad8"), (1, "#5c2a99")]))
    lute = (
        '<g transform="rotate(-38 78 128)">'
        f'<rect x="70" y="40" width="16" height="70" rx="4" fill="#9a5a24" stroke="{o}" stroke-width="5"/>'
        f'<rect x="66" y="26" width="24" height="20" rx="5" fill="#7a4418" stroke="{o}" stroke-width="5"/>'
        f'<g fill="#f5e6c4" stroke="{o}" stroke-width="2"><circle cx="64" cy="31" r="3.5"/><circle cx="64" cy="41" r="3.5"/><circle cx="92" cy="31" r="3.5"/><circle cx="92" cy="41" r="3.5"/></g>'
        f'<path d="M78 96 C108 96 118 124 112 146 C106 168 50 168 44 146 C38 124 48 96 78 96 Z" fill="url(#luteBody)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<circle cx="78" cy="128" r="11" fill="#3a1e0c" stroke="{o}" stroke-width="3"/>'
        f'<rect x="66" y="148" width="24" height="6" rx="2" fill="#6e3d17"/>'
        '<path d="M74 44 L74 151 M78 44 L78 151 M82 44 L82 151" stroke="#fff3d0" stroke-width="1.6"/>'
        '<path d="M56 112 C60 104 66 100 72 100" stroke="#fff" stroke-width="4" fill="none" stroke-linecap="round" opacity="0.5"/>'
        '</g>'
    )
    notes = (
        f'<g stroke="{o}" stroke-width="4" stroke-linejoin="round">'
        f'<path d="M118 50 L158 40 L158 52 L118 62 Z" fill="url(#noteFill)"/>'
        f'<rect x="114" y="54" width="7" height="50" fill="url(#noteFill)"/><rect x="154" y="44" width="7" height="50" fill="url(#noteFill)"/>'
        f'<ellipse cx="110" cy="104" rx="13" ry="10" transform="rotate(-20 110 104)" fill="url(#noteFill)"/>'
        f'<ellipse cx="150" cy="94" rx="13" ry="10" transform="rotate(-20 150 94)" fill="url(#noteFill)"/></g>'
        '<ellipse cx="106" cy="100" rx="4" ry="2.5" fill="#fff" opacity="0.7" transform="rotate(-20 106 100)"/>'
        '<ellipse cx="146" cy="90" rx="4" ry="2.5" fill="#fff" opacity="0.7" transform="rotate(-20 146 90)"/>'
        f'<g stroke="{o}" stroke-width="3.5"><path d="M150 132 L150 158" /><ellipse cx="144" cy="160" rx="9" ry="7" transform="rotate(-20 144 160)" fill="#36b24a"/>'
        f'<path d="M150 132 C158 136 162 142 160 150" fill="none" stroke-linecap="round"/></g>'
    )
    return badge(defs, lute + notes)


def choice_tiptoe():
    o = "#4a2c18"
    defs = rg("footFill", [(0, "#ffe2c8"), (0.6, "#f4b183"), (1, "#d98a5a")], cx=0.4, cy=0.35, r=0.8)

    def foot(x, y, rot, mirror=1):
        toes = [(-12, -34, 6.5), (-1, -38, 6), (9, -35, 5), (16, -29, 4.3), (21, -21, 3.8)]
        t = "".join(f'<circle cx="{tx * mirror}" cy="{ty}" r="{r}"/>' for tx, ty, r in toes)
        sole = f'<path d="M{-14 * mirror} -18 C{-18 * mirror} -30 {18 * mirror} -30 {18 * mirror} -14 C{18 * mirror} 0 {8 * mirror} 6 {8 * mirror} 18 C{8 * mirror} 32 {-14 * mirror} 32 {-12 * mirror} 16 C{-11 * mirror} 4 {-12 * mirror} -8 {-14 * mirror} -18 Z"/>'
        return (f'<g transform="translate({x} {y}) rotate({rot})">'
                + outlined(sole + t, "url(#footFill)", o, 4.5)
                + f'<ellipse cx="{-4 * mirror}" cy="-14" rx="6" ry="4" fill="#fff" opacity="0.5"/></g>')
    lines = (f'<g stroke="{o}" stroke-width="5" stroke-linecap="round" fill="none" opacity="0.75">'
             '<path d="M30 132 L52 126"/><path d="M34 150 L60 143"/><path d="M42 168 L64 160"/>'
             '<path d="M92 72 L112 66"/><path d="M90 90 L106 86"/></g>')
    return badge(defs, lines + foot(82, 136, 18, -1) + foot(132, 86, 18, 1)
                 + sparkle(160, 140, 9, "#ffd24a") + sparkle(60, 60, 7, "#ffd24a"))


def choice_friends():
    o = "#6a1a34"
    defs = rg("heartFill", [(0, "#ffb3cf"), (0.55, "#f05a8f"), (1, "#c22a62")], cx=0.38, cy=0.3, r=0.85)
    icon = (
        f'<path d="{heart_d(100, 104, 62)}" fill="url(#heartFill)" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
        f'<ellipse cx="72" cy="70" rx="12" ry="7" fill="#fff" opacity="0.6" transform="rotate(-30 72 70)"/>'
        f'<ellipse cx="82" cy="98" rx="6" ry="8" fill="{INK}"/><ellipse cx="118" cy="98" rx="6" ry="8" fill="{INK}"/>'
        f'<circle cx="84" cy="95" r="2.4" fill="#fff"/><circle cx="120" cy="95" r="2.4" fill="#fff"/>'
        f'<ellipse cx="68" cy="116" rx="9" ry="5" fill="#ff9ec0"/><ellipse cx="132" cy="116" rx="9" ry="5" fill="#ff9ec0"/>'
        f'<path d="M86 116 C92 128 108 128 114 116" stroke="{INK}" stroke-width="5" fill="none" stroke-linecap="round"/>'
        f'<path d="{heart_d(160, 52, 11)}" fill="#ff8fbd" stroke="{o}" stroke-width="3"/>'
        f'<path d="{heart_d(40, 150, 8)}" fill="#ff8fbd" stroke="{o}" stroke-width="2.5"/>'
    )
    return badge(defs, icon)


def choice_spell():
    o = "#2a1840"
    defs = (lg("wandFill", [(0, "#6b4a8f"), (1, "#3a2458")], 0, 0, 1, 0)
            + rg("wandStar", [(0, "#fffbe0"), (0.6, "#ffe14a"), (1, "#f0a818")], cx=0.4, cy=0.35, r=0.7))
    icon = (
        f'<g transform="rotate(45 100 100)">'
        + outlined('<rect x="92" y="88" width="16" height="92" rx="7"/>', "url(#wandFill)", o)
        + f'<rect x="92" y="88" width="16" height="16" fill="#fff6d8" stroke="{o}" stroke-width="3"/>'
        f'<rect x="92" y="156" width="16" height="10" fill="#e0b13a" stroke="{o}" stroke-width="3"/>'
        f'<path d="M97 110 L97 150" stroke="#fff" stroke-width="3" stroke-linecap="round" opacity="0.4"/>'
        f'</g>'
        + star(84, 78, 36, 16, rot=-100, attrs=f'fill="url(#wandStar)" stroke="#7a4a08" stroke-width="5"')
        + '<circle cx="76" cy="70" r="5" fill="#fff" opacity="0.9"/>'
        + sparkle(140, 52, 14, "#ffd24a") + sparkle(150, 92, 9, "#8e4ad8") + sparkle(46, 128, 10, "#2f7de1")
        + sparkle(112, 34, 7, "#f06aa8") + sparkle(40, 50, 7, "#36b24a")
    )
    return badge(defs, icon)


def choice_light():
    ldefs, lbody = lantern_art(orb=True, prefix="cl")
    icon = (f'<g transform="translate(46 22) scale(0.9)">{lbody}</g>'
            + sparkle(156, 58, 10, "#fff6c0") + sparkle(44, 140, 8, "#fff6c0") + sparkle(152, 150, 7, "#ffd24a"))
    return badge(ldefs, icon)


def choice_lullaby():
    o = "#2b2a5a"
    defs = rg("moonFill", [(0, "#fffbe0"), (0.6, "#ffe27a"), (1, "#f0b52a")], cx=0.35, cy=0.35, r=0.8)
    # crescent: big circle minus offset circle
    c1, r1 = (88, 104), 54
    c2, r2 = (116, 84), 46
    dx, dy = c2[0] - c1[0], c2[1] - c1[1]
    dd = math.hypot(dx, dy)
    a = (r1 * r1 - r2 * r2 + dd * dd) / (2 * dd)
    h = math.sqrt(r1 * r1 - a * a)
    mx, my = c1[0] + a * dx / dd, c1[1] + a * dy / dd
    pA = (mx + h * dy / dd, my - h * dx / dd)
    pB = (mx - h * dy / dd, my + h * dx / dd)
    d = (f"M{pA[0]:.1f} {pA[1]:.1f} A{r1} {r1} 0 1 0 {pB[0]:.1f} {pB[1]:.1f} "
         f"A{r2} {r2} 0 0 1 {pA[0]:.1f} {pA[1]:.1f} Z")

    def z(x, y, s, w):
        return (f'<path d="M{x} {y} L{x + s} {y} L{x} {y + s} L{x + s} {y + s}" fill="none" stroke="{o}" stroke-width="{w + 5}" stroke-linejoin="round" stroke-linecap="round"/>'
                f'<path d="M{x} {y} L{x + s} {y} L{x} {y + s} L{x + s} {y + s}" fill="none" stroke="#9fc4ff" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round"/>')
    icon = (
        f'<path d="{d}" fill="url(#moonFill)" stroke="#7a4a08" stroke-width="5" stroke-linejoin="round"/>'
        '<path d="M50 96 C50 120 62 138 80 146" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.55"/>'
        f'<path d="M66 110 C70 116 78 116 82 110" stroke="#7a4a08" stroke-width="4" fill="none" stroke-linecap="round"/>'
        f'<path d="M80 134 C86 138 94 138 100 134" stroke="#7a4a08" stroke-width="4" fill="none" stroke-linecap="round"/>'
        '<ellipse cx="70" cy="124" rx="7" ry="4" fill="#ffa98a" opacity="0.7"/>'
        + z(128, 104, 20, 6) + z(152, 72, 15, 5) + z(146, 44, 11, 4)
        + star(130, 150, 12, 5.5, attrs=f'fill="#ffe14a" stroke="#7a4a08" stroke-width="3"')
        + star(56, 52, 9, 4, attrs=f'fill="#ffe14a" stroke="#7a4a08" stroke-width="2.5"')
        + sparkle(160, 128, 7, "#fff6c0")
    )
    return badge(defs, icon)


CHOICES = {
    "snack": choice_snack, "song": choice_song, "tiptoe": choice_tiptoe, "friends": choice_friends,
    "spell": choice_spell, "light": choice_light, "lullaby": choice_lullaby,
}


# ---------------------------------------------------------------- registry

def sprites() -> dict:
    out = {}
    for name, c in GEM_COLORS.items():
        out[f"gem_{name}"] = (gem(c), None)
    out["coin"] = (coin(), None)
    out["stone"] = (stone(), None)
    out["key"] = (key(), None)
    out["door_rune"] = (door_rune(), ["frame", "left", "right"])
    for name, c in DOOR_COLORS.items():
        out[f"door_{name}"] = (door(c), None)
    out["chest"] = (chest(), ["base", "lid-closed", "lid-open", "glow"])
    out["cauldron"] = (cauldron(), ["back", "brew", "front"])
    out["spoon"] = (spoon(), None)
    for kind in POTIONS:
        out[f"potion_{kind}"] = (potion(kind), None)
    for kind in ("leaf", "berry", "crystal", "flower", "mushroom"):
        out[f"ingredient_{kind}"] = (ingredient(kind), None)
    out["book"] = (book(), None)
    out["lantern"] = (lantern(), None)
    out["treasure"] = (treasure(), None)
    for kind, fn in CHOICES.items():
        out[f"choice_{kind}"] = (fn(), None)
    return out
