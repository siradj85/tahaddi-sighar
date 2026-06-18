# -*- coding: utf-8 -*-
"""توليد أصول متجر Google Play للعبة تحدي الصغار: أيقونة، غلاف، لقطات شاشة."""
import os, math, random
from PIL import Image, ImageDraw, ImageFont
import arabic_reshaper
from bidi.algorithm import get_display

OUT = os.path.join(os.path.dirname(os.path.dirname(__file__)), "أصول_المتجر")
os.makedirs(OUT, exist_ok=True)

FONT_R = "C:/Windows/Fonts/tahoma.ttf"
FONT_B = "C:/Windows/Fonts/tahomabd.ttf" if os.path.exists("C:/Windows/Fonts/tahomabd.ttf") else FONT_R

PURPLE_TOP = (123, 31, 162)
PURPLE_BOT = (74, 20, 140)
GOLD = (255, 193, 7)
WHITE = (255, 255, 255)
GREEN = (46, 125, 50)
DEEP = (49, 27, 146)
RED = (255, 82, 82)

def ar(t):
    return get_display(arabic_reshaper.reshape(t))

def font(sz, bold=True):
    return ImageFont.truetype(FONT_B if bold else FONT_R, sz)

def vgrad(w, h, c1, c2):
    img = Image.new("RGB", (w, h), c1)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        c = tuple(int(c1[i] + (c2[i]-c1[i])*t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img

def ctext(d, cx, y, text, fnt, fill, anchor="mm"):
    d.text((cx, y), ar(text), font=fnt, fill=fill, anchor=anchor)

def star(d, cx, cy, r, fill, points=5):
    pts = []
    for i in range(points*2):
        ang = math.pi/2 + i*math.pi/points
        rad = r if i % 2 == 0 else r*0.42
        pts.append((cx + rad*math.cos(ang), cy - rad*math.sin(ang)))
    d.polygon(pts, fill=fill)

def heart(d, cx, cy, s, fill):
    d.ellipse([cx-s, cy-s*0.6, cx, cy+s*0.2], fill=fill)
    d.ellipse([cx, cy-s*0.6, cx+s, cy+s*0.2], fill=fill)
    d.polygon([(cx-s*0.92, cy-0.05*s), (cx+s*0.92, cy-0.05*s), (cx, cy+s*0.95)], fill=fill)

def rrect(d, box, r, fill):
    d.rounded_rectangle(box, radius=r, fill=fill)

# ---------- 1) الأيقونة 512 ----------
def make_icon():
    img = vgrad(512, 512, PURPLE_TOP, PURPLE_BOT)
    d = ImageDraw.Draw(img)
    star(d, 256, 250, 180, GOLD)
    # وجه مبتسم
    d.ellipse([212, 215, 240, 243], fill=(93, 64, 55))
    d.ellipse([272, 215, 300, 243], fill=(93, 64, 55))
    d.arc([216, 250, 296, 320], start=20, end=160, fill=(93, 64, 55), width=10)
    img.save(os.path.join(OUT, "icon_512.png"))

# ---------- 2) الغلاف 1024x500 ----------
def make_feature():
    img = vgrad(1024, 500, PURPLE_TOP, PURPLE_BOT)
    d = ImageDraw.Draw(img)
    star(d, 200, 250, 150, GOLD)
    d.ellipse([158, 220, 182, 244], fill=(93,64,55))
    d.ellipse([218, 220, 242, 244], fill=(93,64,55))
    d.arc([165, 255, 235, 315], start=20, end=160, fill=(93,64,55), width=8)
    ctext(d, 640, 200, "تحدي الصغار", font(86), WHITE)
    ctext(d, 640, 290, "ألعاب أسئلة تعليمية لكل الأعمار", font(44, False), GOLD)
    ctext(d, 640, 360, "٤٨٤ سؤالاً • ٩٥ مرحلة • ٤ فئات عمرية", font(34, False), WHITE)
    img.save(os.path.join(OUT, "feature_graphic_1024x500.png"))

W, H = 1080, 1920
def base():
    img = vgrad(W, H, PURPLE_TOP, PURPLE_BOT)
    return img, ImageDraw.Draw(img)

def big_btn(d, y, text, fill=GOLD, tc=DEEP, h=130):
    rrect(d, [120, y, W-120, y+h], 40, fill)
    ctext(d, W//2, y+h//2, text, font(52), tc)

# ---------- لقطة 1: الرئيسية ----------
def shot_home():
    img, d = base()
    star(d, W//2, 430, 210, GOLD)
    d.ellipse([W//2-70, 390, W//2-26, 434], fill=(93,64,55))
    d.ellipse([W//2+26, 390, W//2+70, 434], fill=(93,64,55))
    d.arc([W//2-62, 440, W//2+62, 540], start=20, end=160, fill=(93,64,55), width=12)
    ctext(d, W//2, 720, "تحدي الصغار", font(96), WHITE)
    ctext(d, W//2, 820, "تعلّم وأنت تلعب عبر مراحل ممتعة!", font(42, False), GOLD)
    big_btn(d, 1050, "ابدأ اللعب")
    big_btn(d, 1230, "متابعة (مرحلة 5)", fill=(156,39,176), tc=WHITE)
    star(d, W//2-130, 1520, 34, GOLD)
    ctext(d, W//2+30, 1520, "أفضل نتيجة: 142", font(40, False), WHITE)
    img.save(os.path.join(OUT, "screenshot_1_home.png"))

# ---------- لقطة 2: اختيار العمر ----------
def shot_age():
    img, d = base()
    ctext(d, W//2, 220, "اختر الفئة العمرية", font(62), WHITE)
    groups = ["٣ - ٥ سنوات", "٦ - ٨ سنوات", "٩ - ١٢ سنة", "العائلة والكبار"]
    y = 420
    for g in groups:
        rrect(d, [110, y, W-110, y+230], 40, WHITE)
        ctext(d, W//2, y+115, g, font(56), DEEP)
        y += 300
    img.save(os.path.join(OUT, "screenshot_2_ages.png"))

# ---------- لقطة 3: سؤال ----------
def shot_quiz():
    img, d = base()
    ctext(d, 230, 150, "المرحلة 4", font(40), GOLD, anchor="mm")
    ctext(d, 230, 210, "المهارة", font(44), WHITE, anchor="mm")
    for i in range(3):
        heart(d, W-360 + i*120, 180, 36, RED)
    # شريط الوقت
    rrect(d, [120, 300, W-120, 326], 13, (255,255,255,90))
    rrect(d, [120, 300, 760, 326], 13, GOLD)
    ctext(d, W//2, 380, "سؤال 2 / 5    النقاط: 38", font(34, False), WHITE)
    # بطاقة السؤال
    rrect(d, [90, 440, W-90, 760], 50, WHITE)
    star(d, W//2, 560, 70, GOLD)
    ctext(d, W//2, 690, "ما عاصمة اليابان؟", font(58), DEEP)
    opts = [("بكين", WHITE, DEEP), ("طوكيو", GREEN, WHITE), ("سيول", WHITE, DEEP), ("بانكوك", WHITE, DEEP)]
    y = 850
    for txt, bg, tc in opts:
        rrect(d, [120, y, W-120, y+135], 36, bg)
        ctext(d, W//2, y+67, txt, font(50), tc)
        y += 165
    d.rounded_rectangle([120, 1560, W-120, 1670], radius=36, outline=GOLD, width=5)
    ctext(d, W//2, 1615, "مساعدة (احذف إجابات خاطئة)", font(40, False), GOLD)
    img.save(os.path.join(OUT, "screenshot_3_quiz.png"))

# ---------- لقطة 4: اجتياز مرحلة ----------
def shot_clear():
    img, d = base()
    random.seed(7)
    cols = [GOLD, RED, GREEN, (33,150,243), WHITE]
    for _ in range(120):
        x = random.randint(0, W); yy = random.randint(0, H)
        s = random.randint(10, 26)
        d.rectangle([x, yy, x+s, yy+s*0.6], fill=random.choice(cols))
    star(d, W//2, 480, 150, GOLD)
    ctext(d, W//2, 760, "أحسنت! اجتزت المرحلة", font(70), WHITE)
    for i in range(3):
        star(d, W//2-150+i*150, 920, 60, GOLD)
    ctext(d, W//2, 1060, "النقاط الكلية: 38", font(48, False), GOLD)
    big_btn(d, 1250, "المرحلة التالية")
    big_btn(d, 1430, "شارك تقدّمي", fill=(156,39,176), tc=WHITE)
    img.save(os.path.join(OUT, "screenshot_4_clear.png"))

# ---------- لقطة 5: الفوز والمشاركة ----------
def shot_win():
    img, d = base()
    random.seed(3)
    cols = [GOLD, RED, GREEN, (33,150,243), WHITE]
    for _ in range(90):
        x = random.randint(0, W); yy = random.randint(0, 700)
        s = random.randint(10, 24)
        d.rectangle([x, yy, x+s, yy+s*0.6], fill=random.choice(cols))
    star(d, W//2, 430, 170, GOLD)
    ctext(d, W//2, 720, "بطل تحدي الصغار!", font(74), WHITE)
    rrect(d, [200, 820, W-200, 1120], 50, WHITE)
    ctext(d, W//2, 900, "نتيجتك النهائية", font(40, False), (120,120,120))
    ctext(d, W//2, 1000, "112 / 120", font(96), DEEP)
    big_btn(d, 1250, "شارك فوزك مع أصدقائك")
    big_btn(d, 1430, "العب من جديد", fill=(156,39,176), tc=WHITE)
    img.save(os.path.join(OUT, "screenshot_5_win.png"))

make_icon()
make_feature()
shot_home()
shot_age()
shot_quiz()
shot_clear()
shot_win()
print("تم توليد كل الأصول في:", OUT)
for f in sorted(os.listdir(OUT)):
    print(" -", f)
