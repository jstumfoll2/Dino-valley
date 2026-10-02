"""Characters for The Little Dungeon: the hero (one look per class), Princess Ruby, the baby
dragon companion, the big dragon, the goblin, the little wizard and the Ink Shadow.

Each character is one SVG in a fixed box, split into named layers (<g id="...">). The build
renders every layer on its own at the same size, so the app can stack them and animate parts
(blink, talk, bob, flap). Characters face right; the app mirrors them when needed.
"""
import math

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


def _kid(outfit: dict, hair: str, hat: str, gear: str, feather: tuple, cape: str = "#c7354a", extra_head: str = "") -> str:
    """A big-headed young adventurer in a 300×300 box, three-quarter view facing right, feet on y≈280.
    feather = (x, y, lean, pin colour): where the unlockable feather is tucked into this hat."""
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
<g id="feather">{_feather(*feather)}</g>
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


# ------------------------------------------------------------------ hats that sit on the head

# The kids' head as an ellipsoid seen in three-quarter view (turned right, seen a little from above).
_HX, _HY, _HRX, _HRY = 164.0, 97.0, 67.0, 70.0
_YAW, _PITCH = math.radians(25), math.radians(20)


def _hp(lat, t, tilt=0.0, k=1.0, r=1.0, lift=0.0, back=0.0):
    """Screen point (x, y, facing) of a point on/around the head.

    lat: degrees above the head's 'equator' of a ring tilted back by `tilt` degrees; t: degrees around
    that ring (0 = straight out of the face, -90 = the back-left side we see, ±180 = the back);
    k scales the whole head (k > 1 sits just outside the hair); r scales the ring's radius (a brim);
    lift raises the point along the ring's axis, back pushes it toward the back of the head.
    facing > 0 means the point is on the side of the head turned toward us."""
    la, tl, tt = math.radians(lat), math.radians(tilt), math.radians(t)
    F = (math.sin(_YAW), 0.0, math.cos(_YAW))
    S = (math.cos(_YAW), 0.0, -math.sin(_YAW))
    N = tuple(math.cos(tl) * (0, 1, 0)[i] - math.sin(tl) * F[i] for i in range(3))
    Fp = tuple(math.cos(tl) * F[i] + math.sin(tl) * (0, 1, 0)[i] for i in range(3))
    h, rad = math.sin(la) + lift, math.cos(la) * r
    p = [h * N[i] + rad * (math.cos(tt) * Fp[i] + math.sin(tt) * S[i]) - back * Fp[i] for i in range(3)]
    x, y, z = p
    y2 = y * math.cos(_PITCH) - z * math.sin(_PITCH)
    z2 = y * math.sin(_PITCH) + z * math.cos(_PITCH)
    return _HX + x * _HRX * k, _HY - y2 * _HRY * k, z2


def _seen(lat, tilt, k=1.0):
    """The part of a ring that faces us, from its back-left end to its right end, ends on the silhouette."""
    f = lambda t: _hp(lat, t, tilt, k)[2]
    ends = []
    for sgn in (-1, 1):
        a, b = 0.0, 0.0
        while f(b) > 0 and abs(b) < 180:
            a, b = b, b + 5 * sgn
        for _ in range(30):
            m = (a + b) / 2
            a, b = (m, b) if f(m) > 0 else (a, m)
        ends.append(a)
    n = 36
    return [_hp(lat, ends[0] + (ends[1] - ends[0]) * i / n, tilt, k)[:2] for i in range(n + 1)]


def _rim(p0, p1, k=1.0, top=True):
    """Points along the head's outline (scaled by k) from p0 to p1, over the top (or the short way)."""
    rx, ry = _HRX * k, _HRY * k
    ang = lambda p: math.atan2((p[1] - _HY) / ry, (p[0] - _HX) / rx)
    a0, a1 = ang(p0), ang(p1)
    if top and a1 > a0:
        a1 -= 2 * math.pi
    if not top:
        while a1 - a0 > math.pi: a1 -= 2 * math.pi
        while a0 - a1 > math.pi: a1 += 2 * math.pi
    n = max(2, int(abs(a1 - a0) / 0.08))
    return [(_HX + rx * math.cos(a0 + (a1 - a0) * i / n), _HY + ry * math.sin(a0 + (a1 - a0) * i / n)) for i in range(1, n)]


def _d(pts, close=True):
    s = "M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in pts)
    return s + (" Z" if close else "")


def _band(lat0, lat1, tilt, k=1.0):
    """A strip around the head between two rings (a headband, a hat's rim)."""
    lo, hi = _seen(lat0, tilt, k), _seen(lat1, tilt, k)
    return _d(lo + _rim(lo[-1], hi[-1], k, top=False) + hi[::-1] + _rim(hi[0], lo[0], k, top=False))


def _cap(lat, tilt, k=1.0):
    """Everything of the head above a ring: a helmet or cap that covers the hair."""
    lo = _seen(lat, tilt, k)
    return _d(lo + _rim(lo[-1], lo[0], k))


def _pt(*a, **kw):
    x, y, _ = _hp(*a, **kw)
    return f"{x:.1f} {y:.1f}"


def _xy(*a, **kw):
    return _hp(*a, **kw)[:2]


def _meridian(t, tilt, k, lat0=24, lat1=170):
    """The seen part of a line over the crown, from the brow to the back of the head."""
    pts = [_hp(lat0 + (lat1 - lat0) * i / 60, t, tilt, k) for i in range(61)]
    return [(x, y) for x, y, z in pts if z > 0.02]


def _feather(x, y, ang, pin):
    """The unlockable feather: its quill sits at (x, y), leaning `ang` degrees (negative leans back),
    pinned into the hat with a little button in the hat's colour."""
    tf = f'transform="translate({x} {y}) rotate({ang})"'
    return (f'<path d="M-3 2 C-17 -22 -15 -44 1 -48 C6 -32 7 -14 4 2 Z" fill="#ffd34d" {T} {tf}/>'
            f'<path d="M-6 -12 L-12 -14 M-8 -24 L-14 -27 M2 -18 L6 -22 M1 -32 L4 -36" stroke="#e0a020" stroke-width="1.6" stroke-linecap="round" {tf}/>'
            f'<path d="M1 0 C-3 -16 -4 -30 -1 -42" stroke="#e0a020" stroke-width="2" fill="none" stroke-linecap="round" {tf}/>'
            f'<circle cx="{x}" cy="{y}" r="4.5" fill="{pin}" {_stroke(INK, 2.5)}/>')


# Knight: a round steel helmet with a rim, a centre ridge and a red plume at the back.
_KT, _KK = 12, 1.07
_k_ridge = _d(_meridian(-6, _KT, _KK + 0.02) + _meridian(6, _KT, _KK + 0.02)[::-1])
_k_plume_at = _xy(118, 0, _KT, _KK)
KNIGHT_HAT = f'''
  <defs><linearGradient id="steel" x1="0.1" y1="0" x2="0.8" y2="1">
    <stop offset="0" stop-color="#f4f8fb"/><stop offset="0.45" stop-color="#c3ced8"/><stop offset="1" stop-color="#8794a2"/>
  </linearGradient></defs>
  <path d="M{_k_plume_at[0]:.1f} {_k_plume_at[1] + 4:.1f} C{_k_plume_at[0] - 14:.1f} {_k_plume_at[1] - 14:.1f} {_k_plume_at[0] - 40:.1f} {_k_plume_at[1] - 18:.1f} {_k_plume_at[0] - 62:.1f} {_k_plume_at[1] - 4:.1f}
           C{_k_plume_at[0] - 52:.1f} {_k_plume_at[1] - 2:.1f} {_k_plume_at[0] - 48:.1f} {_k_plume_at[1] + 6:.1f} {_k_plume_at[0] - 54:.1f} {_k_plume_at[1] + 16:.1f}
           C{_k_plume_at[0] - 36:.1f} {_k_plume_at[1] + 6:.1f} {_k_plume_at[0] - 18:.1f} {_k_plume_at[1] + 12:.1f} {_k_plume_at[0]:.1f} {_k_plume_at[1] + 4:.1f} Z" fill="#e23b3b" {S}/>
  <path d="M{_k_plume_at[0] - 8:.1f} {_k_plume_at[1] + 2:.1f} C{_k_plume_at[0] - 24:.1f} {_k_plume_at[1] - 8:.1f} {_k_plume_at[0] - 40:.1f} {_k_plume_at[1] - 8:.1f} {_k_plume_at[0] - 54:.1f} {_k_plume_at[1] - 2:.1f}" stroke="#ff8a7a" stroke-width="3" fill="none" stroke-linecap="round"/>
  <path d="{_cap(23, _KT, _KK)}" fill="url(#steel)" {S}/>
  <path d="{_k_ridge}" fill="#dfe6ec" {T}/>
  <path d="{_band(23, 34, _KT, _KK + 0.03)}" fill="#a3afbb" {S}/>
  {"".join(f'<circle cx="{_xy(28.5, t, _KT, _KK + 0.03)[0]:.1f}" cy="{_xy(28.5, t, _KT, _KK + 0.03)[1]:.1f}" r="2.6" fill="#eef3f7" stroke="{INK}" stroke-width="1.5"/>' for t in (-80, -52, -24, 4, 32))}
  <ellipse cx="136" cy="46" rx="9" ry="16" fill="#fff" opacity="0.45" transform="rotate(38 136 46)"/>'''
KNIGHT_FEATHER = (*_xy(30, -84, _KT, _KK + 0.03), -38, "#a3afbb")

# Wizard: a wide brim tipped back on the head and a soft pointed hat whose tip flops backwards.
_WT, _WL = 16, 24
_w_brim = _d([_xy(_WL, t, _WT, 1.06, r=1.42) for t in range(-180, 180, 6)])
_w_base = _seen(_WL, _WT, 1.06)
_w_l, _w_r = _w_base[0], _w_base[-1]
WIZARD_HAT = f'''
  <defs><linearGradient id="wizCone" x1="0" y1="0" x2="1" y2="0.6">
    <stop offset="0" stop-color="#7d9bff"/><stop offset="1" stop-color="#3550c4"/>
  </linearGradient></defs>
  <path d="{_w_brim}" fill="#3a5bd0" {S}/>
  <path d="M{_w_l[0]:.1f} {_w_l[1]:.1f} {_d(_w_base[1:], False).replace("M", "L", 1)}
           C{_w_r[0] + 4:.1f} {_w_r[1] - 30:.1f} 214 30 190 16 C172 6 150 10 132 14 C116 8 98 10 84 22
           C100 22 112 26 120 34 C108 48 102 62 {_w_l[0]:.1f} {_w_l[1]:.1f} Z" fill="url(#wizCone)" {S}/>
  <path d="{_band(_WL + 1, _WL + 10, _WT, 1.06)}" fill="#ffd34d" {T}/>
  <path d="M150 38 L154 46 L162 46 L156 52 L158 60 L150 55 L143 60 L145 52 L139 46 L147 46 Z" fill="#ffd34d"/>
  <circle cx="186" cy="34" r="4" fill="#ffd34d"/><circle cx="198" cy="56" r="3" fill="#fff6c0"/><circle cx="118" cy="56" r="2.5" fill="#fff6c0"/>
  <circle cx="86" cy="22" r="5" fill="#ffd34d" {T}/>'''
WIZARD_FEATHER = (*_xy(_WL + 5, -78, _WT, 1.06), -50, "#ffd34d")

# Ranger: a soft green cap pulled over the hair with a turned-up rim; its long tip hangs off the back.
_RT, _RK = 12, 1.07
_r_back = _xy(112, 0, _RT, _RK)
RANGER_HAT = f'''
  <defs><linearGradient id="cap" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#6cc06a"/><stop offset="1" stop-color="#2f7a3e"/>
  </linearGradient></defs>
  <path d="M{_r_back[0] + 6:.1f} {_r_back[1] + 2:.1f} C{_r_back[0] - 24:.1f} {_r_back[1] - 6:.1f} 80 32 62 58 C82 50 96 52 108 60 Z" fill="#3f8f4e" {S}/>
  <path d="{_cap(23, _RT, _RK)}" fill="url(#cap)" {S}/>
  <path d="{_band(22, 35, _RT, _RK + 0.04)}" fill="#2f7a3e" {S}/>
  <path d="{_d(_seen(31, _RT, _RK + 0.04), False)}" stroke="#5aa85e" stroke-width="2.5" fill="none" stroke-linecap="round"/>
  <ellipse cx="140" cy="42" rx="8" ry="14" fill="#fff" opacity="0.25" transform="rotate(48 140 42)"/>'''
RANGER_FEATHER = (*_xy(29, -76, _RT, _RK + 0.04), -44, "#2f7a3e")

# Guardian: a headband that wraps round the head, a gold medallion over the brow and a knot at the back.
_GT, _GK = 10, 1.04
_g_front = _xy(37, 0, _GT, _GK)
_g_knot = _xy(33, -112, _GT, _GK)
GUARDIAN_HAT = f'''
  <path d="M{_g_knot[0]:.1f} {_g_knot[1]:.1f} C{_g_knot[0] - 14:.1f} {_g_knot[1] + 10:.1f} {_g_knot[0] - 18:.1f} {_g_knot[1] + 22:.1f} {_g_knot[0] - 14:.1f} {_g_knot[1] + 36:.1f}
           L{_g_knot[0] - 4:.1f} {_g_knot[1] + 30:.1f} C{_g_knot[0] - 6:.1f} {_g_knot[1] + 20:.1f} {_g_knot[0] - 2:.1f} {_g_knot[1] + 10:.1f} {_g_knot[0] + 4:.1f} {_g_knot[1] + 4:.1f} Z" fill="#2a8a8a" {T}/>
  <path d="M{_g_knot[0]:.1f} {_g_knot[1]:.1f} C{_g_knot[0] - 4:.1f} {_g_knot[1] + 14:.1f} {_g_knot[0] + 2:.1f} {_g_knot[1] + 28:.1f} {_g_knot[0] + 6:.1f} {_g_knot[1] + 40:.1f}
           L{_g_knot[0] + 14:.1f} {_g_knot[1] + 34:.1f} C{_g_knot[0] + 10:.1f} {_g_knot[1] + 22:.1f} {_g_knot[0] + 8:.1f} {_g_knot[1] + 12:.1f} {_g_knot[0] + 8:.1f} {_g_knot[1] + 2:.1f} Z" fill="#2fa3a3" {T}/>
  <path d="{_band(29, 44, _GT, _GK)}" fill="#2fa3a3" {S}/>
  <path d="{_d(_seen(40, _GT, _GK), False)}" stroke="#7fdada" stroke-width="2.5" fill="none" stroke-linecap="round" opacity="0.8"/>
  <ellipse cx="{_g_knot[0] + 3:.1f}" cy="{_g_knot[1] + 2:.1f}" rx="7" ry="8" fill="#2fa3a3" {T}/>
  <ellipse cx="{_g_front[0]:.1f}" cy="{_g_front[1]:.1f}" rx="7.5" ry="8.5" fill="#ffd34d" {T}/>
  <circle cx="{_g_front[0] - 2:.1f}" cy="{_g_front[1] - 2.5:.1f}" r="2.2" fill="#fff" opacity="0.85"/>'''
GUARDIAN_FEATHER = (*_xy(39, -92, _GT, _GK), -30, "#2fa3a3")

# Spellkeeper: a gold circlet round the head with a purple band and a pointed gem setting over the brow.
_ST, _SK = 10, 1.04
_s_front = _xy(36, 0, _ST, _SK)
_s_tip = _xy(36, 0, _ST, _SK, lift=0.38)
SPELLKEEPER_HAT = f'''
  <path d="{_band(30, 42, _ST, _SK)}" fill="#8e4ad8" {S}/>
  <path d="{_band(30, 33, _ST, _SK)}" fill="#ffd34d" stroke="none"/>
  <path d="{_band(39, 42, _ST, _SK)}" fill="#ffd34d" stroke="none"/>
  <path d="{_band(30, 42, _ST, _SK)}" fill="none" {S}/>
  {"".join(f'<circle cx="{_xy(36, t, _ST, _SK)[0]:.1f}" cy="{_xy(36, t, _ST, _SK)[1]:.1f}" r="2.4" fill="#ffd34d"/>' for t in (-75, -50, -25, 25, 50))}
  <path d="M{_s_front[0] - 12:.1f} {_s_front[1] + 5:.1f} L{_s_tip[0]:.1f} {_s_tip[1]:.1f} L{_s_front[0] + 11:.1f} {_s_front[1] + 3:.1f} Z" fill="#ffd34d" {T}/>
  <circle cx="{_s_front[0]:.1f}" cy="{_s_front[1] - 4:.1f}" r="5" fill="#ff6b8a" {_stroke(INK, 2)}/>
  <circle cx="{_s_front[0] - 1.5:.1f}" cy="{_s_front[1] - 5.5:.1f}" r="1.5" fill="#fff"/>'''
SPELLKEEPER_FEATHER = (*_xy(36, -86, _ST, _SK), -32, "#8e4ad8")

# Ruby: a little gold crown sitting on top of her head; the far side of the crown shows behind.
_CT, _CL, _CH = 10, 46, 0.34
_c_ts = [-180 + i * 22.5 for i in range(17)]
_c_ring = [(t, _hp(_CL, t, _CT, 1.0)) for t in _c_ts]
def _crown_wall(ts, shade):
    lo = [_xy(_CL, t, _CT, 1.0) for t in ts]
    hi = [_xy(_CL, t, _CT, 1.0, lift=_CH * (1.0 if i % 2 else 0.45)) for i, t in enumerate(ts)]
    return f'<path d="{_d(lo + hi[::-1])}" fill="{shade}" {T}/>'
RUBY_HAT = f'''
  {_crown_wall([90 + i * 18 for i in range(11)], "#e0a92a")}
  {_crown_wall([-90 + i * 18 for i in range(11)], "#ffd34d")}
  {"".join(f'<circle cx="{_xy(_CL, t, _CT, 1.0, lift=_CH)[0]:.1f}" cy="{_xy(_CL, t, _CT, 1.0, lift=_CH)[1]:.1f}" r="3" fill="#fff4c0" {_stroke(INK, 2)}/>' for t in (-72, -36, 0, 36, 72))}
  <circle cx="{_xy(_CL + 9, 0, _CT, 1.0)[0]:.1f}" cy="{_xy(_CL + 9, 0, _CT, 1.0)[1]:.1f}" r="4" fill="#e23b3b" {_stroke(INK, 2)}/>'''
RUBY_FEATHER = (*_xy(_CL + 4, -70, _CT, 1.0), -34, "#ffd34d")


KNIGHT_GEAR = f'''
  <path d="M218 146 L226 146 L226 198 L218 198 Z" fill="#dfe6ec" {T}/>
  <path d="M218 146 L222 132 L226 146 Z" fill="#dfe6ec" {T}/>
  <rect x="206" y="196" width="32" height="8" rx="3" fill="#ffd34d" {T}/>
  <rect x="217" y="204" width="10" height="18" rx="3" fill="#8a5a32" {T}/>'''
KNIGHT_CHEST = f'<path d="M136 176 C150 170 162 170 176 176 L174 210 C162 216 150 216 138 210 Z" fill="#dfe6ec" {T}/><path d="M156 182 L156 206 M146 194 L166 194" stroke="#e23b3b" stroke-width="5" stroke-linecap="round"/>'

WIZARD_GEAR = f'''
  <path d="M232 66 L240 66 L238 272 L230 272 Z" fill="#9a6a3a" {T}/>
  <circle cx="236" cy="56" r="26" fill="#8fe8ff" opacity="0.25"/>
  <circle cx="236" cy="56" r="16" fill="#8fe8ff" {T}/>
  <circle cx="231" cy="51" r="5" fill="#fff" opacity="0.9"/>'''

RANGER_GEAR = f'''
  <path d="M206 120 C244 150 244 230 206 260" fill="none" stroke="#8a5a32" stroke-width="7" stroke-linecap="round"/>
  <path d="M206 120 C244 150 244 230 206 260" fill="none" stroke="{INK}" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
  <path d="M208 122 L208 258" stroke="#f2e6c8" stroke-width="2"/>'''

GUARDIAN_GEAR = f'''
  <ellipse cx="222" cy="214" rx="34" ry="40" fill="#2fa3a3" {S}/>
  <ellipse cx="222" cy="214" rx="24" ry="30" fill="#5cc7c7" {T}/>
  <path d="M222 236 C204 222 204 206 214 204 C220 203 222 208 222 210 C222 208 224 203 230 204 C240 206 240 222 222 236 Z" fill="#ff6b8a" {T}/>'''

SPELLKEEPER_GEAR = f'''
  <path d="M196 200 L222 190 L250 200 L250 236 L222 228 L196 236 Z" fill="#fff4dc" {S}/>
  <path d="M222 190 L222 228" stroke="{INK}" stroke-width="2.5"/>
  <path d="M192 236 L222 228 L254 236 L254 242 L222 234 L192 242 Z" fill="#8e4ad8" {T}/>
  <circle cx="222" cy="180" r="18" fill="#fff6a0" opacity="0.45"/>'''

RUBY_HAIR = "#c8341f"
RUBY_EXTRA = f'''<path d="M100 92 C88 118 90 150 104 170 C112 150 110 122 112 100 Z" fill="{RUBY_HAIR}" {S}/>'''

CLASSES = {
    "knight": (_outfit("#e65a5a", "#a8323a", chest=KNIGHT_CHEST), "#6b3f1f", KNIGHT_HAT, KNIGHT_GEAR, KNIGHT_FEATHER),
    "wizard": (_outfit("#6f8cf2", "#3a4fb8"), "#4a2f1a", WIZARD_HAT, WIZARD_GEAR, WIZARD_FEATHER),
    "ranger": (_outfit("#7cc46a", "#3f8f4e", pants="#6b4a2a"), "#8a5a2a", RANGER_HAT, RANGER_GEAR, RANGER_FEATHER),
    "guardian": (_outfit("#5cc7c7", "#2a8a8a", pants="#4a5a7a"), "#2a1a10", GUARDIAN_HAT, GUARDIAN_GEAR, GUARDIAN_FEATHER),
    "spellkeeper": (_outfit("#b98af0", "#7a3fc4", pants="#4a3a6a"), "#e0b050", SPELLKEEPER_HAT, SPELLKEEPER_GEAR,
                    SPELLKEEPER_FEATHER),
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
        "ruby": (_kid(_outfit("#ff7a6a", "#c8341f", pants="#7a3a5a"), RUBY_HAIR, RUBY_HAT, "", RUBY_FEATHER, cape="#ffb020", extra_head=RUBY_EXTRA),
                 KID_LAYERS),
    }
    for name, (outfit, hair, hat, gear, feather) in CLASSES.items():
        out[f"hero_{name}"] = (_kid(outfit, hair, hat, gear, feather), KID_LAYERS)
    return out
