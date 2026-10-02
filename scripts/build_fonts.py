#!/usr/bin/env python3
"""앱에 넣을 명조 폰트를 만든다 (Lora · Noto Serif KR · JP · TC, 모두 OFL).

- 가변 폰트에서 필요한 굵기 두 가지(Medium 500, SemiBold 600)만 고정 폰트로 뽑는다.
- Noto Serif KR은 23MB라, 자주 쓰는 한글 2,350자(KS X 1001)와 문구(design/strings.json) · 명언(data/quotes.csv) · 질문(data/questions.csv)에 나오는 글자만 남긴다.
- 앱은 담긴 글자 목록(SerifCoverage.kt)을 보고, 한 글자라도 없으면 그 문장 전체를 기본 글꼴로 쓴다 (글자마다 글꼴이 섞이지 않게).
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
    "NotoSerifJP": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoserifjp/NotoSerifJP%5Bwght%5D.ttf",
    "NotoSerifTC": "https://raw.githubusercontent.com/google/fonts/main/ofl/notoseriftc/NotoSerifTC%5Bwght%5D.ttf",
}
FAMILY = {"Lora": "Lora", "NotoSerifKR": "Noto Serif KR", "NotoSerifJP": "Noto Serif JP", "NotoSerifTC": "Noto Serif TC"}
# 일본어 · 번체 중국어는 크기를 아끼려 한 굵기 (Medium) 만. 자주 쓰는 한자 (JIS 1수준 · Big5 상용) + 앱의 글자
ONE_WEIGHT = {"NotoSerifJP", "NotoSerifTC"}
CJK_PUNCT = "".join(chr(c) for c in range(0x3000, 0x3040)) + "".join(chr(c) for c in range(0xFF01, 0xFF5F))


def jp_base() -> str:
    kana = "".join(chr(c) for c in range(0x3041, 0x3097)) + "".join(chr(c) for c in range(0x30A1, 0x30FB)) + "ー・"
    level1 = "".join(bytes([hi, lo]).decode("euc-jp", "ignore") for hi in range(0xB0, 0xD0) for lo in range(0xA1, 0xFF))  # JIS 1수준 2,965자
    return kana + level1 + CJK_PUNCT


def tc_base() -> str:
    common = "".join(bytes([hi, lo]).decode("big5", "ignore") for hi in range(0xA4, 0xC7) for lo in list(range(0x40, 0x7F)) + list(range(0xA1, 0xFF)))  # Big5 상용 5,401자
    return common + CJK_PUNCT
WEIGHTS = {"Medium": 500, "SemiBold": 600}


def used_text() -> str:
    text = "".join((ROOT / "data" / f).read_text(encoding="utf-8") for f in ("quotes.csv", "questions.csv", "constellations.csv"))
    for lang in json.loads((ROOT / "design" / "strings.json").read_text(encoding="utf-8")).values():
        text += "".join(lang.values())
    ksx1001 = "".join(bytes([hi, lo]).decode("euc-kr") for hi in range(0xB0, 0xC9) for lo in range(0xA1, 0xFF))  # 한글 2,350자
    jamo = "".join(chr(c) for c in range(0x3131, 0x3164))  # ㄱ … ㅣ
    ascii_ = "".join(chr(c) for c in range(0x20, 0x7F))
    return text + ksx1001 + jamo + ascii_ + "·—–‘’“”…"


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
            if key in ONE_WEIGHT and style != "Medium":
                continue
            font = instancer.instantiateVariableFont(TTFont(src), {"wght": weight})
            if key != "Lora":
                keep = text + {"NotoSerifJP": jp_base(), "NotoSerifTC": tc_base()}.get(key, "")
                opts = subset.Options(); opts.layout_features = ["*"]; opts.name_IDs = ["*"]
                sub = subset.Subsetter(opts); sub.populate(text=keep); sub.subset(font)
                if key in ONE_WEIGHT:
                    COVER[key] = "".join(sorted(chr(c) for c in font.getBestCmap() if c >= 0x3000))
            ps = f"{key}-{style}"
            rename(font, FAMILY[key], style, ps)
            path = OUT / f"{ps}.ttf"
            font.save(path)
            print(f"{path.relative_to(ROOT)}  {path.stat().st_size // 1024} KB")
            ANDROID_OUT.mkdir(parents=True, exist_ok=True)
            (ANDROID_OUT / f"{key.lower()}_{style.lower()}.ttf").write_bytes(path.read_bytes())
    write_coverage(text)


COVER: dict = {}


def write_coverage(text: str) -> None:
    """담긴 글자 목록을 앱 코드로 (design/SerifCoverage.kt): 한글 (KR), 일본어 가나 · 한자 (JP), 번체 한자 (TC)."""
    hangul = "".join(sorted({ch for ch in text if "가" <= ch <= "힣" or "ㄱ" <= ch <= "ㆎ"}))
    path = ROOT / "android/app/src/main/java/io/github/graviton94/carpediem/design/SerifCoverage.kt"
    path.write_text("// 자동 생성 파일 — scripts/build_fonts.py 를 실행하세요.\npackage io.github.graviton94.carpediem.design\n\n"
                    "/** 앱에 넣은 Noto Serif KR 이 가진 한글 글자. */\n"
                    f"internal const val SERIF_KR_CHARS = \"{hangul}\"\n"
                    "/** 앱에 넣은 Noto Serif JP · TC 가 가진 글자 (U+3000 이상: 가나 · 한자 · 전각 문장부호). */\n"
                    f"internal const val SERIF_JP_CHARS = \"{COVER.get('NotoSerifJP', '')}\"\n"
                    f"internal const val SERIF_TC_CHARS = \"{COVER.get('NotoSerifTC', '')}\"\n", encoding="utf-8")
    print(f"{path.relative_to(ROOT)}  한글 {len(hangul)}자 · JP {len(COVER.get('NotoSerifJP', ''))}자 · TC {len(COVER.get('NotoSerifTC', ''))}자")


if __name__ == "__main__":
    main()
