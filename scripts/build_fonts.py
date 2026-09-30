#!/usr/bin/env python3
"""앱에 넣을 명조 폰트를 만든다 (Lora · Noto Serif KR, 둘 다 OFL).

- 가변 폰트에서 필요한 굵기 두 가지(Medium 500, SemiBold 600)만 고정 폰트로 뽑는다.
- Noto Serif KR은 23MB라, 문구(design/strings.json)와 명언(data/quotes.csv)에 나오는 글자만 남긴다.
  문구나 명언을 바꾸면 다시 실행한다.

    pip install fonttools
    python3 scripts/build_fonts.py
"""
import json, pathlib, urllib.request
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = pathlib.Path(__file__).resolve().parents[1]
CACHE = ROOT / ".cache"
OUT = ROOT / "ios" / "Shared" / "Resources" / "Fonts"
ANDROID_OUT = ROOT / "android" / "app" / "src" / "main" / "res" / "font"  # 안드로이드는 소문자 · 밑줄 이름
SOURCES = {
    "Lora": "https://raw.githubusercontent.com/google/fonts/main/ofl/lora/Lora%5Bwght%5D.ttf",
    "NotoSerifKR": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoserifkr/NotoSerifKR%5Bwght%5D.ttf",
}
FAMILY = {"Lora": "Lora", "NotoSerifKR": "Noto Serif KR"}
WEIGHTS = {"Medium": 500, "SemiBold": 600}


def used_text() -> str:
    text = (ROOT / "data" / "quotes.csv").read_text(encoding="utf-8")
    for lang in json.loads((ROOT / "design" / "strings.json").read_text(encoding="utf-8")).values():
        text += "".join(lang.values())
    return text + "0123456789.,·—–‘’“”'\"()[]?!:;%/ -"


def rename(font: TTFont, family: str, style: str, ps: str) -> None:
    name = font["name"]
    name.names = [n for n in name.names if n.nameID not in (1, 2, 3, 4, 6, 16, 17)]
    for nid, value in {1: f"{family} {style}", 2: "Regular", 3: ps, 4: f"{family} {style}", 6: ps, 16: family, 17: style}.items():
        name.setName(value, nid, 3, 1, 0x409)
        name.setName(value, nid, 1, 0, 0)
    font["OS/2"].usWeightClass = WEIGHTS[style]


def main() -> None:
    CACHE.mkdir(exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    text = used_text()
    for key, url in SOURCES.items():
        src = CACHE / f"{key}-variable.ttf"
        if not src.exists():
            urllib.request.urlretrieve(url, src)
        for style, weight in WEIGHTS.items():
            font = instancer.instantiateVariableFont(TTFont(src), {"wght": weight})
            if key == "NotoSerifKR":
                opts = subset.Options(); opts.layout_features = ["*"]; opts.name_IDs = ["*"]
                sub = subset.Subsetter(opts); sub.populate(text=text); sub.subset(font)
            ps = f"{key}-{style}"
            rename(font, FAMILY[key], style, ps)
            path = OUT / f"{ps}.ttf"
            font.save(path)
            print(f"{path.relative_to(ROOT)}  {path.stat().st_size // 1024} KB")
            ANDROID_OUT.mkdir(parents=True, exist_ok=True)
            (ANDROID_OUT / f"{key.lower()}_{style.lower()}.ttf").write_bytes(path.read_bytes())


if __name__ == "__main__":
    main()
