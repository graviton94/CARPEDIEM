#!/usr/bin/env python3
"""디자인 원본 파일에서 앱 코드를 만든다. 원본을 바꾼 뒤 실행한다.

  design/tokens.json   → ios/Shared/Design/Tokens.swift, android/app/.../design/Tokens.kt
  design/strings.json  → ios/Shared/Design/L10n.swift, ios/Shared/Resources/{ko,en}.lproj/Localizable.strings
                         android/app/src/main/res/values{,-ko}/strings.xml
  data/*.csv           → ios/Shared/Resources/, android/app/src/main/assets/ (앱에 그대로 포함)

    python3 scripts/generate.py          # 파일 생성
    python3 scripts/generate.py --check  # 생성 결과가 저장소와 같은지 확인 (CI)
"""
import json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
IOS = ROOT / "ios" / "Shared"
HEADER = "// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.\n"


def camel(key: str) -> str:
    parts = re.split(r"[.\-_]", key)
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])


def hexcolor(h: str) -> str:
    h = h.lstrip("#")
    if len(h) == 6:
        h += "FF"
    return "0x" + h.upper()


def tokens_swift(t: dict) -> str:
    out = [HEADER, "import SwiftUI\n", "enum Tokens {"]
    out.append("    enum Space {")
    for k, v in t["space"].items():
        out.append(f"        static let {k}: CGFloat = {v}")
    out.append("    }\n")

    lay = t["layout"]
    out.append("    enum Layout {")
    for cls, ref in lay["pageMargin"].items():
        out.append(f"        static let pageMargin{cls.capitalize()}: CGFloat = Space.{ref}")
    out.append(f"        static let cardPadding: CGFloat = Space.{lay['cardPadding']}")
    out.append(f"        static let widgetPadding: CGFloat = Space.{lay['widgetPadding']}")
    out.append(f"        static let tapTarget: CGFloat = {lay['tapTarget']['ios']}")
    out.append("    }\n")

    ios_style = {"largeTitle": ".largeTitle", "title2": ".title2", "title3": ".title3", "headline": ".headline", "body": ".body",
                 "callout": ".callout", "subheadline": ".subheadline", "footnote": ".footnote", "caption": ".caption1", "caption2": ".caption2"}
    out.append("    enum TypeScale {")
    for k, v in t["type"].items():
        if k.startswith("_"):
            continue
        fam = ".serif" if v["family"] == "serif" else ".text"
        w = "." + v["weight"]
        if k == "display":
            s = v["size"]
            out.append(f"        static func display(_ c: DeviceClass) -> TypeToken {{")
            out.append(f"            let size: CGFloat = c == .compact ? {s['compact']} : c == .large ? {s['large']} : {s['regular']}")
            out.append(f"            return TypeToken(size: size, style: .largeTitle, family: {fam}, weight: {w}, tracking: {v.get('tracking', 0)})")
            out.append("        }")
        else:
            out.append(f"        static let {k} = TypeToken(size: {v['size']}, style: {ios_style[v['ios']]}, family: {fam}, weight: {w}, tracking: 0)")
    out.append("    }\n")

    out.append("    enum Palette {")
    for k, v in t["color"].items():
        if k == "season":
            continue
        out.append(f"        static let {k} = DynamicColor(light: {hexcolor(v['light'])}, dark: {hexcolor(v['dark'])})")
    seasons = t["color"]["season"]
    out.append("        static let seasons: [DynamicColor] = [")
    for s in ("spring", "summer", "autumn", "winter"):
        out.append(f"            DynamicColor(light: {hexcolor(seasons[s]['light'])}, dark: {hexcolor(seasons[s]['dark'])}),")
    out.append("        ]")
    out.append("    }\n")

    r = t["radius"]
    out.append("    enum Radius {")
    out.append(f"        static let sm: CGFloat = {r['sm']}")
    out.append(f"        static let md: CGFloat = {r['md']}")
    out.append(f"        static let lg: CGFloat = {r['lg']['ios']}")
    out.append(f"        static let pill: CGFloat = {r['pill']}")
    out.append("    }\n")

    out.append("    enum Stroke {")
    for k, v in t["stroke"].items():
        out.append(f"        static let {k}: CGFloat = {v}")
    out.append("    }\n")

    e = t["effect"]
    out.append("    enum Effect {")
    out.append(f"        static let glowStrength: Double = {e['glowStrength']}")
    out.append(f"        static let glassOpacity: Double = {e['glassOpacity']}")
    out.append(f"        static let glassBlur: CGFloat = {e['glassBlur']}")
    out.append("    }\n")

    g = t["grid"]
    out.append("    enum Grid {")
    for k, v in g["columns"].items():
        out.append(f"        static let {k}Columns = {v}")
    out.append(f"        static let dotRatio: CGFloat = {g['dotRatio']}")
    out.append(f"        static let nowRatio: CGFloat = {g['nowRatio']}")
    out.append(f"        static let widgetMediumColumns = {g['widgetMediumColumns']}")
    out.append(f"        static let widgetMediumTextRatio: CGFloat = {g['widgetMediumTextRatio']}")
    out.append(f"        static let widgetLargeColumns = {g['widgetLargeColumns']}")
    out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"


PLACEHOLDER = re.compile(r"\{(\w+)\}")


def strings_outputs(s: dict) -> dict:
    ko, en = s["ko"], s["en"]
    if ko.keys() != en.keys():
        raise SystemExit(f"strings.json: 언어별 키가 다릅니다 {set(ko) ^ set(en)}")
    files = {}
    params = {}
    for key in ko:
        names = PLACEHOLDER.findall(ko[key])
        if sorted(names) != sorted(PLACEHOLDER.findall(en[key])):
            raise SystemExit(f"strings.json: '{key}' 의 자리표시자가 언어마다 다릅니다")
        params[key] = names
    for lang, table in s.items():
        lines = [f"/* {HEADER.strip()[3:]} */"]
        for key, value in table.items():
            names = params[key]
            v = PLACEHOLDER.sub(lambda m: f"%{names.index(m.group(1)) + 1}$@", value).replace('"', '\\"')
            lines.append(f'"{key}" = "{v}";')
        files[f"ios/Shared/Resources/{lang}.lproj/Localizable.strings"] = "\n".join(lines) + "\n"
    sw = [HEADER, "import Foundation\n", "enum L10n {"]
    for key, names in params.items():
        name = camel(key)
        if names:
            args = ", ".join(f"_ {n}: String" for n in names)
            call = ", ".join(names)
            sw.append(f"    static func {name}({args}) -> String {{ tr(\"{key}\", {call}) }}")
        else:
            sw.append(f"    static var {name}: String {{ tr(\"{key}\") }}")
    sw.append("")
    sw.append("    static let allKeys: [String] = [" + ", ".join(f'"{k}"' for k in params) + "]")
    sw.append("")
    sw.append("    private static func tr(_ key: String, _ args: String...) -> String {")
    sw.append("        let format = Bundle.main.localizedString(forKey: key, value: nil, table: nil)")
    sw.append("        return args.isEmpty ? format : String(format: format, arguments: args.map { $0 as NSString })")
    sw.append("    }")
    sw.append("}")
    files["ios/Shared/Design/L10n.swift"] = "\n".join(sw) + "\n"
    return files


# ───────────────────────── Android ─────────────────────────
KT_PKG = "io.github.graviton94.carpediem.design"
KT_DIR = "android/app/src/main/java/io/github/graviton94/carpediem/design"


def argb(h: str) -> str:
    h = h.lstrip("#")
    a = h[6:8] if len(h) == 8 else "FF"
    return "0x" + (a + h[:6]).upper()


def tokens_kotlin(t: dict) -> str:
    o = ["// 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요.", f"package {KT_PKG}", "",
         "import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.text.font.FontWeight",
         "import androidx.compose.ui.unit.dp", "import androidx.compose.ui.unit.sp", "", "object Tokens {"]
    o.append("    object Space {")
    for k, v in t["space"].items():
        o.append(f"        val {k} = {v}.dp")
    o.append("    }\n")
    lay = t["layout"]
    o.append("    object Layout {")
    for cls, ref in lay["pageMargin"].items():
        o.append(f"        val pageMargin{cls.capitalize()} = Space.{ref}")
    o.append(f"        val cardPadding = Space.{lay['cardPadding']}")
    o.append(f"        val widgetPadding = Space.{lay['widgetPadding']}")
    o.append(f"        val tapTarget = {lay['tapTarget']['android']}.dp")
    o.append("    }\n")
    weight = {"regular": "FontWeight.Normal", "medium": "FontWeight.Medium", "semibold": "FontWeight.SemiBold", "bold": "FontWeight.Bold"}
    o.append("    object TypeScale {")
    for k, v in t["type"].items():
        if k.startswith("_"):
            continue
        fam = "TypeToken.Family.Serif" if v["family"] == "serif" else "TypeToken.Family.Text"
        if k == "display":
            s = v["size"]
            o.append(f"        fun display(c: DeviceClass) = TypeToken(when (c) {{ DeviceClass.Compact -> {s['compact']}; DeviceClass.Large -> {s['large']}; else -> {s['regular']} }}.sp, {fam}, {weight[v['weight']]}, {v.get('tracking', 0)}f)")
        else:
            o.append(f"        val {k} = TypeToken({v['size']}.sp, {fam}, {weight[v['weight']]}, 0f)")
    o.append("    }\n")
    o.append("    object Palette {")
    for k, v in t["color"].items():
        if k == "season":
            continue
        o.append(f"        val {k} = DynamicColor(Color({argb(v['light'])}), Color({argb(v['dark'])}))")
    se = t["color"]["season"]
    o.append("        val seasons = listOf(" + ", ".join(f"DynamicColor(Color({argb(se[s]['light'])}), Color({argb(se[s]['dark'])}))" for s in ("spring", "summer", "autumn", "winter")) + ")")
    o.append("    }\n")
    r = t["radius"]
    o.append("    object Radius {")
    o.append(f"        val sm = {r['sm']}.dp")
    o.append(f"        val md = {r['md']}.dp")
    o.append(f"        val lg = {r['lg']['android']}.dp")
    o.append(f"        val pill = {r['pill']}.dp")
    o.append("    }\n")
    o.append("    object Stroke {")
    for k, v in t["stroke"].items():
        o.append(f"        val {k} = {v}.dp")
    o.append("    }\n")
    e = t["effect"]
    o.append("    object Effect {")
    o.append(f"        const val glowStrength = {e['glowStrength']}f")
    o.append(f"        const val glassOpacity = {e['glassOpacity']}f")
    o.append("    }\n")
    g = t["grid"]
    o.append("    object Grid {")
    for k, v in g["columns"].items():
        o.append(f"        const val {k}Columns = {v}")
    o.append(f"        const val dotRatio = {g['dotRatio']}f")
    o.append(f"        const val nowRatio = {g['nowRatio']}f")
    o.append(f"        const val widgetMediumColumns = {g['widgetMediumColumns']}")
    o.append(f"        const val widgetMediumTextRatio = {g['widgetMediumTextRatio']}f")
    o.append(f"        const val widgetLargeColumns = {g['widgetLargeColumns']}")
    o.append("    }")
    o.append("}")
    return "\n".join(o) + "\n"


def xml_escape(v: str) -> str:
    v = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\\", "\\\\")
    v = v.replace("'", "\\'").replace('"', '\\"')
    if v[:1] in ("@", "?"):
        v = "\\" + v
    return v


def android_strings(s: dict) -> dict:
    files = {}
    params = {k: PLACEHOLDER.findall(v) for k, v in s["ko"].items()}
    for lang, folder in (("en", "values"), ("ko", "values-ko")):
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- 자동 생성 파일 — 직접 고치지 말고 scripts/generate.py 를 실행하세요. -->", "<resources>"]
        for key, value in s[lang].items():
            names = params[key]
            v = xml_escape(value)
            v = PLACEHOLDER.sub(lambda m: f"%{names.index(m.group(1)) + 1}$s", v)
            lines.append(f'    <string name="{key.replace(".", "_")}">{v}</string>')
        lines.append("</resources>")
        files[f"android/app/src/main/res/{folder}/strings.xml"] = "\n".join(lines) + "\n"
    return files


def android_outputs(tokens: dict, strings: dict) -> dict:
    files = {f"{KT_DIR}/Tokens.kt": tokens_kotlin(tokens)}
    files.update(android_strings(strings))
    for name in ("quotes.csv", "life-expectancy.csv"):
        files[f"android/app/src/main/assets/{name}"] = (ROOT / "data" / name).read_text(encoding="utf-8")
    return files


def main() -> None:
    check = "--check" in sys.argv
    tokens = json.loads((ROOT / "design/tokens.json").read_text(encoding="utf-8"))
    strings = json.loads((ROOT / "design/strings.json").read_text(encoding="utf-8"))
    files = {"ios/Shared/Design/Tokens.swift": tokens_swift(tokens)}
    files.update(strings_outputs(strings))
    for name in ("quotes.csv", "life-expectancy.csv"):
        files[f"ios/Shared/Resources/{name}"] = (ROOT / "data" / name).read_text(encoding="utf-8")
    files.update(android_outputs(tokens, strings))
    stale = []
    for rel, content in files.items():
        path = ROOT / rel
        if check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                stale.append(rel)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
            print("생성:", rel)
    if check and stale:
        raise SystemExit("생성 파일이 원본과 다릅니다. python3 scripts/generate.py 를 실행하세요:\n  " + "\n  ".join(stale))
    if check:
        print("생성 파일이 최신입니다.")


if __name__ == "__main__":
    main()
