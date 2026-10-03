"""The people and creatures of Whisperwood: townspeople, shopkeepers, animals, monsters, the Baron.

Same contract and house style as heroes.py: sprites() -> {name: (svg, layers)}. Every character
is one 240x260 SVG split into the small-character layers (shadow, body, eye-open, eye-closed,
mouth-calm, mouth-talk) so the app can blink them and move their mouths. The `body` layer holds
everything that isn't eyes or mouth. Characters face the viewer, slightly right; the app mirrors them.
Chunky shapes, a thick warm outline, gradient shading, white highlights, a soft shadow.
"""
import math

from props import dark, ink, light, mix

INK = "#3d2a1c"
LAYERS = ["shadow", "body", "eye-open", "eye-closed", "mouth-calm", "mouth-talk"]
W, H = 240, 260


def S(color=INK, w=4):
    return f'stroke="{color}" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round"'


def grad(gid, c, kind="radial"):
    """A soft gradient from a light spot to the shaded edge of colour c."""
    if kind == "radial":
        return (f'<radialGradient id="{gid}" cx="0.38" cy="0.3" r="0.85"><stop offset="0" stop-color="{light(c, 0.45)}"/>'
                f'<stop offset="0.55" stop-color="{c}"/><stop offset="1" stop-color="{dark(c, 0.22)}"/></radialGradient>')
    return (f'<linearGradient id="{gid}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{light(c, 0.3)}"/>'
            f'<stop offset="1" stop-color="{dark(c, 0.25)}"/></linearGradient>')


def eyes(lx, rx, y, r=7.5, pupil="#2a170b", white=False, spread=0.0, dy=0):
    """(open, closed) eye groups: round dark eyes with a highlight, or white eyes with pupils."""
    o = INK
    if white:
        op = (f'<ellipse cx="{lx}" cy="{y}" rx="{r + 3}" ry="{r + 5}" fill="#fffbe6" stroke="{o}" stroke-width="3"/>'
              f'<ellipse cx="{rx}" cy="{y}" rx="{r + 3}" ry="{r + 5}" fill="#fffbe6" stroke="{o}" stroke-width="3"/>'
              f'<circle cx="{lx + 2}" cy="{y + 2 + dy}" r="{r * 0.62}" fill="{pupil}"/><circle cx="{rx + 2}" cy="{y + 2 + dy}" r="{r * 0.62}" fill="{pupil}"/>'
              f'<circle cx="{lx + 4}" cy="{y - 1 + dy}" r="2.4" fill="#fff"/><circle cx="{rx + 4}" cy="{y - 1 + dy}" r="2.4" fill="#fff"/>')
    else:
        op = (f'<circle cx="{lx}" cy="{y}" r="{r}" fill="{pupil}"/><circle cx="{rx}" cy="{y}" r="{r}" fill="{pupil}"/>'
              f'<circle cx="{lx + r * 0.4:.1f}" cy="{y - r * 0.4:.1f}" r="{r * 0.36:.1f}" fill="#fff"/>'
              f'<circle cx="{rx + r * 0.4:.1f}" cy="{y - r * 0.4:.1f}" r="{r * 0.36:.1f}" fill="#fff"/>')
    cl = (f'<path d="M{lx - r - 2} {y + 2} C{lx - r + 2} {y + r + 3} {lx + r - 2} {y + r + 3} {lx + r + 2} {y + 2} '
          f'M{rx - r - 2} {y + 2} C{rx - r + 2} {y + r + 3} {rx + r - 2} {y + r + 3} {rx + r + 2} {y + 2}" '
          f'stroke="{o}" stroke-width="3.5" fill="none" stroke-linecap="round"/>')
    return op, cl


def mouth(x, y, w=16, tint="#7a1f22", tongue="#ff7b8a", color=INK):
    """(calm, talk) mouth groups centred at (x, y)."""
    calm = f'<path d="M{x - w / 2} {y} C{x - w / 4} {y + w / 3} {x + w / 4} {y + w / 3} {x + w / 2} {y}" stroke="{color}" stroke-width="3.5" fill="none" stroke-linecap="round"/>'
    talk = (f'<ellipse cx="{x}" cy="{y + 3}" rx="{w / 2.6:.1f}" ry="{w / 3:.1f}" fill="{tint}" stroke="{color}" stroke-width="3"/>'
            f'<ellipse cx="{x}" cy="{y + 6}" rx="{w / 4.5:.1f}" ry="{w / 9:.1f}" fill="{tongue}"/>')
    return calm, talk


def beak_mouth(x, y, w=14, color="#e8a020", edge="#7a4a0a"):
    """A beak that closes (calm) or opens (talk)."""
    calm = (f'<path d="M{x - w} {y - 6} C{x - w / 2} {y - 12} {x + w / 2} {y - 12} {x + w} {y - 6} C{x + w / 2} {y + 8} {x - w / 2} {y + 8} {x - w} {y - 6} Z" '
            f'fill="{color}" stroke="{edge}" stroke-width="3.5" stroke-linejoin="round"/>')
    talk = (f'<path d="M{x - w} {y - 8} C{x - w / 2} {y - 14} {x + w / 2} {y - 14} {x + w} {y - 8} C{x + w / 2} {y - 1} {x - w / 2} {y - 1} {x - w} {y - 8} Z" '
            f'fill="{color}" stroke="{edge}" stroke-width="3.5" stroke-linejoin="round"/>'
            f'<path d="M{x - w * 0.8} {y - 2} C{x - w / 3} {y + 14} {x + w / 3} {y + 14} {x + w * 0.8} {y - 2} Z" fill="#7a1f22" stroke="{edge}" stroke-width="3.5" stroke-linejoin="round"/>'
            f'<ellipse cx="{x}" cy="{y + 6}" rx="{w / 3:.1f}" ry="3" fill="#ff7b8a"/>')
    return calm, talk


def cheeks(lx, rx, y, c="#ff9a9a", rx_=8, ry=5):
    return f'<ellipse cx="{lx}" cy="{y}" rx="{rx_}" ry="{ry}" fill="{c}" opacity="0.6"/><ellipse cx="{rx}" cy="{y}" rx="{rx_}" ry="{ry}" fill="{c}" opacity="0.6"/>'


def ground_shadow(cx=120, rx=62, ry=9, y=248, op=0.16):
    return f'<ellipse cx="{cx}" cy="{y}" rx="{rx}" ry="{ry}" fill="#000" opacity="{op}"/>'


def fit(scale, cx=120, base=248):
    """A transform that shrinks a figure toward its feet, for tall hats and big creatures."""
    return f'translate({cx} {base}) scale({scale}) translate({-cx} {-base})'


def rig(defs, body, eye, mth, shadow=None, tf=None):
    """Assemble the layered SVG. [tf] is applied to body, eyes and mouths alike so they stay lined up."""
    eo, ec = eye
    mc, mt = mth
    shadow = shadow or ground_shadow()
    t = f' transform="{tf}"' if tf else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">\n<defs>{defs}</defs>\n'
            f'<g id="shadow">{shadow}</g>\n<g id="body"{t}>{body}</g>\n<g id="eye-open"{t}>{eo}</g>\n<g id="eye-closed"{t}>{ec}</g>\n'
            f'<g id="mouth-calm"{t}>{mc}</g>\n<g id="mouth-talk"{t}>{mt}</g>\n</svg>')


# ---------------------------------------------------------------- the people

SKINS = {"fair": "#f1c7a0", "tan": "#d9a273", "brown": "#a8714a", "rosy": "#f4b9a0", "green": "#8fcf5a"}


def person(spec):
    """A chibi townsperson. spec keys: skin, robe, trim, hair (colour or None), hairstyle, hat, beard, apron,
    glasses, prop, build (slim/round/tall), cheeks."""
    s = spec
    skin = SKINS[s.get("skin", "fair")]
    robe = s["robe"]
    trim = s.get("trim", "#ffd34d")
    build = s.get("build", "round")
    o = INK
    st = S(o)
    w_bot = {"slim": 52, "round": 66, "tall": 56}[build]
    top = {"slim": 138, "round": 146, "tall": 128}[build]
    head_y = {"slim": 92, "round": 100, "tall": 84}[build]
    defs = grad("skin", skin) + grad("robe", robe, "lin") + grad("hairg", s.get("hair") or "#6b4a32", "lin") + grad("trimg", trim, "lin")
    parts = []
    # held prop behind the body
    prop = s.get("prop")
    if prop == "ladle":
        parts.append(f'<path d="M186 150 L204 236" {S("#7a4a26", 7)}/><ellipse cx="184" cy="146" rx="15" ry="11" fill="#c9ced6" {S(o, 4)} transform="rotate(-20 184 146)"/>')
    if prop == "hammer":
        parts.append(f'<path d="M180 160 L204 238" {S("#7a4a26", 8)}/><rect x="162" y="140" width="46" height="28" rx="6" fill="#9aa4b3" {S(o, 4)} transform="rotate(-18 185 154)"/>')
    if prop == "rod":
        parts.append(f'<path d="M190 236 L222 70" {S("#7a4a26", 5)}/><path d="M222 70 C226 110 214 130 208 140" stroke="#fff" stroke-width="2" fill="none"/><circle cx="208" cy="142" r="5" fill="#e23b3b" {S(o, 2.5)}/>')
    if prop == "spear":
        parts.append(f'<path d="M192 240 L196 40" {S("#7a4a26", 6)}/><path d="M196 24 L206 52 L186 52 Z" fill="#c9ced6" {S(o, 4)}/>')
    if prop == "staff":
        parts.append(f'<path d="M190 242 L192 60" {S("#7a4a26", 7)}/><circle cx="192" cy="54" r="15" fill="#7fd4ff" {S(o, 4)}/><circle cx="187" cy="49" r="5" fill="#fff" opacity="0.9"/>')
    if prop == "lantern":
        parts.append(f'<path d="M190 120 L190 150" {S("#4a3a30", 4)}/><rect x="176" y="148" width="28" height="38" rx="7" fill="#ffd77a" {S("#4a3a30", 4)}/><ellipse cx="190" cy="166" rx="9" ry="13" fill="#fff6c2" opacity="0.9"/><path d="M178 148 L190 134 L202 148" fill="none" {S("#4a3a30", 4)}/>')
    if prop == "basket":
        parts.append(f'<path d="M150 190 C150 176 206 176 206 190 L200 232 C196 240 160 240 156 232 Z" fill="#c99a5a" {S(o, 4)}/><path d="M160 190 C160 160 196 160 196 190" fill="none" {S("#8a5a2a", 5)}/><circle cx="168" cy="184" r="7" fill="#6fbf73" {S(o, 2.5)}/><circle cx="184" cy="180" r="7" fill="#ff8fb0" {S(o, 2.5)}/><circle cx="196" cy="186" r="6" fill="#fff3a0" {S(o, 2.5)}/>')
    if prop == "bag":
        parts.append(f'<path d="M150 150 C190 140 214 170 206 214 C196 232 160 232 150 214 Z" fill="#b5835a" {st}/><path d="M158 158 C176 150 196 156 200 172" stroke="#7a4a26" stroke-width="5" fill="none"/><circle cx="184" cy="176" r="7" fill="#ffd34d" {S(o, 3)}/>')
    # body
    parts.append(
        f'<path d="M{120 - w_bot} 244 C{120 - w_bot + 4} 206 {120 - 30} {top + 10} {120 - 26} {top} C{120 - 10} {top - 6} {120 + 10} {top - 6} {120 + 26} {top} '
        f'C{120 + 30} {top + 10} {120 + w_bot - 4} 206 {120 + w_bot} 244 Z" fill="url(#robe)" {st}/>')
    parts.append(f'<path d="M{120 - w_bot + 2} 232 L{120 + w_bot - 2} 232" stroke="url(#trimg)" stroke-width="7" stroke-linecap="round"/>')
    parts.append(f'<path d="M104 {top + 4} L120 {top + 22} L136 {top + 4}" fill="none" stroke="{dark(robe, 0.3)}" stroke-width="4" stroke-linecap="round"/>')
    if s.get("apron"):
        ap = s["apron"]
        parts.append(f'<path d="M92 {top + 14} C96 {top + 8} 144 {top + 8} 148 {top + 14} L156 240 L84 240 Z" fill="{ap}" {S(o, 3.5)}/><path d="M92 {top + 14} L104 {top - 6} M148 {top + 14} L136 {top - 6}" stroke="{ap}" stroke-width="5" {S(o, 2)}/>')
    # arms (little round hands at the sides)
    parts.append(f'<circle cx="{120 - w_bot + 8}" cy="{214}" r="11" fill="url(#skin)" {S(o, 3.5)}/><circle cx="{120 + w_bot - 8}" cy="214" r="11" fill="url(#skin)" {S(o, 3.5)}/>')
    # head
    parts.append(f'<ellipse cx="120" cy="{head_y}" rx="47" ry="49" fill="url(#skin)" {st}/>')
    hs = s.get("hairstyle")
    hair = s.get("hair")
    if hair and hs:
        if hs == "bun":
            parts.append(f'<circle cx="120" cy="{head_y - 52}" r="20" fill="url(#hairg)" {st}/>')
        if hs in ("bun", "short", "curly"):
            parts.append(f'<path d="M73 {head_y - 6} C70 {head_y - 52} 100 {head_y - 58} 120 {head_y - 58} C142 {head_y - 58} 170 {head_y - 50} 167 {head_y - 6} C156 {head_y - 28} 140 {head_y - 36} 120 {head_y - 36} C100 {head_y - 36} 84 {head_y - 28} 73 {head_y - 6} Z" fill="url(#hairg)" {st}/>')
        if hs == "curly":
            for cx_, cy_ in [(78, head_y - 28), (94, head_y - 50), (120, head_y - 56), (146, head_y - 50), (162, head_y - 28)]:
                parts.append(f'<circle cx="{cx_}" cy="{cy_}" r="14" fill="url(#hairg)" {S(o, 3.5)}/>')
        if hs == "long":
            parts.append(f'<path d="M72 {head_y} C66 {head_y + 40} 72 {head_y + 62} 84 {head_y + 70} C90 {head_y + 50} 88 {head_y + 20} 92 {head_y - 10} Z M168 {head_y} C174 {head_y + 40} 168 {head_y + 62} 156 {head_y + 70} C150 {head_y + 50} 152 {head_y + 20} 148 {head_y - 10} Z" fill="url(#hairg)" {st}/>')
            parts.append(f'<path d="M72 {head_y - 4} C70 {head_y - 54} 100 {head_y - 58} 120 {head_y - 58} C142 {head_y - 58} 170 {head_y - 52} 168 {head_y - 4} C156 {head_y - 30} 140 {head_y - 36} 120 {head_y - 36} C100 {head_y - 36} 84 {head_y - 30} 72 {head_y - 4} Z" fill="url(#hairg)" {st}/>')
        if hs == "wild":
            parts.append(f'<path d="M68 {head_y} C56 {head_y - 40} 80 {head_y - 70} 120 {head_y - 66} C160 {head_y - 70} 184 {head_y - 40} 172 {head_y} C166 {head_y - 22} 150 {head_y - 34} 120 {head_y - 34} C90 {head_y - 34} 74 {head_y - 22} 68 {head_y} Z" fill="url(#hairg)" {st}/>')
    if s.get("beard"):
        bc = s["beard"]
        parts.append(f'<path d="M76 {head_y + 12} C76 {head_y + 62} 104 {head_y + 78} 120 {head_y + 78} C136 {head_y + 78} 164 {head_y + 62} 164 {head_y + 12} C150 {head_y + 30} 136 {head_y + 24} 120 {head_y + 24} C104 {head_y + 24} 90 {head_y + 30} 76 {head_y + 12} Z" fill="{bc}" {st}/>')
    if s.get("moustache"):
        parts.append(f'<path d="M96 {head_y + 22} C104 {head_y + 12} 116 {head_y + 16} 120 {head_y + 22} C124 {head_y + 16} 136 {head_y + 12} 144 {head_y + 22} C136 {head_y + 30} 124 {head_y + 26} 120 {head_y + 24} C116 {head_y + 26} 104 {head_y + 30} 96 {head_y + 22} Z" fill="{s["moustache"]}" {S(o, 3.5)}/>')
    if s.get("beard") or s.get("moustache"):
        parts.append(f'<ellipse cx="120" cy="{head_y + 33}" rx="15" ry="9" fill="url(#skin)" {S(o, 2.5)}/>')
    parts.append(f'<path d="M118 {head_y + 8} C112 {head_y + 20} 116 {head_y + 26} 124 {head_y + 24}" stroke="{o}" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.7"/>')
    if s.get("cheeks", True):
        parts.append(cheeks(88, 152, head_y + 18))
    if s.get("glasses"):
        parts.append(f'<circle cx="102" cy="{head_y + 2}" r="15" fill="#cfeaff" fill-opacity="0.35" {S(s["glasses"], 4)}/><circle cx="138" cy="{head_y + 2}" r="15" fill="#cfeaff" fill-opacity="0.35" {S(s["glasses"], 4)}/><path d="M117 {head_y + 2} L123 {head_y + 2}" {S(s["glasses"], 4)}/>')
    hat = s.get("hat")
    hc = s.get("hatcolor", "#c2549a")
    if hat == "chef":
        parts.append(f'<path d="M84 {head_y - 36} C60 {head_y - 52} 72 {head_y - 90} 100 {head_y - 82} C104 {head_y - 104} 140 {head_y - 104} 142 {head_y - 82} C170 {head_y - 90} 180 {head_y - 52} 156 {head_y - 36} Z" fill="#fff" {st}/><rect x="84" y="{head_y - 40}" width="72" height="14" rx="5" fill="#f2f2f2" {st}/>')
    if hat == "cap":
        parts.append(f'<path d="M74 {head_y - 20} C74 {head_y - 58} 166 {head_y - 58} 166 {head_y - 20} Z" fill="{hc}" {st}/><path d="M150 {head_y - 22} C176 {head_y - 20} 190 {head_y - 14} 192 {head_y - 8} C176 {head_y - 8} 156 {head_y - 12} 146 {head_y - 18} Z" fill="{dark(hc, 0.15)}" {S(o, 3.5)}/>')
    if hat == "straw":
        parts.append(f'<ellipse cx="120" cy="{head_y - 24}" rx="76" ry="16" fill="#e8c46a" {st}/><path d="M80 {head_y - 26} C80 {head_y - 62} 160 {head_y - 62} 160 {head_y - 26} Z" fill="#f0d27f" {st}/><path d="M80 {head_y - 32} L160 {head_y - 32}" stroke="{hc}" stroke-width="7"/>')
    if hat == "helmet":
        parts.append(f'<path d="M72 {head_y - 8} C70 {head_y - 62} 170 {head_y - 62} 168 {head_y - 8} L152 {head_y - 14} L88 {head_y - 14} Z" fill="url(#steel)" {st}/><path d="M120 {head_y - 60} L120 {head_y - 16}" stroke="#7d8a98" stroke-width="5"/><circle cx="120" cy="{head_y - 64}" r="7" fill="#e23b3b" {S(o, 3)}/>')
        defs += '<linearGradient id="steel" x1="0.1" y1="0" x2="0.8" y2="1"><stop offset="0" stop-color="#f4f8fb"/><stop offset="0.45" stop-color="#c3ced8"/><stop offset="1" stop-color="#8794a2"/></linearGradient>'
    if hat == "turban":
        parts.append(f'<path d="M72 {head_y - 10} C66 {head_y - 62} 174 {head_y - 62} 168 {head_y - 10} C150 {head_y - 28} 90 {head_y - 28} 72 {head_y - 10} Z" fill="{hc}" {st}/><path d="M86 {head_y - 34} C110 {head_y - 20} 140 {head_y - 20} 156 {head_y - 36}" stroke="{light(hc, 0.4)}" stroke-width="5" fill="none"/><circle cx="120" cy="{head_y - 40}" r="8" fill="#ffd34d" {S(o, 3)}/>')
    if hat == "hood":
        parts.append(f'<path d="M66 {head_y + 20} C54 {head_y - 50} 90 {head_y - 70} 120 {head_y - 70} C150 {head_y - 70} 186 {head_y - 50} 174 {head_y + 20} C162 {head_y - 20} 146 {head_y - 36} 120 {head_y - 36} C94 {head_y - 36} 78 {head_y - 20} 66 {head_y + 20} Z" fill="{hc}" {st}/>')
    if hat == "wizard":
        parts.append(f'<path d="M56 {head_y - 20} C86 {head_y - 34} 154 {head_y - 34} 184 {head_y - 20} C170 {head_y - 10} 150 {head_y - 6} 120 {head_y - 6} C90 {head_y - 6} 70 {head_y - 10} 56 {head_y - 20} Z" fill="{dark(hc, 0.12)}" {st}/>'
                     f'<path d="M80 {head_y - 26} C94 {head_y - 62} 112 {head_y - 90} 156 {head_y - 108} C146 {head_y - 80} 150 {head_y - 52} 164 {head_y - 28} C136 {head_y - 20} 106 {head_y - 20} 80 {head_y - 26} Z" fill="{hc}" {st}/>'
                     f'<circle cx="112" cy="{head_y - 52}" r="4" fill="#ffd34d"/><circle cx="134" cy="{head_y - 76}" r="3" fill="#ffd34d"/>')
    if hat == "tophat":
        parts.append(f'<ellipse cx="120" cy="{head_y - 30}" rx="62" ry="12" fill="#2b2433" {st}/><path d="M86 {head_y - 32} L90 {head_y - 96} C110 {head_y - 104} 130 {head_y - 104} 150 {head_y - 96} L154 {head_y - 32} Z" fill="#3a3046" {st}/><path d="M88 {head_y - 46} L152 {head_y - 46}" stroke="#a8301f" stroke-width="9"/>')
    if hat == "bandana":
        parts.append(f'<path d="M72 {head_y - 8} C70 {head_y - 56} 170 {head_y - 56} 168 {head_y - 8} C150 {head_y - 24} 90 {head_y - 24} 72 {head_y - 8} Z" fill="{hc}" {st}/><circle cx="96" cy="{head_y - 30}" r="3.5" fill="#fff"/><circle cx="120" cy="{head_y - 38}" r="3.5" fill="#fff"/><circle cx="144" cy="{head_y - 30}" r="3.5" fill="#fff"/><path d="M166 {head_y - 20} C190 {head_y - 28} 198 {head_y - 8} 190 {head_y + 4} C182 {head_y - 4} 176 {head_y - 8} 166 {head_y - 10} Z" fill="{hc}" {S(o, 3.5)}/>')
    if hat == "crown":
        parts.append(f'<path d="M84 {head_y - 34} L88 {head_y - 66} L104 {head_y - 48} L120 {head_y - 72} L136 {head_y - 48} L152 {head_y - 66} L156 {head_y - 34} Z" fill="#ffd34d" {st}/><circle cx="120" cy="{head_y - 62}" r="5" fill="#e23b3b" {S(o, 2.5)}/>')
    if hat == "flowers":
        for k, (fx, fc) in enumerate([(84, "#ff8fb0"), (104, "#fff3a0"), (124, "#ff8fb0"), (144, "#b3e5ff"), (160, "#fff3a0")]):
            parts.append(f'<circle cx="{fx}" cy="{head_y - 44 + (k % 2) * 4}" r="9" fill="{fc}" {S(o, 3)}/><circle cx="{fx}" cy="{head_y - 44 + (k % 2) * 4}" r="3.5" fill="#ffb020"/>')
    return defs, "".join(parts), head_y


def build_person(spec):
    defs, body, hy = person(spec)
    pupil = spec.get("pupil", "#2a170b")
    eye = eyes(103, 137, hy + 2, pupil=pupil)
    mth = mouth(120, hy + (33 if (spec.get("beard") or spec.get("moustache")) else 30), 14 if spec.get("beard") else 16)
    tf = fit(0.84) if spec.get("hat") in ("wizard", "tophat") else (fit(0.92) if spec.get("hat") in ("helmet", "chef") or spec.get("prop") in ("spear", "staff") else None)
    return rig(defs, body, eye, mth, tf=tf)


# ---------------------------------------------------------------- the cast, one spec each

PEOPLE = {
    "healer_willow": dict(skin="brown", robe="#7ac07f", trim="#fff3a0", hair="#d8d8e0", hairstyle="long", hat="hood", hatcolor="#4f9d6a", prop="basket", build="round"),
    "hazel_hermit": dict(skin="fair", robe="#8a7a5a", trim="#c9b27a", hair="#cfcfd6", hairstyle="wild", beard="#cfcfd6", prop="staff", build="slim"),
    "old_merlo": dict(skin="fair", robe="#6a4fc2", trim="#ffd34d", hair="#f2f2f8", hairstyle="short", beard="#f2f2f8", hat="wizard", hatcolor="#7a3fc4", glasses="#c98a12", prop="staff", build="tall"),
    "bandit_bess": dict(skin="tan", robe="#5a3a4a", trim="#e23b3b", hair="#5a3320", hairstyle="long", hat="bandana", hatcolor="#e23b3b", build="slim"),
    "lumi_lamp": dict(skin="rosy", robe="#ffb347", trim="#fff3a0", hair="#f0c84a", hairstyle="curly", prop="lantern", build="slim"),

    "mayor_tilly": dict(skin="tan", robe="#4f9d6a", trim="#ffd34d", hair="#d8d8e0", hairstyle="bun", glasses="#7a4a26", build="round", hat="flowers", hatcolor="#ff8fb0"),
    "baker_bun": dict(skin="rosy", robe="#f3a6c0", trim="#ffffff", hair="#8a5a32", hairstyle="short", apron="#fffaf0", hat="chef", prop="ladle", build="round"),
    "smith_brogan": dict(skin="brown", robe="#8a6a4a", trim="#c9ced6", hair="#3a2a1c", hairstyle="short", beard="#3a2a1c", apron="#6b4a32", prop="hammer", build="tall"),
    "merchant_zig": dict(skin="tan", robe="#e2753a", trim="#ffd34d", hair="#2a1c10", hairstyle="curly", hat="turban", hatcolor="#3a9ad6", prop="bag", moustache="#2a1c10", build="round"),
    "captain_hob": dict(skin="fair", robe="#4a6fc2", trim="#e8c46a", hair="#8a5a32", hairstyle="short", moustache="#8a5a32", hat="helmet", prop="spear", build="tall"),
    "finn_fisher": dict(skin="tan", robe="#3a8aa8", trim="#ffffff", hair="#c9c2b0", hairstyle="short", beard="#c9c2b0", hat="straw", hatcolor="#e23b3b", prop="rod", build="round"),
}


def sprites() -> dict:
    out = {}
    for pid, spec in PEOPLE.items():
        out[f"npc_{pid}"] = (build_person(spec), LAYERS)
    return out
