"""Draws the swatch and theme terminal fronts in the AE2 machine style. Usage: python3 tools/front_texture.py"""
import colorsys
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/appliedswatches/textures/block')


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


WHITE, WHITE2, LIGHT, PANEL = hx('f2f2f2'), hx('e8e8ea'), hx('d5d6dc'), hx('dedfe3')
SEAM, GREY_EDGE, GREY_EDGE2, GREY, GREY_HI = hx('cbccd4'), hx('878fa5'), hx('787e97'), hx('b3b6c6'), hx('bfc1cd')
NAVY = hx('262b54')
SWATCHES = [['d83a3a', 'ea8a2a', 'eece3a', '4cb652', '30aaaa'],
            ['3a68d6', '5a4ad0', '9a4ac8', 'da5ca8', 'ececf0']]
# Cyberpunk, desert and underwater theme strips.
THEMES = [['ff2bd6', '18e0ff', '7a2bff', '2a1050', 'f2f25a'],
          ['dbcf9f', 'c9a46b', 'a15325', '985e43', '6e8a3a'],
          ['63a598', '335b4b', 'acc8be', '23466e', '2d8c8c']]


def casing():
    """White upper body, grey lower band, like the other AE2 machines; screen and lower strip framed in navy."""
    img = Image.new('RGBA', (16, 16))
    p = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15)
            if y == 0:
                c = WHITE
            elif y <= 7:
                c = (WHITE2 if y % 2 == 0 else WHITE) if edge else (LIGHT if y == 1 or x in (1, 14) and y < 5 else PANEL)
            elif y == 8:
                c = SEAM if edge or x in (1, 6, 8, 9, 10, 13) else LIGHT
            elif y == 9 or y == 15 or edge:
                c = GREY_EDGE2 if (y == 15 and x not in (3, 4, 5, 6, 9)) or (edge and y in (12, 14)) else GREY_EDGE
            else:
                c = GREY_HI if y >= 13 and x in (1, 2, 6, 8, 9, 10, 12, 13, 14) else GREY
            p[x, y] = c
    for y in range(2, 9):
        for x in range(2, 14):
            p[x, y] = NAVY
    for x in range(2, 14):
        p[x, 10] = NAVY
        p[x, 13] = NAVY
    for y in (11, 12):
        p[2, y] = NAVY
        p[13, y] = NAVY
    return img, p


def strip(p, y, cols):
    """Five 2x2 swatches from x=3, lit on top."""
    for i, col in enumerate(cols):
        c = hx(col)
        for dy in range(2):
            k = 1.12 if dy == 0 else 0.86
            for dx in range(2):
                p[3 + i * 2 + dx, y + dy] = tuple(min(255, int(v * k)) for v in c[:3]) + (255,)


def swatch_front():
    img, p = casing()
    strip(p, 3, SWATCHES[0])
    strip(p, 6, SWATCHES[1])
    for y in (11, 12):
        for x in range(3, 13):
            r, g, b = colorsys.hsv_to_rgb((x - 3) / 10, 0.75, 0.95 if y == 11 else 0.8)
            p[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    return img


def theme_front():
    img, p = casing()
    strip(p, 3, THEMES[0])
    strip(p, 6, THEMES[1])
    strip(p, 11, THEMES[2])
    return img


swatch_front().save(os.path.join(OUT, 'swatch_terminal_front.png'))
theme_front().save(os.path.join(OUT, 'theme_terminal_front.png'))
