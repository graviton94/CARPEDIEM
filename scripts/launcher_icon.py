#!/usr/bin/env python3
"""build_launcher_icon.js 가 그린 층을 앱 아이콘으로 넣는다.

단색 (테마 아이콘 · 알림) 은 돌 모양을 하얗게, 두 눈은 동그란 구멍 + 눈동자로 다시 그린다.
    python3 scripts/launcher_icon.py OUT_DIR
"""
import sys, pathlib, shutil
from PIL import Image, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "android/app/src/main/res/mipmap-xxxhdpi"
src_dir = pathlib.Path(sys.argv[1])

src = Image.open(src_dir / "mono_src_432.png").convert("RGBA")
r, g, b, a = src.split()
lum = Image.merge("RGB", (r, g, b)).convert("L")
solid = a.point(lambda v: 255 if v > 140 else 0).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
hole = lum.point(lambda v: 255 if v > 228 else 0).filter(ImageFilter.MaxFilter(11)).filter(ImageFilter.MinFilter(7))
bb = hole.getbbox()
cols = [sum(1 for y in range(bb[1], bb[3]) if hole.getpixel((x, y))) for x in range(bb[0], bb[2])]
gaps = [i for i, c in enumerate(cols) if c == 0]
split = bb[0] + (gaps[len(gaps) // 2] if gaps else (bb[2] - bb[0]) // 2)
out = solid.copy()
d = ImageDraw.Draw(out)
for x0, x1 in ((bb[0], split), (split, bb[2])):
    e = hole.crop((x0, bb[1], x1, bb[3])).getbbox()
    ex0, ey0, ex1, ey1 = e[0] + x0, e[1] + bb[1], e[2] + x0, e[3] + bb[1]
    cx, cy, rr = (ex0 + ex1) / 2, (ey0 + ey1) / 2, max(ex1 - ex0, ey1 - ey0) / 2
    pts = [(x, y) for x in range(int(ex0), int(ex1)) for y in range(int(ey0), int(ey1)) if lum.getpixel((x, y)) < 45 and a.getpixel((x, y)) > 200]
    px, py = sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts)
    d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), fill=0)
    pr = rr * 0.5
    d.ellipse((px - pr, py - pr, px + pr, py + pr), fill=255)
mono = Image.new("RGBA", src.size, (255, 255, 255, 0))
mono.putalpha(out)
mono.save(RES / "ic_launcher_monochrome.png")
shutil.copy(src_dir / "fg_432.png", RES / "ic_launcher_foreground.png")
shutil.copy(src_dir / "bg_432.png", RES / "ic_launcher_background.png")
shutil.copy(src_dir / "play_512.png", ROOT / "docs/store/icon-512.png")
print("아이콘을 넣었어요:", RES, "· docs/store/icon-512.png")
