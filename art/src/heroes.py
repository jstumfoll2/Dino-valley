"""Characters for The Little Dungeon: the hero (one look per class), Princess Ruby, the baby
dragon companion, the big dragon, the goblin, the little wizard and the Ink Shadow.

Each character is one SVG in a fixed box, split into named layers (<g id="...">). The build
renders every layer on its own at the same size, so the app can stack them and animate parts
(blink, talk, bob, flap). Characters face right; the app mirrors them when needed.
"""

INK = "#3d2a1c"


def _stroke(color=INK, width=4):
    return f'stroke="{color}" stroke-width="{width}" stroke-linejoin="round" stroke-linecap="round"'


# ------------------------------------------------------------------ dragons

BABY = {
    "light": "#ffc07a", "mid": "#ff8f3f", "dark": "#e5641f", "spike": "#ffcf3f",
    "belly_top": "#fff3d6", "belly_bottom": "#ffd88a", "stripe": "#f0b45e",
    "outline": "#6e3010", "spot": "#e5641f", "cheek": "#ff7f96",
    "wing": "#ffd36b", "wing_dark": "#f0a02c", "horn": "#fff4dc",
}

BIG = {
    "light": "#b8a4ff", "mid": "#8a6fe6", "dark": "#6447c4", "spike": "#ffcf5a",
    "belly_top": "#fff6dc", "belly_bottom": "#ffd99a", "stripe": "#eec27a",
    "outline": "#2e1e66", "spot": "#6447c4", "cheek": "#ff8fb0",
    "wing": "#7fd4ff", "wing_dark": "#3a9ad6", "horn": "#fff4dc",
}

DRAGON_LAYERS = ["shadow", "wing-back", "tail", "leg-back", "body", "leg-front", "wing-front", "arm", "head",
                 "eye-open", "eye-closed", "mouth-calm", "mouth-happy", "mouth-talk"]


def dragon(p: dict) -> str:
    """A friendly round dragon in a 300×300 box, facing right, feet on y≈278."""
    o = p["outline"]
    s = _stroke(o)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 300" width="300" height="300">
<defs>
  <radialGradient id="skin" cx="0.38" cy="0.3" r="0.85">
    <stop offset="0" stop-color="{p["light"]}"/><stop offset="0.55" stop-color="{p["mid"]}"/><stop offset="1" stop-color="{p["dark"]}"/>
  </radialGradient>
  <linearGradient id="bellyFill" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="{p["belly_top"]}"/><stop offset="1" stop-color="{p["belly_bottom"]}"/>
  </linearGradient>
  <linearGradient id="membrane" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="{p["wing"]}"/><stop offset="1" stop-color="{p["wing_dark"]}"/>
  </linearGradient>
  <linearGradient id="spikeFill" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#fff2b0"/><stop offset="1" stop-color="{p["spike"]}"/>
  </linearGradient>
  <radialGradient id="iris" cx="0.35" cy="0.35" r="0.8">
    <stop offset="0" stop-color="#3f8a4a"/><stop offset="1" stop-color="#123018"/>
  </radialGradient>
  <clipPath id="bellyClip"><path d="M164 150 C192 162 196 212 186 242 C176 264 142 268 128 254 C144 226 148 182 164 150 Z"/></clipPath>
</defs>
<g id="shadow"><ellipse cx="150" cy="282" rx="96" ry="11" fill="#000" opacity="0.16"/></g>
<g id="wing-back">
  <path d="M128 128 C108 84 70 52 30 48 C44 64 46 78 40 90 C56 92 64 102 62 116 C80 112 94 122 98 136 C108 134 118 134 128 128 Z" fill="url(#membrane)" {s}/>
  <path d="M128 128 C104 96 74 70 30 48 M128 128 C100 108 80 98 40 90 M128 128 C104 120 86 116 62 116" stroke="{o}" stroke-width="2.5" fill="none" opacity="0.55"/>
</g>
<g id="tail">
  <path d="M100 205 C70 214 44 222 22 204 C34 198 46 194 56 188 C72 180 84 171 98 163 Z" fill="url(#skin)" {s}/>
  <path d="M26 206 L2 192 L10 214 L-2 226 L22 222 Z" fill="url(#spikeFill)" {s} transform="translate(10 -6)"/>
  <circle cx="60" cy="200" r="5" fill="{p["spot"]}" opacity="0.5"/><circle cx="80" cy="190" r="6" fill="{p["spot"]}" opacity="0.5"/>
</g>
<g id="leg-back">
  <path d="M104 222 C96 240 98 262 106 276 L140 276 C142 262 138 242 130 226 Z" fill="{p["dark"]}" {s}/>
  <ellipse cx="121" cy="277" rx="24" ry="8" fill="{p["dark"]}" {s}/>
</g>
<g id="body">
  <path d="M130 114 L122 96 L142 108 Z M110 130 L96 114 L120 121 Z M96 152 L80 140 L102 137 Z M90 176 L72 170 L94 163 Z"
        fill="url(#spikeFill)" {s}/>
  <path d="M152 108 C206 108 220 168 210 214 C201 254 166 274 132 270 C94 266 80 232 86 196 C92 150 108 110 152 108 Z" fill="url(#skin)" {s}/>
  <ellipse cx="124" cy="150" rx="14" ry="28" fill="#fff" opacity="0.2" transform="rotate(20 124 150)"/>
  <circle cx="118" cy="170" r="7" fill="{p["spot"]}" opacity="0.4"/><circle cx="106" cy="200" r="6" fill="{p["spot"]}" opacity="0.4"/>
  <circle cx="118" cy="228" r="7" fill="{p["spot"]}" opacity="0.4"/>
  <path d="M164 150 C192 162 196 212 186 242 C176 264 142 268 128 254 C144 226 148 182 164 150 Z" fill="url(#bellyFill)" stroke="{o}" stroke-width="2.5" stroke-opacity="0.35"/>
  <g clip-path="url(#bellyClip)" stroke="{p["stripe"]}" stroke-width="3" fill="none" opacity="0.8" stroke-linecap="round">
    <path d="M140 182 C158 186 178 186 196 180"/><path d="M134 204 C154 208 176 208 196 202"/>
    <path d="M130 226 C150 230 172 230 192 224"/><path d="M128 248 C146 252 166 250 186 244"/>
  </g>
</g>
<g id="leg-front">
  <path d="M150 230 C172 228 182 250 180 268 L182 277 L144 277 C140 262 140 244 150 230 Z" fill="url(#skin)" {s}/>
  <g fill="#fff6e6" stroke="{o}" stroke-width="2"><circle cx="181" cy="275" r="4"/><circle cx="172" cy="278" r="4"/><circle cx="163" cy="278" r="4"/></g>
</g>
<g id="wing-front">
  <path d="M150 128 C150 96 168 72 196 62 C190 76 192 88 200 96 C188 98 182 106 184 118 C172 116 160 122 158 134 Z" fill="url(#membrane)" {s}/>
  <path d="M152 130 C160 104 176 80 196 62 M154 130 C168 112 182 100 200 96" stroke="{o}" stroke-width="2.5" fill="none" opacity="0.55"/>
</g>
<g id="arm">
  <path d="M194 172 C208 174 218 184 215 193 C212 199 204 196 197 190 C192 186 190 178 194 172 Z" fill="url(#skin)" {s}/>
  <g fill="#fff6e6" stroke="{o}" stroke-width="1.5"><circle cx="216" cy="190" r="2.6"/><circle cx="212" cy="196" r="2.6"/></g>
</g>
<g id="head">
  <path d="M168 40 C160 22 162 8 170 0 C176 14 182 26 186 36 Z" fill="{p["horn"]}" {s}/>
  <path d="M196 34 C196 16 204 4 214 0 C214 14 214 26 212 34 Z" fill="{p["horn"]}" {s}/>
  <path d="M138 76 C144 34 208 18 246 38 C272 52 284 84 270 106 C256 128 214 134 180 128 C152 122 132 102 138 76 Z" fill="url(#skin)" {s}/>
  <path d="M140 70 L124 62 L138 84 Z" fill="url(#spikeFill)" {s}/>
  <ellipse cx="200" cy="46" rx="30" ry="10" fill="#fff" opacity="0.24" transform="rotate(-8 200 46)"/>
  <circle cx="160" cy="62" r="6" fill="{p["spot"]}" opacity="0.45"/><circle cx="152" cy="88" r="5" fill="{p["spot"]}" opacity="0.45"/>
  <ellipse cx="262" cy="70" rx="3.5" ry="2.4" fill="{o}"/><ellipse cx="254" cy="66" rx="3" ry="2" fill="{o}"/>
  <ellipse cx="234" cy="102" rx="13" ry="7" fill="{p["cheek"]}" opacity="0.75"/>
  <path d="M186 44 C196 38 208 38 216 44" stroke="{o}" stroke-width="4" fill="none" stroke-linecap="round"/>
</g>
<g id="eye-open">
  <ellipse cx="204" cy="70" rx="20" ry="22" fill="#fff" stroke="{o}" stroke-width="3"/>
  <circle cx="209" cy="73" r="13" fill="url(#iris)"/>
  <circle cx="210" cy="74" r="7" fill="#081408"/>
  <circle cx="215" cy="66" r="5" fill="#fff"/><circle cx="205" cy="81" r="2.4" fill="#fff"/>
</g>
<g id="eye-closed">
  <path d="M185 72 C195 83 213 83 223 72" stroke="{o}" stroke-width="4.5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-calm">
  <path d="M214 110 C232 120 252 116 266 104" stroke="{o}" stroke-width="4" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-happy">
  <path d="M210 104 C226 138 258 130 270 98 C252 108 230 110 210 104 Z" fill="#7a1f22" {s}/>
  <ellipse cx="242" cy="122" rx="12" ry="6" fill="#ff7b8a"/>
  <path d="M224 107 L228 114 L232 108 Z M256 106 L259 112 L263 104 Z" fill="#fff"/>
</g>
<g id="mouth-talk">
  <ellipse cx="248" cy="112" rx="9" ry="7" fill="#7a1f22" stroke="{o}" stroke-width="3"/>
  <ellipse cx="248" cy="115" rx="5" ry="2.5" fill="#ff7b8a"/>
</g>
</svg>'''


# ------------------------------------------------------------------ kids: the hero and Ruby

KID_LAYERS = ["shadow", "cape", "legs", "arm-back", "body", "head", "eye-open", "eye-closed",
              "mouth-calm", "mouth-happy", "mouth-talk", "hat", "feather", "gear", "arm-front"]

SKIN = {"light": "#ffe0bd", "mid": "#f3c08e", "dark": "#d99a66", "cheek": "#ff9a9a"}


def _kid(outfit: dict, hair: str, hat: str, gear: str, cape: str = "#c7354a", extra_head: str = "") -> str:
    """A big-headed young adventurer in a 300×300 box, three-quarter view facing right, feet on y≈280."""
    o = INK
    s = _stroke(o)
    thin = _stroke(o, 3)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 300" width="300" height="300">
<defs>
  <radialGradient id="face" cx="0.4" cy="0.35" r="0.75">
    <stop offset="0" stop-color="{SKIN["light"]}"/><stop offset="0.7" stop-color="{SKIN["mid"]}"/><stop offset="1" stop-color="{SKIN["dark"]}"/>
  </radialGradient>
  <linearGradient id="tunic" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="{outfit["light"]}"/><stop offset="1" stop-color="{outfit["dark"]}"/>
  </linearGradient>
  <linearGradient id="capeFill" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="{cape}"/><stop offset="1" stop-color="#7a1f30"/>
  </linearGradient>
  <radialGradient id="iris" cx="0.35" cy="0.35" r="0.8">
    <stop offset="0" stop-color="#8a5a32"/><stop offset="1" stop-color="#2a170b"/>
  </radialGradient>
</defs>
<g id="shadow"><ellipse cx="152" cy="284" rx="70" ry="10" fill="#000" opacity="0.16"/></g>
<g id="cape">
  <path d="M120 168 C96 200 88 244 96 274 C120 282 150 280 168 270 C166 236 160 200 150 170 Z" fill="url(#capeFill)" {s}/>
</g>
<g id="legs">
  <path d="M130 236 L128 270 L150 270 L150 238 Z M158 238 L160 270 L182 270 L178 236 Z" fill="{outfit["pants"]}" {s}/>
  <path d="M122 268 C122 260 128 258 140 258 C152 258 156 264 156 272 C156 280 122 282 122 268 Z" fill="{outfit["boots"]}" {s}/>
  <path d="M156 268 C156 260 162 258 174 258 C186 258 192 264 192 272 C192 280 156 282 156 268 Z" fill="{outfit["boots"]}" {s}/>
</g>
<g id="arm-back">
  <path d="M128 182 C114 196 108 214 112 228 C118 232 126 230 128 224 C128 212 134 200 140 192 Z" fill="url(#tunic)" {s}/>
  <circle cx="118" cy="230" r="9" fill="url(#face)" {thin}/>
</g>
<g id="body">
  <path d="M126 170 C142 160 170 160 184 170 C192 196 196 222 194 244 C170 252 140 252 116 244 C114 220 118 192 126 170 Z" fill="url(#tunic)" {s}/>
  <path d="M118 226 C142 232 170 232 194 226 L194 236 C170 242 142 242 116 236 Z" fill="{outfit["belt"]}" {thin}/>
  <rect x="148" y="224" width="16" height="14" rx="3" fill="#ffd34d" {thin}/>
  {outfit.get("chest", "")}
  <ellipse cx="138" cy="190" rx="8" ry="18" fill="#fff" opacity="0.18" transform="rotate(14 138 190)"/>
</g>
<g id="head">
  <path d="M100 98 C96 52 130 30 168 32 C206 34 230 62 226 100 C222 140 196 166 162 166 C126 166 104 140 100 98 Z" fill="url(#face)" {s}/>
  <ellipse cx="114" cy="110" rx="10" ry="14" fill="url(#face)" {thin}/>
  <path d="M98 96 C92 50 132 22 172 26 C210 30 232 58 228 92 C214 70 196 60 170 60 C150 74 126 80 104 80 C104 88 102 94 98 96 Z" fill="{hair}" {s}/>
  <path d="M150 60 C160 52 176 50 186 56" stroke="#fff" stroke-width="4" opacity="0.25" fill="none" stroke-linecap="round"/>
  <ellipse cx="148" cy="128" rx="11" ry="7" fill="{SKIN["cheek"]}" opacity="0.6"/>
  <ellipse cx="210" cy="126" rx="9" ry="6" fill="{SKIN["cheek"]}" opacity="0.6"/>
  <path d="M196 112 C200 118 200 122 196 124" stroke="{o}" stroke-width="3" fill="none" stroke-linecap="round"/>
  {extra_head}
</g>
<g id="eye-open">
  <ellipse cx="166" cy="104" rx="11" ry="13" fill="#fff" stroke="{o}" stroke-width="2.5"/>
  <circle cx="169" cy="106" r="7.5" fill="url(#iris)"/><circle cx="169.5" cy="106.5" r="4" fill="#120a05"/>
  <circle cx="172" cy="102" r="2.8" fill="#fff"/>
  <ellipse cx="207" cy="102" rx="9" ry="12" fill="#fff" stroke="{o}" stroke-width="2.5"/>
  <circle cx="209" cy="104" r="6.5" fill="url(#iris)"/><circle cx="209.5" cy="104.5" r="3.5" fill="#120a05"/>
  <circle cx="211.5" cy="100.5" r="2.4" fill="#fff"/>
  <path d="M154 86 C162 82 172 82 178 86 M198 84 C204 80 212 80 216 84" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>
</g>
<g id="eye-closed">
  <path d="M156 106 C162 112 172 112 177 106 M199 104 C204 110 211 110 215 104" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>
  <path d="M154 86 C162 82 172 82 178 86 M198 84 C204 80 212 80 216 84" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-calm">
  <path d="M178 136 C186 142 196 142 202 136" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-happy">
  <path d="M174 132 C178 152 204 152 208 132 C196 136 186 136 174 132 Z" fill="#7a1f22" {thin}/>
  <ellipse cx="191" cy="144" rx="7" ry="3.5" fill="#ff7b8a"/>
  <path d="M178 134 L204 134 L203 138 L179 138 Z" fill="#fff"/>
</g>
<g id="mouth-talk">
  <ellipse cx="190" cy="140" rx="7" ry="6" fill="#7a1f22" stroke="{o}" stroke-width="3"/>
  <ellipse cx="190" cy="143" rx="4" ry="2" fill="#ff7b8a"/>
</g>
<g id="hat">{hat}</g>
<g id="feather">
  <path d="M132 44 C118 20 120 0 136 -2 C140 14 142 30 140 46 Z" fill="#ffd34d" {thin} transform="translate(-6 8) rotate(-12 136 44)"/>
  <path d="M134 40 C130 26 130 14 134 4" stroke="#e0a020" stroke-width="2" fill="none" transform="translate(-6 8) rotate(-12 136 44)"/>
</g>
<g id="gear">{gear}</g>
<g id="arm-front">
  <path d="M182 178 C200 184 214 196 222 206 C220 214 212 218 206 212 C198 204 188 198 176 192 Z" fill="url(#tunic)" {s}/>
  <circle cx="220" cy="212" r="9" fill="url(#face)" {thin}/>
</g>
</svg>'''


def _outfit(light, dark, pants="#5a4a7a", boots="#6b4226", belt="#7a4a26", chest=""):
    return {"light": light, "dark": dark, "pants": pants, "boots": boots, "belt": belt, "chest": chest}


S = _stroke()
T = _stroke(width=3)

KNIGHT_HAT = f'''
  <path d="M100 92 C96 46 130 22 166 22 C204 22 232 48 228 92 L214 90 C212 62 194 46 166 46 C138 46 118 62 116 92 Z" fill="#c9d3dc" {S}/>
  <path d="M104 86 L226 86" stroke="#8a98a6" stroke-width="5"/>
  <circle cx="166" cy="34" r="6" fill="#ffd34d" {T}/>
  <path d="M164 22 C160 6 168 -2 180 2 C176 10 174 16 172 24 Z" fill="#e23b3b" {T}/>'''
KNIGHT_GEAR = f'''
  <path d="M218 146 L226 146 L226 198 L218 198 Z" fill="#dfe6ec" {T}/>
  <path d="M218 146 L222 132 L226 146 Z" fill="#dfe6ec" {T}/>
  <rect x="206" y="196" width="32" height="8" rx="3" fill="#ffd34d" {T}/>
  <rect x="217" y="204" width="10" height="18" rx="3" fill="#8a5a32" {T}/>'''
KNIGHT_CHEST = f'<path d="M136 176 C150 170 162 170 176 176 L174 210 C162 216 150 216 138 210 Z" fill="#dfe6ec" {T}/><path d="M156 182 L156 206 M146 194 L166 194" stroke="#e23b3b" stroke-width="5" stroke-linecap="round"/>'

WIZARD_HAT = f'''
  <path d="M96 84 C120 74 206 70 232 82 C224 92 196 96 166 96 C134 96 106 94 96 84 Z" fill="#3a5bd0" {S}/>
  <path d="M120 82 C134 46 150 14 196 -6 C186 18 192 44 214 80 C184 88 148 88 120 82 Z" fill="#4f74ea" {S}/>
  <path d="M150 50 L154 58 L162 58 L156 64 L158 72 L150 67 L143 72 L145 64 L139 58 L147 58 Z" fill="#ffd34d"/>
  <circle cx="182" cy="34" r="4" fill="#ffd34d"/><circle cx="174" cy="66" r="3" fill="#fff6c0"/>'''
WIZARD_GEAR = f'''
  <path d="M232 66 L240 66 L238 272 L230 272 Z" fill="#9a6a3a" {T}/>
  <circle cx="236" cy="56" r="26" fill="#8fe8ff" opacity="0.25"/>
  <circle cx="236" cy="56" r="16" fill="#8fe8ff" {T}/>
  <circle cx="231" cy="51" r="5" fill="#fff" opacity="0.9"/>'''

RANGER_HAT = f'''
  <path d="M94 104 C86 52 122 18 166 18 C210 18 238 50 232 98 C226 76 210 56 188 50 C160 44 134 52 118 70 C108 82 102 94 94 104 Z" fill="#3f8f4e" {S}/>
  <path d="M120 36 C106 14 96 6 84 4 C96 20 100 36 106 52 Z" fill="#3f8f4e" {S}/>'''
RANGER_GEAR = f'''
  <path d="M206 120 C244 150 244 230 206 260" fill="none" stroke="#8a5a32" stroke-width="7" stroke-linecap="round"/>
  <path d="M206 120 C244 150 244 230 206 260" fill="none" stroke="{INK}" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
  <path d="M208 122 L208 258" stroke="#f2e6c8" stroke-width="2"/>'''

GUARDIAN_HAT = f'''
  <path d="M100 82 C120 70 208 68 228 80 L226 92 C206 82 122 84 102 94 Z" fill="#2fa3a3" {S}/>
  <circle cx="166" cy="78" r="7" fill="#ffd34d" {T}/>'''
GUARDIAN_GEAR = f'''
  <ellipse cx="222" cy="214" rx="34" ry="40" fill="#2fa3a3" {S}/>
  <ellipse cx="222" cy="214" rx="24" ry="30" fill="#5cc7c7" {T}/>
  <path d="M222 236 C204 222 204 206 214 204 C220 203 222 208 222 210 C222 208 224 203 230 204 C240 206 240 222 222 236 Z" fill="#ff6b8a" {T}/>'''

SPELLKEEPER_HAT = f'''
  <path d="M104 84 C126 70 204 68 226 82 C214 90 190 92 166 92 C140 92 116 90 104 84 Z" fill="#8e4ad8" {S}/>
  <path d="M150 74 L166 50 L182 74 Z" fill="#ffd34d" {T}/>
  <circle cx="166" cy="64" r="5" fill="#ff6b8a"/>'''
SPELLKEEPER_GEAR = f'''
  <path d="M196 200 L222 190 L250 200 L250 236 L222 228 L196 236 Z" fill="#fff4dc" {S}/>
  <path d="M222 190 L222 228" stroke="{INK}" stroke-width="2.5"/>
  <path d="M192 236 L222 228 L254 236 L254 242 L222 234 L192 242 Z" fill="#8e4ad8" {T}/>
  <circle cx="222" cy="180" r="18" fill="#fff6a0" opacity="0.45"/>'''

RUBY_HAIR = "#c8341f"
RUBY_HAT = f'''
  <path d="M136 30 L144 6 L156 24 L166 2 L176 24 L188 6 L196 30 C176 36 156 36 136 30 Z" fill="#ffd34d" {S}/>
  <circle cx="166" cy="20" r="4" fill="#e23b3b"/>'''
RUBY_EXTRA = f'''<path d="M100 92 C88 118 90 150 104 170 C112 150 110 122 112 100 Z" fill="{RUBY_HAIR}" {S}/>'''

CLASSES = {
    "knight": (_outfit("#e65a5a", "#a8323a", chest=KNIGHT_CHEST), "#6b3f1f", KNIGHT_HAT, KNIGHT_GEAR),
    "wizard": (_outfit("#6f8cf2", "#3a4fb8"), "#4a2f1a", WIZARD_HAT, WIZARD_GEAR),
    "ranger": (_outfit("#7cc46a", "#3f8f4e", pants="#6b4a2a"), "#8a5a2a", RANGER_HAT, RANGER_GEAR),
    "guardian": (_outfit("#5cc7c7", "#2a8a8a", pants="#4a5a7a"), "#2a1a10", GUARDIAN_HAT, GUARDIAN_GEAR),
    "spellkeeper": (_outfit("#b98af0", "#7a3fc4", pants="#4a3a6a"), "#e0b050", SPELLKEEPER_HAT, SPELLKEEPER_GEAR),
}


# ------------------------------------------------------------------ the goblin, the wizard, the shadow

GOBLIN_LAYERS = ["shadow", "body", "head", "eye-open", "eye-closed", "mouth-scared", "mouth-happy", "mouth-talk"]


def goblin() -> str:
    o = "#24451c"
    s = _stroke(o)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 240" width="240" height="240">
<defs>
  <radialGradient id="gskin" cx="0.4" cy="0.35" r="0.8">
    <stop offset="0" stop-color="#c4ec8a"/><stop offset="0.6" stop-color="#8fcf5a"/><stop offset="1" stop-color="#5a9a36"/>
  </radialGradient>
</defs>
<g id="shadow"><ellipse cx="120" cy="226" rx="56" ry="8" fill="#000" opacity="0.16"/></g>
<g id="body">
  <path d="M90 150 C96 136 144 136 150 150 C158 176 158 204 150 220 L90 220 C82 204 82 176 90 150 Z" fill="#b5835a" {s}/>
  <path d="M88 196 L152 196" stroke="#7a4a26" stroke-width="6"/>
  <path d="M92 218 C92 210 98 208 108 208 C116 208 118 214 118 222 Z M122 222 C122 214 124 208 132 208 C142 208 148 210 148 218 Z" fill="url(#gskin)" {s}/>
  <path d="M90 156 C72 170 70 188 78 196 C86 198 90 190 92 180 Z M150 156 C168 170 170 188 162 196 C154 198 150 190 148 180 Z" fill="url(#gskin)" {s}/>
</g>
<g id="head">
  <path d="M70 92 C40 70 18 74 10 82 C26 92 40 104 70 112 Z" fill="url(#gskin)" {s}/>
  <path d="M170 92 C200 70 222 74 230 82 C214 92 200 104 170 112 Z" fill="url(#gskin)" {s}/>
  <path d="M28 84 C40 88 52 94 64 102 M212 84 C200 88 188 94 176 102" stroke="#ff9aa8" stroke-width="4" opacity="0.7" fill="none" stroke-linecap="round"/>
  <path d="M60 96 C60 56 88 38 120 38 C152 38 180 56 180 96 C180 132 154 150 120 150 C86 150 60 132 60 96 Z" fill="url(#gskin)" {s}/>
  <path d="M118 104 C112 116 116 124 124 122" stroke="{o}" stroke-width="3" fill="none" stroke-linecap="round"/>
  <ellipse cx="86" cy="116" rx="9" ry="5" fill="#ff9aa8" opacity="0.6"/><ellipse cx="154" cy="116" rx="9" ry="5" fill="#ff9aa8" opacity="0.6"/>
  <path d="M100 40 C104 28 112 24 118 30 M122 38 C128 26 136 24 140 32" stroke="{o}" stroke-width="3" fill="none" stroke-linecap="round"/>
</g>
<g id="eye-open">
  <ellipse cx="96" cy="90" rx="15" ry="17" fill="#fffbe6" stroke="{o}" stroke-width="3"/>
  <ellipse cx="144" cy="90" rx="15" ry="17" fill="#fffbe6" stroke="{o}" stroke-width="3"/>
  <circle cx="99" cy="93" r="8" fill="#3a2410"/><circle cx="147" cy="93" r="8" fill="#3a2410"/>
  <circle cx="102" cy="89" r="3" fill="#fff"/><circle cx="150" cy="89" r="3" fill="#fff"/>
</g>
<g id="eye-closed">
  <path d="M82 92 C90 100 102 100 110 92 M130 92 C138 100 150 100 158 92" stroke="{o}" stroke-width="4" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-scared">
  <path d="M104 134 C110 128 116 136 120 130 C124 136 130 128 136 134" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-happy">
  <path d="M100 126 C106 146 134 146 140 126 C128 132 112 132 100 126 Z" fill="#6a1a1a" stroke="{o}" stroke-width="3"/>
  <path d="M110 129 L114 136 L118 130 Z M124 130 L128 136 L132 129 Z" fill="#fff"/>
</g>
<g id="mouth-talk">
  <ellipse cx="120" cy="134" rx="8" ry="7" fill="#6a1a1a" stroke="{o}" stroke-width="3"/>
</g>
</svg>'''


SMALL_LAYERS = ["shadow", "body", "eye-open", "eye-closed", "mouth-calm", "mouth-talk"]


def little_wizard() -> str:
    o = INK
    s = _stroke(o)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 260" width="240" height="260">
<defs>
  <radialGradient id="wface" cx="0.4" cy="0.35" r="0.75">
    <stop offset="0" stop-color="#e8b88c"/><stop offset="1" stop-color="#b9805a"/>
  </radialGradient>
  <linearGradient id="robe" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ff9ad2"/><stop offset="1" stop-color="#c2549a"/></linearGradient>
</defs>
<g id="shadow"><ellipse cx="120" cy="248" rx="58" ry="8" fill="#000" opacity="0.16"/></g>
<g id="body">
  <path d="M84 150 C96 140 144 140 156 150 C168 190 174 222 176 244 L64 244 C66 222 72 190 84 150 Z" fill="url(#robe)" {s}/>
  <path d="M70 230 L170 230" stroke="#ffd34d" stroke-width="5" opacity="0.8"/>
  <path d="M74 96 C74 66 96 52 120 52 C146 52 166 66 166 96 C166 128 146 148 120 148 C94 148 74 128 74 96 Z" fill="url(#wface)" {s}/>
  <path d="M72 104 C64 130 70 150 82 160 C86 140 84 120 88 108 Z M168 104 C176 130 170 150 158 160 C154 140 156 120 152 108 Z" fill="#f2f2f2" {s}/>
  <path d="M60 74 C90 62 150 62 180 74 C170 82 150 86 120 86 C90 86 70 82 60 74 Z" fill="#7a3fc4" {s}/>
  <path d="M84 72 C96 40 110 16 150 0 C140 24 146 48 160 70 C134 78 108 78 84 72 Z" fill="#9a5ae0" {s}/>
  <circle cx="118" cy="44" r="4" fill="#ffd34d"/><circle cx="134" cy="22" r="3" fill="#ffd34d"/>
  <ellipse cx="96" cy="116" rx="8" ry="5" fill="#ff9a9a" opacity="0.6"/><ellipse cx="144" cy="116" rx="8" ry="5" fill="#ff9a9a" opacity="0.6"/>
</g>
<g id="eye-open">
  <circle cx="104" cy="102" r="7" fill="#2a170b"/><circle cx="136" cy="102" r="7" fill="#2a170b"/>
  <circle cx="106" cy="99" r="2.5" fill="#fff"/><circle cx="138" cy="99" r="2.5" fill="#fff"/>
</g>
<g id="eye-closed">
  <path d="M96 104 C100 109 108 109 112 104 M128 104 C132 109 140 109 144 104" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-calm"><path d="M112 124 C116 130 124 130 128 124" stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/></g>
<g id="mouth-talk"><ellipse cx="120" cy="127" rx="6" ry="5" fill="#7a1f22" stroke="{o}" stroke-width="3"/></g>
</svg>'''


def ink_shadow() -> str:
    o = "#120a2a"
    s = _stroke(o)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 300" width="300" height="300">
<defs>
  <radialGradient id="ink" cx="0.4" cy="0.3" r="0.85">
    <stop offset="0" stop-color="#6a5a9a"/><stop offset="0.6" stop-color="#3a2d66"/><stop offset="1" stop-color="#1e1640"/>
  </radialGradient>
</defs>
<g id="shadow"><ellipse cx="150" cy="282" rx="100" ry="11" fill="#000" opacity="0.18"/></g>
<g id="body">
  <path d="M60 270 C40 220 50 120 100 76 C130 50 180 48 214 76 C262 118 268 220 244 270 C226 262 214 280 198 270 C184 282 168 266 152 278 C136 266 120 282 104 270 C88 282 76 264 60 270 Z" fill="url(#ink)" {s}/>
  <ellipse cx="120" cy="110" rx="18" ry="34" fill="#fff" opacity="0.12" transform="rotate(20 120 110)"/>
  <path d="M236 160 C258 140 270 112 262 90 L276 84 C282 112 270 148 244 172 Z" fill="#3a2d66" {s}/>
  <path d="M262 90 L292 40 L278 92 Z" fill="#fff4dc" {s}/>
  <circle cx="58" cy="200" r="6" fill="#3a2d66" {s}/><circle cx="40" cy="230" r="4" fill="#3a2d66"/>
</g>
<g id="eye-open">
  <ellipse cx="128" cy="140" rx="22" ry="26" fill="#fff8d0" stroke="{o}" stroke-width="3"/>
  <ellipse cx="188" cy="140" rx="22" ry="26" fill="#fff8d0" stroke="{o}" stroke-width="3"/>
  <circle cx="132" cy="146" r="10" fill="#1e1640"/><circle cx="192" cy="146" r="10" fill="#1e1640"/>
  <circle cx="136" cy="140" r="4" fill="#fff"/><circle cx="196" cy="140" r="4" fill="#fff"/>
</g>
<g id="eye-closed">
  <path d="M108 144 C118 154 138 154 148 144 M168 144 C178 154 198 154 208 144" stroke="#fff8d0" stroke-width="5" fill="none" stroke-linecap="round"/>
</g>
<g id="mouth-calm"><path d="M140 190 C152 196 166 196 178 190" stroke="#fff8d0" stroke-width="5" fill="none" stroke-linecap="round"/></g>
<g id="mouth-talk"><ellipse cx="159" cy="194" rx="12" ry="10" fill="#120a2a" stroke="#fff8d0" stroke-width="4"/></g>
</svg>'''


def sprites() -> dict:
    out = {
        "dragon_baby": (dragon(BABY), DRAGON_LAYERS),
        "dragon_big": (dragon(BIG), DRAGON_LAYERS),
        "goblin": (goblin(), GOBLIN_LAYERS),
        "wizard": (little_wizard(), SMALL_LAYERS),
        "shadow": (ink_shadow(), SMALL_LAYERS),
        "ruby": (_kid(_outfit("#ff7a6a", "#c8341f", pants="#7a3a5a"), RUBY_HAIR, RUBY_HAT, "", cape="#ffb020", extra_head=RUBY_EXTRA),
                 KID_LAYERS),
    }
    for name, (outfit, hair, hat, gear) in CLASSES.items():
        out[f"hero_{name}"] = (_kid(outfit, hair, hat, gear), KID_LAYERS)
    return out
