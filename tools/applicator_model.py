"""Builds the Swatch Applicator item: a 32x32 texture in the AE2 machine palette and a cuboid model, proportioned
from a modeled reference of lach's sketch (long casing, screen on top, grip below). Usage: python3 tools/applicator_model.py"""
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/appliedswatches')


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


WHITE, WHITE2, LIGHT, PANEL, SEAM = hx('f2f2f2'), hx('e8e8ea'), hx('d5d6dc'), hx('dedfe3'), hx('cbccd4')
GREY_EDGE, GREY_EDGE2, GREY, GREY_HI = hx('878fa5'), hx('787e97'), hx('b3b6c6'), hx('bfc1cd')
NAVY, NAVY_D, NAVY_DD = hx('262b54'), hx('1b1f3d'), hx('12152a')
GRIP, GRIP_D = hx('4a4f60'), hx('3a3e4c')
FLUIX, FLUIX_HI, FLUIX_D = hx('8a5ad8'), hx('c7a8ff'), hx('5a3a9a')

# Texture regions, in pixels: name -> (x0, y0, x1, y1).
R = {'side': (0, 0, 16, 6), 'top': (16, 0, 32, 6), 'bottom': (0, 8, 16, 14), 'grip': (16, 8, 24, 16),
     'bezel': (24, 8, 32, 16), 'tip': (0, 16, 8, 24), 'cap': (8, 16, 16, 24), 'ring': (16, 16, 24, 24)}


def texture():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    p = img.load()

    def region(name, f):
        x0, y0, x1, y1 = R[name]
        for y in range(y0, y1):
            for x in range(x0, x1):
                p[x, y] = f(x - x0, y - y0, x1 - x0, y1 - y0)

    region('side', lambda x, y, w, h: WHITE if y == 0 else SEAM if y == h - 1 else
           FLUIX if y == 3 and 2 <= x < w - 2 else (WHITE2 if x in (0, w - 1) else PANEL))
    region('top', lambda x, y, w, h: WHITE if y in (0, h - 1) or x in (0, w - 1) else LIGHT if x in (1, w - 2) else PANEL)
    region('bottom', lambda x, y, w, h: GREY_EDGE2 if y in (0, h - 1) or x in (0, w - 1) else GREY_HI if y == 1 else GREY)
    region('grip', lambda x, y, w, h: GRIP_D if y % 2 == 1 or x in (0, w - 1) else GRIP)
    region('bezel', lambda x, y, w, h: NAVY_D if x in (0, w - 1) or y in (0, h - 1) else NAVY if x in (1, w - 2) or y in (1, h - 2) else NAVY_DD)
    region('tip', lambda x, y, w, h: FLUIX_D if x in (0, w - 1) or y in (0, h - 1) else FLUIX_HI if 2 < x < w - 3 and 2 < y < h - 3 else FLUIX)
    region('cap', lambda x, y, w, h: GREY_EDGE if x in (0, w - 1) or y in (0, h - 1) else GREY_HI)
    region('ring', lambda x, y, w, h: GREY_EDGE2 if y in (0, h - 1) else GREY_EDGE)
    img.save(os.path.join(ROOT, 'textures/item/swatch_applicator.png'))


def uv(name):
    x0, y0, x1, y1 = R[name]
    return [x0 / 2, y0 / 2, x1 / 2, y1 / 2]


def box(frm, to, faces, rotation=None):
    """faces: dict face -> region name; a single region name applies to all six."""
    if isinstance(faces, str):
        faces = {f: faces for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    e = {'from': frm, 'to': to, 'faces': {f: {'uv': uv(r), 'texture': '#0'} for f, r in faces.items()}}
    if rotation:
        e['rotation'] = rotation
    return e


def model():
    # Barrel runs along +x (emitter at x=16), screen on top near the back, grip under the back third.
    els = [
        box([1, 7, 5], [13, 12, 11], {'north': 'side', 'south': 'side', 'up': 'top', 'down': 'bottom', 'east': 'cap', 'west': 'cap'}),
        box([1, 6, 5.5], [12, 7, 10.5], 'bottom'),
        box([0, 7.5, 5.5], [1, 11.5, 10.5], 'cap'),
        box([12.5, 6.5, 4.5], [13.5, 12.5, 11.5], 'ring'),
        box([13.5, 8, 6], [15, 11, 10], 'ring'),
        box([15, 8.5, 6.5], [16, 10.5, 9.5], {'north': 'ring', 'south': 'ring', 'up': 'ring', 'down': 'ring', 'west': 'ring', 'east': 'tip'}),
        box([3.5, 12, 5], [9.5, 13, 11], 'bezel'),
        box([2.5, 1, 6], [5.5, 7, 10], 'grip', {'angle': 22.5, 'axis': 'z', 'origin': [4, 7, 8]}),
        box([6, 4, 7.5], [7, 6, 8.5], 'grip'),
        box([5, 3.5, 6.5], [8, 4.5, 9.5], 'bottom'),
    ]
    base = {'textures': {'0': 'appliedswatches:item/swatch_applicator', 'particle': 'appliedswatches:item/swatch_applicator'},
            'elements': els}
    with open(os.path.join(ROOT, 'models/item/swatch_applicator_base.json'), 'w') as f:
        json.dump(base, f, indent=2)
    item = {
        'parent': 'minecraft:builtin/entity',
        'gui_light': 'front',
        'textures': {'particle': 'appliedswatches:item/swatch_applicator'},
        'display': {
            'gui': {'rotation': [30, 135, 0], 'translation': [0, 0, 0], 'scale': [1.15, 1.15, 1.15]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 1], 'scale': [0.6, 0.6, 0.6]},
            'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 1], 'scale': [0.6, 0.6, 0.6]},
            'firstperson_righthand': {'rotation': [25, 95, 0], 'translation': [-1, 4, 0], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_lefthand': {'rotation': [0, 95, 5], 'translation': [1, 3, 0], 'scale': [0.7, 0.7, 0.7]},
        },
    }
    with open(os.path.join(ROOT, 'models/item/swatch_applicator.json'), 'w') as f:
        json.dump(item, f, indent=2)


if __name__ == '__main__':
    os.makedirs(os.path.join(ROOT, 'textures/item'), exist_ok=True)
    texture()
    model()
