#!/usr/bin/env python3
"""나라별 0세 기대수명을 받아 data/life-expectancy.csv 로 저장한다.

1순위: UN World Population Prospects 2024 (Demographic Indicators, Medium)
2순위: World Bank API (UN WPP를 바탕으로 한 같은 지표) — UN 서버가 응답하지 않을 때만

    python3 scripts/fetch_life_expectancy.py            # 2023년 값
    python3 scripts/fetch_life_expectancy.py --year 2024
"""
import argparse, csv, gzip, io, json, pathlib, sys, urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "data" / "life-expectancy.csv"
WPP_URLS = [
    "https://population.un.org/wpp/assets/Excel%20Files/1_Indicator%20(Standard)/CSV_FILES/WPP2024_Demographic_Indicators_Medium.csv.gz",
    "https://population.un.org/wpp/Download/Files/1_Indicator%20(Standard)/CSV_FILES/WPP2024_Demographic_Indicators_Medium.csv.gz",
]
WB = "https://api.worldbank.org/v2/country/all/indicator/{ind}?format=json&per_page=20000&date={year}"
UA = {"User-Agent": "carpediem-data-fetch/1.0"}


def get(url: str) -> bytes:
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120) as r:
        return r.read()


def from_wpp(year: int):
    last = None
    for url in WPP_URLS:
        try:
            raw = get(url)
        except Exception as e:  # 다음 주소 시도
            last = e
            continue
        text = gzip.decompress(raw).decode("utf-8-sig")
        rows = []
        for r in csv.DictReader(io.StringIO(text)):
            if r.get("Variant", "Medium") != "Medium" or r["Time"] != str(year):
                continue
            kind = r.get("LocTypeName", "")
            code = "WLD" if kind == "World" else r.get("ISO2_code", "").strip()
            if kind not in ("World", "Country/Area") or not code:
                continue
            rows.append((code, r["Location"], float(r["LEx"]), float(r["LExMale"]), float(r["LExFemale"])))
        if rows:
            return rows, f"UN World Population Prospects 2024, {year}년 (Medium), {url}"
    raise RuntimeError(f"UN WPP 받기 실패: {last}")


def from_world_bank(year: int):
    def series(ind):
        meta, data = json.loads(get(WB.format(ind=ind, year=year)))
        return {d["country"]["id"]: (d["countryiso3code"], d["country"]["value"], d["value"]) for d in data if d["value"] is not None}
    total, male, female = (series(i) for i in ("SP.DYN.LE00.IN", "SP.DYN.LE00.MA.IN", "SP.DYN.LE00.FE.IN"))
    countries = json.loads(get("https://api.worldbank.org/v2/country?format=json&per_page=400"))[1]
    real = {c["iso2Code"] for c in countries if c["region"]["value"] != "Aggregates"}
    rows = []
    for iso2, (iso3, name, t) in total.items():
        if iso2 not in male or iso2 not in female:
            continue
        if iso2 == "1W":
            code = "WLD"
        elif iso2 in real:
            code = iso2
        else:
            continue
        rows.append((code, name, t, male[iso2][2], female[iso2][2]))
    return rows, f"World Bank (UN WPP 기반) SP.DYN.LE00.IN / .MA.IN / .FE.IN, {year}년"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--year", type=int, default=2023)
    year = ap.parse_args().year
    try:
        rows, source = from_wpp(year)
    except Exception as e:
        print(f"UN WPP 실패, World Bank 로 대체: {e}", file=sys.stderr)
        rows, source = from_world_bank(year)
    rows.sort(key=lambda r: (r[0] != "WLD", r[0]))
    with OUT.open("w", encoding="utf-8", newline="") as f:
        f.write("# 0세 기대수명 (년). 열: code(ISO 3166-1 alpha-2, 세계=WLD), 영문 이름, 전체, 남성, 여성\n")
        f.write(f"# 출처: {source}\n")
        f.write("# 나라 이름은 앱에서 기기 언어로 표시하고, en 열은 참고용이다. 다시 받으려면: python3 scripts/fetch_life_expectancy.py\n")
        w = csv.writer(f)
        w.writerow(["code", "en", "total", "male", "female"])
        for code, name, t, m, fe in rows:
            w.writerow([code, name, f"{t:.1f}", f"{m:.1f}", f"{fe:.1f}"])
    print(f"{len(rows)}개 나라·지역 저장 ({source})")
    kr = next((r for r in rows if r[0] == "KR"), None)
    print("KR:", kr)


if __name__ == "__main__":
    main()
