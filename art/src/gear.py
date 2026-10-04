"""Gear for the hero: hats, things to hold, clothes and boots, serious and silly.

Each piece is drawn once, in the hero's own 300x300 frame (the head is an ellipse near (164, 97), the
front hand is at (220, 212), the body sits between x 120..198 and y 160..250, the boots at y 258..282),
so the same drawing is both the overlay the app lays over the hero (`gear_<id>`, a transparent 300x300
picture) and, scaled to fit, the shop and bag icon (`item_<id>`).
"""
from props import dark, doc, light, mix, rg, lg, shadow, sparkle

INK = "#3d2a1c"


def S(color=INK, w=4):
    return f'stroke="{color}" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round"'


def G(id_, c, kind="lin"):
    if kind == "radial":
        return rg(id_, [(0, light(c, 0.45)), (0.55, c), (1, dark(c, 0.22))], cx=0.38, cy=0.3, r=0.85)
    return lg(id_, [(0, light(c, 0.3)), (1, dark(c, 0.25))], 0, 0, 1, 1)


# ---------------------------------------------------------------- on the head (hero head: centre 164,97)

def leather_cap():
    c = "#a8703a"
    return G("g", c), (
        f'<path d="M92 84 C90 30 238 30 236 84 C216 74 190 70 164 70 C138 70 112 74 92 84 Z" fill="url(#g)" {S()}/>'
        f'<path d="M104 76 C132 64 196 64 224 76" fill="none" stroke="{dark(c, 0.35)}" stroke-width="3.5" stroke-dasharray="6 6" stroke-linecap="round"/>'
        f'<path d="M164 36 L164 68" stroke="{dark(c, 0.3)}" stroke-width="4"/><circle cx="164" cy="34" r="7" fill="{light(c, 0.2)}" {S(INK, 3.5)}/>'
        f'<path d="M120 50 C134 40 150 38 158 40" stroke="#fff" stroke-width="5" opacity="0.3" fill="none" stroke-linecap="round"/>'
    ), (84, 24, 160, 66)


def iron_helm():
    return ('<linearGradient id="g" x1="0.1" y1="0" x2="0.8" y2="1"><stop offset="0" stop-color="#f4f8fb"/><stop offset="0.45" stop-color="#c3ced8"/><stop offset="1" stop-color="#8794a2"/></linearGradient>'), (
        f'<path d="M92 90 C86 24 242 24 236 90 L220 84 L108 84 Z" fill="url(#g)" {S()}/>'
        f'<path d="M92 90 L236 90 L232 104 L96 104 Z" fill="#9aa8b6" {S(INK, 4)}/>'
        f'<path d="M164 26 L164 88" stroke="#7d8a98" stroke-width="6"/><path d="M130 40 L130 86 M198 40 L198 86" stroke="#7d8a98" stroke-width="3"/>'
        f'<circle cx="100" cy="97" r="4" fill="#eef3f7" {S(INK, 2)}/><circle cx="228" cy="97" r="4" fill="#eef3f7" {S(INK, 2)}/>'
        f'<path d="M116 52 C132 38 150 34 160 36" stroke="#fff" stroke-width="6" opacity="0.5" fill="none" stroke-linecap="round"/>'
    ), (84, 18, 160, 116)


def wizard_hat():
    c = "#7a3fc4"
    return G("g", c) + G("h", dark(c, 0.15)), (
        f'<path d="M70 82 C104 68 224 68 258 82 C240 96 206 100 164 100 C122 100 88 96 70 82 Z" fill="url(#h)" {S()}/>'
        f'<path d="M104 78 C122 40 140 12 214 2 C190 24 196 54 218 78 C186 88 140 88 104 78 Z" fill="url(#g)" {S()}/>'
        f'<path d="M110 70 C150 78 190 74 214 68" stroke="#ffd34d" stroke-width="7" fill="none" stroke-linecap="round" opacity="0.9"/>'
        f'<circle cx="150" cy="40" r="5" fill="#ffd34d"/><circle cx="180" cy="22" r="4" fill="#ffd34d"/><circle cx="194" cy="56" r="4" fill="#ffd34d"/><path d="M130 58 l4 -9 l4 9 l-9 -5 h10 z" fill="#ffe680"/>'
    ), (62, 0, 204, 104)


def chicken_hat():
    c = "#fff6e0"
    return G("g", c, "radial"), (
        f'<ellipse cx="164" cy="50" rx="58" ry="34" fill="url(#g)" {S()}/>'
        f'<path d="M110 52 C92 46 88 28 104 24 C106 38 116 42 126 44 Z" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<path d="M134 38 C132 12 150 10 154 26 C158 6 178 8 178 28 C188 16 204 28 190 46 Z" fill="#e23b3b" {S()}/>'
        f'<path d="M208 52 C236 50 244 64 234 74 C222 72 212 66 208 60 Z" fill="#f0a020" {S(INK, 3.5)}/>'
        f'<path d="M212 76 C214 90 224 90 224 76 Z" fill="#e23b3b" {S(INK, 3)}/>'
        f'<circle cx="196" cy="48" r="5" fill="#2a1c14"/><circle cx="198" cy="46" r="1.8" fill="#fff"/>'
        f'<path d="M126 60 C140 76 166 78 180 66" fill="#f3dca8" {S(INK, 3.5)}/>'
        f'<path d="M92 76 L84 94 M112 80 L108 98" {S("#f0a020", 5)}/>'
    ), (80, 4, 164, 98)


def duck_helmet():
    c = "#ffd84a"
    return G("g", c, "radial"), (
        f'<ellipse cx="164" cy="60" rx="64" ry="38" fill="url(#g)" {S()}/>'
        f'<circle cx="164" cy="28" r="38" fill="url(#g)" {S()}/>'
        f'<path d="M190 30 C226 24 240 36 230 48 C216 52 200 48 190 42 Z" fill="#f08a20" {S(INK, 3.5)}/>'
        f'<circle cx="168" cy="20" r="5" fill="#2a1c14"/><circle cx="170" cy="18" r="1.8" fill="#fff"/>'
        f'<path d="M104 64 C112 78 132 82 146 76 C126 74 112 68 104 64 Z" fill="{dark(c, 0.12)}" {S(INK, 3)}/>'
        f'<ellipse cx="144" cy="12" rx="12" ry="6" fill="#fff" opacity="0.5" transform="rotate(-25 144 12)"/>'
    ), (96, -10, 140, 100)


def pot_helmet():
    c = "#8d949e"
    return G("g", c), (
        f'<path d="M98 88 C98 34 230 34 230 88 L226 96 L102 96 Z" fill="url(#g)" {S()}/>'
        f'<path d="M92 92 L236 92" {S("#6b727c", 9)}/>'
        f'<path d="M96 60 C70 56 66 84 96 80" fill="none" {S("#3d4148", 8)}/><path d="M232 60 C258 56 262 84 232 80" fill="none" {S("#3d4148", 8)}/>'
        f'<path d="M150 36 C152 20 176 20 178 36 Z" fill="#3d4148" {S()}/><circle cx="164" cy="22" r="7" fill="#6b727c" {S(INK, 3)}/>'
        f'<path d="M116 56 C134 44 150 42 160 44" stroke="#fff" stroke-width="6" opacity="0.45" fill="none" stroke-linecap="round"/>'
        f'<path d="M120 84 C140 90 188 90 208 84" stroke="#4c5158" stroke-width="3" fill="none" opacity="0.5"/>'
    ), (62, 10, 204, 92)


# ---------------------------------------------------------------- in the hand (front hand at 220,212)

def wooden_sword():
    c = "#c99a5a"
    return G("g", c), (
        f'<path d="M214 214 L214 112 L220 70 L226 112 L226 214 Z" fill="url(#g)" {S()}/>'
        f'<path d="M220 78 L220 206" stroke="{dark(c, 0.3)}" stroke-width="3" opacity="0.6"/>'
        f'<rect x="196" y="204" width="48" height="12" rx="5" fill="#8a5a2a" {S()}/>'
        f'<rect x="214" y="214" width="12" height="26" rx="4" fill="#7a4a26" {S()}/>'
        f'<circle cx="220" cy="242" r="7" fill="#8a5a2a" {S(INK, 3.5)}/>'
    ), (192, 60, 56, 190)


def knight_sword():
    return ('<linearGradient id="g" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#eef3f7"/><stop offset="0.5" stop-color="#c3ced8"/><stop offset="1" stop-color="#8794a2"/></linearGradient>'), (
        f'<path d="M213 208 L213 106 L220 56 L227 106 L227 208 Z" fill="url(#g)" {S()}/>'
        f'<path d="M220 66 L220 204" stroke="#7d8a98" stroke-width="3"/>'
        f'<path d="M190 206 C196 198 244 198 250 206 L250 216 L190 216 Z" fill="#ffd34d" {S()}/>'
        f'<rect x="213" y="214" width="14" height="28" rx="4" fill="#c7354a" {S()}/><path d="M213 224 L227 224 M213 232 L227 232" stroke="#7a1f2a" stroke-width="3"/>'
        f'<circle cx="220" cy="246" r="8" fill="#ffd34d" {S(INK, 3.5)}/><circle cx="220" cy="246" r="3" fill="#e23b3b"/>'
        f'<path d="M216 100 L216 190" stroke="#fff" stroke-width="3" opacity="0.7"/>'
    ), (186, 46, 68, 214)


def magic_wand():
    return (rg("w", [(0, "#fff7c2"), (1, "#ffcf3a")], cx=0.5, cy=0.5, r=0.6)), (
        f'<path d="M216 240 L226 88" {S("#7a4a26", 9)}/><path d="M216 240 L226 88" stroke="#a8703a" stroke-width="4" stroke-linecap="round"/>'
        f'<circle cx="226" cy="74" r="26" fill="#ffe680" opacity="0.35"/>'
        f'<path d="M226 44 L233 66 L256 68 L238 82 L245 104 L226 91 L207 104 L214 82 L196 68 L219 66 Z" fill="url(#w)" {S("#a8701a", 4)}/>'
        f'<path d="M258 40 l3 8 l8 3 l-8 3 l-3 8 l-3 -8 l-8 -3 l8 -3 z" fill="#fff"/><path d="M190 100 l2 6 l6 2 l-6 2 l-2 6 l-2 -6 l-6 -2 l6 -2 z" fill="#fff"/>'
    ), (186, 30, 80, 220)


def spoon_sword():
    c = "#c99a5a"
    return G("g", c, "radial"), (
        f'<path d="M216 244 L220 130" {S("#8a5a2a", 14)}/><path d="M216 244 L220 130" stroke="{light(c, 0.2)}" stroke-width="7" stroke-linecap="round"/>'
        f'<ellipse cx="220" cy="92" rx="34" ry="50" fill="url(#g)" {S()}/><ellipse cx="220" cy="98" rx="22" ry="34" fill="{dark(c, 0.12)}" opacity="0.6"/>'
        f'<ellipse cx="206" cy="72" rx="8" ry="16" fill="#fff" opacity="0.4" transform="rotate(-12 206 72)"/>'
    ), (182, 38, 76, 214)


def banana_blaster():
    c = "#ffd84a"
    return G("g", c, "radial"), (
        f'<path d="M196 230 C176 190 190 120 250 96 C252 112 238 122 226 130 C210 144 214 190 232 216 C220 232 206 236 196 230 Z" fill="url(#g)" {S()}/>'
        f'<path d="M248 92 L262 80 L266 92 L254 100 Z" fill="#7a5a2a" {S(INK, 3.5)}/>'
        f'<path d="M196 230 L204 242 L216 236" fill="#7a5a2a" {S(INK, 3.5)}/>'
        f'<path d="M214 128 C224 120 238 112 246 108" stroke="#fff" stroke-width="5" opacity="0.5" fill="none" stroke-linecap="round"/>'
        f'<path d="M206 204 C204 182 210 160 222 148" stroke="{dark(c, 0.25)}" stroke-width="3" fill="none" opacity="0.6"/>'
        f'<path d="M262 74 l3 8 l8 3 l-8 3 l-3 8 l-3 -8 l-8 -3 l8 -3 z" fill="#fff"/>'
    ), (186, 70, 92, 180)


def noodle_whip():
    c = "#f3d27a"
    return G("g", c), (
        f'<path d="M214 236 C230 200 196 180 214 150 C232 120 198 100 216 72 C228 52 244 62 248 80" fill="none" {S(INK, 15)}/>'
        f'<path d="M214 236 C230 200 196 180 214 150 C232 120 198 100 216 72 C228 52 244 62 248 80" fill="none" stroke="url(#g)" stroke-width="9" stroke-linecap="round"/>'
        f'<path d="M222 190 C216 180 214 166 220 154" stroke="#fff" stroke-width="3" opacity="0.6" fill="none"/>'
        f'<circle cx="250" cy="84" r="9" fill="#e23b3b" {S(INK, 3)}/><circle cx="216" cy="238" r="9" fill="#7a4a26" {S(INK, 3)}/>'
    ), (192, 44, 70, 210)


# ---------------------------------------------------------------- on the body (torso x 120..198, y 160..252)

def padded_tunic():
    c = "#b57a3c"
    q = "".join(f'<path d="M{x} 172 L{x + 20} 252" stroke="{dark(c, 0.3)}" stroke-width="2.5" opacity="0.6"/>' for x in (130, 146, 162, 178))
    q += "".join(f'<path d="M122 {y} L196 {y}" stroke="{dark(c, 0.3)}" stroke-width="2.5" opacity="0.6"/>' for y in (188, 206, 224))
    return G("g", c), (
        f'<path d="M122 170 C142 158 172 158 192 170 C202 196 204 226 200 250 C170 258 142 258 114 250 C112 224 114 196 122 170 Z" fill="url(#g)" {S()}/>{q}'
        f'<path d="M142 162 L162 188 L182 162" fill="none" stroke="{dark(c, 0.35)}" stroke-width="5" stroke-linecap="round"/>'
        f'<circle cx="162" cy="196" r="5" fill="#ffd34d" {S(INK, 2.5)}/><circle cx="162" cy="218" r="5" fill="#ffd34d" {S(INK, 2.5)}/>'
    ), (108, 154, 100, 108)


def chain_mail():
    rings = "".join(f'<circle cx="{x + (8 if (y // 12) % 2 else 0)}" cy="{y}" r="5.5" fill="none" stroke="#6b7684" stroke-width="2.4"/>' for y in range(176, 252, 12) for x in range(126, 200, 16))
    return ('<linearGradient id="g" x1="0.1" y1="0" x2="0.9" y2="1"><stop offset="0" stop-color="#e2e8ee"/><stop offset="0.5" stop-color="#aab6c2"/><stop offset="1" stop-color="#7c8896"/></linearGradient>'), (
        f'<path d="M120 172 C140 158 176 158 196 172 C206 196 208 228 204 252 C172 260 140 260 110 252 C108 228 110 196 120 172 Z" fill="url(#g)" {S()}/>{rings}'
        f'<path d="M112 244 C140 252 176 252 204 244" fill="none" stroke="#5a6572" stroke-width="5"/>'
        f'<path d="M138 164 C150 180 174 180 186 164" fill="none" stroke="#5a6572" stroke-width="4"/>'
        f'<ellipse cx="140" cy="196" rx="8" ry="20" fill="#fff" opacity="0.3" transform="rotate(14 140 196)"/>'
    ), (104, 154, 108, 112)


def pillow_armor():
    c = "#fff0f6"
    return G("g", c, "radial"), (
        f'<path d="M112 176 C112 156 206 156 206 176 C216 206 216 232 204 252 C172 262 140 262 114 252 C102 232 102 206 112 176 Z" fill="url(#g)" {S()}/>'
        f'<path d="M126 170 C150 182 174 182 196 170 M118 244 C146 252 178 252 202 244" fill="none" stroke="#ff9ac8" stroke-width="5" stroke-linecap="round" stroke-dasharray="1 9"/>'
        f'<path d="M160 188 L152 176 L160 182 L168 176 Z" fill="#ff7aa8" {S(INK, 3)}/><circle cx="160" cy="184" r="5" fill="#ff7aa8" {S(INK, 3)}/>'
        f'<path d="M106 176 L100 188 M104 252 L98 244 M212 176 L218 188 M210 252 L216 244" {S("#ff9ac8", 5)}/>'
        f'<ellipse cx="136" cy="200" rx="14" ry="26" fill="#fff" opacity="0.6" transform="rotate(14 136 200)"/>'
    ), (92, 154, 136, 112)


def dino_pajamas():
    c = "#59c47a"
    spikes = "".join(f'<path d="M{x} {y} L{x - 18} {y - 14} L{x - 4} {y + 6} Z" fill="#ffd34d" {S(INK, 3)}/>' for x, y in ((120, 178), (116, 200), (118, 222)))
    return G("g", c), (
        f'<path d="M126 236 C80 244 66 214 82 196 C92 214 110 222 130 218 Z" fill="url(#g)" {S()}/>{spikes}'
        f'<path d="M122 170 C142 158 172 158 192 170 C202 196 204 226 200 250 C170 258 142 258 114 250 C112 224 114 196 122 170 Z" fill="url(#g)" {S()}/>'
        f'<ellipse cx="160" cy="214" rx="24" ry="30" fill="#e6f8b8" {S(INK, 3.5)}/>'
        f'<path d="M142 168 L162 188 L182 168" fill="none" stroke="{dark(c, 0.35)}" stroke-width="5" stroke-linecap="round"/>'
        f'<circle cx="140" cy="190" r="5" fill="#2e9a58" opacity="0.6"/><circle cx="184" cy="222" r="6" fill="#2e9a58" opacity="0.6"/><circle cx="178" cy="184" r="4" fill="#2e9a58" opacity="0.6"/>'
    ), (62, 150, 150, 112)


# ---------------------------------------------------------------- on the feet (boots at y 258..282)

def _boots(c, trim, extra_l="", extra_r="", shaft=232):
    g = G("g", c)
    left = f'<path d="M126 {shaft} L124 262 C118 266 118 282 128 284 C146 286 158 282 158 270 C158 262 154 260 152 256 L150 {shaft} Z" fill="url(#g)" {S()}/><path d="M124 {shaft + 2} L152 {shaft + 2}" stroke="{trim}" stroke-width="8" stroke-linecap="round"/>{extra_l}'
    right = f'<path d="M160 {shaft} L158 256 C156 260 156 266 158 270 C160 282 176 286 192 284 C202 282 202 266 196 262 L186 {shaft} Z" fill="url(#g)" {S()}/><path d="M158 {shaft + 2} L186 {shaft + 2}" stroke="{trim}" stroke-width="8" stroke-linecap="round"/>{extra_r}'
    return g, left + right


def sturdy_boots():
    g, b = _boots("#8a5a32", "#d9a85a")
    return g, b + '<path d="M130 266 L150 266 M166 266 L188 266" stroke="#5a3a1c" stroke-width="3" opacity="0.6"/>', (110, 226, 100, 64)


def puddle_boots():
    g, b = _boots("#ffd84a", "#3a9ad6", shaft=228)
    return g, b + '<ellipse cx="136" cy="246" rx="5" ry="12" fill="#fff" opacity="0.5"/><ellipse cx="172" cy="246" rx="5" ry="12" fill="#fff" opacity="0.5"/>', (110, 222, 100, 68)


def squeaky_boots():
    g, b = _boots("#e23b3b", "#ffffff")
    ducks = (f'<circle cx="140" cy="248" r="8" fill="#ffd84a" {S(INK, 2.5)}/><path d="M145 247 L152 248 L145 251 Z" fill="#f08a20"/>'
             f'<circle cx="174" cy="248" r="8" fill="#ffd84a" {S(INK, 2.5)}/><path d="M179 247 L186 248 L179 251 Z" fill="#f08a20"/>'
             f'<path d="M196 250 l8 -4 M198 258 l9 0" stroke="{INK}" stroke-width="3" stroke-linecap="round"/>')
    return g, b + ducks, (110, 226, 108, 64)


def bunny_slippers():
    c = "#fdfbff"
    g = G("g", c, "radial")
    slip = lambda x, y, flip: (
        f'<ellipse cx="{x}" cy="{y}" rx="32" ry="16" fill="url(#g)" {S()}/>'
        f'<path d="M{x - 12} {y - 10} C{x - 22} {y - 56} {x - 4} {y - 60} {x - 2} {y - 12} Z M{x + 2} {y - 12} C{x + 4} {y - 60} {x + 22} {y - 56} {x + 12} {y - 10} Z" fill="url(#g)" {S(INK, 3.5)}/>'
        f'<path d="M{x - 11} {y - 20} C{x - 14} {y - 40} {x - 8} {y - 42} {x - 6} {y - 20} Z M{x + 6} {y - 20} C{x + 8} {y - 42} {x + 14} {y - 40} {x + 11} {y - 20} Z" fill="#ffb3cc"/>'
        f'<circle cx="{x + 22 * flip}" cy="{y}" r="5" fill="#ffb3cc" {S(INK, 2.5)}/>'
    )
    return g, slip(142, 270, 1) + slip(176, 270, 1), (110, 214, 100, 74)


GEAR = {
    "leather_cap": leather_cap, "iron_helm": iron_helm, "wizard_hat": wizard_hat, "chicken_hat": chicken_hat,
    "duck_helmet": duck_helmet, "pot_helmet": pot_helmet,
    "wooden_sword": wooden_sword, "knight_sword": knight_sword, "magic_wand": magic_wand, "spoon_sword": spoon_sword,
    "banana_blaster": banana_blaster, "noodle_whip": noodle_whip,
    "padded_tunic": padded_tunic, "chain_mail": chain_mail, "pillow_armor": pillow_armor, "dino_pajamas": dino_pajamas,
    "sturdy_boots": sturdy_boots, "puddle_boots": puddle_boots, "squeaky_boots": squeaky_boots, "bunny_slippers": bunny_slippers,
}


def overlay(fn) -> str:
    """The gear in the hero's frame, transparent, to lay over the hero."""
    defs, body, _ = fn()
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 300" width="300" height="300">\n<defs>{defs}</defs>\n'
            f'<g id="all">{body}</g>\n</svg>')


def icon(fn) -> str:
    """The same gear, shrunk to fit a 160x160 icon with a soft shadow."""
    defs, body, (x, y, w, h) = fn()
    s = min(132 / w, 132 / h)
    tx = 80 - (x + w / 2) * s
    ty = 80 - (y + h / 2) * s
    return doc(160, 160, defs, f'{shadow(80, 148, 44, 7, 0.16)}<g transform="translate({tx:.1f} {ty:.1f}) scale({s:.3f})">{body}</g>')


def sprites() -> dict:
    out = {}
    for gid, fn in GEAR.items():
        out[f"gear_{gid}"] = (overlay(fn), None)
        out[f"item_{gid}"] = (icon(fn), None)
    return out
