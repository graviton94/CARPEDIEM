#!/usr/bin/env python3
"""앱 아이콘 ‘엔소 · 궤도’를 그린다 (기본 · 다크 · 틴트, 1024px).

가늘고 온전한 원 = 영원, 굵고 열린 붓길 = 한 번뿐인 삶, 열린 자리의 한 점 = 오늘.
단색 면만 쓴다 (그라디언트·유리 없음).

    pip install pillow
    python3 scripts/build_icon.py
"""
import math, pathlib
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "ios" / "App" / "Assets.xcassets" / "AppIcon.appiconset"
SS = 4  # 가장자리를 매끄럽게 하려고 4배로 그린 뒤 줄인다
PALETTES = {
    "AppIcon": {"bg": (231, 230, 219, 255), "ink": (95, 114, 54, 255), "soft": (169, 176, 131, 255), "dot": (217, 138, 43, 255)},
    "AppIcon-Dark": {"bg": (17, 19, 13, 255), "ink": (164, 184, 106, 255), "soft": (86, 97, 58, 255), "dot": (245, 180, 92, 255)},
    "AppIcon-Tinted": {"bg": (0, 0, 0, 255), "ink": (255, 255, 255, 255), "soft": (115, 115, 115, 255), "dot": (255, 255, 255, 255)},
}
CX, CY, R = 512, 522, 300


def draw(p: dict, keep_alpha: bool = False) -> Image.Image:
    s = SS
    img = Image.new("RGBA", (1024 * s, 1024 * s), p["bg"])
    d = ImageDraw.Draw(img)
    # 영원: 가는 원
    w = 22 * s
    d.ellipse([(CX - R) * s - w / 2, (CY - R) * s - w / 2, (CX + R) * s + w / 2, (CY + R) * s + w / 2], outline=p["soft"], width=w)
    # 삶: 굵기가 줄어드는 붓길
    pts, n = [], 200
    start, span = -math.pi * 0.28, math.pi * 1.6
    for i in range(n + 1):
        a = start + span * i / n
        pts.append((CX + math.cos(a) * R, CY + math.sin(a) * R))
    left, right = [], []
    for i, (x, y) in enumerate(pts):
        x1, y1 = pts[min(i + 1, n)]
        x0, y0 = pts[max(i - 1, 0)]
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy) or 1
        width = (112 + (30 - 112) * (i / n) ** 1.4) / 2
        nx, ny = -dy / ln * width, dx / ln * width
        left.append(((x + nx) * s, (y + ny) * s))
        right.append(((x - nx) * s, (y - ny) * s))
    d.polygon(left + right[::-1], fill=p["ink"])
    for (x, y), r in ((pts[0], 56), (pts[-1], 15)):
        d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=p["ink"])
    # 오늘: 열린 자리의 한 점
    a = -math.pi * 0.48
    x, y, r = CX + math.cos(a) * R, CY + math.sin(a) * R, 44
    d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=p["dot"])
    out = img.resize((1024, 1024), Image.LANCZOS)
    return out if keep_alpha else out.convert("RGB")


ANDROID_RES = ROOT / "android" / "app" / "src" / "main" / "res"


def android_layer(p: dict, transparent_bg: bool) -> Image.Image:
    """적응형 아이콘 전경 (108dp, 가운데 72dp 안전 영역). 1024 그림을 66%로 줄여 가운데 둔다."""
    art = draw({**p, "bg": (0, 0, 0, 0)} if transparent_bg else p, keep_alpha=True)
    canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    inner = art.resize((int(432 * 0.66), int(432 * 0.66)), Image.LANCZOS)
    canvas.paste(inner, ((432 - inner.width) // 2, (432 - inner.height) // 2), inner)
    return canvas


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, palette in PALETTES.items():
        draw(palette).save(OUT / f"{name}.png")
        print(f"{(OUT / name).relative_to(ROOT)}.png")
    mip = ANDROID_RES / "mipmap-xxxhdpi"
    mip.mkdir(parents=True, exist_ok=True)
    android_layer(PALETTES["AppIcon"], True).save(mip / "ic_launcher_foreground.png")
    mono = {"bg": (0, 0, 0, 0), "ink": (255, 255, 255, 255), "soft": (255, 255, 255, 110), "dot": (255, 255, 255, 255)}
    android_layer(mono, True).save(mip / "ic_launcher_monochrome.png")
    print("android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_{foreground,monochrome}.png")


if __name__ == "__main__":
    main()
