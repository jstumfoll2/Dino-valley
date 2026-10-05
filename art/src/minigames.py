"""Art for the minigames (see docs/art-plan.md): bells, lamps, coins, plates, map pieces, rhyme cards and the like.
Each is a 160x160 icon in the house style from items.py (chunky warm outline, gradient shading, white highlight, soft shadow).
Build with `python3 art/build.py minigames`; ART_RES=somewhere redirects the output so new pieces can be looked at first.
"""
import math

from items import G, INK, S, icon
from props import dark, lg, light, mix, rg, shadow, sparkle, star


def mark(kind, cx, cy, s):
    """The mark that always goes with a color (red triangle, blue ring, green square, yellow plus, purple diamond, orange bar): same as the app's HueMark."""
    o = f'stroke="{INK}" stroke-width="{s * 0.28:.1f}" stroke-linejoin="round" stroke-linecap="round"'
    if kind == "triangle":
        return f'<path d="M{cx} {cy - s} L{cx + s * 0.95} {cy + s * 0.75} L{cx - s * 0.95} {cy + s * 0.75} Z" fill="#fff" {o}/>'
    if kind == "ring":
        return (f'<circle cx="{cx}" cy="{cy}" r="{s * 0.72}" fill="none" stroke="{INK}" stroke-width="{s * 0.62:.1f}"/>'
                f'<circle cx="{cx}" cy="{cy}" r="{s * 0.72}" fill="none" stroke="#fff" stroke-width="{s * 0.36:.1f}"/>')
    if kind == "plus":
        a, b = s * 0.34, s
        return (f'<path d="M{cx - a} {cy - b} H{cx + a} V{cy - a} H{cx + b} V{cy + a} H{cx + a} V{cy + b} H{cx - a} V{cy + a} H{cx - b} V{cy - a} H{cx - a} Z" fill="#fff" {o}/>')
    raise ValueError(kind)


def bell(color, kind):
    return icon(G("g", color, "radial") + lg("r", [(0, light(color, 0.2)), (1, dark(color, 0.35))]) + G("m", "#d9a441"), (
        f'<path d="M80 40 C80 26 98 26 98 38" fill="none" {S("#8a6a3a", 7)}/>'
        f'<path d="M40 110 C40 66 56 42 80 42 C104 42 120 66 120 110 L134 124 L26 124 Z" fill="url(#g)" {S()}/>'
        f'<path d="M26 124 L134 124 C134 132 126 134 80 134 C34 134 26 132 26 124 Z" fill="url(#r)" {S()}/>'
        f'<circle cx="80" cy="140" r="9" fill="url(#m)" {S(INK, 3.5)}/>'
        f'<path d="M54 100 C54 74 62 58 72 52" fill="none" stroke="#fff" stroke-width="7" stroke-linecap="round" opacity="0.6"/>'
        + mark(kind, 82, 92, 15)
    ))


def lamp(on):
    glass = "#ffe680" if on else "#59607a"
    frame = "#8a6a3a" if on else "#4a4f63"
    halo = (f'<circle cx="80" cy="84" r="62" fill="url(#h)"/>' if on else "")
    defs = rg("h", [(0, "#fff3b0", 0.85), (1, "#ffd34d", 0.0)], cx=0.5, cy=0.5, r=0.5) + G("g", glass, "radial")
    rays = (sparkle(30, 40, 10, "#fff6c8") + sparkle(132, 36, 8, "#fff6c8") + sparkle(138, 100, 7, "#fff") if on else "")
    return icon(defs, (
        halo
        + f'<path d="M80 22 C80 12 94 12 94 22" fill="none" {S(frame, 6)}/>'
        + f'<path d="M52 44 L108 44 L98 30 L62 30 Z" fill="{frame}" {S()}/>'
        + f'<path d="M52 44 H108 V112 C108 124 96 130 80 130 C64 130 52 124 52 112 Z" fill="url(#g)" {S()}/>'
        + f'<path d="M50 112 H110 L104 138 H56 Z" fill="{frame}" {S()}/>'
        + f'<path d="M80 52 V112 M66 52 V112 M94 52 V112" stroke="{frame}" stroke-width="3" opacity="0.55"/>'
        + (f'<path d="M80 74 C92 88 94 98 90 106 C86 114 74 114 70 106 C66 98 70 90 80 74 Z" fill="#fffbe0" stroke="#ffb22e" stroke-width="3"/>' if on else "")
        + f'<ellipse cx="66" cy="68" rx="6" ry="14" fill="#fff" opacity="{0.55 if on else 0.25}" transform="rotate(12 66 68)"/>'
        + rays
    ))


def coin(value, color, r):
    face = {1: "#c9783a", 2: "#c8d0dc", 5: "#f2c33a"}[value]
    teeth = "".join(
        f'<circle cx="{80 + (r - 3) * math.cos(a):.1f}" cy="{84 + (r - 3) * math.sin(a):.1f}" r="3.2" fill="{dark(color, 0.18)}"/>'
        for a in (i * math.pi / 12 for i in range(24))
    )
    return icon(G("g", color, "radial"), (
        shadow(80, 84 + r + 8, r * 0.8, 6, 0.18)
        + f'<circle cx="80" cy="84" r="{r}" fill="url(#g)" {S()}/>' + teeth
        + f'<circle cx="80" cy="84" r="{r - 11}" fill="none" stroke="{dark(color, 0.3)}" stroke-width="3"/>'
        + f'<text x="80" y="{84 + r * 0.36:.0f}" text-anchor="middle" font-family="Georgia, serif" font-weight="900" font-size="{r * 1.05:.0f}" fill="{dark(face, 0.55)}" stroke="#fff" stroke-opacity="0.35" stroke-width="1.5">{value}</text>'
        + f'<ellipse cx="{80 - r * 0.45:.0f}" cy="{84 - r * 0.5:.0f}" rx="{r * 0.2:.0f}" ry="{r * 0.1:.0f}" fill="#fff" opacity="0.7" transform="rotate(-35 {80 - r * 0.45:.0f} {84 - r * 0.5:.0f})"/>'
    ), shad=False)


def plate():
    return icon(G("g", "#f4ead8", "radial") + G("r", "#d8c4a0"), (
        f'<ellipse cx="80" cy="104" rx="64" ry="30" fill="url(#r)" {S()}/>'
        f'<ellipse cx="80" cy="98" rx="64" ry="30" fill="url(#g)" {S()}/>'
        f'<ellipse cx="80" cy="98" rx="42" ry="17" fill="none" stroke="#d8c4a0" stroke-width="4"/>'
        f'<ellipse cx="52" cy="88" rx="12" ry="4" fill="#fff" opacity="0.8" transform="rotate(-12 52 88)"/>'
    ))


def chest():
    return icon(G("w", "#b5773a") + G("b", "#e8b83a") + G("d", "#8a5628"), (
        f'<path d="M26 74 C26 44 134 44 134 74 L134 84 L26 84 Z" fill="url(#w)" {S()}/>'
        f'<rect x="26" y="82" width="108" height="50" rx="6" fill="url(#d)" {S()}/>'
        f'<path d="M26 74 C26 44 134 44 134 74" fill="none" stroke="#fff" stroke-opacity="0.25" stroke-width="5"/>'
        f'<rect x="44" y="52" width="14" height="80" fill="url(#b)" {S(INK, 3.5)}/><rect x="102" y="52" width="14" height="80" fill="url(#b)" {S(INK, 3.5)}/>'
        f'<rect x="26" y="78" width="108" height="10" fill="url(#b)" {S(INK, 3.5)}/>'
        f'<rect x="68" y="76" width="24" height="26" rx="5" fill="url(#b)" {S()}/><circle cx="80" cy="88" r="4.5" fill="{INK}"/><rect x="78" y="90" width="4" height="8" rx="2" fill="{INK}"/>'
        + sparkle(122, 40, 11, "#fff6c8") + sparkle(36, 36, 8, "#fff") + sparkle(142, 70, 6, "#ffe680")
    ))


def x_mark():
    return icon("", (
        f'<path d="M36 36 L124 124 M124 36 L36 124" stroke="{INK}" stroke-width="30" stroke-linecap="round"/>'
        f'<path d="M36 36 L124 124 M124 36 L36 124" stroke="#e23b4a" stroke-width="18" stroke-linecap="round"/>'
        f'<path d="M44 40 L70 66" stroke="#fff" stroke-opacity="0.5" stroke-width="5" stroke-linecap="round"/>'
    ), shad=False)


def compass():
    pts = lambda a, r: f"{80 + r * math.sin(math.radians(a)):.1f},{84 - r * math.cos(math.radians(a)):.1f}"
    return icon(G("g", "#f3e4bf", "radial") + G("r", "#d9a441"), (
        f'<circle cx="80" cy="84" r="62" fill="url(#r)" {S()}/><circle cx="80" cy="84" r="52" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<polygon points="{pts(0, 46)} {pts(20, 10)} {pts(90, 10)} {pts(160, 10)} {pts(180, 46)} {pts(200, 10)} {pts(270, 10)} {pts(340, 10)}" fill="#fff" {S(INK, 3)}/>'
        f'<polygon points="{pts(0, 46)} {pts(20, 10)} {pts(0, 0)} {pts(340, 10)}" fill="#e23b4a" {S(INK, 3)}/>'
        f'<polygon points="{pts(90, 46)} {pts(110, 10)} {pts(90, 0)} {pts(70, 10)}" fill="#9a8a6a" {S(INK, 3)}/>'
        f'<polygon points="{pts(270, 46)} {pts(290, 10)} {pts(270, 0)} {pts(250, 10)}" fill="#9a8a6a" {S(INK, 3)}/>'
        f'<polygon points="{pts(180, 46)} {pts(200, 10)} {pts(180, 0)} {pts(160, 10)}" fill="#9a8a6a" {S(INK, 3)}/>'
        f'<circle cx="80" cy="84" r="6" fill="#d9a441" {S(INK, 3)}/>'
    ))


def scroll():
    return icon(G("p", "#f3e4bf") + G("e", "#d9b36a"), (
        f'<rect x="30" y="38" width="100" height="92" rx="5" fill="url(#p)" {S()}/>'
        f'<rect x="24" y="30" width="112" height="16" rx="8" fill="url(#e)" {S()}/><rect x="24" y="122" width="112" height="16" rx="8" fill="url(#e)" {S()}/>'
        f'<path d="M44 108 C54 70 70 110 84 84 C94 66 108 74 114 58" fill="none" stroke="#9a3a2a" stroke-width="4.5" stroke-dasharray="2 9" stroke-linecap="round"/>'
        f'<path d="M106 50 L122 66 M122 50 L106 66" stroke="#e23b4a" stroke-width="6" stroke-linecap="round"/>'
        f'<circle cx="46" cy="110" r="5" fill="#3a8a3a" {S(INK, 2.5)}/>'
    ))


def pie():
    return icon(G("c", "#e8a850", "radial") + G("d", "#c9783a") + G("f", "#d8402e", "radial"), (
        f'<ellipse cx="80" cy="108" rx="58" ry="22" fill="url(#d)" {S()}/>'
        f'<path d="M22 100 C22 66 138 66 138 100 C138 124 108 136 80 136 C52 136 22 124 22 100 Z" fill="url(#c)" {S()}/>'
        f'<path d="M40 86 L120 118 M60 76 L132 104 M28 100 L100 72 M44 120 L132 90" stroke="#b8702a" stroke-width="5" stroke-linecap="round" opacity="0.85"/>'
        f'<circle cx="62" cy="96" r="5" fill="url(#f)"/><circle cx="96" cy="98" r="5" fill="url(#f)"/><circle cx="80" cy="82" r="5" fill="url(#f)"/>'
        f'<ellipse cx="56" cy="84" rx="14" ry="5" fill="#fff" opacity="0.5" transform="rotate(-14 56 84)"/>'
        + sparkle(130, 56, 9, "#fff") + sparkle(34, 62, 6, "#fff")
    ))


def apple():
    return icon(G("g", "#e23b4a", "radial") + G("l", "#4fae45"), (
        f'<path d="M80 52 C60 38 26 48 28 92 C30 126 56 144 80 134 C104 144 130 126 132 92 C134 48 100 38 80 52 Z" fill="url(#g)" {S()}/>'
        f'<path d="M80 52 C80 40 84 34 92 28" fill="none" {S("#8a5628", 6)}/>'
        f'<path d="M86 36 C98 22 120 24 124 34 C112 40 96 42 86 36 Z" fill="url(#l)" {S()}/>'
        f'<ellipse cx="54" cy="78" rx="9" ry="16" fill="#fff" opacity="0.6" transform="rotate(20 54 78)"/>'
    ))


def bat_small():
    c = "#6a5a8a"
    return icon(G("g", c, "radial") + G("w", dark(c, 0.15)), (
        f'<path d="M80 84 C70 60 40 44 14 56 C24 66 26 80 22 94 C34 88 44 96 48 110 C58 98 70 100 80 112 Z" fill="url(#w)" {S()}/>'
        f'<path d="M80 84 C90 60 120 44 146 56 C136 66 134 80 138 94 C126 88 116 96 112 110 C102 98 90 100 80 112 Z" fill="url(#w)" {S()}/>'
        f'<ellipse cx="80" cy="94" rx="24" ry="28" fill="url(#g)" {S()}/>'
        f'<path d="M62 70 L58 44 L74 62 Z M98 70 L102 44 L86 62 Z" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<circle cx="70" cy="88" r="8" fill="#fff" {S(INK, 3)}/><circle cx="90" cy="88" r="8" fill="#fff" {S(INK, 3)}/>'
        f'<circle cx="72" cy="90" r="4" fill="{INK}"/><circle cx="88" cy="90" r="4" fill="{INK}"/>'
        f'<path d="M72 104 Q80 112 88 104" fill="none" {S(INK, 3)}/><path d="M76 104 v5 M84 104 v5" stroke="#fff" stroke-width="3" stroke-linecap="round"/>'
    ))


def stall():
    return icon(G("a", "#e23b4a") + G("w", "#c9953a") + G("c", "#f4ead8"), (
        f'<rect x="28" y="62" width="8" height="74" fill="#8a5628" {S(INK, 3.5)}/><rect x="124" y="62" width="8" height="74" fill="#8a5628" {S(INK, 3.5)}/>'
        f'<rect x="26" y="98" width="108" height="38" rx="5" fill="url(#w)" {S()}/>'
        f'<path d="M20 64 L36 24 H124 L140 64 Z" fill="url(#c)" {S()}/>'
        f'<path d="M36 24 H58 L52 64 H20 Z M80 24 H102 L108 64 H84 Z" fill="url(#a)" {S(INK, 3.5)}/>'
        f'<path d="M20 64 q10 14 20 0 q10 14 20 0 q10 14 20 0 q10 14 20 0 q10 14 20 0 q10 14 20 0 q10 14 20 0" fill="url(#c)" {S(INK, 3.5)}/>'
        f'<circle cx="52" cy="92" r="9" fill="#e23b4a" {S(INK, 3)}/><circle cx="72" cy="94" r="9" fill="#e23b4a" {S(INK, 3)}/><circle cx="92" cy="92" r="9" fill="#f2c33a" {S(INK, 3)}/><circle cx="112" cy="94" r="9" fill="#e23b4a" {S(INK, 3)}/>'
    ))


def eyes(cx, cy, gap, r=5.5):
    return "".join(f'<circle cx="{cx + d}" cy="{cy}" r="{r}" fill="#fff" {S(INK, 2.5)}/><circle cx="{cx + d}" cy="{cy + 1}" r="{r * 0.5}" fill="{INK}"/>' for d in (-gap, gap))


def r_cat():
    c = "#e8a850"
    return icon(G("g", c, "radial"), (
        f'<ellipse cx="80" cy="118" rx="40" ry="28" fill="url(#g)" {S()}/>'
        f'<path d="M116 124 C140 120 146 94 132 84" fill="none" {S(c, 12)}/><path d="M116 124 C140 120 146 94 132 84" fill="none" stroke="{INK}" stroke-width="3" opacity="0.0"/>'
        f'<path d="M42 62 L38 28 L66 46 Z M118 62 L122 28 L94 46 Z" fill="url(#g)" {S()}/>'
        f'<ellipse cx="80" cy="74" rx="40" ry="34" fill="url(#g)" {S()}/>'
        + eyes(80, 70, 17) +
        f'<path d="M74 84 L86 84 L80 91 Z" fill="#e87a8a" {S(INK, 2.5)}/><path d="M80 91 Q72 99 64 94 M80 91 Q88 99 96 94" fill="none" {S(INK, 3)}/>'
        f'<path d="M50 84 L30 80 M50 90 L30 94 M110 84 L130 80 M110 90 L130 94" stroke="{INK}" stroke-width="2.5" stroke-linecap="round"/>'
        f'<path d="M60 52 q4 -8 8 0 M92 52 q4 -8 8 0" fill="none" stroke="#b86a2a" stroke-width="4" stroke-linecap="round"/>'
    ))


def r_hat():
    return icon(G("g", "#7a4fd1") + G("b", "#f2c33a"), (
        f'<ellipse cx="80" cy="112" rx="64" ry="18" fill="#5a3aa0" {S()}/>'
        f'<path d="M44 108 L62 30 C74 22 90 22 100 30 L116 108 C100 120 60 120 44 108 Z" fill="url(#g)" {S()}/>'
        f'<path d="M48 92 C64 104 96 104 112 92 L114 104 C98 116 62 116 46 104 Z" fill="url(#b)" {S(INK, 3.5)}/>'
        + star(80, 62, 12, 5, attrs=f'fill="#ffe680" {S("#c98a12", 2.5)}') + sparkle(30, 56, 8, "#cfe0ff")
    ))


def r_mat():
    return icon(G("g", "#5aa05a") + G("r", "#e8c870"), (
        f'<path d="M18 74 L142 74 L130 128 L30 128 Z" fill="url(#g)" {S()}/>'
        f'<path d="M30 128 v12 M42 128 v12 M54 128 v12 M66 128 v12 M78 128 v12 M90 128 v12 M102 128 v12 M114 128 v12 M126 128 v12" stroke="#c9a040" stroke-width="4" stroke-linecap="round"/>'
        f'<path d="M34 86 L126 86 L120 116 L40 116 Z" fill="none" stroke="url(#r)" stroke-width="5" stroke-dasharray="14 8"/>'
    ))


def r_dog():
    c = "#c99a5a"
    return icon(G("g", c, "radial") + G("e", "#8a5628"), (
        f'<path d="M36 52 C14 56 14 100 34 108 C42 94 44 70 46 56 Z" fill="url(#e)" {S()}/><path d="M124 52 C146 56 146 100 126 108 C118 94 116 70 114 56 Z" fill="url(#e)" {S()}/>'
        f'<ellipse cx="80" cy="76" rx="42" ry="40" fill="url(#g)" {S()}/>'
        f'<ellipse cx="80" cy="96" rx="22" ry="17" fill="#f4ead8" {S(INK, 3.5)}/><ellipse cx="80" cy="88" rx="9" ry="6" fill="{INK}"/><circle cx="76" cy="86" r="2" fill="#fff"/>'
        + eyes(80, 64, 18) +
        f'<path d="M80 94 V100 M70 104 Q80 112 90 104" fill="none" {S(INK, 3)}/><path d="M74 110 Q80 128 86 110 Z" fill="#e87a8a" {S(INK, 2.5)}/>'
    ))


def r_log():
    return icon(G("g", "#b5773a", "radial") + G("w", "#8a5628"), (
        f'<path d="M24 56 H128 V116 H24 Z" fill="url(#w)" {S()}/>'
        f'<ellipse cx="128" cy="86" rx="22" ry="30" fill="url(#g)" {S()}/><ellipse cx="128" cy="86" rx="14" ry="20" fill="none" stroke="#8a5628" stroke-width="3.5"/><ellipse cx="128" cy="86" rx="6" ry="9" fill="none" stroke="#8a5628" stroke-width="3.5"/>'
        f'<path d="M40 70 q18 6 36 0 M52 96 q24 8 50 0" fill="none" stroke="#c99a5a" stroke-width="4" stroke-linecap="round"/>'
        f'<ellipse cx="30" cy="86" rx="8" ry="30" fill="#6a4020" opacity="0.18"/>'
        f'<path d="M52 56 C56 40 72 38 76 48 C66 50 58 54 52 56 Z" fill="#4fae45" {S(INK, 3)}/>'
    ))


def r_frog():
    c = "#4fae45"
    return icon(G("g", c, "radial"), (
        f'<ellipse cx="80" cy="102" rx="52" ry="36" fill="url(#g)" {S()}/>'
        f'<circle cx="52" cy="62" r="20" fill="url(#g)" {S()}/><circle cx="108" cy="62" r="20" fill="url(#g)" {S()}/>'
        f'<circle cx="52" cy="62" r="12" fill="#fff" {S(INK, 3)}/><circle cx="108" cy="62" r="12" fill="#fff" {S(INK, 3)}/><circle cx="54" cy="64" r="6" fill="{INK}"/><circle cx="106" cy="64" r="6" fill="{INK}"/>'
        f'<path d="M44 100 Q80 128 116 100" fill="none" {S(INK, 4)}/><circle cx="68" cy="92" r="2.5" fill="{INK}"/><circle cx="92" cy="92" r="2.5" fill="{INK}"/>'
        f'<ellipse cx="34" cy="132" rx="16" ry="8" fill="url(#g)" {S()}/><ellipse cx="126" cy="132" rx="16" ry="8" fill="url(#g)" {S()}/>'
        f'<circle cx="70" cy="116" r="5" fill="#e87a8a" opacity="0.7"/><circle cx="90" cy="116" r="5" fill="#e87a8a" opacity="0.7"/>'
    ))


def r_pig():
    c = "#f4a6b4"
    return icon(G("g", c, "radial"), (
        f'<path d="M38 50 L34 26 L62 40 Z M122 50 L126 26 L98 40 Z" fill="url(#g)" {S()}/>'
        f'<ellipse cx="80" cy="84" rx="50" ry="44" fill="url(#g)" {S()}/>'
        f'<ellipse cx="80" cy="96" rx="22" ry="16" fill="#f08a9c" {S()}/><ellipse cx="72" cy="96" rx="4" ry="6" fill="{INK}"/><ellipse cx="88" cy="96" rx="4" ry="6" fill="{INK}"/>'
        + eyes(80, 70, 22, 5) +
        f'<circle cx="46" cy="90" r="7" fill="#f08a9c" opacity="0.6"/><circle cx="114" cy="90" r="7" fill="#f08a9c" opacity="0.6"/>'
    ))


def r_wig():
    """A powdered wig: a high dome of white curls with rolled curls at each side."""
    c = "#f0eef8"
    side = "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="url(#g)" {S()}/>' for x, y, r in (
        (38, 92, 17), (30, 118, 15), (122, 92, 17), (130, 118, 15), (46, 126, 13), (114, 126, 13)))
    return icon(G("g", c, "radial") + G("d", "#c8c2dc"), (
        f'<path d="M36 100 C26 56 50 26 80 26 C110 26 134 56 124 100 C112 86 98 80 80 80 C62 80 48 86 36 100 Z" fill="url(#d)" {S()}/>'
        + side +
        f'<circle cx="58" cy="52" r="16" fill="url(#g)" {S()}/><circle cx="80" cy="42" r="16" fill="url(#g)" {S()}/><circle cx="102" cy="52" r="16" fill="url(#g)" {S()}/>'
        f'<path d="M80 58 V86" stroke="#a89ec8" stroke-width="4" stroke-linecap="round"/>'
        f'<path d="M72 40 q8 -8 16 0" fill="none" stroke="#fff" stroke-width="4" stroke-linecap="round"/>'
    ))


def r_sun():
    rays = "".join(
        f'<path d="M{80 + 46 * math.cos(a):.1f} {80 + 46 * math.sin(a):.1f} L{80 + 66 * math.cos(a):.1f} {80 + 66 * math.sin(a):.1f}" stroke="{INK}" stroke-width="13" stroke-linecap="round"/>'
        f'<path d="M{80 + 46 * math.cos(a):.1f} {80 + 46 * math.sin(a):.1f} L{80 + 66 * math.cos(a):.1f} {80 + 66 * math.sin(a):.1f}" stroke="#ffc21f" stroke-width="7" stroke-linecap="round"/>'
        for a in (i * math.pi / 5 for i in range(10))
    )
    return icon(G("g", "#ffd84a", "radial"), rays + f'<circle cx="80" cy="80" r="40" fill="url(#g)" {S()}/>' + eyes(80, 74, 14, 4.5)
                + f'<path d="M64 90 Q80 104 96 90" fill="none" {S(INK, 3.5)}/><circle cx="58" cy="88" r="6" fill="#f08a6a" opacity="0.55"/><circle cx="102" cy="88" r="6" fill="#f08a6a" opacity="0.55"/>', shad=False)


def r_bun():
    return icon(G("g", "#e8a850", "radial") + G("t", "#c9783a"), (
        f'<ellipse cx="80" cy="112" rx="56" ry="20" fill="url(#t)" {S()}/>'
        f'<path d="M24 106 C24 56 136 56 136 106 C136 122 112 130 80 130 C48 130 24 122 24 106 Z" fill="url(#g)" {S()}/>'
        f'<path d="M50 74 q8 -6 16 0 M82 68 q8 -6 16 0 M104 82 q8 -6 14 0" fill="none" stroke="#fff3d6" stroke-width="5" stroke-linecap="round"/>'
        f'<ellipse cx="52" cy="82" rx="12" ry="6" fill="#fff" opacity="0.5" transform="rotate(-20 52 82)"/>'
        f'<circle cx="64" cy="104" r="3" fill="#8a5628"/><circle cx="100" cy="108" r="3" fill="#8a5628"/><circle cx="80" cy="96" r="3" fill="#8a5628"/>'
    ))


def r_bee():
    return icon(G("g", "#ffd84a", "radial") + lg("w", [(0, "#ffffff", 0.9), (1, "#bfe0ff", 0.8)]), (
        f'<ellipse cx="56" cy="58" rx="22" ry="30" fill="url(#w)" {S("#6a8ab0", 3.5)} transform="rotate(-24 56 58)"/><ellipse cx="102" cy="52" rx="22" ry="30" fill="url(#w)" {S("#6a8ab0", 3.5)} transform="rotate(24 102 52)"/>'
        f'<ellipse cx="80" cy="96" rx="40" ry="32" fill="url(#g)" {S()}/>'
        f'<path d="M62 68 Q56 96 64 124 M84 66 Q80 96 86 128 M104 72 Q108 96 102 120" fill="none" stroke="{INK}" stroke-width="10" stroke-linecap="round"/>'
        f'<path d="M118 100 l14 4 l-14 4" fill="{INK}"/>'
        + eyes(54, 92, 0, 5) + f'<path d="M48 70 q-10 -14 -2 -22 M60 68 q4 -16 12 -18" fill="none" {S(INK, 3)}/>'
        f'<path d="M44 108 Q52 114 58 106" fill="none" {S(INK, 3)}/>'
    ))


def r_tree():
    return icon(G("g", "#4fae45", "radial") + G("t", "#8a5628"), (
        f'<path d="M68 150 L72 90 H88 L92 150 Z" fill="url(#t)" {S()}/>'
        f'<circle cx="52" cy="76" r="28" fill="url(#g)" {S()}/><circle cx="108" cy="76" r="28" fill="url(#g)" {S()}/><circle cx="80" cy="46" r="32" fill="url(#g)" {S()}/><circle cx="80" cy="80" r="30" fill="url(#g)" {S()}/>'
        f'<circle cx="64" cy="44" r="6" fill="#fff" opacity="0.35"/><circle cx="96" cy="84" r="5" fill="#e23b4a" {S(INK, 2.5)}/><circle cx="60" cy="86" r="5" fill="#e23b4a" {S(INK, 2.5)}/><circle cx="104" cy="56" r="5" fill="#e23b4a" {S(INK, 2.5)}/>'
    ))


def r_fox():
    c = "#e8782a"
    return icon(G("g", c, "radial"), (
        f'<path d="M34 70 L30 22 L66 48 Z M126 70 L130 22 L94 48 Z" fill="url(#g)" {S()}/><path d="M38 56 L38 36 L52 48 Z M122 56 L122 36 L108 48 Z" fill="{INK}" opacity="0.75"/>'
        f'<path d="M24 84 C24 56 136 56 136 84 C136 104 108 136 80 136 C52 136 24 104 24 84 Z" fill="url(#g)" {S()}/>'
        f'<path d="M24 92 C40 90 56 100 80 138 C104 100 120 90 136 92 C130 112 106 136 80 136 C54 136 30 112 24 92 Z" fill="#fff3e0" {S(INK, 3)}/>'
        + eyes(80, 80, 20, 5) + f'<ellipse cx="80" cy="116" rx="8" ry="6" fill="{INK}"/>'
        f'<path d="M74 78 Q80 84 86 78" fill="none" stroke="none"/>'
    ))


def r_box():
    return icon(G("g", "#c9953a") + G("t", "#e0b060") + G("s", "#a8762a"), (
        f'<path d="M30 56 L80 40 L130 56 L80 72 Z" fill="url(#t)" {S()}/>'
        f'<path d="M30 56 L80 72 V138 L30 120 Z" fill="url(#g)" {S()}/><path d="M130 56 L80 72 V138 L130 120 Z" fill="url(#s)" {S()}/>'
        f'<path d="M64 48 L114 64 L96 70 L48 54 Z" fill="#f4ead8" opacity="0.8" {S(INK, 2.5)}/>'
        f'<path d="M42 92 L68 100" stroke="{INK}" stroke-width="3.5" stroke-linecap="round"/><path d="M100 100 L118 94" stroke="#6a4a1a" stroke-width="3.5" stroke-linecap="round"/>'
    ))


def r_car():
    return icon(G("g", "#e23b4a") + G("w", "#4a4f63", "radial"), (
        f'<path d="M20 108 L20 88 C20 78 30 74 40 72 L56 48 C60 42 66 40 74 40 H100 C108 40 114 44 118 50 L130 72 C142 74 146 82 146 92 V108 Z" fill="url(#g)" {S()}/>'
        f'<path d="M62 52 H78 V72 H50 Z M86 52 H102 C106 52 108 54 110 58 L116 72 H86 Z" fill="#cfe9ff" {S(INK, 3.5)}/>'
        f'<circle cx="48" cy="112" r="17" fill="url(#w)" {S()}/><circle cx="48" cy="112" r="6" fill="#cfd6e4" {S(INK, 2.5)}/><circle cx="120" cy="112" r="17" fill="url(#w)" {S()}/><circle cx="120" cy="112" r="6" fill="#cfd6e4" {S(INK, 2.5)}/>'
        f'<rect x="136" y="88" width="10" height="9" rx="3" fill="#ffe680" {S(INK, 2.5)}/><path d="M26 90 H40" stroke="#fff" stroke-opacity="0.6" stroke-width="4" stroke-linecap="round"/>'
    ))


def r_jar():
    return icon(G("g", "#d8402e", "radial") + G("k", "#c99a5a"), (
        f'<rect x="48" y="36" width="64" height="16" rx="5" fill="url(#k)" {S()}/>'
        f'<path d="M54 52 H106 C116 62 120 76 120 96 C120 126 112 138 100 140 H60 C48 138 40 126 40 96 C40 76 44 62 54 52 Z" fill="#e8f6ff" fill-opacity="0.55" {S()}/>'
        f'<path d="M44 84 C50 78 110 78 116 84 L118 112 C118 130 110 136 100 136 H60 C50 136 42 130 42 112 Z" fill="url(#g)" {S(INK, 3)}/>'
        f'<rect x="56" y="94" width="48" height="26" rx="4" fill="#fff7d6" {S(INK, 2.5)}/><path d="M64 104 H96 M64 112 H88" stroke="#c9a040" stroke-width="3.5" stroke-linecap="round"/>'
        f'<ellipse cx="54" cy="76" rx="5" ry="12" fill="#fff" opacity="0.6" transform="rotate(10 54 76)"/>'
    ))


def bunny():
    """The magic bunny that pops out of the wizard's hat: a round white bunny sitting up, with pink ears."""
    fur = "#fbf6ee"
    return icon(G("b", fur, "radial") + G("e", "#ffb3c4"), (
        f'<path d="M58 70 C44 32 50 8 66 10 C82 12 80 50 74 74 Z" fill="url(#b)" {S()}/><path d="M63 62 C56 36 58 22 66 21 C73 23 72 46 70 62 Z" fill="url(#e)"/>'
        f'<path d="M102 70 C116 32 110 8 94 10 C78 12 80 50 86 74 Z" fill="url(#b)" {S()}/><path d="M97 62 C104 36 102 22 94 21 C87 23 88 46 90 62 Z" fill="url(#e)"/>'
        f'<ellipse cx="80" cy="120" rx="38" ry="30" fill="url(#b)" {S()}/><circle cx="116" cy="132" r="11" fill="#fff" {S(INK, 3.5)}/>'
        f'<ellipse cx="62" cy="144" rx="13" ry="7" fill="url(#b)" {S(INK, 3.5)}/><ellipse cx="98" cy="144" rx="13" ry="7" fill="url(#b)" {S(INK, 3.5)}/>'
        f'<circle cx="80" cy="84" r="30" fill="url(#b)" {S()}/>'
        + eyes(80, 80, 12, 5) +
        f'<path d="M75 92 L85 92 L80 98 Z" fill="#ff8aa0" {S(INK, 2.5)}/><path d="M80 98 Q74 105 68 101 M80 98 Q86 105 92 101" fill="none" {S(INK, 2.8)}/>'
        f'<ellipse cx="60" cy="94" rx="7" ry="4.5" fill="#ffb3c4" opacity="0.7"/><ellipse cx="100" cy="94" rx="7" ry="4.5" fill="#ffb3c4" opacity="0.7"/>'
    ))


def frog():
    """The magic frog that hops across the screen: a friendly green frog with big eyes and a wide smile."""
    c = "#6fbf4a"
    return icon(G("g", c, "radial") + G("l", "#eaf6b8"), (
        f'<ellipse cx="38" cy="132" rx="22" ry="12" fill="url(#g)" {S()}/><ellipse cx="122" cy="132" rx="22" ry="12" fill="url(#g)" {S()}/>'
        f'<ellipse cx="80" cy="112" rx="46" ry="34" fill="url(#g)" {S()}/><ellipse cx="80" cy="122" rx="28" ry="20" fill="url(#l)" opacity="0.9"/>'
        f'<ellipse cx="50" cy="138" rx="14" ry="7" fill="url(#g)" {S(INK, 3.5)}/><ellipse cx="110" cy="138" rx="14" ry="7" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<circle cx="56" cy="66" r="19" fill="url(#g)" {S()}/><circle cx="104" cy="66" r="19" fill="url(#g)" {S()}/>'
        f'<circle cx="56" cy="64" r="12" fill="#fff" {S(INK, 3)}/><circle cx="104" cy="64" r="12" fill="#fff" {S(INK, 3)}/>'
        f'<circle cx="58" cy="66" r="6" fill="{INK}"/><circle cx="102" cy="66" r="6" fill="{INK}"/><circle cx="60" cy="63" r="2" fill="#fff"/><circle cx="104" cy="63" r="2" fill="#fff"/>'
        f'<path d="M48 104 Q80 126 112 104" fill="none" {S(INK, 4)}/>'
        f'<circle cx="72" cy="94" r="2.5" fill="{INK}"/><circle cx="88" cy="94" r="2.5" fill="{INK}"/>'
        f'<ellipse cx="56" cy="99" rx="7" ry="4.5" fill="#ff9aa8" opacity="0.6"/><ellipse cx="104" cy="99" rx="7" ry="4.5" fill="#ff9aa8" opacity="0.6"/>'
    ))


def heart_broken():
    """The heart that pops when a sum is missed on the dice screen: a red heart split in two along a zigzag, the halves a little apart."""
    zig = "80,40 68,64 90,84 70,106 88,126 78,150"
    left = f'0,0 {zig.replace(" ", " ")} 0,160'
    right = f'160,0 {zig} 160,160'
    heart = "M80 138 C16 98 22 44 56 42 C70 42 80 52 80 64 C80 52 90 42 104 42 C138 44 144 98 80 138 Z"
    defs = G("h", "#e23b4a", "radial") + f'<clipPath id="cl"><polygon points="{left}"/></clipPath><clipPath id="cr"><polygon points="{right}"/></clipPath>'
    return icon(defs, (
        f'<g transform="translate(-7 2) rotate(-7 80 140)" clip-path="url(#cl)"><path d="{heart}" fill="url(#h)" {S()}/><path d="M34 62 C36 54 44 50 52 52" stroke="#fff" stroke-width="7" stroke-linecap="round" fill="none" opacity="0.55"/></g>'
        f'<g transform="translate(7 2) rotate(7 80 140)" clip-path="url(#cr)"><path d="{heart}" fill="url(#h)" {S()}/></g>'
    ))


SPRITES = {
    "bell_red": lambda: bell("#e23b4a", "triangle"),
    "bell_blue": lambda: bell("#2f7de1", "ring"),
    "bell_yellow": lambda: bell("#f5c518", "plus"),
    "lamp_off": lambda: lamp(False),
    "lamp_on": lambda: lamp(True),
    "coin_1": lambda: coin(1, "#d98a4a", 34),
    "coin_2": lambda: coin(2, "#d8dee8", 42),
    "coin_5": lambda: coin(5, "#f6cf4a", 54),
    "plate": plate,
    "chest": chest,
    "x_mark": x_mark,
    "compass": compass,
    "scroll": scroll,
    "pie": pie,
    "apple": apple,
    "bat_small": bat_small,
    "bunny": bunny, "frog": frog, "heart_broken": heart_broken,
    "stall": stall,
    "rhyme_cat": r_cat, "rhyme_hat": r_hat, "rhyme_bat": bat_small, "rhyme_mat": r_mat,
    "rhyme_dog": r_dog, "rhyme_log": r_log, "rhyme_frog": r_frog, "rhyme_pig": r_pig, "rhyme_wig": r_wig,
    "rhyme_sun": r_sun, "rhyme_bun": r_bun, "rhyme_bee": r_bee, "rhyme_tree": r_tree,
    "rhyme_fox": r_fox, "rhyme_box": r_box, "rhyme_car": r_car, "rhyme_jar": r_jar,
}


def sprites() -> dict:
    return {f"mini_{k}": (fn(), None) for k, fn in SPRITES.items()}
