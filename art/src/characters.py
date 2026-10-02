"""Character art for Dino Valley: Rex, Mama, eggs and props.

Every character is one SVG in a fixed square box, split into named layers (<g id="...">).
The render step exports each layer as its own transparent image, all the same size, so the
app can stack them and animate them separately (blink the eyes, swap mouths, tilt the head).
"""

OUTLINE = "#6e3010"

REX = {
    "light": "#ffb36e", "mid": "#ff8a3d", "dark": "#e5621d", "spike": "#d9531a",
    "belly_top": "#fff4de", "belly_bottom": "#ffd39a", "stripe": "#efb46e",
    "outline": OUTLINE, "spot": "#e5621d", "cheek": "#ff7f96",
}

MAMA = {
    "light": "#9fe3c9", "mid": "#5cc5a3", "dark": "#36a283", "spike": "#2f8f74",
    "belly_top": "#fbf7e4", "belly_bottom": "#e6efc0", "stripe": "#c9d79a",
    "outline": "#1f5a49", "spot": "#36a283", "cheek": "#ff8fa6",
}


def dino(p: dict, lashes: bool = False) -> str:
    """A friendly T. rex in a 300×300 box, facing right, feet on y≈278."""
    o = p["outline"]
    stroke = f'stroke="{o}" stroke-width="4" stroke-linejoin="round" stroke-linecap="round"'
    lash = (
        f'<g id="lashes"><path d="M190 52 L184 42 M200 48 L198 38 M210 49 L214 40" stroke="{o}" '
        f'stroke-width="3.5" stroke-linecap="round" fill="none"/></g>'
        if lashes else ""
    )
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 300" width="300" height="300">
<defs>
  <radialGradient id="skin" cx="0.38" cy="0.3" r="0.85">
    <stop offset="0" stop-color="{p["light"]}"/><stop offset="0.55" stop-color="{p["mid"]}"/><stop offset="1" stop-color="{p["dark"]}"/>
  </radialGradient>
  <linearGradient id="belly" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="{p["belly_top"]}"/><stop offset="1" stop-color="{p["belly_bottom"]}"/>
  </linearGradient>
  <linearGradient id="spike" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="{p["mid"]}"/><stop offset="1" stop-color="{p["spike"]}"/>
  </linearGradient>
  <radialGradient id="iris" cx="0.35" cy="0.35" r="0.8">
    <stop offset="0" stop-color="#7a4a26"/><stop offset="1" stop-color="#26140a"/>
  </radialGradient>
  <clipPath id="bellyClip"><path d="M164 150 C192 162 196 212 186 242 C176 264 142 268 128 254 C144 226 148 182 164 150 Z"/></clipPath>
</defs>
<g id="shadow"><ellipse cx="150" cy="282" rx="96" ry="11" fill="#000" opacity="0.16"/></g>
<g id="tail">
  <path d="M100 205 C70 212 40 224 12 206 C26 199 40 194 54 188 C70 180 84 171 98 163 Z" fill="url(#skin)" {stroke}/>
  <circle cx="58" cy="200" r="5" fill="{p["spot"]}" opacity="0.5"/><circle cx="78" cy="190" r="6" fill="{p["spot"]}" opacity="0.5"/>
</g>
<g id="leg-back">
  <path d="M104 222 C96 240 98 262 106 276 L140 276 C142 262 138 242 130 226 Z" fill="{p["dark"]}" {stroke}/>
  <ellipse cx="121" cy="277" rx="24" ry="8" fill="{p["dark"]}" {stroke}/>
</g>
<g id="body">
  <path d="M128 112 L114 90 L142 105 Z M108 126 L87 110 L118 117 Z M94 148 L69 138 L100 133 Z M88 172 L62 168 L92 159 Z M86 196 L61 199 L87 183 Z"
        fill="url(#spike)" {stroke}/>
  <path d="M152 108 C206 108 220 168 210 214 C201 254 166 274 132 270 C94 266 80 232 86 196 C92 150 108 110 152 108 Z" fill="url(#skin)" {stroke}/>
  <ellipse cx="124" cy="150" rx="14" ry="28" fill="#fff" opacity="0.18" transform="rotate(20 124 150)"/>
  <circle cx="118" cy="168" r="7" fill="{p["spot"]}" opacity="0.45"/><circle cx="106" cy="198" r="6" fill="{p["spot"]}" opacity="0.45"/>
  <circle cx="134" cy="134" r="5" fill="{p["spot"]}" opacity="0.45"/><circle cx="118" cy="226" r="7" fill="{p["spot"]}" opacity="0.45"/>
  <path d="M164 150 C192 162 196 212 186 242 C176 264 142 268 128 254 C144 226 148 182 164 150 Z" fill="url(#belly)" stroke="{o}" stroke-width="2.5" stroke-opacity="0.35"/>
  <g clip-path="url(#bellyClip)" stroke="{p["stripe"]}" stroke-width="3" fill="none" opacity="0.8" stroke-linecap="round">
    <path d="M140 182 C158 186 178 186 196 180"/><path d="M134 204 C154 208 176 208 196 202"/>
    <path d="M130 226 C150 230 172 230 192 224"/><path d="M128 248 C146 252 166 250 186 244"/>
  </g>
</g>
<g id="leg-front">
  <path d="M150 230 C172 228 182 250 180 268 L182 277 L144 277 C140 262 140 244 150 230 Z" fill="url(#skin)" {stroke}/>
  <g fill="#fff6e6" stroke="{o}" stroke-width="2"><circle cx="181" cy="275" r="4"/><circle cx="172" cy="278" r="4"/><circle cx="163" cy="278" r="4"/></g>
</g>
<g id="arm">
  <path d="M194 172 C208 174 218 184 215 193 C212 199 204 196 197 190 C192 186 190 178 194 172 Z" fill="url(#skin)" {stroke}/>
  <g fill="#fff6e6" stroke="{o}" stroke-width="1.5"><circle cx="216" cy="190" r="2.6"/><circle cx="212" cy="196" r="2.6"/></g>
</g>
<g id="head">
  <path d="M138 76 C144 34 208 18 246 38 C272 52 284 84 270 106 C256 128 214 134 180 128 C152 122 132 102 138 76 Z" fill="url(#skin)" {stroke}/>
  <ellipse cx="196" cy="46" rx="32" ry="11" fill="#fff" opacity="0.22" transform="rotate(-8 196 46)"/>
  <circle cx="160" cy="62" r="6" fill="{p["spot"]}" opacity="0.5"/><circle cx="174" cy="44" r="4" fill="{p["spot"]}" opacity="0.5"/>
  <circle cx="152" cy="86" r="5" fill="{p["spot"]}" opacity="0.5"/>
  <ellipse cx="262" cy="70" rx="3.5" ry="2.4" fill="{o}"/>
  <ellipse cx="236" cy="102" rx="13" ry="7" fill="{p["cheek"]}" opacity="0.75"/>
  <path d="M188 44 C198 38 210 38 218 44" stroke="{o}" stroke-width="4" fill="none" stroke-linecap="round"/>
</g>
{lash}
<g id="eye-open">
  <ellipse cx="204" cy="70" rx="19" ry="21" fill="#fff" stroke="{o}" stroke-width="3"/>
  <circle cx="209" cy="73" r="12" fill="url(#iris)"/>
  <circle cx="210" cy="74" r="6.5" fill="#120a05"/>
  <circle cx="214.5" cy="67" r="4.5" fill="#fff"/><circle cx="205" cy="80" r="2.2" fill="#fff"/>
</g>
<g id="eye-closed">
  <path d="M186 72 C196 82 212 82 222 72" stroke="{o}" stroke-width="4.5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-calm">
  <path d="M214 110 C232 120 252 116 266 104" stroke="{o}" stroke-width="4" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-happy">
  <path d="M210 104 C226 138 258 130 270 98 C252 108 230 110 210 104 Z" fill="#7a1f22" {stroke}/>
  <ellipse cx="242" cy="122" rx="12" ry="6" fill="#ff7b8a"/>
  <path d="M222 107 L226 114 L230 108 Z M240 109 L244 116 L248 109 Z M255 106 L258 112 L262 104 Z" fill="#fff"/>
</g>
<g id="mouth-talk">
  <ellipse cx="248" cy="112" rx="9" ry="7" fill="#7a1f22" stroke="{o}" stroke-width="3"/>
  <ellipse cx="248" cy="115" rx="5" ry="2.5" fill="#ff7b8a"/>
</g>
</svg>'''


DINO_LAYERS = ["shadow", "tail", "leg-back", "body", "leg-front", "arm", "head",
               "eye-open", "eye-closed", "mouth-calm", "mouth-happy", "mouth-talk"]


EGGS = {
    "blue": ("#d4f0ff", "#7cc8f0", "#3f97c9"),
    "green": ("#e6f8d2", "#a6db7e", "#68ad43"),
    "orange": ("#fff0d9", "#ffb27a", "#e2793a"),
}

EGG_PATH = ("M100 14 C140 14 172 86 172 150 C172 208 140 246 100 246 "
            "C60 246 28 208 28 150 C28 86 60 14 100 14 Z")
CRACK = "L28 0 L28 128 L48 116 L64 132 L82 114 L100 132 L118 112 L136 130 L152 114 L172 128 L172 0 Z"


def egg(name: str) -> str:
    light, mid, dark = EGGS[name]
    speckles = "".join(
        f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{dark}" opacity="0.55"/>'
        for x, y, rx, ry in [(70, 90, 7, 9), (122, 70, 5, 6), (132, 150, 9, 7), (82, 176, 6, 5),
                             (110, 206, 8, 6), (62, 136, 4, 5), (146, 112, 4, 4), (96, 120, 3, 3)]
    )
    top_clip = "M0 0 " + CRACK
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 260" width="200" height="260">
<defs>
  <radialGradient id="shell" cx="0.36" cy="0.3" r="0.9">
    <stop offset="0" stop-color="{light}"/><stop offset="0.5" stop-color="{mid}"/><stop offset="1" stop-color="{dark}"/>
  </radialGradient>
  <clipPath id="clipTop"><path d="{top_clip}"/></clipPath>
  <clipPath id="clipBottom"><path d="M0 260 L200 260 L200 0 L172 0 L172 128 L152 114 L136 130 L118 112 L100 132 L82 114 L64 132 L48 116 L28 128 L28 0 L0 0 Z"/></clipPath>
  <g id="shellArt">
    <path d="{EGG_PATH}" fill="url(#shell)"/>
    {speckles}
    <ellipse cx="74" cy="70" rx="14" ry="26" fill="#fff" opacity="0.5" transform="rotate(18 74 70)"/>
    <path d="{EGG_PATH}" fill="none" stroke="{OUTLINE}" stroke-width="5"/>
  </g>
</defs>
<g id="shadow"><ellipse cx="100" cy="250" rx="64" ry="9" fill="#000" opacity="0.18"/></g>
<g id="whole"><use href="#shellArt"/></g>
<g id="top"><g clip-path="url(#clipTop)"><use href="#shellArt"/></g>
  <path d="M28 128 L48 116 L64 132 L82 114 L100 132 L118 112 L136 130 L152 114 L172 128" fill="none" stroke="{OUTLINE}" stroke-width="4" stroke-linejoin="round"/></g>
<g id="bottom"><g clip-path="url(#clipBottom)"><use href="#shellArt"/></g>
  <path d="M28 128 L48 116 L64 132 L82 114 L100 132 L118 112 L136 130 L152 114 L172 128" fill="none" stroke="{OUTLINE}" stroke-width="4" stroke-linejoin="round"/></g>
</svg>'''


EGG_LAYERS = ["shadow", "whole", "top", "bottom"]


def baby() -> str:
    """A hatchling peeking out: big eyes, tiny smile, a bit of shell on its head."""
    p = REX
    o = p["outline"]
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">
<defs><radialGradient id="skin" cx="0.4" cy="0.3" r="0.8">
  <stop offset="0" stop-color="{p["light"]}"/><stop offset="0.6" stop-color="{p["mid"]}"/><stop offset="1" stop-color="{p["dark"]}"/></radialGradient></defs>
<g id="all">
  <path d="M40 200 C36 140 60 92 104 90 C150 88 170 130 166 200 Z" fill="url(#skin)" stroke="{o}" stroke-width="4"/>
  <ellipse cx="80" cy="130" rx="16" ry="18" fill="#fff" stroke="{o}" stroke-width="3"/>
  <ellipse cx="128" cy="130" rx="16" ry="18" fill="#fff" stroke="{o}" stroke-width="3"/>
  <circle cx="84" cy="133" r="9" fill="#26140a"/><circle cx="132" cy="133" r="9" fill="#26140a"/>
  <circle cx="87" cy="128" r="3.5" fill="#fff"/><circle cx="135" cy="128" r="3.5" fill="#fff"/>
  <ellipse cx="66" cy="158" rx="10" ry="5" fill="{p["cheek"]}" opacity="0.5"/><ellipse cx="142" cy="158" rx="10" ry="5" fill="{p["cheek"]}" opacity="0.5"/>
  <path d="M92 160 C100 170 112 170 120 160" stroke="{o}" stroke-width="4" fill="none" stroke-linecap="round"/>
  <path d="M70 96 L84 74 L98 92 L112 70 L128 92 L140 78 L146 98 C128 86 90 86 70 96 Z" fill="#fff0d9" stroke="{o}" stroke-width="3" stroke-linejoin="round"/>
</g>
</svg>'''


def nest() -> str:
    """A twiggy nest, drawn so eggs can sit in its bowl."""
    twigs = []
    import random
    r = random.Random(4)
    for _ in range(70):
        y = r.uniform(70, 150)
        x1 = r.uniform(10, 120)
        x2 = x1 + r.uniform(80, 170)
        bend = r.uniform(-14, 14)
        shade = r.choice(["#8a5a2b", "#a26c36", "#6f4520", "#b9844a"])
        twigs.append(f'<path d="M{x1:.0f} {y:.0f} Q{(x1+x2)/2:.0f} {y+bend:.0f} {min(x2, 390):.0f} {y + r.uniform(-8, 8):.0f}" '
                     f'stroke="{shade}" stroke-width="{r.uniform(3, 7):.1f}" fill="none" stroke-linecap="round"/>')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 180" width="400" height="180">
<defs><clipPath id="bowl"><path d="M8 80 C20 150 100 176 200 176 C300 176 380 150 392 80 C330 104 70 104 8 80 Z"/></clipPath></defs>
<g id="back"><ellipse cx="200" cy="82" rx="192" ry="34" fill="#5a3818"/><ellipse cx="200" cy="86" rx="168" ry="24" fill="#3e2610"/></g>
<g id="front">
  <path d="M8 80 C20 150 100 176 200 176 C300 176 380 150 392 80 C330 104 70 104 8 80 Z" fill="#8a5a2b" stroke="{OUTLINE}" stroke-width="4"/>
  <g clip-path="url(#bowl)">{"".join(twigs)}</g>
  <path d="M8 80 C70 104 330 104 392 80" fill="none" stroke="{OUTLINE}" stroke-width="4"/>
</g>
</svg>'''


NEST_LAYERS = ["back", "front"]


def stone() -> str:
    """A round river stepping stone; the app draws the numeral on top."""
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 150" width="240" height="150">
<defs><radialGradient id="rock" cx="0.4" cy="0.3" r="0.8"><stop offset="0" stop-color="#e3dccf"/><stop offset="0.6" stop-color="#b7ab98"/><stop offset="1" stop-color="#857963"/></radialGradient></defs>
<g id="all">
  <ellipse cx="120" cy="112" rx="112" ry="30" fill="#2f6f8f" opacity="0.35"/>
  <path d="M14 96 C12 50 60 18 120 18 C184 18 230 50 226 96 C222 128 180 136 120 136 C60 136 16 128 14 96 Z" fill="url(#rock)" stroke="{OUTLINE}" stroke-width="5"/>
  <ellipse cx="86" cy="48" rx="34" ry="10" fill="#fff" opacity="0.35"/>
  <circle cx="170" cy="100" r="6" fill="#857963" opacity="0.5"/><circle cx="60" cy="104" r="4" fill="#857963" opacity="0.5"/>
</g>
</svg>'''


def bush() -> str:
    """A fluffy fern bush; it parts to show what's hiding behind it."""
    import random
    r = random.Random(11)
    leaves = []
    for i in range(46):
        cx = r.uniform(40, 360)
        cy = r.uniform(40, 200) + abs(cx - 200) * 0.25
        rx = r.uniform(26, 44)
        shade = r.choice(["#3f9a3a", "#4fae45", "#5fc152", "#378a33", "#6cc95c"])
        leaves.append(f'<ellipse cx="{cx:.0f}" cy="{cy:.0f}" rx="{rx:.0f}" ry="{rx*0.55:.0f}" fill="{shade}" '
                      f'stroke="#1f5a22" stroke-width="3" transform="rotate({r.uniform(-50, 50):.0f} {cx:.0f} {cy:.0f})"/>')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="400" height="260">
<g id="all"><ellipse cx="200" cy="240" rx="180" ry="18" fill="#000" opacity="0.15"/>{"".join(leaves)}</g>
</svg>'''


def mushroom() -> str:
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 180" width="160" height="180">
<defs><radialGradient id="glowFill" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="#b8fff2" stop-opacity="0.9"/><stop offset="1" stop-color="#b8fff2" stop-opacity="0"/></radialGradient>
<radialGradient id="cap" cx="0.4" cy="0.3" r="0.8"><stop offset="0" stop-color="#d9fff8"/><stop offset="0.5" stop-color="#5fe0cf"/><stop offset="1" stop-color="#2a9e9a"/></radialGradient></defs>
<g id="glow"><circle cx="80" cy="80" r="80" fill="url(#glowFill)"/></g>
<g id="all">
  <path d="M66 96 C64 130 62 150 58 168 L102 168 C98 150 96 130 94 96 Z" fill="#f4f0e2" stroke="#2c4a5a" stroke-width="4"/>
  <path d="M18 100 C18 52 50 30 80 30 C110 30 142 52 142 100 C120 92 40 92 18 100 Z" fill="url(#cap)" stroke="#2c4a5a" stroke-width="4"/>
  <circle cx="58" cy="62" r="7" fill="#fff" opacity="0.8"/><circle cx="96" cy="54" r="5" fill="#fff" opacity="0.8"/><circle cx="114" cy="78" r="6" fill="#fff" opacity="0.8"/>
</g>
</svg>'''


def sprites() -> dict:
    """name -> (svg, [layers]); a layer list of None means render the whole image."""
    out = {
        "rex": (dino(REX), DINO_LAYERS),
        "mama": (dino(MAMA, lashes=True), DINO_LAYERS + ["lashes"]),
        "baby": (baby(), None),
        "nest": (nest(), NEST_LAYERS),
        "stone": (stone(), None),
        "bush": (bush(), None),
        "mushroom": (mushroom(), ["glow", "all"]),
    }
    for name in EGGS:
        out[f"egg_{name}"] = (egg(name), EGG_LAYERS)
    return out
