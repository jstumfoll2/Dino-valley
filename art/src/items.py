"""Item icons (food, fighting things, tools, charms, story pieces) and the picture icons for choices
and menus. Each icon is one 160x160 SVG in the house style: chunky, thick warm outline, gradient
shading, white highlights, a soft shadow. Gear icons come from gear.py.
"""
import math

from props import dark, doc, light, mix, rg, lg, shadow, sparkle, star

INK = "#3d2a1c"


def S(color=INK, w=4):
    return f'stroke="{color}" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round"'


def G(id_, c, kind="lin"):
    if kind == "radial":
        return rg(id_, [(0, light(c, 0.45)), (0.55, c), (1, dark(c, 0.22))], cx=0.38, cy=0.3, r=0.85)
    return lg(id_, [(0, light(c, 0.3)), (1, dark(c, 0.25))], 0, 0, 1, 1)


def icon(defs, body, shad=True):
    return doc(160, 160, defs, (shadow(80, 148, 44, 7, 0.16) if shad else "") + body)


# ================================================================ items

def berry():
    c = "#e23b4a"
    return icon(G("g", c, "radial") + G("l", "#4fae45"), (
        f'<path d="M80 40 C96 20 120 22 124 34 C108 32 92 40 84 52 Z" fill="url(#l)" {S()}/>'
        + "".join(f'<circle cx="{x}" cy="{y}" r="24" fill="url(#g)" {S()}/><circle cx="{x - 8}" cy="{y - 9}" r="6" fill="#fff" opacity="0.75"/>' for x, y in ((56, 96), (104, 96), (80, 70), (80, 120)))
    ))


def honey_cake():
    return icon(G("c", "#f0b95a", "radial") + G("i", "#fff3d6") + G("h", "#e8920a"), (
        f'<ellipse cx="80" cy="116" rx="54" ry="24" fill="#d68a2a" {S()}/><path d="M26 112 C26 76 134 76 134 112 C134 132 108 142 80 142 C52 142 26 132 26 112 Z" fill="url(#c)" {S()}/>'
        f'<path d="M26 100 C26 70 134 70 134 100 C134 108 126 108 124 100 C120 112 112 112 110 102 C104 116 96 112 94 102 C84 120 72 110 70 100 C60 114 50 108 48 100 C42 112 30 110 26 100 Z" fill="url(#h)" {S()}/>'
        f'<circle cx="80" cy="64" r="11" fill="#e23b4a" {S()}/><circle cx="76" cy="60" r="3.5" fill="#fff"/><path d="M80 54 C80 44 88 40 94 40" fill="none" {S("#4a7a2a", 3.5)}/>'
    ))


def big_potion():
    return icon(G("g", "#e23b5a", "radial") + G("k", "#c99a5a"), (
        f'<path d="M62 50 L62 70 C34 84 28 124 52 140 C68 148 92 148 108 140 C132 124 126 84 98 70 L98 50 Z" fill="#e8f6ff" fill-opacity="0.55" {S()}/>'
        f'<path d="M40 112 C44 90 62 82 80 86 C98 82 118 90 122 112 C124 130 108 142 80 142 C52 142 38 130 40 112 Z" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<rect x="64" y="34" width="32" height="20" rx="5" fill="url(#k)" {S()}/><rect x="60" y="48" width="40" height="8" rx="3" fill="#ffd34d" {S(INK, 3)}/>'
        f'<ellipse cx="62" cy="104" rx="8" ry="14" fill="#fff" opacity="0.6" transform="rotate(14 62 104)"/><circle cx="96" cy="122" r="4" fill="#fff" opacity="0.7"/><circle cx="84" cy="112" r="3" fill="#fff" opacity="0.7"/>'
    ))


def sleep_dust():
    return icon(G("g", "#6a7fd8") + G("b", "#c9b27a"), (
        f'<path d="M58 62 C46 90 40 120 50 138 C70 148 90 148 110 138 C120 120 114 90 102 62 Z" fill="url(#g)" {S()}/>'
        f'<path d="M54 62 L106 62 L102 50 L58 50 Z" fill="url(#b)" {S()}/><path d="M64 50 C64 36 96 36 96 50" fill="none" {S("#8a6a3a", 5)}/>'
        f'<path d="M64 100 l14 0 l-14 14 l14 0" fill="none" stroke="#fff" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M86 88 l9 0 l-9 9 l9 0" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.85"/>'
        + sparkle(120, 40, 12, "#cfe0ff") + sparkle(36, 54, 8, "#cfe0ff") + sparkle(130, 80, 6, "#fff")
    ))


def spark_bomb():
    return icon(rg("g", [(0, "#6a6a80"), (0.5, "#34344a"), (1, "#1c1c2a")], cx=0.35, cy=0.3, r=0.85), (
        f'<circle cx="76" cy="98" r="44" fill="url(#g)" {S()}/><ellipse cx="60" cy="80" rx="12" ry="8" fill="#fff" opacity="0.6" transform="rotate(-30 60 80)"/>'
        f'<rect x="64" y="46" width="24" height="14" rx="4" fill="#8a6a3a" {S(INK, 3.5)}/><path d="M80 46 C86 30 100 30 108 22" fill="none" {S("#c99a5a", 5)}/>'
        + star(112, 20, 14, 6, attrs=f'fill="#ffe680" {S("#c98a12", 3)}') + sparkle(130, 44, 9) + sparkle(94, 12, 7, "#ff9a3a")
        + f'<path d="M64 112 L88 112 M76 100 L76 124" stroke="#ffd34d" stroke-width="5" stroke-linecap="round" opacity="0.9"/>'
    ))


def smoke_pearl():
    return icon(rg("g", [(0, "#ffffff"), (0.5, "#cfd6e4"), (1, "#8a96b0")], cx=0.35, cy=0.3, r=0.85), (
        '<circle cx="50" cy="60" r="20" fill="#e6e9f0" stroke="#9aa4b8" stroke-width="4"/><circle cx="108" cy="52" r="16" fill="#e6e9f0" stroke="#9aa4b8" stroke-width="4"/><circle cx="82" cy="40" r="18" fill="#eef0f6" stroke="#9aa4b8" stroke-width="4"/>'
        f'<circle cx="80" cy="102" r="36" fill="url(#g)" {S("#5a6580")}/><ellipse cx="68" cy="90" rx="10" ry="6" fill="#fff" opacity="0.85" transform="rotate(-30 68 90)"/>'
    ))


def friendship_cookie():
    return icon(G("g", "#e0a458", "radial"), (
        f'<circle cx="80" cy="88" r="52" fill="url(#g)" {S()}/>'
        + "".join(f'<ellipse cx="{x}" cy="{y}" rx="8" ry="6" fill="#5a3a24" {S(INK, 2.5)}/>' for x, y in ((50, 70), (108, 66), (56, 112), (104, 112), (84, 56)))
        + f'<path d="M80 108 C60 92 66 76 80 84 C94 76 100 92 80 108 Z" fill="#ff6a8a" {S(INK, 3.5)}/>'
        f'<path d="M44 66 C52 52 66 46 76 46" stroke="#fff" stroke-width="5" opacity="0.5" fill="none" stroke-linecap="round"/>'
    ))


def bubble_shield():
    return icon(G("s", "#6a9ad8"), (
        f'<circle cx="80" cy="84" r="56" fill="#d6f0ff" fill-opacity="0.45" {S("#6aa8d8")}/>'
        f'<path d="M80 44 L112 56 L110 92 C108 108 94 116 80 122 C66 116 52 108 50 92 L48 56 Z" fill="url(#s)" {S()}/>'
        f'<path d="M80 54 L80 112 M60 72 L100 72" stroke="#fff" stroke-width="5" opacity="0.7" stroke-linecap="round"/>'
        f'<path d="M44 58 C48 40 60 30 74 28" stroke="#fff" stroke-width="7" fill="none" stroke-linecap="round" opacity="0.8"/><circle cx="118" cy="110" r="5" fill="#fff" opacity="0.8"/>'
    ))


def rope():
    c = "#d8b070"
    coils = "".join(f'<ellipse cx="80" cy="{y}" rx="{rx}" ry="{ry}" fill="none" {S(INK, 15)}/><ellipse cx="80" cy="{y}" rx="{rx}" ry="{ry}" fill="none" stroke="{c}" stroke-width="9"/>' for y, rx, ry in ((104, 46, 20), (84, 44, 20), (64, 40, 18)))
    ticks = "".join(f'<path d="M{80 + 40 * math.cos(a):.0f} {y + 17 * math.sin(a):.0f} l5 -8" stroke="{dark(c, 0.35)}" stroke-width="3"/>' for y in (104, 84) for a in (0.6, 1.4, 2.2, 3.9, 4.8))
    return icon("", coils + ticks + f'<path d="M122 70 C146 60 150 36 134 28" fill="none" {S(INK, 15)}/><path d="M122 70 C146 60 150 36 134 28" fill="none" stroke="{c}" stroke-width="9" stroke-linecap="round"/>')


def lantern():
    return icon(G("f", "#5a4a40") + rg("l", [(0, "#fff9cf"), (0.6, "#ffd77a"), (1, "#f0a830")], cx=0.5, cy=0.5, r=0.6), (
        '<circle cx="80" cy="84" r="52" fill="#ffd77a" opacity="0.25"/>'
        f'<path d="M60 50 C60 28 100 28 100 50" fill="none" {S("#4a3a30", 6)}/>'
        f'<path d="M54 56 L106 56 L112 110 L48 110 Z" fill="url(#l)" {S("#4a3a30", 5)}/><rect x="46" y="48" width="68" height="12" rx="5" fill="url(#f)" {S()}/><rect x="44" y="108" width="72" height="14" rx="5" fill="url(#f)" {S()}/>'
        f'<path d="M80 62 C70 80 72 96 80 104 C88 96 90 80 80 62 Z" fill="#fff" opacity="0.9"/><path d="M70 60 L66 108 M90 60 L94 108" stroke="#4a3a30" stroke-width="3" opacity="0.7"/>'
    ))


def _key(c, edge, gem=None):
    return icon(G("g", c) + (rg("m", [(0, "#c9f1ff"), (0.5, "#4fb0f0"), (1, "#1d5fb0")], cx=0.35, cy=0.3) if gem else ""), (
        f'<g transform="rotate(-35 80 80)"><circle cx="46" cy="80" r="26" fill="url(#g)" {S(edge)}/><circle cx="46" cy="80" r="12" fill="{"url(#m)" if gem else "#f4efe6"}" {S(edge, 3.5)}/>'
        f'<rect x="64" y="73" width="74" height="14" rx="7" fill="url(#g)" {S(edge)}/><rect x="104" y="84" width="12" height="22" rx="3" fill="url(#g)" {S(edge, 4)}/><rect x="122" y="84" width="12" height="16" rx="3" fill="url(#g)" {S(edge, 4)}/></g>'
        f'<path d="M36 64 A22 22 0 0 1 52 56" stroke="#fff" stroke-width="4" stroke-linecap="round" fill="none" opacity="0.7" transform="rotate(-35 80 80)"/>'
        + (sparkle(120, 40, 10) if gem else "")
    ))


def rusty_key():
    return _key("#a8693a", "#4a2a14")


def silver_key():
    return _key("#d8dee8", "#4a5566", gem=True)


def lucky_clover():
    g = G("g", "#3fae52", "radial")
    leaves = "".join(f'<path d="M80 86 C{80 + 30 * math.cos(a) - 30 * math.sin(a):.0f} {86 + 30 * math.sin(a) + 30 * math.cos(a):.0f} {80 + 54 * math.cos(a):.0f} {86 + 54 * math.sin(a):.0f} {80 + 34 * math.cos(a) + 24 * math.sin(a):.0f} {86 + 34 * math.sin(a) - 24 * math.cos(a):.0f} C{80 + 20 * math.cos(a):.0f} {86 + 20 * math.sin(a):.0f} 80 86 80 86 Z" fill="url(#g)" {S()}/>' for a in (0.0, 1.5708, 3.1416, 4.7124))
    leaves = "".join(f'<ellipse cx="{80 + 28 * math.cos(a):.0f}" cy="{86 + 28 * math.sin(a):.0f}" rx="22" ry="22" fill="url(#g)" {S()}/>' for a in (math.pi / 4, 3 * math.pi / 4, 5 * math.pi / 4, 7 * math.pi / 4))
    return icon(g, f'<path d="M80 90 C76 116 84 130 96 138" fill="none" {S("#2e8a42", 7)}/>' + leaves + '<path d="M64 70 L96 102 M96 70 L64 102" stroke="#2e8a42" stroke-width="3" opacity="0.6"/>' + sparkle(122, 40, 11) + sparkle(36, 40, 7, "#fff3a0"))


def owl_feather():
    c = "#a8703a"
    return icon(G("g", c), (
        f'<path d="M36 142 C32 90 64 36 128 22 C136 70 112 120 56 132 Z" fill="url(#g)" {S()}/>'
        f'<path d="M36 142 C60 110 90 70 124 30" fill="none" {S(dark(c, 0.35), 4)}/>'
        + "".join(f'<path d="M{x} {y} L{x + 16} {y - 22}" stroke="{dark(c, 0.3)}" stroke-width="3" stroke-linecap="round" opacity="0.7"/>' for x, y in ((56, 116), (68, 98), (82, 80), (96, 62)))
        + f'<circle cx="92" cy="92" r="11" fill="#f6e3b8" {S(INK, 3)}/><circle cx="92" cy="92" r="5" fill="#2a1c14"/><circle cx="94" cy="90" r="1.8" fill="#fff"/>'
        + sparkle(130, 100, 9, "#ffe680")
    ))


def hint_scroll():
    return icon(G("p", "#fff3d6") + G("r", "#e8c46a"), (
        f'<rect x="38" y="46" width="84" height="76" rx="6" fill="url(#p)" {S()}/>'
        f'<rect x="30" y="38" width="100" height="18" rx="9" fill="url(#r)" {S()}/><rect x="30" y="112" width="100" height="18" rx="9" fill="url(#r)" {S()}/>'
        f'<path d="M62 78 C62 62 98 62 98 78 C98 90 80 88 80 100" fill="none" {S("#7a4a8a", 7)}/><circle cx="80" cy="110" r="4.5" fill="#7a4a8a"/>'
        f'<path d="M122 90 C142 90 142 124 124 124" fill="none" {S("#c7354a", 5)}/><circle cx="124" cy="124" r="6" fill="#c7354a" {S(INK, 2.5)}/>'
        + sparkle(26, 70, 8, "#ffe680")
    ))


def storybook_page():
    return icon(G("p", "#fffaf0"), (
        '<circle cx="80" cy="84" r="62" fill="#ffe680" opacity="0.28"/>'
        f'<path d="M40 40 L112 36 L120 120 L48 126 Z" fill="url(#p)" {S()}/>'
        + "".join(f'<path d="M{52 + k * 1.2} {58 + k * 14} L{104 + k * 1.2} {54 + k * 14}" stroke="#b9a27a" stroke-width="3.5" stroke-linecap="round" opacity="0.8"/>' for k in range(5))
        + star(80, 54, 10, 5, attrs=f'fill="#ffd34d" {S("#c98a12", 2.5)}') + sparkle(130, 40, 11) + sparkle(30, 100, 8)
    ))


def silver_quill():
    return icon(lg("g", [(0, "#ffffff"), (0.5, "#d8dee8"), (1, "#9aa6ba")], 0, 0, 1, 1), (
        f'<path d="M40 138 C30 90 60 40 128 22 C134 70 112 116 60 132 Z" fill="url(#g)" {S("#5a6580")}/>'
        f'<path d="M34 144 L70 108" {S("#5a6580", 5)}/><path d="M40 138 C70 100 98 66 126 30" fill="none" stroke="#7a86a0" stroke-width="3.5"/>'
        + "".join(f'<path d="M{x} {y} L{x + 14} {y - 20}" stroke="#7a86a0" stroke-width="2.5" stroke-linecap="round" opacity="0.7"/>' for x, y in ((56, 116), (70, 98), (84, 80), (98, 62)))
        + f'<path d="M28 150 C26 142 32 140 36 144 Z" fill="#2a2440"/>' + sparkle(130, 100, 9) + sparkle(40, 40, 7)
    ))


def ink_cleaner():
    return icon(G("g", "#6ad8e8", "radial") + G("k", "#c99a5a"), (
        f'<path d="M62 48 L62 68 C34 82 30 122 52 138 C68 146 92 146 108 138 C130 122 126 82 98 68 L98 48 Z" fill="#e8fbff" fill-opacity="0.6" {S()}/>'
        f'<path d="M40 112 C44 90 62 84 80 88 C98 84 118 90 122 112 C124 130 108 142 80 142 C52 142 38 130 40 112 Z" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<rect x="64" y="34" width="32" height="20" rx="5" fill="url(#k)" {S()}/>'
        f'<circle cx="62" cy="112" r="6" fill="#fff" opacity="0.8"/><circle cx="90" cy="102" r="8" fill="#fff" opacity="0.8"/><circle cx="100" cy="126" r="5" fill="#fff" opacity="0.8"/>'
        + star(80, 118, 11, 5, attrs=f'fill="#fff" {S("#3a9ab0", 2.5)}') + sparkle(126, 54, 10, "#fff") + sparkle(34, 70, 7, "#fff")
    ))


def recipe_page():
    return icon(G("p", "#fff3d6"), (
        f'<path d="M40 40 L112 36 L120 120 L48 126 Z" fill="url(#p)" {S()}/>'
        f'<circle cx="86" cy="66" r="16" fill="#e0a458" {S(INK, 3)}/><path d="M72 62 C76 54 96 54 100 62" fill="#fff3d6" {S(INK, 2.5)}/><circle cx="86" cy="52" r="4" fill="#e23b4a"/>'
        + "".join(f'<path d="M{52} {92 + k * 10} L{104} {88 + k * 10}" stroke="#b9a27a" stroke-width="3" stroke-linecap="round" opacity="0.8"/>' for k in range(3))
        + '<circle cx="52" cy="104" r="8" fill="#8a5a2a" opacity="0.55"/><circle cx="108" cy="46" r="6" fill="#8a5a2a" opacity="0.5"/>'
    ))


def magic_beans():
    return icon(G("b", "#c99a5a"), (
        f'<path d="M50 70 C40 100 40 128 56 142 C72 150 100 150 116 142 C126 128 124 100 112 70 Z" fill="url(#b)" {S()}/>'
        f'<path d="M46 70 L120 70 L114 56 L52 56 Z" fill="#a8703a" {S()}/><path d="M60 56 C60 42 106 42 106 56" fill="none" {S("#6a4a1a", 5)}/>'
        + "".join(f'<ellipse cx="{x}" cy="{y}" rx="9" ry="6" fill="#9ad86a" {S(INK, 2.5)} transform="rotate({r} {x} {y})"/>' for x, y, r in ((70, 108, -20), (92, 120, 25), (84, 96, 70)))
        + sparkle(126, 44, 9, "#b9ff9a") + sparkle(34, 56, 6, "#b9ff9a")
    ))


ITEMS = {
    "berry": berry, "honey_cake": honey_cake, "big_potion": big_potion, "sleep_dust": sleep_dust, "spark_bomb": spark_bomb,
    "smoke_pearl": smoke_pearl, "friendship_cookie": friendship_cookie, "bubble_shield": bubble_shield, "rope": rope,
    "lantern": lantern, "rusty_key": rusty_key, "silver_key": silver_key, "lucky_clover": lucky_clover,
    "owl_feather": owl_feather, "hint_scroll": hint_scroll, "storybook_page": storybook_page, "silver_quill": silver_quill,
    "ink_cleaner": ink_cleaner, "recipe_page": recipe_page, "magic_beans": magic_beans,
}


# ================================================================ picture icons for choices and menus

def badge(color, glyph):
    """A round coloured badge with a white picture on it."""
    defs = rg("b", [(0, light(color, 0.35)), (0.7, color), (1, dark(color, 0.22))], cx=0.35, cy=0.28, r=0.9)
    return doc(160, 160, defs,
               shadow(80, 148, 46, 7, 0.16)
               + f'<circle cx="80" cy="76" r="62" fill="url(#b)" {S(dark(color, 0.45), 6)}/>'
               + f'<path d="M36 52 C44 36 62 26 82 24" stroke="#fff" stroke-width="7" fill="none" stroke-linecap="round" opacity="0.4"/>'
               + glyph)


W = '#ffffff'


def g_ask():
    return f'<path d="M44 52 C44 40 116 40 116 52 L116 88 C116 98 104 100 94 100 L78 114 L80 100 C60 100 44 98 44 88 Z" fill="{W}" {S(INK, 4)}/><text x="80" y="92" font-family="Georgia,serif" font-size="46" font-weight="900" fill="#3a8ad6" text-anchor="middle">?</text>'


def g_listen():
    return f'<path d="M56 100 C40 84 46 40 80 38 C110 38 118 70 100 86 C92 94 94 104 84 112 C74 120 62 112 56 100 Z" fill="{W}" {S(INK, 4)}/><path d="M72 58 C86 52 98 66 88 78 C82 84 82 90 78 94" fill="none" {S("#e5641f", 5)}/>'


def g_give():
    return f'<path d="M80 108 C48 86 54 60 80 70 C106 60 112 86 80 108 Z" fill="#ff6a8a" {S(INK, 4)}/><path d="M34 120 C48 108 60 112 74 110 L106 112 C118 112 122 122 112 124 L70 126" fill="{W}" {S(INK, 4)}/>'


def g_help():
    return f'<circle cx="62" cy="62" r="14" fill="{W}" {S(INK, 4)}/><path d="M40 108 C40 84 84 84 84 108 Z" fill="{W}" {S(INK, 4)}/><circle cx="102" cy="68" r="12" fill="{W}" {S(INK, 4)}/><path d="M82 112 C82 90 122 90 122 112 Z" fill="{W}" {S(INK, 4)}/><path d="M80 46 L80 62 M72 54 L88 54" {S("#ffd34d", 7)}/>'


def g_buy():
    return f'<path d="M48 70 L112 70 L118 118 C118 126 42 126 42 118 Z" fill="{W}" {S(INK, 4)}/><path d="M62 70 C62 44 98 44 98 70" fill="none" {S(INK, 5)}/><circle cx="80" cy="96" r="16" fill="#ffd34d" {S("#a8701a", 4)}/><path d="M80 86 L80 106 M73 92 C78 86 88 90 87 96 C86 100 74 98 73 103 C74 108 84 108 88 102" fill="none" stroke="#a8701a" stroke-width="3.5" stroke-linecap="round"/>'


def g_leave():
    return f'<path d="M40 112 L40 68 C40 54 56 54 56 68 L56 52 C56 38 72 38 72 52 L72 46 C72 32 88 32 88 46 L88 56 C88 44 104 44 104 58 L104 92 C104 112 90 124 72 124 C54 124 44 120 40 112 Z" fill="{W}" {S(INK, 4)}/><path d="M112 54 C122 62 122 80 114 90" fill="none" {S("#ffd34d", 7)}/>'


def g_fight():
    return f'<path d="M44 44 L108 108 M116 44 L52 108" {S(INK, 15)}/><path d="M44 44 L108 108 M116 44 L52 108" stroke="#e8eef4" stroke-width="9" stroke-linecap="round"/><path d="M100 96 L120 116 M60 96 L40 116" {S("#c98a12", 9)}/><circle cx="116" cy="120" r="7" fill="#c7354a" {S(INK, 3)}/><circle cx="44" cy="120" r="7" fill="#c7354a" {S(INK, 3)}/>'


def g_laugh():
    return f'<circle cx="80" cy="76" r="40" fill="#ffd84a" {S(INK, 4)}/><path d="M60 68 C64 60 72 60 74 68 M86 68 C88 60 96 60 100 68" fill="none" {S(INK, 5)}/><path d="M56 82 C60 112 100 112 104 82 Z" fill="#7a1f22" {S(INK, 4)}/><ellipse cx="80" cy="102" rx="12" ry="5" fill="#ff7b8a"/><path d="M46 70 C40 74 40 84 48 86 M114 70 C120 74 120 84 112 86" fill="none" stroke="#3a9ad6" stroke-width="5" stroke-linecap="round"/>'


def g_sing():
    return f'<path d="M62 100 L62 50 L108 42 L108 92" fill="none" {S(INK, 6)}/><ellipse cx="52" cy="102" rx="14" ry="10" fill="{W}" {S(INK, 4)}/><ellipse cx="98" cy="94" rx="14" ry="10" fill="{W}" {S(INK, 4)}/><path d="M62 50 L108 42 L108 58 L62 66 Z" fill="{W}" {S(INK, 4)}/>'


def g_cookie():
    return f'<circle cx="80" cy="76" r="40" fill="#e0a458" {S(INK, 4)}/>' + "".join(f'<ellipse cx="{x}" cy="{y}" rx="7" ry="5" fill="#5a3a24"/>' for x, y in ((62, 62), (98, 60), (64, 96), (96, 94), (80, 78)))


def g_coin():
    return f'<ellipse cx="80" cy="104" rx="34" ry="12" fill="#e0a010" {S("#8a5a08", 4)}/><ellipse cx="80" cy="92" rx="34" ry="12" fill="#f8cf3a" {S("#8a5a08", 4)}/><ellipse cx="80" cy="80" rx="34" ry="12" fill="#f8cf3a" {S("#8a5a08", 4)}/><ellipse cx="80" cy="68" rx="34" ry="12" fill="#ffe680" {S("#8a5a08", 4)}/><path d="M80 62 L80 74" stroke="#8a5a08" stroke-width="4"/>'


def g_think():
    return f'<circle cx="56" cy="112" r="6" fill="{W}" {S(INK, 3)}/><circle cx="46" cy="124" r="4" fill="{W}" {S(INK, 3)}/><path d="M48 70 C44 44 76 36 90 46 C112 36 126 60 114 74 C124 92 100 104 84 96 C66 106 44 92 48 70 Z" fill="{W}" {S(INK, 4)}/><path d="M80 52 C68 52 66 68 74 76 L74 84 L88 84 L88 76 C96 68 94 52 80 52 Z" fill="#ffe680" {S("#a8701a", 3.5)}/>'


def g_yes():
    return f'<path d="M42 82 L68 108 L120 48" fill="none" {S(INK, 18)}/><path d="M42 82 L68 108 L120 48" fill="none" stroke="#ffffff" stroke-width="11" stroke-linecap="round" stroke-linejoin="round"/>'


def g_no():
    return f'<path d="M48 48 L112 108 M112 48 L48 108" fill="none" {S(INK, 18)}/><path d="M48 48 L112 108 M112 48 L48 108" fill="none" stroke="#ffffff" stroke-width="11" stroke-linecap="round"/>'


def g_puzzle():
    return f'<path d="M44 56 L74 56 C70 40 94 40 90 56 L116 56 L116 82 C132 78 132 104 116 100 L116 120 L44 120 Z" fill="{W}" {S(INK, 4)}/>'


def g_gift():
    return f'<rect x="44" y="70" width="72" height="50" rx="5" fill="#ff7aa8" {S(INK, 4)}/><rect x="40" y="58" width="80" height="18" rx="5" fill="#ff9ac0" {S(INK, 4)}/><path d="M80 58 L80 120" stroke="#ffd34d" stroke-width="12"/><path d="M80 58 C60 34 44 46 62 58 Z M80 58 C100 34 116 46 98 58 Z" fill="#ffd34d" {S(INK, 3.5)}/>'


def g_inn():
    return f'<path d="M36 108 L36 74 L124 74 L124 108 Z" fill="{W}" {S(INK, 4)}/><rect x="36" y="96" width="88" height="12" fill="#c7354a" {S(INK, 3)}/><rect x="42" y="60" width="28" height="18" rx="9" fill="#fff0c0" {S(INK, 3.5)}/><path d="M100 36 C86 40 82 58 92 66 C82 66 74 56 80 42 C86 30 98 30 100 36 Z" fill="#ffe680" {S("#a8701a", 3)}/>'


def g_enter():
    return f'<path d="M40 122 L40 64 C40 24 120 24 120 64 L120 122 Z" fill="#2a2030" {S(INK, 5)}/><path d="M52 122 L52 68 C52 42 108 42 108 68 L108 122" fill="none" stroke="#6a5a7a" stroke-width="4"/><circle cx="68" cy="84" r="5" fill="#ffd34d"/><circle cx="92" cy="84" r="5" fill="#ffd34d"/>'


def g_attack():
    return f'<path d="M48 120 L116 40" {S(INK, 16)}/><path d="M48 120 L116 40" stroke="#e8eef4" stroke-width="9" stroke-linecap="round"/><path d="M40 112 L60 128" {S("#c98a12", 10)}/><path d="M104 36 l6 -14 l8 12 l14 -2 l-8 12" fill="none" stroke="#ffe680" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>'


def g_peace():
    return f'<path d="M80 108 C48 86 54 58 80 68 C106 58 112 86 80 108 Z" fill="#ff6a8a" {S(INK, 4)}/><path d="M40 70 C60 56 80 62 90 52 M120 70 C100 56 80 62 70 52" fill="none" stroke="#4fae45" stroke-width="7" stroke-linecap="round" opacity="0.9"/><circle cx="52" cy="62" r="6" fill="#4fae45"/><circle cx="108" cy="62" r="6" fill="#4fae45"/>'


def g_shop():
    return f'<path d="M36 70 L124 70 L118 50 L42 50 Z" fill="#e23b4a" {S(INK, 4)}/><path d="M36 70 C36 82 56 82 56 70 C56 82 72 82 72 70 C72 82 88 82 88 70 C88 82 104 82 104 70 C104 82 124 82 124 70" fill="{W}" {S(INK, 4)}/><rect x="44" y="82" width="72" height="40" rx="4" fill="{W}" {S(INK, 4)}/><rect x="66" y="94" width="28" height="28" rx="3" fill="#a8703a" {S(INK, 3.5)}/>'


UI = {
    "talk_ask": ("#3a8ad6", g_ask), "talk_listen": ("#8a5ad6", g_listen), "talk_give": ("#e0567a", g_give), "talk_help": ("#3fae52", g_help),
    "talk_buy": ("#e0a010", g_buy), "talk_leave": ("#6a7a8a", g_leave), "talk_fight": ("#c7354a", g_fight), "talk_laugh": ("#f08a20", g_laugh),
    "talk_sing": ("#d65ab8", g_sing), "talk_cookie": ("#a8703a", g_cookie), "talk_coin": ("#e0a010", g_coin), "talk_think": ("#5a8ad6", g_think),
    "talk_yes": ("#3fae52", g_yes), "talk_no": ("#c7354a", g_no), "talk_puzzle": ("#8a5ad6", g_puzzle), "talk_gift": ("#e0567a", g_gift),
    "hub_inn": ("#5a4a9a", g_inn), "hub_leave": ("#6a7a8a", g_leave), "hub_enter": ("#4a3a6a", g_enter), "hub_attack": ("#c7354a", g_attack),
    "hub_fight": ("#c7354a", g_fight), "hub_peace": ("#3fae52", g_peace), "hub_shop": ("#e0a010", g_shop),
}


def sprites() -> dict:
    out = {f"item_{k}": (fn(), None) for k, fn in ITEMS.items()}
    for k, (color, glyph) in UI.items():
        out[k] = (badge(color, glyph()), None)
    return out
