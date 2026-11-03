"""Builds the Swatch Applicator item: a 32x32 texture in the AE2 machine palette and a cuboid model shaped like a
handheld spectrophotometer (upright grip, measuring aperture at the foot, wide head on top with the screen tilted back
toward the player). Usage: python3 tools/applicator_model.py"""
import colorsys
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/appliedswatches')

# Head tilt: the whole head rotates about z through HEAD_ORIGIN, front (+x) end up. ApplicatorRenderer mirrors these.
HEAD_ANGLE = 22.5
HEAD_ORIGIN = [8, 13.5, 8]


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# AE2 machine palette, same as tools/front_texture.py: white upper body, grey lower band, navy frames.
WHITE, WHITE2, LIGHT, PANEL, SEAM = hx('f2f2f2'), hx('e8e8ea'), hx('d5d6dc'), hx('dedfe3'), hx('cbccd4')
GREY, GREY_HI, GREY_EDGE, GREY_EDGE2 = hx('b3b6c6'), hx('bfc1cd'), hx('878fa5'), hx('787e97')
SHADE = GREY_HI
METAL, METAL_HI, METAL_D = GREY, hx('d4d6e0'), GREY_EDGE2
NAVY, NAVY_D, NAVY_DD = hx('262b54'), hx('1b1f3d'), hx('12152a')
GRILLE, GRILLE_D = GREY, GREY_EDGE
FLUIX, FLUIX_HI, FLUIX_D = hx('8a5ad8'), hx('c7a8ff'), hx('5a3a9a')
LED = hx('5be37a')


def rainbow(i, n, lit=True):
    """The terminals' rainbow strip: hue steps across, brighter top row."""
    r, g, b = colorsys.hsv_to_rgb(i / n, 0.75, 0.95 if lit else 0.8)
    return int(r * 255), int(g * 255), int(b * 255), 255

# Texture regions, in pixels: name -> (x0, y0, x1, y1).
R = {'side': (0, 0, 16, 6), 'top': (16, 0, 32, 6), 'under': (0, 6, 16, 12), 'cap': (16, 6, 22, 12),
     'bezel': (22, 6, 32, 12), 'grip': (0, 12, 8, 24), 'grip_front': (8, 12, 12, 24), 'grille': (12, 12, 18, 20),
     'foot': (18, 12, 26, 16), 'ring': (26, 12, 32, 16), 'tip': (18, 16, 24, 22), 'button': (24, 16, 28, 18),
     'led': (28, 16, 30, 18), 'strip': (24, 18, 32, 20)}


def texture():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    p = img.load()

    def region(name, f):
        x0, y0, x1, y1 = R[name]
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = f(x - x0, y - y0, x1 - x0, y1 - y0)

    # Head side: white shell with a navy-framed rainbow band, grey seam underneath.
    region('side', lambda x, y, w, h: WHITE if y == 0 else LIGHT if y == 1 else GREY_EDGE if y == h - 1 else SEAM if y == h - 2 else
           NAVY if x in (0, 1, w - 2, w - 1) else rainbow(x - 2, w - 4, y == 2))
    region('top', lambda x, y, w, h: WHITE2 if y in (0, h - 1) or x in (0, w - 1) else WHITE)
    region('under', lambda x, y, w, h: GREY_EDGE2 if y in (0, h - 1) or x in (0, w - 1) else GREY)
    region('cap', lambda x, y, w, h: WHITE2 if x in (0, w - 1) or y in (0, h - 1) else WHITE)
    region('bezel', lambda x, y, w, h: NAVY_D if x in (0, w - 1) or y in (0, h - 1) else NAVY if x in (1, w - 2) or y in (1, h - 2) else NAVY_DD)
    # Grip: white body over the AE2 grey lower band.
    region('grip', lambda x, y, w, h: (GREY_EDGE if x in (0, w - 1) or y == h - 1 else SEAM if y == h - 4 else GREY_HI if y == h - 3 else GREY) if y >= h - 4
           else SEAM if x in (0, w - 1) else WHITE2 if x in (1, w - 2) else WHITE)
    region('grip_front', lambda x, y, w, h: (GREY_EDGE if x in (0, w - 1) else GREY) if y >= h - 4
           else SEAM if x in (0, w - 1) else WHITE2 if y % 3 == 2 else WHITE)
    region('grille', lambda x, y, w, h: SEAM if x in (0, w - 1) or y in (0, h - 1) else GRILLE_D if (x + y) % 2 == 0 else GRILLE)
    # Foot: the grey lower band of an AE2 machine with the terminal's navy-framed rainbow strip.
    region('foot', lambda x, y, w, h: GREY_EDGE2 if y == h - 1 else NAVY if y == 0 or x in (0, w - 1) else rainbow(x - 1, w - 2, y == 1))
    region('ring', lambda x, y, w, h: METAL_D if y in (0, h - 1) else METAL_HI if y == 1 else METAL)
    region('tip', lambda x, y, w, h: FLUIX_D if x in (0, w - 1) or y in (0, h - 1) else FLUIX_HI if 1 < x < w - 2 and 1 < y < h - 2 else FLUIX)
    region('button', lambda x, y, w, h: NAVY_D if y == h - 1 else NAVY)
    region('led', lambda x, y, w, h: LED)
    region('strip', lambda x, y, w, h: rainbow(x, w, y == 0))
    img.save(os.path.join(ROOT, 'textures/item/swatch_applicator.png'))


def uv(name):
    x0, y0, x1, y1 = R[name]
    return [x0 / 2, y0 / 2, x1 / 2, y1 / 2]


ALL = ('north', 'south', 'east', 'west', 'up', 'down')


def box(frm, to, faces, rotation=None, name=None):
    """faces: dict face -> region name (a 'default' key fills the rest); a single region name applies to all six."""
    if isinstance(faces, str):
        faces = {'default': faces}
    full = {f: faces.get(f, faces.get('default')) for f in ALL}
    e = {'from': frm, 'to': to, 'faces': {f: {'uv': uv(r), 'texture': '#0'} for f, r in full.items() if r}}
    if name:
        e['name'] = name
    if rotation:
        e['rotation'] = rotation
    return e


def head(frm, to, faces, name):
    return box(frm, to, faces, {'angle': HEAD_ANGLE, 'axis': 'z', 'origin': HEAD_ORIGIN}, name)


def model():
    # Upright grip with the aperture foot at the bottom, wide head on top, front of the head at +x.
    els = [
        head([2, 12, 4.5], [14, 15, 11.5], {'default': 'side', 'up': 'top', 'down': 'under', 'east': 'cap', 'west': 'cap'}, 'head'),
        head([1, 12.5, 5.25], [2, 14.75, 10.75], {'default': 'cap', 'down': 'under'}, 'head_back'),
        head([14, 12.5, 5.25], [15, 14.75, 10.75], {'default': 'cap', 'down': 'under'}, 'head_front'),
        head([3, 15, 5.25], [13, 15.25, 10.75], 'bezel', 'screen_bezel'),
        head([11.25, 15.25, 6], [12.5, 15.4, 10], 'strip', 'screen_keys'),
        head([12.75, 15.25, 10.25], [13.25, 15.4, 10.6], 'led', 'led'),
        head([7.5, 12.1, 11.5], [10.5, 12.9, 11.8], 'button', 'button_s'),
        head([7.5, 12.1, 4.2], [10.5, 12.9, 4.5], 'button', 'button_n'),
        box([5, 3, 5.75], [9, 12.5, 10.25], {'default': 'grip', 'east': 'grip_front', 'up': 'under', 'down': 'under'}, name='grip'),
        box([9, 7.5, 6.5], [9.75, 11.5, 9.5], {'default': 'grip_front', 'up': 'under', 'down': 'under'}, name='grip_bump'),
        box([5.5, 5.5, 10.25], [8.5, 10.5, 10.5], 'grille', name='grille_s'),
        box([5.5, 5.5, 5.5], [8.5, 10.5, 5.75], 'grille', name='grille_n'),
        box([4, 1, 5], [10, 3, 11], {'default': 'foot', 'up': 'under', 'down': 'under'}, name='foot'),
        box([5.5, 0.25, 6.5], [8.5, 1, 9.5], {'default': 'ring', 'down': 'tip'}, name='aperture'),
    ]
    base = {'textures': {'0': 'appliedswatches:item/swatch_applicator', 'particle': 'appliedswatches:item/swatch_applicator'},
            'elements': els}
    with open(os.path.join(ROOT, 'models/item/swatch_applicator_base.json'), 'w') as f:
        json.dump(base, f, indent=2)
    item = {
        'parent': 'appliedswatches:item/swatch_applicator_base',
        'gui_light': 'front',
        'textures': {'particle': 'appliedswatches:item/swatch_applicator'},
        'display': {
            'gui': {'rotation': [25, -45, 0], 'translation': [0, 0, 0], 'scale': [1.0, 1.0, 1.0]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [80, 90, 0], 'translation': [0, 1.5, 3.5], 'scale': [0.55, 0.55, 0.55]},
            'thirdperson_lefthand': {'rotation': [80, 90, 0], 'translation': [0, 1.5, 3.5], 'scale': [0.55, 0.55, 0.55]},
            'firstperson_righthand': {'rotation': [0, 90, 0], 'translation': [-2.5, 2.5, -2], 'scale': [0.35, 0.35, 0.35]},
            'firstperson_lefthand': {'rotation': [0, 90, 0], 'translation': [2.5, 2.5, -2], 'scale': [0.35, 0.35, 0.35]},
        },
    }
    with open(os.path.join(ROOT, 'models/item/swatch_applicator.json'), 'w') as f:
        json.dump(item, f, indent=2)


if __name__ == '__main__':
    os.makedirs(os.path.join(ROOT, 'textures/item'), exist_ok=True)
    texture()
    model()
