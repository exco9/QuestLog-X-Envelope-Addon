"""Generate original, self-contained SVG illustrations for the project README."""
from pathlib import Path
from html import escape

OUT = Path(__file__).parent / "images"
OUT.mkdir(exist_ok=True)

def svg(width, height, body, title):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title">
<title id="title">{escape(title)}</title>
<defs><linearGradient id="paper" x2="0" y2="1"><stop stop-color="#f5e8d1"/><stop offset="1" stop-color="#e8d3b1"/></linearGradient></defs>
{body}</svg>'''

def text(x, y, value, size=20, color="#59452e", weight="normal"):
    return f'<text x="{x}" y="{y}" font-family="Verdana, sans-serif" font-size="{size}" font-weight="{weight}" fill="{color}">{escape(value)}</text>'

def envelope():
    return '''<g stroke="#8c6638" stroke-width="5" stroke-linejoin="miter">
<path fill="#b78a4c" d="M6 26H90V86H6Z"/><path fill="#f1d79a" d="M0 20H84V80H0Z"/>
<path fill="#ddba73" d="M0 80L42 46L84 80Z"/><path fill="#ffe6ad" d="M0 20L42 53L84 20Z"/>
</g><path fill="#933d40" d="M34 43H50V47H54V61H50V65H34V61H30V47H34Z"/><path fill="#c46659" d="M35 47H47V51H35Z"/>'''

def package():
    return '''<g stroke="#8c6638" stroke-width="5" stroke-linejoin="miter"><path fill="#d6ac6b" d="M0 28L42 8L84 28V72L42 92L0 72Z"/><path fill="#f1d79a" d="M0 28L42 48L84 28L42 8Z"/><path fill="#bf9155" d="M42 48V92L84 72V28Z"/></g><path fill="#f6e8c6" d="M29 14L42 8L55 14L13 35L0 28Z M33 44L46 50V91L33 86Z"/>'''

def circle():
    return '''<g fill="none" stroke="#9270ca" stroke-width="4"><path d="M25 8H59L82 31V65L59 88H25L2 65V31Z"/><path d="M29 18H55L72 35V61L55 78H29L12 61V35Z M42 19L69 67H15Z M42 78L15 30H69Z"/></g><g fill="#ceaff3"><path d="M40 38H45V57H40Z M33 45H52V50H33Z"/><path d="M78 0H82V6H88V10H82V16H78V10H72V6H78Z"/></g>'''

def signature():
    return '''<path fill="#f9eedb" stroke="#956e42" stroke-width="3" d="M0 34H88V82H0Z"/><path fill="none" stroke="#885c99" stroke-width="3" d="M12 65Q36 31 30 52T21 66Q42 55 45 59T43 68Q55 43 57 60T77 60 M10 74L77 71"/><path fill="#f4e4bd" stroke="#8c6638" stroke-width="3" d="M52 39L62 14L78 2L88 6L85 24L67 41Z"/><path fill="none" stroke="#8c6638" stroke-width="3" d="M49 53L79 13"/>'''

banner = '<rect width="960" height="270" rx="16" fill="#24251f"/><path d="M0 234H960V270H0Z" fill="#191c18"/><path d="M0 238H960" stroke="#9e7946" stroke-width="4"/>'
banner += '<g transform="translate(48 68) scale(1.4)">' + envelope() + '</g>'
banner += text(205, 106, 'QUESTLOG', 53, '#f6c46d', 'bold')
banner += text(207, 155, '× ENVELOPE', 37, '#f1e3cb', 'bold')
banner += text(208, 194, 'Deliver a story. Seal a promise. Sign your next quest.', 18, '#cbbc9f')
banner += text(48, 257, 'MINECRAFT 1.21.1  /  FABRIC + NEOFORGE', 13, '#cbbc9f')
(OUT / 'banner.svg').write_text(svg(960, 270, banner, 'QuestLog × Envelope — quests delivered by mail'), encoding='utf-8')

cards = [
    ('Quest letters', ['Send invitations, clues and quest rewards', 'as physical Envelope mail.'], envelope()),
    ('Package rewards', ['Deliver supplies and loot in packages.', 'Use multiple pages for larger rewards.'], package()),
    ('Magic circles', ['Hold to activate a quest or command.', 'Finish with a magical glow and particles.'], circle()),
    ('Player signatures', ['Sign with a name or @s. Add a frame,', 'a command and a lasting enchanted shine.'], signature()),
]
board = '<rect width="960" height="490" rx="16" fill="#24251f"/>'
for index, (title, lines, icon) in enumerate(cards):
    x, y = 20 + (index % 2) * 470, 20 + (index // 2) * 230
    board += f'<rect x="{x}" y="{y}" width="450" height="210" rx="12" fill="url(#paper)"/>'
    board += f'<g transform="translate({x + 25} {y + 37})">{icon}</g>'
    board += text(x + 130, y + 64, title, 24, weight='bold')
    for n, line in enumerate(lines):
        board += text(x + 130, y + 100 + n * 25, line, 13)
    board += f'<path d="M{x+25} {y+158}H{x+425}" stroke="#ceb893"/>'
    hints = ['Rich text · Wax seals · Quest invitations', 'Native Envelope packages · Six slots per page', 'Position · Size · Ink color · Magic color', 'Feather animation · Writing sound · Sign once']
    board += text(x + 25, y + 185, hints[index], 13, '#7c6548')
(OUT / 'features.svg').write_text(svg(960, 490, board, 'Feature overview: quest letters, package rewards, magic circles and player signatures'), encoding='utf-8')
