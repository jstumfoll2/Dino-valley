"""Animals and monsters of Whisperwood, drawn like the people in cast.py: one 240x260 SVG split into
shadow, body, eye-open, eye-closed, mouth-calm, mouth-talk. The monsters are friendly-looking on
purpose: round, chunky, and a little silly, because this is a game for a four-year-old.
"""
import math

from cast import (INK, LAYERS, S, beak_mouth, build_person, cheeks, eyes, fit, grad, ground_shadow, mouth, rig)
from props import dark, light, mix

o = INK


def blob(c, gid):
    return grad(gid, c)


# ================================================================ animals and townsfolk who aren't people

def owl():
    brown, belly = "#9a6a3c", "#f6e3b8"
    defs = grad("ob", brown) + grad("bel", belly) + grad("wing", dark(brown, 0.1), "lin")
    scallops = "".join(f'<path d="M{x} {y} C{x + 8} {y + 10} {x + 22} {y + 10} {x + 30} {y}" stroke="{mix(brown, belly, 0.4)}" stroke-width="3.5" fill="none" stroke-linecap="round"/>'
                       for y in (166, 188, 210) for x in ((92, 122) if (y // 22) % 2 else (80, 108, 136)))
    body = (
        f'<path d="M58 140 C26 166 30 222 66 240 C80 214 82 176 82 150 Z" fill="url(#wing)" {S(o)}/>'
        f'<path d="M182 140 C214 166 210 222 174 240 C160 214 158 176 158 150 Z" fill="url(#wing)" {S(o)}/>'
        f'<ellipse cx="120" cy="176" rx="68" ry="72" fill="url(#ob)" {S(o)}/>'
        f'<ellipse cx="120" cy="190" rx="44" ry="54" fill="url(#bel)" {S(mix(brown, "#000000", 0.4), 3.5)}/>{scallops}'
        f'<path d="M86 244 L80 252 M96 246 L96 254 M106 244 L112 252" {S("#c97a10", 5)}/><path d="M134 244 L128 252 M144 246 L144 254 M154 244 L160 252" {S("#c97a10", 5)}/>'
        f'<path d="M84 74 L70 34 L104 56 Z M156 74 L170 34 L136 56 Z" fill="{dark(brown, 0.1)}" {S(o)}/>'
        f'<ellipse cx="120" cy="100" rx="68" ry="54" fill="url(#ob)" {S(o)}/>'
        f'<circle cx="94" cy="102" r="31" fill="{belly}" {S(mix(brown, "#000000", 0.4), 3.5)}/><circle cx="146" cy="102" r="31" fill="{belly}" {S(mix(brown, "#000000", 0.4), 3.5)}/>'
        f'<circle cx="94" cy="102" r="34" fill="none" {S("#c98a12", 5)}/><circle cx="146" cy="102" r="34" fill="none" {S("#c98a12", 5)}/><path d="M127 98 L113 98" {S("#c98a12", 5)}/>'
        f'<path d="M104 146 L120 158 L136 146 L136 160 L120 166 L104 160 Z" fill="#e23b3b" {S(o, 3.5)}/>'
    )
    return rig(defs, body, eyes(94, 146, 102, r=10, white=True), beak_mouth(120, 128, 13))


def fox():
    orange, cream = "#f08a35", "#fff1d6"
    defs = grad("fb", orange) + grad("fc", cream) + grad("vest", "#3f8f5a", "lin")
    body = (
        f'<path d="M168 224 C220 214 232 160 208 118 C206 150 196 168 170 176 Z" fill="url(#fb)" {S(o)}/>'
        f'<path d="M206 124 C226 138 228 170 214 196 C206 176 204 150 206 124 Z" fill="url(#fc)" {S(o, 3.5)}/>'
        f'<path d="M80 244 C76 204 88 168 96 156 C108 148 132 148 144 156 C152 168 164 204 160 244 Z" fill="url(#fb)" {S(o)}/>'
        f'<path d="M100 160 C108 154 132 154 140 160 L146 244 L94 244 Z" fill="url(#vest)" {S(o, 3.5)}/>'
        f'<circle cx="112" cy="214" r="5" fill="#ffd34d" {S(o, 2.5)}/><circle cx="128" cy="214" r="5" fill="#ffd34d" {S(o, 2.5)}/>'
        f'<circle cx="82" cy="214" r="12" fill="url(#fb)" {S(o, 3.5)}/><circle cx="158" cy="214" r="12" fill="url(#fb)" {S(o, 3.5)}/>'
        f'<path d="M70 86 L60 28 L102 62 Z M170 86 L180 28 L138 62 Z" fill="url(#fb)" {S(o)}/><path d="M72 76 L68 44 L92 62 Z M168 76 L172 44 L148 62 Z" fill="#3a2418"/>'
        f'<path d="M64 110 C60 66 90 54 120 54 C150 54 180 66 176 110 C174 134 150 148 120 148 C90 148 66 134 64 110 Z" fill="url(#fb)" {S(o)}/>'
        f'<path d="M64 112 C80 120 100 126 120 126 C140 126 160 120 176 112 C174 134 150 150 120 150 C90 150 66 134 64 112 Z" fill="url(#fc)" {S(o, 3)}/>'
        f'<ellipse cx="120" cy="118" rx="9" ry="7" fill="#2a1c14"/><ellipse cx="117" cy="116" rx="3" ry="2" fill="#fff" opacity="0.8"/>'
        f'<path d="M86 78 C94 84 104 86 112 84 M154 78 C146 84 136 86 128 84" stroke="{o}" stroke-width="4.5" fill="none" stroke-linecap="round"/>'
    )
    return rig(defs, body, eyes(98, 142, 98, r=7, pupil="#2a1c14"), mouth(120, 136, 18))


def frog_knight():
    green, belly = "#6fbf4a", "#eaf6b8"
    defs = grad("gb", green) + grad("gl", belly) + grad("st", "#c3ced8", "lin")
    body = (
        f'<path d="M80 150 C50 170 46 226 64 246 L176 246 C194 226 190 170 160 150 Z" fill="#c7354a" {S(o)}/>'
        f'<ellipse cx="120" cy="196" rx="62" ry="52" fill="url(#gb)" {S(o)}/><ellipse cx="120" cy="206" rx="40" ry="38" fill="url(#gl)" {S(mix(green, "#000000", 0.4), 3)}/>'
        f'<ellipse cx="82" cy="244" rx="26" ry="10" fill="url(#gb)" {S(o, 3.5)}/><ellipse cx="158" cy="244" rx="26" ry="10" fill="url(#gb)" {S(o, 3.5)}/>'
        f'<path d="M190 200 L214 120" {S("#7a4a26", 7)}/><path d="M214 126 L222 94 L206 94 Z" fill="url(#st)" {S(o, 3.5)}/><rect x="196" y="190" width="28" height="9" rx="3" fill="#ffd34d" {S(o, 3)}/>'
        f'<circle cx="62" cy="196" r="12" fill="url(#gb)" {S(o, 3.5)}/><circle cx="178" cy="196" r="12" fill="url(#gb)" {S(o, 3.5)}/>'
        f'<ellipse cx="120" cy="124" rx="70" ry="48" fill="url(#gb)" {S(o)}/>'
        f'<circle cx="84" cy="84" r="26" fill="url(#gb)" {S(o)}/><circle cx="156" cy="84" r="26" fill="url(#gb)" {S(o)}/>'
        f'<path d="M92 66 C92 34 148 34 148 66 L140 74 L100 74 Z" fill="url(#st)" {S(o)}/><path d="M120 36 L120 70" stroke="#7d8a98" stroke-width="5"/>'
        f'<path d="M124 38 C150 20 170 26 176 44 C160 36 144 38 132 50 Z" fill="#e23b3b" {S(o, 3.5)}/>'
        f'{cheeks(78, 162, 138, "#ff9a9a", 9, 6)}'
    )
    return rig(defs, body, eyes(84, 156, 88, r=8, white=True), mouth(120, 136, 40))


def troll(palette=("#8fa88a", "#d8d2b8"), club=False, scale=1.0, moss=True, sad=False):
    skin, tusk = palette
    defs = grad("tb", skin) + grad("cloth", "#8a5a32", "lin")
    body = (
        (f'<path d="M184 230 L206 120" {S("#7a4a26", 12)}/><ellipse cx="208" cy="106" rx="24" ry="30" fill="#8a5a32" {S(o)}/>' if club else "")
        + f'<ellipse cx="120" cy="190" rx="84" ry="62" fill="url(#tb)" {S(o)}/>'
        f'<ellipse cx="120" cy="204" rx="50" ry="42" fill="{light(skin, 0.18)}" {S(mix(skin, "#000000", 0.4), 3)}/>'
        f'<path d="M78 224 L162 224 L170 248 L70 248 Z" fill="url(#cloth)" {S(o, 3.5)}/>'
        f'<path d="M44 150 C14 170 14 218 36 232 C56 232 64 210 64 190 Z" fill="url(#tb)" {S(o)}/><circle cx="34" cy="232" r="19" fill="url(#tb)" {S(o)}/>'
        f'<path d="M196 150 C226 170 226 218 204 232 C184 232 176 210 176 190 Z" fill="url(#tb)" {S(o)}/><circle cx="206" cy="232" r="19" fill="url(#tb)" {S(o)}/>'
        + (f'<path d="M50 156 C58 138 82 144 84 158 C74 152 62 154 50 156 Z M156 156 C162 140 186 138 192 156 C176 150 166 150 156 156 Z" fill="#5fae45" {S(o, 3)}/>' if moss else "")
        + f'<path d="M72 104 L42 90 L66 122 Z M168 104 L198 90 L174 122 Z" fill="url(#tb)" {S(o, 3.5)}/>'
        f'<ellipse cx="120" cy="100" rx="56" ry="48" fill="url(#tb)" {S(o)}/>'
        + (f'<path d="M84 60 C92 38 112 44 114 58 C104 52 94 54 84 60 Z M128 56 C132 40 152 42 158 60 C146 54 138 52 128 56 Z" fill="#5fae45" {S(o, 3)}/>' if moss else "")
        + f'<ellipse cx="120" cy="112" rx="14" ry="12" fill="{light(skin, 0.15)}" {S(o, 3.5)}/>'
        f'<path d="M84 116 L90 138 L100 122 Z M156 116 L150 138 L140 122 Z" fill="{tusk}" {S(o, 3)}/>'
        f'<path d="M84 80 C94 88 104 86 108 82 M156 80 C146 88 136 86 132 82" stroke="{o}" stroke-width="{4.5}" fill="none" stroke-linecap="round"'
        + (' transform="rotate(0)"' if not sad else ' ') + '/>'
    )
    return rig(defs, body, eyes(98, 142, 96, r=6.5, pupil="#2a1c14"), mouth(120, 132, 30), tf=fit(scale) if scale != 1 else None)


def fairy():
    defs = grad("fk", "#f6cdb0") + grad("pet", "#ff9ac8", "lin") + grad("hr", "#ffd0e8", "lin") + '<linearGradient id="wg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e8fbff" stop-opacity="0.95"/><stop offset="1" stop-color="#b9e3ff" stop-opacity="0.75"/></linearGradient>'
    inner = (
        f'<path d="M120 150 C60 90 20 110 34 160 C50 190 100 176 120 160 Z" fill="url(#wg)" {S("#7fb8e0", 4)}/>'
        f'<path d="M120 150 C180 90 220 110 206 160 C190 190 140 176 120 160 Z" fill="url(#wg)" {S("#7fb8e0", 4)}/>'
        f'<path d="M120 164 C70 170 50 210 74 224 C96 226 114 200 120 176 Z" fill="url(#wg)" {S("#7fb8e0", 4)}/>'
        f'<path d="M120 164 C170 170 190 210 166 224 C144 226 126 200 120 176 Z" fill="url(#wg)" {S("#7fb8e0", 4)}/>'
        f'<path d="M104 190 L96 236 M136 190 L144 236" {S("#f6cdb0", 7)}/><ellipse cx="94" cy="240" rx="11" ry="6" fill="#ff9ac8" {S(o, 3.5)}/><ellipse cx="146" cy="240" rx="11" ry="6" fill="#ff9ac8" {S(o, 3.5)}/>'
        f'<path d="M88 182 C84 156 156 156 152 182 C168 196 178 216 164 222 C148 214 92 214 76 222 C62 216 72 196 88 182 Z" fill="url(#pet)" {S(o)}/>'
        f'<circle cx="82" cy="190" r="8" fill="url(#fk)" {S(o, 3)}/><circle cx="158" cy="190" r="8" fill="url(#fk)" {S(o, 3)}/>'
        f'<path d="M158 188 L186 150" {S("#7a4a26", 4)}/><path d="M186 140 L190 150 L200 152 L192 158 L194 168 L186 162 L178 168 L180 158 L172 152 L182 150 Z" fill="#ffe680" {S(o, 2.5)}/>'
        f'<ellipse cx="120" cy="108" rx="40" ry="42" fill="url(#fk)" {S(o)}/>'
        f'<path d="M80 108 C70 60 120 52 126 62 C140 54 170 70 160 108 C152 88 138 80 120 80 C102 80 90 88 80 108 Z" fill="url(#hr)" {S(o)}/>'
        f'<path d="M82 112 C76 140 82 154 92 158 C96 142 92 124 96 112 Z" fill="url(#hr)" {S(o)}/>'
        f'{cheeks(96, 144, 124, "#ff9a9a", 7, 4)}'
        f'<circle cx="150" cy="70" r="9" fill="#ffe680" {S(o, 2.5)}/>'
    )
    return rig(defs, inner, eyes(106, 134, 108, r=6.5), mouth(120, 130, 12), tf=fit(0.9), shadow=ground_shadow(rx=40, ry=6))


def hen():
    cream = "#fff2d0"
    defs = grad("hb", cream) + grad("wg", "#f3dca8", "lin")
    body = (
        f'<path d="M62 150 C26 110 40 80 60 100 C58 120 70 138 84 144 Z" fill="url(#hb)" {S(o)}/><path d="M178 150 C214 110 200 80 180 100 C182 120 170 138 156 144 Z" fill="url(#hb)" {S(o)}/>'
        f'<ellipse cx="120" cy="176" rx="70" ry="66" fill="url(#hb)" {S(o)}/>'
        f'<path d="M64 170 C58 204 84 224 116 218 C100 200 92 186 92 168 Z" fill="url(#wg)" {S(o, 3.5)}/><path d="M176 170 C182 204 156 224 124 218 C140 200 148 186 148 168 Z" fill="url(#wg)" {S(o, 3.5)}/>'
        f'<path d="M96 240 L88 254 M96 240 L100 254 M96 240 L108 252" {S("#e8a020", 5)}/><path d="M144 240 L136 254 M144 240 L140 254 M144 240 L156 252" {S("#e8a020", 5)}/>'
        f'<circle cx="120" cy="100" r="50" fill="url(#hb)" {S(o)}/>'
        f'<path d="M96 56 C94 30 112 28 116 46 C118 24 138 24 138 46 C146 34 160 44 148 62 Z" fill="#e23b3b" {S(o)}/>'
        f'<path d="M70 70 C80 56 160 56 170 70 C150 78 90 78 70 70 Z" fill="#3a9ad6" {S(o, 3.5)}/><circle cx="96" cy="68" r="3.5" fill="#fff"/><circle cx="120" cy="66" r="3.5" fill="#fff"/><circle cx="144" cy="68" r="3.5" fill="#fff"/>'
        f'<path d="M112 134 C108 154 118 160 124 148 C128 160 138 152 132 134 Z" fill="#e23b3b" {S(o, 3.5)}/>'
        f'{cheeks(84, 156, 112, "#ff9a9a", 9, 6)}'
    )
    return rig(defs, body, eyes(98, 142, 98, r=7.5), beak_mouth(120, 118, 14))


def otter():
    brown, cream = "#9a6440", "#f1dcb8"
    defs = grad("tb", brown) + grad("tc", cream)
    body = (
        f'<path d="M172 236 C226 230 238 190 214 160 C212 190 196 206 168 212 Z" fill="url(#tb)" {S(o)}/>'
        f'<path d="M76 244 C70 204 86 162 96 154 C110 146 130 146 144 154 C154 162 170 204 164 244 Z" fill="url(#tb)" {S(o)}/>'
        f'<ellipse cx="120" cy="204" rx="34" ry="42" fill="url(#tc)" {S(mix(brown, "#000000", 0.4), 3)}/>'
        f'<circle cx="80" cy="210" r="12" fill="url(#tb)" {S(o, 3.5)}/><circle cx="160" cy="210" r="12" fill="url(#tb)" {S(o, 3.5)}/>'
        f'<circle cx="74" cy="64" r="16" fill="url(#tb)" {S(o)}/><circle cx="166" cy="64" r="16" fill="url(#tb)" {S(o)}/>'
        f'<ellipse cx="120" cy="104" rx="56" ry="50" fill="url(#tb)" {S(o)}/>'
        f'<ellipse cx="120" cy="124" rx="34" ry="26" fill="url(#tc)" {S(mix(brown, "#000000", 0.4), 3)}/>'
        f'<ellipse cx="120" cy="110" rx="10" ry="7" fill="#2a1c14"/><ellipse cx="117" cy="108" rx="3" ry="2" fill="#fff" opacity="0.8"/>'
        f'<path d="M96 120 L66 114 M96 126 L64 130 M144 120 L174 114 M144 126 L176 130" stroke="{o}" stroke-width="2.5" stroke-linecap="round"/>'
        f'{cheeks(78, 162, 118, "#ff9a9a", 8, 5)}'
    )
    return rig(defs, body, eyes(100, 140, 96, r=7.5), mouth(120, 132, 16))


# ================================================================ monsters

def slime(c="#59c47a", crown=False):
    defs = grad("sl", c)
    body = (
        f'<path d="M34 246 C24 200 52 140 120 134 C188 140 216 200 206 246 C190 252 170 242 150 250 C130 242 110 252 90 244 C70 252 48 250 34 246 Z" fill="url(#sl)" {S(o)}/>'
        f'<ellipse cx="86" cy="164" rx="22" ry="12" fill="#fff" opacity="0.45" transform="rotate(-24 86 164)"/>'
        f'<circle cx="168" cy="210" r="7" fill="{light(c, 0.5)}" opacity="0.8"/><circle cx="62" cy="214" r="5" fill="{light(c, 0.5)}" opacity="0.8"/><circle cx="150" cy="186" r="4" fill="{light(c, 0.5)}" opacity="0.8"/>'
        f'{cheeks(76, 164, 206, "#ff9a9a", 10, 6)}'
        + (f'<path d="M92 142 L96 112 L108 128 L120 104 L132 128 L144 112 L148 142 Z" fill="#ffd34d" {S(o)}/>' if crown else '')
    )
    return rig(defs, body, eyes(96, 144, 184, r=11), mouth(120, 212, 24), tf=fit(1.3))


def bat(c="#8a6fb0", crown=False, big=False):
    defs = grad("bb", c) + grad("bw", dark(c, 0.15), "lin")
    wl = "M96 140 C60 100 20 100 8 130 C20 124 30 134 34 150 C44 140 56 148 58 164 C72 154 84 160 92 176 Z"
    wr = "M144 140 C180 100 220 100 232 130 C220 124 210 134 206 150 C196 140 184 148 182 164 C168 154 156 160 148 176 Z"
    body = (
        f'<path d="{wl}" fill="url(#bw)" {S(o)}/><path d="{wr}" fill="url(#bw)" {S(o)}/>'
        f'<path d="M80 54 L70 18 L104 40 Z M160 54 L170 18 L136 40 Z" fill="url(#bb)" {S(o)}/>'
        f'<ellipse cx="120" cy="160" rx="52" ry="58" fill="url(#bb)" {S(o)}/><ellipse cx="120" cy="176" rx="30" ry="36" fill="{light(c, 0.45)}" {S(o, 3)}/>'
        f'<circle cx="120" cy="104" r="50" fill="url(#bb)" {S(o)}/>'
        f'<path d="M104 140 L110 154 L116 140 M124 140 L130 154 L136 140" fill="#fff" {S(o, 2.5)}/>'
        f'<path d="M104 232 L98 248 M136 232 L142 248" {S("#e8a020", 6)}/>'
        + (f'<path d="M86 66 L90 34 L104 50 L120 26 L136 50 L150 34 L154 66 Z" fill="#ffd34d" {S(o)}/><circle cx="120" cy="40" r="5" fill="#e23b3b" {S(o, 2.5)}/>' if crown else '')
        + f'{cheeks(86, 154, 120, "#ff9a9a", 8, 5)}'
    )
    return rig(defs, body, eyes(100, 140, 100, r=10, white=True, pupil="#3a2a60"), mouth(120, 128, 16, tint="#4a1a30"))


def spider(c="#7a5aa0", mark="#e8b44a", crown=False, big=False):
    defs = grad("sb", c) + grad("sh", light(c, 0.1))
    legs = ""
    for side in (-1, 1):
        for k, (dx, dy, ex, ey) in enumerate([(40, 130, 100, 70), (50, 150, 112, 140), (50, 170, 110, 210), (40, 188, 96, 250)]):
            x0 = 120 + side * 30
            x1 = 120 + side * dx
            x2 = 120 + side * ex
            legs += f'<path d="M{x0} {dy + 20} C{x1 + side * 20} {dy - 20} {x2} {ey - 10} {x2} {ey + (6 if k == 3 else 0)}" fill="none" {S(o, 11)}/><path d="M{x0} {dy + 20} C{x1 + side * 20} {dy - 20} {x2} {ey - 10} {x2} {ey + (6 if k == 3 else 0)}" fill="none" stroke="{dark(c, 0.1)}" stroke-width="5" stroke-linecap="round"/>'
    body = (
        legs
        + f'<ellipse cx="120" cy="182" rx="66" ry="58" fill="url(#sb)" {S(o)}/>'
        f'<path d="M120 138 L138 170 L120 206 L102 170 Z" fill="{mark}" {S(o, 3.5)}/><ellipse cx="96" cy="158" rx="14" ry="8" fill="#fff" opacity="0.35" transform="rotate(-30 96 158)"/>'
        f'<ellipse cx="120" cy="112" rx="46" ry="40" fill="url(#sh)" {S(o)}/>'
        f'<circle cx="96" cy="84" r="6" fill="#2a1c40" {S(o, 2)}/><circle cx="144" cy="84" r="6" fill="#2a1c40" {S(o, 2)}/><circle cx="120" cy="78" r="5" fill="#2a1c40" {S(o, 2)}/>'
        f'<path d="M104 134 L100 150 L112 140 Z M136 134 L140 150 L128 140 Z" fill="#fff" {S(o, 2.5)}/>'
        f'{cheeks(86, 154, 124, "#ff9a9a", 7, 5)}'
        + (f'<path d="M92 76 L96 48 L108 62 L120 40 L132 62 L144 48 L148 76 Z" fill="#ffd34d" {S(o)}/><circle cx="120" cy="52" r="5" fill="#e23b3b" {S(o, 2.5)}/>' if crown else '')
    )
    return rig(defs, body, eyes(104, 136, 106, r=9, white=True, pupil="#2a1c40"), mouth(120, 130, 16, tint="#4a1a30"))


def wolf(c="#8c8c98", ruff="#d8d8e0", moon=False, glow="#2a170b"):
    defs = grad("wb", c) + grad("wr", ruff)
    body = (
        f'<path d="M170 236 C220 230 232 180 210 150 C204 184 190 200 168 206 Z" fill="url(#wb)" {S(o)}/>'
        f'<path d="M80 244 C70 200 84 168 96 156 C112 148 130 148 144 156 C156 168 170 200 160 244 Z" fill="url(#wb)" {S(o)}/><ellipse cx="120" cy="208" rx="30" ry="36" fill="url(#wr)" {S(mix(c, "#000000", 0.4), 3)}/>'
        f'<ellipse cx="86" cy="244" rx="20" ry="9" fill="url(#wb)" {S(o, 3.5)}/><ellipse cx="154" cy="244" rx="20" ry="9" fill="url(#wb)" {S(o, 3.5)}/>'
        + (f'<path d="M64 136 C40 150 44 188 68 190 C80 176 92 170 108 168 C100 158 84 144 64 136 Z M176 136 C200 150 196 188 172 190 C160 176 148 170 132 168 C140 158 156 144 176 136 Z" fill="url(#wr)" {S(o, 3.5)}/>' if moon else '')
        + f'<path d="M66 84 L52 24 L100 60 Z M174 84 L188 24 L140 60 Z" fill="url(#wb)" {S(o)}/><path d="M68 72 L62 42 L90 62 Z M172 72 L178 42 L150 62 Z" fill="#e9a8b8"/>'
        f'<ellipse cx="120" cy="104" rx="58" ry="52" fill="url(#wb)" {S(o)}/>'
        f'<ellipse cx="120" cy="124" rx="32" ry="26" fill="url(#wr)" {S(mix(c, "#000000", 0.4), 3)}/>'
        f'<ellipse cx="120" cy="112" rx="11" ry="8" fill="#2a1c14"/><ellipse cx="117" cy="110" rx="3.5" ry="2" fill="#fff" opacity="0.8"/>'
        + (f'<path d="M112 58 C104 66 104 80 114 86 C108 78 110 66 120 62 C124 60 120 58 112 58 Z" fill="#cfeaff" {S("#4a6a9a", 2.5)}/>' if moon else '')
        + f'{cheeks(76, 164, 118, "#ff9a9a", 8, 5)}'
    )
    return rig(defs, body, eyes(98, 142, 96, r=8, pupil=glow), mouth(120, 134, 18))


def skeleton(captain=False):
    bone, shade = "#f3efe0", "#cfc8b0"
    defs = grad("bn", bone)
    ribs = "".join(f'<path d="M{86 + i * 2} {y} C{104} {y + 12} {136} {y + 12} {154 - i * 2} {y}" fill="none" {S(o, 8)}/><path d="M{86 + i * 2} {y} C{104} {y + 12} {136} {y + 12} {154 - i * 2} {y}" fill="none" stroke="{bone}" stroke-width="4" stroke-linecap="round"/>'
                   for i, y in enumerate((164, 182, 200)))
    body = (
        f'<path d="M104 210 L98 244 M136 210 L142 244" {S(o, 14)}/><path d="M104 210 L98 244 M136 210 L142 244" stroke="{bone}" stroke-width="8" stroke-linecap="round"/>'
        f'<ellipse cx="94" cy="248" rx="16" ry="7" fill="url(#bn)" {S(o, 3.5)}/><ellipse cx="146" cy="248" rx="16" ry="7" fill="url(#bn)" {S(o, 3.5)}/>'
        f'<path d="M120 150 L120 214" {S(o, 14)}/><path d="M120 150 L120 214" stroke="{bone}" stroke-width="8" stroke-linecap="round"/>{ribs}'
        f'<path d="M92 160 C66 176 60 204 70 226 M148 160 C174 176 180 204 170 226" fill="none" {S(o, 12)}/><path d="M92 160 C66 176 60 204 70 226 M148 160 C174 176 180 204 170 226" fill="none" stroke="{bone}" stroke-width="6" stroke-linecap="round"/>'
        f'<circle cx="68" cy="230" r="9" fill="url(#bn)" {S(o, 3.5)}/><circle cx="172" cy="230" r="9" fill="url(#bn)" {S(o, 3.5)}/>'
        f'<path d="M72 98 C70 52 170 52 168 98 C168 118 154 126 148 140 L92 140 C86 126 72 118 72 98 Z" fill="url(#bn)" {S(o)}/>'
        f'<ellipse cx="98" cy="98" rx="19" ry="21" fill="#2a1c24"/><ellipse cx="142" cy="98" rx="19" ry="21" fill="#2a1c24"/>'
        f'<path d="M120 112 L113 128 L127 128 Z" fill="#2a1c24"/>'
        f'<path d="M88 138 L152 138 L148 156 L92 156 Z" fill="url(#bn)" {S(o, 3.5)}/>'
        + (f'<path d="M64 74 C80 36 160 36 176 74 C150 66 90 66 64 74 Z" fill="#2b3a5c" {S(o)}/><path d="M60 78 L180 78" {S("#2b3a5c", 8)}/><circle cx="120" cy="50" r="5" fill="#ffd34d"/>'
           f'<path d="M84 80 L116 108" stroke="#1c1c24" stroke-width="5"/><ellipse cx="98" cy="98" rx="20" ry="21" fill="#1c1c24" {S(o, 2)}/>' if captain else '')
    )
    teeth = "".join(f'<path d="M{x} 140 L{x} 154" stroke="{o}" stroke-width="3"/>' for x in range(96, 152, 8))
    calm = f'<path d="M90 146 L150 146" stroke="{o}" stroke-width="3.5"/>{teeth}'
    talk = f'<path d="M92 140 L148 140 L144 168 C132 176 108 176 96 168 Z" fill="#2a1c24" {S(o, 3.5)}/><path d="M96 140 L96 150 M108 140 L108 152 M120 140 L120 152 M132 140 L132 152 M144 140 L144 150" stroke="{bone}" stroke-width="6"/>'
    eye_open = '<circle cx="100" cy="100" r="6" fill="#fffbe6"/><circle cx="144" cy="100" r="6" fill="#fffbe6"/>'
    eye_closed = f'<path d="M86 100 C94 92 106 92 112 100 M130 100 C136 92 148 92 156 100" stroke="{bone}" stroke-width="4" fill="none" stroke-linecap="round"/>'
    return rig(defs, body, (eye_open, eye_closed), (calm, talk))


def ghost(c="#dff2ff"):
    defs = grad("gh", c)
    body = (
        f'<path d="M50 246 C36 160 64 70 120 66 C176 70 204 160 190 246 C176 232 164 252 148 238 C134 254 122 232 108 244 C94 254 80 232 66 246 Z" fill="url(#gh)" fill-opacity="0.92" {S("#6a8ab0")}/>'
        f'<path d="M58 170 C32 168 24 188 36 202 C50 204 62 196 66 184 Z M182 170 C208 168 216 188 204 202 C190 204 178 196 174 184 Z" fill="url(#gh)" fill-opacity="0.92" {S("#6a8ab0", 3.5)}/>'
        f'<ellipse cx="92" cy="110" rx="16" ry="9" fill="#fff" opacity="0.6" transform="rotate(-24 92 110)"/>'
        f'{cheeks(82, 158, 160, "#ffb3c8", 10, 6)}'
    )
    ec = '<path d="M84 142 C92 150 104 150 110 142 M130 142 C136 150 148 150 156 142" stroke="#2a3a60" stroke-width="4" fill="none" stroke-linecap="round"/>'
    eo = '<ellipse cx="98" cy="140" rx="11" ry="15" fill="#1c2440"/><ellipse cx="142" cy="140" rx="11" ry="15" fill="#1c2440"/><circle cx="102" cy="134" r="3.5" fill="#fff"/><circle cx="146" cy="134" r="3.5" fill="#fff"/>'
    calm = f'<ellipse cx="120" cy="170" rx="8" ry="6" fill="#1c2440"/>'
    talk = f'<ellipse cx="120" cy="174" rx="12" ry="14" fill="#1c2440"/>'
    return rig(defs, body, (eo, ec), (calm, talk), shadow=ground_shadow(op=0.1))


def boar(c="#8a5a3a"):
    defs = grad("bo", c) + grad("sn", "#e8a8a0")
    body = (
        f'<path d="M180 214 C210 204 216 176 206 160 C196 176 190 186 178 190 Z" fill="url(#bo)" {S(o)}/>'
        f'<ellipse cx="120" cy="190" rx="84" ry="56" fill="url(#bo)" {S(o)}/>'
        f'<path d="M50 150 L60 130 L72 148 L84 126 L96 146 L110 124 L122 146 L136 126 L148 148 L162 130 L172 152 L186 134" fill="none" {S(dark(c, 0.3), 7)}/>'
        f'<path d="M76 232 L74 250 M104 236 L102 252 M140 236 L138 252 M168 232 L166 250" {S(o, 14)}/><path d="M76 232 L74 250 M104 236 L102 252 M140 236 L138 252 M168 232 L166 250" stroke="{dark(c, 0.2)}" stroke-width="8" stroke-linecap="round"/>'
        f'<path d="M66 84 L54 48 L92 66 Z M174 84 L186 48 L148 66 Z" fill="url(#bo)" {S(o)}/>'
        f'<ellipse cx="120" cy="104" rx="64" ry="52" fill="url(#bo)" {S(o)}/>'
        f'<ellipse cx="120" cy="124" rx="38" ry="28" fill="url(#sn)" {S(o, 3.5)}/>'
        f'<ellipse cx="108" cy="124" rx="6" ry="8" fill="#6a2a2a"/><ellipse cx="132" cy="124" rx="6" ry="8" fill="#6a2a2a"/>'
        f'<path d="M86 140 C74 150 70 134 78 122 C82 132 88 136 94 136 Z M154 140 C166 150 170 134 162 122 C158 132 152 136 146 136 Z" fill="#fff6dc" {S(o, 3)}/>'
        f'{cheeks(72, 168, 104, "#ff9a9a", 8, 5)}'
    )
    return rig(defs, body, eyes(92, 148, 92, r=7), mouth(120, 150, 22))


def crab(c="#7a8fb0"):
    defs = grad("cb", c) + grad("cr", light(c, 0.1))
    body = (
        f'<path d="M62 190 C20 170 6 120 36 90 C48 84 62 90 66 104 C52 106 44 118 54 134 C66 146 84 158 92 170 Z" fill="url(#cb)" {S(o)}/>'
        f'<path d="M178 190 C220 170 234 120 204 90 C192 84 178 90 174 104 C188 106 196 118 186 134 C174 146 156 158 148 170 Z" fill="url(#cb)" {S(o)}/>'
        f'<path d="M36 90 L24 66 L50 80 Z M204 90 L216 66 L190 80 Z" fill="url(#cb)" {S(o, 3.5)}/>'
        f'<path d="M62 232 L36 250 M80 238 L60 256 M178 232 L204 250 M160 238 L180 256" {S(o, 11)}/><path d="M62 232 L36 250 M80 238 L60 256 M178 232 L204 250 M160 238 L180 256" stroke="{dark(c, 0.1)}" stroke-width="5" stroke-linecap="round"/>'
        f'<ellipse cx="120" cy="196" rx="76" ry="52" fill="url(#cr)" {S(o)}/>'
        f'<path d="M72 170 C80 150 160 150 168 170 M88 150 L96 186 M120 146 L120 190 M152 150 L144 186" fill="none" stroke="{dark(c, 0.3)}" stroke-width="4" stroke-linecap="round"/>'
        f'<ellipse cx="94" cy="166" rx="18" ry="8" fill="#fff" opacity="0.4" transform="rotate(-20 94 166)"/>'
        f'<path d="M100 150 L98 120 M140 150 L142 120" {S(o, 11)}/><path d="M100 150 L98 120 M140 150 L142 120" stroke="{c}" stroke-width="5" stroke-linecap="round"/>'
        f'{cheeks(84, 156, 206, "#ff9a9a", 9, 6)}'
    )
    return rig(defs, body, eyes(98, 142, 114, r=8, white=True), mouth(120, 208, 24))


def golem(c="#8a8f98"):
    defs = grad("gm", c) + grad("gms", light(c, 0.08))
    moss = "#5fae45"

    def block(x, y, w, h, fill="url(#gm)", r=8):
        return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" {S(o)}/>'
    body = (
        block(40, 150, 40, 80) + block(160, 150, 40, 80) + block(34, 210, 52, 36) + block(154, 210, 52, 36)
        + block(70, 138, 100, 88) + block(86, 224, 30, 26) + block(124, 224, 30, 26)
        + f'<path d="M72 150 L168 150 M72 190 L168 190 M120 138 L120 224" stroke="{dark(c, 0.3)}" stroke-width="4"/>'
        + block(76, 56, 88, 82, "url(#gms)", 12)
        + f'<path d="M44 150 C48 134 74 136 76 150 C66 146 54 146 44 150 Z M158 148 C164 132 188 134 194 150 C180 144 170 144 158 148 Z" fill="{moss}" {S(o, 3)}/>'
        f'<path d="M86 60 C92 44 112 46 114 58 C104 54 94 54 86 60 Z M126 56 C130 42 148 44 154 58 C144 52 136 52 126 56 Z" fill="{moss}" {S(o, 3)}/>'
        f'<path d="M92 190 C96 180 108 182 108 192 C102 188 98 188 92 190 Z" fill="{moss}" {S(o, 2.5)}/>'
        f'<path d="M84 90 L106 94 M156 90 L134 94" stroke="{o}" stroke-width="5" stroke-linecap="round"/>'
    )
    eo = '<rect x="88" y="98" width="24" height="16" rx="5" fill="#ffe680" stroke="#7a5a10" stroke-width="3"/><rect x="128" y="98" width="24" height="16" rx="5" fill="#ffe680" stroke="#7a5a10" stroke-width="3"/>'
    ec = '<path d="M88 108 L112 108 M128 108 L152 108" stroke="#7a5a10" stroke-width="5" stroke-linecap="round"/>'
    calm = f'<path d="M98 126 L142 126" stroke="{o}" stroke-width="5" stroke-linecap="round"/>'
    talk = f'<rect x="98" y="120" width="44" height="16" rx="6" fill="#2a2420" stroke="{o}" stroke-width="3.5"/>'
    return rig(defs, body, (eo, ec), (calm, talk))


def imp(c="#3b4a8a"):
    defs = grad("ib", c) + grad("ibl", light(c, 0.15))
    body = (
        f'<path d="M168 220 C214 220 226 180 206 154 C210 184 196 196 168 200 Z" fill="url(#ib)" {S(o)}/><path d="M204 156 L222 130 L226 158 Z" fill="#ffd34d" {S(o, 3)}/>'
        f'<path d="M84 164 C30 128 14 176 40 198 C62 192 78 182 88 174 Z M156 164 C210 128 226 176 200 198 C178 192 162 182 152 174 Z" fill="url(#ibl)" {S(o, 3.5)}/>'
        f'<ellipse cx="120" cy="196" rx="52" ry="52" fill="url(#ib)" {S(o)}/><path d="M92 244 L84 252 M148 244 L156 252" {S(o, 12)}/>'
        f'<path d="M164 190 L196 150" {S("#e8e0c8", 5)}/><path d="M196 150 C206 128 216 132 216 140 C208 140 202 146 196 150 Z" fill="#fff" {S(o, 3)}/>'
        f'<path d="M78 72 L64 26 L104 54 Z M162 72 L176 26 L136 54 Z" fill="url(#ib)" {S(o)}/>'
        f'<path d="M62 100 C60 56 180 56 178 100 C178 134 154 148 120 148 C86 148 62 134 62 100 Z" fill="url(#ib)" {S(o)}/>'
        f'<path d="M56 98 L38 84 L58 118 Z M184 98 L202 84 L182 118 Z" fill="url(#ib)" {S(o, 3.5)}/>'
        f'<ellipse cx="100" cy="70" rx="14" ry="7" fill="#fff" opacity="0.3" transform="rotate(-20 100 70)"/>'
        f'<path d="M96 168 C94 186 104 188 102 172 M138 176 C136 194 146 194 142 178" fill="#1c2a5a" {S("#1c2a5a", 6)}/>'
        f'<circle cx="88" cy="208" r="9" fill="#12182e" opacity="0.8"/><circle cx="150" cy="214" r="7" fill="#12182e" opacity="0.8"/>'
    )
    calm = f'<path d="M92 130 C100 148 140 148 148 130 C134 138 106 138 92 130 Z" fill="#7a1f22" {S(o, 3)}/><path d="M102 134 L106 142 L110 135 Z M130 135 L134 142 L138 134 Z" fill="#fff"/>'
    talk = f'<ellipse cx="120" cy="138" rx="17" ry="14" fill="#7a1f22" {S(o, 3.5)}/><path d="M106 128 L110 138 L116 129 Z M124 129 L130 138 L134 128 Z" fill="#fff"/><ellipse cx="120" cy="146" rx="9" ry="4" fill="#ff7b8a"/>'
    return rig(defs, body, eyes(98, 142, 100, r=9, white=True, pupil="#ffb020"), (calm, talk))


def door_warden(c="#a8693c"):
    """Knocker, the Door Warden: a tall wooden door on stubby boots, with a brass knocker for a nose and a mail slot for a mouth."""
    brass = "#e8b84a"
    defs = grad("dw", c, "lin") + grad("dwi", light(c, 0.1), "lin") + grad("bz", brass)
    iron = "#4a4650"
    body = (
        f'<rect x="76" y="222" width="32" height="28" rx="9" fill="{dark(c, 0.3)}" {S(o)}/><rect x="132" y="222" width="32" height="28" rx="9" fill="{dark(c, 0.3)}" {S(o)}/>'
        f'<path d="M64 130 C40 146 34 176 42 200" fill="none" {S(o, 17)}/><path d="M64 130 C40 146 34 176 42 200" fill="none" stroke="{dark(c, 0.1)}" stroke-width="11" stroke-linecap="round"/>'
        f'<path d="M176 130 C200 146 206 176 198 200" fill="none" {S(o, 17)}/><path d="M176 130 C200 146 206 176 198 200" fill="none" stroke="{dark(c, 0.1)}" stroke-width="11" stroke-linecap="round"/>'
        f'<circle cx="42" cy="204" r="12" fill="url(#bz)" {S(o, 3.5)}/><circle cx="198" cy="204" r="12" fill="url(#bz)" {S(o, 3.5)}/>'
        f'<path d="M60 226 L60 82 C60 20 180 20 180 82 L180 226 Z" fill="url(#dw)" {S(o, 5)}/>'
        f'<path d="M74 214 L74 84 C74 42 166 42 166 84 L166 214 Z" fill="none" stroke="{dark(c, 0.3)}" stroke-width="4" opacity="0.55"/>'
        f'<path d="M92 62 L92 222 M148 62 L148 222" stroke="{dark(c, 0.3)}" stroke-width="3" opacity="0.35"/>'
        f'<rect x="84" y="196" width="72" height="22" rx="6" fill="url(#dwi)" {S(dark(c, 0.35), 3.5)}/>'
        f'<rect x="54" y="76" width="16" height="12" rx="3" fill="{iron}" {S(o, 3)}/><rect x="54" y="134" width="16" height="12" rx="3" fill="{iron}" {S(o, 3)}/><rect x="54" y="192" width="16" height="12" rx="3" fill="{iron}" {S(o, 3)}/>'
        f'<path d="M80 76 L106 69 M160 76 L134 69" stroke="{o}" stroke-width="6" stroke-linecap="round"/>'
        f'<circle cx="120" cy="116" r="11" fill="url(#bz)" {S(o, 3.5)}/><circle cx="120" cy="142" r="19" fill="none" {S(o, 11)}/><circle cx="120" cy="142" r="19" fill="none" stroke="{brass}" stroke-width="6"/>'
        f'<path d="M110 134 C112 128 118 126 124 126" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.7"/>'
        f'<circle cx="82" cy="152" r="3.5" fill="{iron}"/><circle cx="158" cy="152" r="3.5" fill="{iron}"/>'
    )
    calm = f'<rect x="92" y="170" width="56" height="9" rx="4" fill="#2a1a10" {S(brass, 4)}/>'
    talk = (f'<rect x="92" y="166" width="56" height="22" rx="7" fill="#2a1a10" {S(brass, 4)}/>'
            f'<path d="M100 184 L132 184 L136 176 L104 176 Z" fill="#fff6dc" {S(o, 2)}/><path d="M104 178 L130 178" stroke="#b9a27a" stroke-width="2"/>')
    return rig(defs, body, eyes(98, 142, 94, r=9, white=True), (calm, talk))


BARON = dict(skin="fair", robe="#3a3046", trim="#a8301f", hair="#9a9aa6", hairstyle="short", hat="tophat", moustache="#4a4a58", build="tall", cheeks=False)

MONSTERS = {
    "slime": lambda: slime(),
    "bat": lambda: bat(),
    "spider": lambda: spider(),
    "wolf_pup": lambda: wolf(c="#a8a8b4", ruff="#ecece8"),
    "skeleton": lambda: skeleton(),
    "ghost": lambda: ghost(),
    "boar": lambda: boar(),
    "rock_crab": lambda: crab(),
    "cave_troll": lambda: troll(("#a8946f", "#f0e8cc"), club=True, moss=False),
    "web_weaver": lambda: spider("#8a3a5a", "#ffd34d"),
    "moon_wolf": lambda: wolf("#c8d0e0", "#ffffff", moon=True, glow="#2a6ac8"),
    "mossy_golem": lambda: golem(),
    "captain_rattlebones": lambda: skeleton(captain=True),
    "spider_queen": lambda: spider("#5a3a8a", "#ffd34d", crown=True),
    "bat_king": lambda: bat("#5a4a8a", crown=True),
    "inky_imp": lambda: imp(),
    "knocker": lambda: door_warden(),
}

ANIMALS = {
    "professor_hoot": owl,
    "rascal_fox": fox,
    "sir_ribbit": frog_knight,
    "grumble": lambda: troll(sad=True),
    "fern_fairy": fairy,
    "henrietta_hen": hen,
    "otto_otter": otter,
}


def sprites() -> dict:
    out = {}
    for pid, fn in ANIMALS.items():
        out[f"npc_{pid}"] = (fn(), LAYERS)
    for mid, fn in MONSTERS.items():
        out[f"monster_{mid}"] = (fn(), LAYERS)
    # Monsters that are also people: the same painting under the monster's own name.
    out["monster_baron_grumblewick"] = (build_person(BARON), LAYERS)
    out["monster_sneaky_fox"] = out["npc_rascal_fox"]
    out["monster_grumble_troll"] = out["npc_grumble"]
    return out
