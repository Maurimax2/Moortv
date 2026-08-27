# The three ad canvases. Everything is inlined as base64, so each file is a
# single artefact that renders the same anywhere with no network at open time.
#
# Laid out as a flex column rather than absolutely positioned blocks: the first
# pass pinned everything to edges and the pieces collided at the bottom, cutting
# off prices and burying the trust line under the call to action.
import base64, os

REPO = '/home/user/Moortv'
ART = f'{REPO}/core/designsystem/src/main/res/drawable-nodpi'
FONTS = '/tmp/claude-0/-home-user-Moortv/4f970a36-c965-5b2b-b7f1-0337312c9e2d/scratchpad/shots'
OUT = f'{REPO}/marketing'

def b64(p):
    return base64.b64encode(open(p, 'rb').read()).decode()

def face(weight, file):
    return (f"@font-face{{font-family:Cairo;font-weight:{weight};font-style:normal;"
            f"src:url(data:font/ttf;base64,{b64(f'{FONTS}/{file}')}) format('truetype')}}")

def img(p):
    return f"data:image/{'png' if p.endswith('.png') else 'webp'};base64,{b64(p)}"

FACES = face(900, 'cairo_black.ttf') + face(700, 'cairo_bold.ttf') + face(600, 'cairo_semibold.ttf')
LOGO, MESSI, YAMAL = img(f'{REPO}/web/mark.png'), img(f'{ART}/player_02.webp'), img(f'{ART}/player_04.webp')
POSTERS = [img(f'{ART}/poster_{n}.webp') for n in (
    'batman', 'got', 'oppenheimer', 'breakingbad', 'spiderman',
    'casadepapel', 'punisher', 'walkingdead', 'fury')]

BASE = """
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:1080px;height:1080px;overflow:hidden}
body{background:#08080B;color:#fff;font-family:Cairo,sans-serif;
  -webkit-font-smoothing:antialiased}
.canvas{position:relative;width:1080px;height:1080px;overflow:hidden}
/* The column owns the vertical rhythm; art sits behind it. */
.col{position:relative;z-index:5;height:100%;display:flex;flex-direction:column}
.mark{height:88px;width:auto}
.n{font-weight:900;font-feature-settings:"tnum";direction:ltr;white-space:nowrap}
.grow{flex:1;min-height:0}
.sub{font-weight:600;color:#B9B3C4}
.trust{font-weight:600;color:#8B8399;white-space:nowrap}
.cta{display:inline-flex;align-items:center;justify-content:center;background:#f97316;
  color:#12060A;font-weight:900;border-radius:999px;white-space:nowrap}
.tier{display:flex;flex-direction:column;align-items:center;justify-content:center;
  border-radius:18px;background:#141319;border:1px solid #272233}
.tier .lab{font-weight:700;color:#A79FB4;line-height:1.3}
.tier .val{font-weight:900;color:#fff;line-height:1.15}
.tier.best{background:#f97316;border-color:#f97316}
.tier.best .lab{color:#411904}
.tier.best .val{color:#12060A}
.badge{background:#fff;color:#12060A;font-weight:900;border-radius:999px;
  white-space:nowrap;align-self:center}
"""

def page(title, css, body):
    return (f'<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8">'
            f'<title>{title}</title><style>{FACES}{BASE}{css}</style></head>'
            f'<body><div class="canvas">{body}</div></body></html>')

TIERS = [('شهر', '350'), ('3 أشهر', '700'), ('6 أشهر', '1000'), ('سنة', '1500')]
TRUST = 'سمارت تي في · أندرويد · آيفون · ريسيفر'
SUB = 'اشتراك واحد شامل جميع المحتوى'
CTA = 'تجربة مجانية — راسلنا الآن'
BEST = 'الأكثر طلباً'

# ------------------------------------------------------- A — the number alone
def variant_a():
    css = """
    .col{padding:52px 56px 56px}
    /* A glow that belongs to the number, not a wash across the whole canvas. */
    .glow{position:absolute;top:150px;right:-140px;width:900px;height:700px;z-index:1;
      background:radial-gradient(closest-side,rgba(107,33,181,.80),rgba(107,33,181,0) 70%)}
    .player{position:absolute;bottom:-8px;left:-70px;height:790px;z-index:2;
      filter:saturate(.95) brightness(.98)}
    .fade{position:absolute;inset:auto 0 0 0;height:300px;z-index:3;
      background:linear-gradient(to top,#08080B 20%,rgba(8,8,11,.55) 62%,rgba(8,8,11,0))}
    .head{display:flex;justify-content:flex-start}
    .hook{margin-top:44px}
    .pre{font-size:62px;font-weight:900;line-height:1}
    .line{display:flex;align-items:flex-end;gap:22px;margin-top:2px}
    .line .n{font-size:296px;line-height:.84;color:#f97316;
      text-shadow:0 20px 54px rgba(249,115,22,.30)}
    .line .unit{font-size:62px;font-weight:900;line-height:1.6}
    .sub{font-size:37px;margin-top:16px}
    /* Kept clear of the player so the cards never sit on him. */
    .rail{width:704px;margin-left:auto;display:flex;gap:14px;align-items:flex-end}
    .cell{flex:1;display:flex;flex-direction:column;gap:10px}
    .cell.pick{flex:1.3}
    .tier{height:132px;gap:6px}
    .tier .lab{font-size:26px}
    .tier .val{font-size:44px}
    .tier.best{height:164px}
    .tier.best .lab{font-size:28px}
    .tier.best .val{font-size:56px}
    .badge{font-size:23px;padding:8px 20px}
    .trust{font-size:25px;width:704px;margin-left:auto;text-align:center;margin-top:22px}
    .cta{font-size:36px;padding:25px 50px;width:704px;margin-left:auto;margin-top:22px}
    """
    cells = ''
    for label, price in TIERS:
        if label == 'سنة':
            cells += (f'<div class="cell pick"><div class="badge">{BEST}</div>'
                      f'<div class="tier best"><span class="lab">{label}</span>'
                      f'<span class="val n">{price}</span></div></div>')
        else:
            cells += (f'<div class="cell"><div class="tier"><span class="lab">{label}</span>'
                      f'<span class="val n">{price}</span></div></div>')
    body = f"""
    <div class="glow"></div>
    <img class="player" src="{MESSI}" alt="">
    <div class="fade"></div>
    <div class="col">
      <div class="head"><img class="mark" src="{LOGO}" alt=""></div>
      <div class="hook">
        <div class="pre">ابدأ بـ</div>
        <div class="line"><span class="n">350</span><span class="unit">أوقية فقط</span></div>
      </div>
      <div class="sub">{SUB}</div>
      <div class="grow"></div>
      <div class="rail">{cells}</div>
      <div class="trust">{TRUST}</div>
      <div class="cta">{CTA}</div>
    </div>
    """
    return page('MAURIMAX — 350', css, body)

# --------------------------------------------------------- B — the poster wall
def variant_b():
    css = """
    .col{padding:34px 50px 34px;width:596px;margin-right:0;margin-left:auto}
    .wall{position:absolute;top:-140px;left:-210px;width:790px;height:1420px;z-index:1;
      display:grid;grid-template-columns:repeat(3,246px);gap:16px;
      transform:rotate(-10deg);transform-origin:top left}
    .wall img{width:100%;height:368px;object-fit:cover;border-radius:12px;
      filter:saturate(.8) brightness(.58)}
    /* Fades the wall out beneath the copy instead of dimming the whole canvas. */
    .veil{position:absolute;inset:0;z-index:2;background:
      linear-gradient(to left,#08080B 40%,rgba(8,8,11,.90) 56%,rgba(8,8,11,.10) 100%)}
    .player{position:absolute;bottom:0;left:-34px;height:430px;z-index:3;
      filter:saturate(.9) brightness(.9)}
    .head{display:flex;justify-content:flex-start}
    .hook{margin-top:34px}
    .pre{font-size:42px;font-weight:900;line-height:1}
    .line .n{font-size:176px;line-height:.84;color:#f97316;display:block;margin-top:4px}
    .unit{font-size:42px;font-weight:900;display:block;margin-top:2px}
    .sub{font-size:29px;margin-top:10px}
    .stack{display:flex;flex-direction:column;gap:8px}
    .row{display:flex;align-items:center;justify-content:space-between;
      background:#141319;border:1px solid #272233;border-radius:14px;padding:9px 18px}
    .row .lab{font-weight:700;font-size:27px;color:#A79FB4}
    .row .val{font-weight:900;font-size:34px}
    .row.best{background:#f97316;border-color:#f97316;padding:12px 18px}
    .row.best .lab{color:#411904;font-size:29px}
    .row.best .val{color:#12060A;font-size:41px}
    .badge{font-size:20px;padding:5px 14px;align-self:flex-start;margin-bottom:-4px}
    .trust{font-size:21px;margin-top:12px;text-align:center}
    .cta{font-size:29px;padding:16px 32px;width:100%;margin-top:12px}
    """
    rows = ''
    for label, price in TIERS:
        if label == 'سنة':
            rows += (f'<div class="badge">{BEST}</div>'
                     f'<div class="row best"><span class="lab">{label}</span>'
                     f'<span class="val n">{price}</span></div>')
        else:
            rows += (f'<div class="row"><span class="lab">{label}</span>'
                     f'<span class="val n">{price}</span></div>')
    wall = ''.join(f'<img src="{p}" alt="">' for p in POSTERS)
    body = f"""
    <div class="wall">{wall}</div>
    <img class="player" src="{YAMAL}" alt="">
    <div class="veil"></div>
    <div class="col">
      <div class="head"><img class="mark" src="{LOGO}" alt=""></div>
      <div class="hook">
        <div class="pre">ابدأ بـ</div>
        <div class="line"><span class="n">350</span><span class="unit">أوقية فقط</span></div>
      </div>
      <div class="sub">{SUB}</div>
      <div class="grow"></div>
      <div class="stack">{rows}</div>
      <div class="trust">{TRUST}</div>
      <div class="cta">{CTA}</div>
    </div>
    """
    return page('MAURIMAX — الجدار', css, body)

# ----------------------------------------------------------- C — the diagonal
def variant_c():
    css = """
    .col{padding:50px 56px 0}
    .field{position:absolute;inset:0;z-index:0;background:#08080B}
    /* One hard diagonal edge, not a gradient poured across the canvas. */
    .band{position:absolute;inset:0;z-index:1;background:#2E0A54;
      clip-path:polygon(0 0,100% 0,100% 46%,0 66%)}
    .band2{position:absolute;inset:0;z-index:1;background:#5A18A0;
      clip-path:polygon(0 0,100% 0,100% 38%,0 58%);opacity:.92}
    .strip{position:absolute;inset:auto 0 0 0;height:174px;z-index:1;background:#f97316;
      clip-path:polygon(0 26%,100% 0,100% 100%,0 100%)}
    .player{position:absolute;bottom:150px;left:-52px;height:588px;z-index:2;
      filter:drop-shadow(0 26px 50px rgba(0,0,0,.6))}
    .head{display:flex;justify-content:flex-start}
    .hook{margin-top:24px;text-align:right}
    .pre{font-size:56px;font-weight:900;line-height:1}
    .line .n{font-size:286px;line-height:.84;display:block;margin-top:2px;
      text-shadow:0 18px 44px rgba(0,0,0,.45)}
    .unit{font-size:56px;font-weight:900;display:block;margin-top:4px}
    .sub{font-size:34px;color:#E4D8F5;margin-top:14px}
    /* Held to the right so the grid never lands on the player. */
    .grid{width:474px;margin-left:auto;display:grid;grid-template-columns:1fr 1fr;gap:12px}
    .cell{display:flex;flex-direction:column;gap:8px}
    .tier{height:108px;gap:4px;background:rgba(8,8,11,.78);border-color:rgba(255,255,255,.16)}
    .tier .lab{font-size:25px;color:#C9C1D6}
    .tier .val{font-size:40px}
    .tier.best{background:#08080B;border-color:#f97316}
    .tier.best .lab{color:#F5BE92}
    .tier.best .val{color:#f97316}
    .badge{font-size:21px;padding:6px 16px}
    .trust{width:474px;margin-left:auto;font-size:23px;color:#B7ADC9;
      text-align:center;margin-bottom:14px}
    .foot{height:174px;display:flex;align-items:center;justify-content:flex-start;
      margin-top:16px}
    .cta{font-size:35px;padding:0;background:none;color:#12060A}
    """
    cells = ''
    for label, price in TIERS:
        if label == 'سنة':
            cells += (f'<div class="cell"><div class="badge">{BEST}</div>'
                      f'<div class="tier best"><span class="lab">{label}</span>'
                      f'<span class="val n">{price}</span></div></div>')
        else:
            cells += (f'<div class="cell"><div class="tier"><span class="lab">{label}</span>'
                      f'<span class="val n">{price}</span></div></div>')
    body = f"""
    <div class="field"></div><div class="band"></div><div class="band2"></div>
    <div class="strip"></div>
    <img class="player" src="{YAMAL}" alt="">
    <div class="col">
      <div class="head"><img class="mark" src="{LOGO}" alt=""></div>
      <div class="hook">
        <div class="pre">ابدأ بـ</div>
        <div class="line"><span class="n">350</span><span class="unit">أوقية فقط</span></div>
      </div>
      <div class="sub">{SUB}</div>
      <div class="grow"></div>
      <div class="trust">{TRUST}</div>
      <div class="grid">{cells}</div>
      <div class="foot"><span class="cta">{CTA}</span></div>
    </div>
    """
    return page('MAURIMAX — القطري', css, body)

os.makedirs(OUT, exist_ok=True)
for name, html in (('ad-a-number', variant_a()),
                   ('ad-b-wall', variant_b()),
                   ('ad-c-diagonal', variant_c())):
    open(f'{OUT}/{name}.html', 'w', encoding='utf-8').write(html)
    print(name, os.path.getsize(f'{OUT}/{name}.html') // 1024, 'KB')
