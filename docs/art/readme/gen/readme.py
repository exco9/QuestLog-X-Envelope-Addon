"""README illustrations, authored on a native pixel grid with actual mod textures."""
from pathlib import Path
from PIL import Image, ImageDraw
from common import OUT, ROOT, PALETTE, hex2rgba, save_png, write_json, scale_nearest
from tex import new

INPUT = ROOT / 'inputs'
REPO = ROOT.parents[2]
MOD = REPO / 'common/src/main/resources/assets/questlog_envelope'
C = {name: hex2rgba(value) for name, value in PALETTE['colors'].items()}

# Original 5x7 pixel lettering; no system fonts or antialiased text.
ROWS = {
 'A':'01110/10001/10001/11111/10001/10001/10001',
 'B':'11110/10001/10001/11110/10001/10001/11110',
 'C':'01111/10000/10000/10000/10000/10000/01111',
 'D':'11110/10001/10001/10001/10001/10001/11110',
 'E':'11111/10000/10000/11110/10000/10000/11111',
 'F':'11111/10000/10000/11110/10000/10000/10000',
 'G':'01111/10000/10000/10111/10001/10001/01111',
 'H':'10001/10001/10001/11111/10001/10001/10001',
 'I':'111/010/010/010/010/010/111',
 'J':'00111/00010/00010/00010/10010/10010/01100',
 'K':'10001/10010/10100/11000/10100/10010/10001',
 'L':'10000/10000/10000/10000/10000/10000/11111',
 'M':'10001/11011/10101/10101/10001/10001/10001',
 'N':'10001/11001/10101/10011/10001/10001/10001',
 'O':'01110/10001/10001/10001/10001/10001/01110',
 'P':'11110/10001/10001/11110/10000/10000/10000',
 'Q':'01110/10001/10001/10001/10101/10010/01101',
 'R':'11110/10001/10001/11110/10100/10010/10001',
 'S':'01111/10000/10000/01110/00001/00001/11110',
 'T':'11111/00100/00100/00100/00100/00100/00100',
 'U':'10001/10001/10001/10001/10001/10001/01110',
 'V':'10001/10001/10001/10001/10001/01010/00100',
 'W':'10001/10001/10001/10101/10101/10101/01010',
 'X':'10001/10001/01010/00100/01010/10001/10001',
 'Y':'10001/10001/01010/00100/00100/00100/00100',
 'Z':'11111/00001/00010/00100/01000/10000/11111',
 'a':'00000/00000/01110/00001/01111/10001/01111',
 'b':'10000/10000/11110/10001/10001/10001/11110',
 'c':'00000/00000/01111/10000/10000/10000/01111',
 'd':'00001/00001/01111/10001/10001/10001/01111',
 'e':'00000/00000/01110/10001/11111/10000/01111',
 'f':'0011/0100/1110/0100/0100/0100/0100',
 'g':'00000/00000/01111/10001/01111/00001/11110',
 'h':'10000/10000/11110/10001/10001/10001/10001',
 'i':'1/0/1/1/1/1/1',
 'j':'01/00/01/01/01/01/10',
 'k':'1000/1000/1001/1010/1100/1010/1001',
 'l':'10/10/10/10/10/10/01',
 'm':'00000/00000/11010/10101/10101/10101/10101',
 'n':'00000/00000/11110/10001/10001/10001/10001',
 'o':'00000/00000/01110/10001/10001/10001/01110',
 'p':'00000/00000/11110/10001/11110/10000/10000',
 'q':'00000/00000/01111/10001/01111/00001/00001',
 'r':'0000/0000/1011/1100/1000/1000/1000',
 's':'00000/00000/01111/10000/01110/00001/11110',
 't':'0100/0100/1111/0100/0100/0100/0011',
 'u':'00000/00000/10001/10001/10001/10001/01111',
 'v':'00000/00000/10001/10001/10001/01010/00100',
 'w':'00000/00000/10001/10001/10101/10101/01010',
 'x':'00000/00000/10001/01010/00100/01010/10001',
 'y':'00000/00000/10001/10001/01111/00001/11110',
 'z':'00000/00000/11111/00010/00100/01000/11111',
 '0':'01110/10011/10101/10101/11001/10001/01110',
 '1':'010/110/010/010/010/010/111',
 '2':'01110/10001/00001/00010/00100/01000/11111',
 '3':'11110/00001/00001/01110/00001/00001/11110',
 '.':'0/0/0/0/0/0/1', ',':'00/00/00/00/00/01/10',
 ':':'0/0/1/0/1/0/0', '-':'000/000/000/111/000/000/000',
 '+':'00000/00100/00100/11111/00100/00100/00000',
 '/':'00001/00001/00010/00100/01000/10000/10000',
 '@':'01110/10001/10111/10101/10111/10000/01111',
 '&':'01100/10010/10100/01000/10101/10010/01101',
 '!':'1/1/1/1/1/0/1', ' ':'000/000/000/000/000/000/000',
}

def width(label, scale=1):
    return sum((len(ROWS[ch].split('/')[0])+1)*scale for ch in label)-scale

def text(image, xy, label, color='ink', scale=1):
    x, y = xy
    assert x >= 0 and y >= 0 and x + width(label, scale) <= image.width, label
    assert y + 7*scale <= image.height, label
    draw = ImageDraw.Draw(image)
    for ch in label:
        rows = ROWS[ch].split('/')
        for row, cells in enumerate(rows):
            for col, bit in enumerate(cells):
                if bit == '1':
                    draw.rectangle((x+col*scale,y+row*scale,x+(col+1)*scale-1,y+(row+1)*scale-1), fill=C[color])
        x += (len(rows[0])+1)*scale

def paper(w, h):
    """Preserve native torn edges/fold; tile the paper center instead of stretching."""
    source = Image.open(INPUT/'paper.png').convert('RGBA').crop((0,0,176,192))
    output = new(w,h)
    border = 18
    for y in range(h):
        sy = y if y < border else (192-h+y if y >= h-border else border+(y-border)%(192-2*border))
        for x in range(w):
            sx = x if x < border else (176-w+x if x >= w-border else border+(x-border)%(176-2*border))
            output.putpixel((x,y), source.getpixel((sx,sy)))
    return output

def sprite(name, scale=1):
    return scale_nearest(Image.open(INPUT/name).convert('RGBA'), scale)

def circle(color='magic'):
    source = Image.open(MOD/'textures/gui/magic_circle.png').convert('RGBA')
    result = new(*source.size, C[color])
    result.putalpha(source.getchannel('A'))
    return result

def signature(label='Alex'):
    atlas = Image.open(MOD/'textures/font/signature.png').convert('RGBA')
    result = new(80,9)
    x = 0
    for ch in label:
        index = ord(ch)-32
        tile = atlas.crop(((index%16)*16,(index//16)*9,(index%16+1)*16,(index//16+1)*9))
        bbox = tile.getbbox()
        if bbox:
            tile = tile.crop((bbox[0],0,bbox[2],9))
            ink = new(*tile.size, C['magic'])
            ink.putalpha(tile.getchannel('A'))
            result.alpha_composite(ink,(x,0))
            x += tile.width+1
    return result.crop((0,0,x,9))

def sparkle(image, xy):
    x,y = xy
    draw = ImageDraw.Draw(image)
    draw.line((x-3,y,x+3,y),fill=C['magic_light'])
    draw.line((x,y-3,x,y+3),fill=C['magic_light'])
    draw.point((x,y),fill=C['paper_hi'])

def frame(image, box):
    draw = ImageDraw.Draw(image)
    draw.rectangle(box, outline=C['wood_deep'], width=1)
    x0,y0,x1,y1 = box
    draw.line((x0+1,y0+1,x1-1,y0+1), fill=C['paper_hi'])
    draw.line((x0+1,y0+1,x0+1,y1-1), fill=C['paper_hi'])

def banner():
    output = new(480,150,C['background'])
    draw = ImageDraw.Draw(output)
    draw.rectangle((1,1,478,147),outline=C['wood_deep'],width=2)
    draw.line((3,3,476,3),fill=C['wood_hi'])
    draw.line((3,4,3,145),fill=C['wood_hi'])
    output.alpha_composite(paper(115,111),(17,16))
    output.alpha_composite(sprite('letter.png',5),(34,32))
    # Chunky, extruded pixel title using paper and brown tones from the real UI.
    for label,xy,scale in [('QUESTLOG',(154,23),4),('ENVELOPE',(154,68),3)]:
        x,y = xy
        for depth in (4,3,2,1):
            text(output,(x+depth,y+depth),label,'wood_deep',scale)
        text(output,(x,y),label,'paper_hi',scale)
        text(output,(x,y-1),label,'paper',scale)
    text(output,(155,106),'Quests delivered by mail.','paper')
    text(output,(154,59),'+','magic_light')
    draw.line((17,133,463,133),fill=C['wood_deep'])
    text(output,(20,138),'MINECRAFT 1.21.1 / FABRIC + NEOFORGE','paper')
    return output

def features():
    output = new(480,452,C['background'])
    content = [
        ('QUEST LETTERS','Send quest invitations and rewards through Envelope.',
         'Rich text, wax seals and one-click quest saving.',
         'Letter rewards / Quest invitations'),
        ('PACKAGE REWARDS','Deliver supplies and loot as physical mail.',
         'Six slots per page. More pages, more packages.',
         'Native Envelope packages / Multiple pages'),
        ('MAGIC CIRCLES','Hold to unlock a quest or run a command.',
         'Choose position, size, ink and magical glow.',
         'Hold to activate / Enchanted shine'),
        ('PLAYER SIGNATURES','Sign with a name or @s. Optional frame and command.',
         'Feather animation, writing sound and enchanted shine.',
         'Custom ink / Sign once / Server command'),
    ]
    for index,(title,line1,line2,footer) in enumerate(content):
        y = 8+index*111
        output.alpha_composite(paper(464,103),(8,y))
        text(output,(116,y+14),title,'ink',2)
        text(output,(116,y+41),line1)
        text(output,(116,y+54),line2)
        draw = ImageDraw.Draw(output)
        draw.line((116,y+73,451,y+73),fill=C['wood_hi'])
        text(output,(116,y+83),footer,'wood_deep')
        if index < 2:
            output.alpha_composite(sprite('letter.png' if index==0 else 'package.png',4),(26,y+18))
        elif index == 2:
            output.alpha_composite(circle(),(24,y+17))
            sparkle(output,(89,y+20))
            sparkle(output,(26,y+85))
        else:
            frame(output,(23,y+55,97,y+84))
            output.alpha_composite(scale_nearest(signature(),2),(29,y+60))
            output.alpha_composite(sprite('letter_and_quill.png',3),(39,y+9))
            sparkle(output,(94,y+46))
    return output

def build():
    base = OUT/'textures/gui/sprites/readme'
    manifest = {'target':'README illustrations for Minecraft 1.21.1',
                'scaling':'2x nearest-neighbor; no antialiasing', 'images':{}}
    for name,image in [('banner',banner()),('features',features())]:
        save_png(image,base/f'{name}_native.png')
        save_png(scale_nearest(image,2),base/f'{name}.png')
        manifest['images'][name] = {'native_size':list(image.size),'scale':2}
    write_json(OUT/'gui/readme.json',manifest)
