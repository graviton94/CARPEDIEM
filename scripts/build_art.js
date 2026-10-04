// design/art/src 의 손그림 코드로 정원 그림을 파일로 굽습니다.
// 정원 (하늘 · 땅 · 자리 여섯 · 계절 한 장 · 해 · 달) = 한지 (hanji_*.js), 옛 꾸밈 obj_* 과 응원 · 테마 그림 = 크레용 파스텔 (mix.js).
// 사용법: npm i playwright (또는 전역 설치) 후  node scripts/build_art.js
// 결과
//   design/art/                       : 보기용 (obj_*.png · season_*.jpg · theme_*.jpg · donation.jpg · store_bg.jpg)
//   android/app/src/main/assets/garden: 앱용 (sky_* · strip_* · tree_* · post_* · moss_* · kite · card_* · sun · moon · fiber · obj_* · sparkle · tooth_*)
//   상자 크기 · 기준점은 design/tokens.json garden.decor 를 읽어 그대로 씀 (앱과 같은 값)
// 하루(조약돌)는 번호마다 달라서 굽지 않습니다. 앱이 벡터로 직접 그립니다 (android/core HaruShape · ui/garden/Haru.kt).
const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
// 한지 그림은 따로 묶음 안에: 옛 크레용 엔진과 이름이 겹쳐도 (paper · sun · ground …) 서로 덮어쓰지 않게
const hanji = ["hanji_base.js", "hanji_haru.js", "hanji_garden.js", "hanji_cards.js", "hanji_export.js"];
const tokens = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "design/tokens.json"), "utf8")).garden;
const D = Object.assign({}, tokens.decor, { stripLineY: tokens.layout.stripLineY, stripHeight: tokens.layout.stripHeight });
const read = (f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8");
const src = files.map(read).join("\n") + "\nvar HANJI = (function () {\n" + hanji.map(read).join("\n") +
  "\nreturn { skyApp, stripApp, treeApp, postApp, mossApp, kiteApp, cardApp, sunApp, moonApp, moonFullApp, fiberApp, rainbowApp, auroraApp, snailApp, flyApp, guestApp, windApp, kiteFoldApp, pondApp, logsApp, cloudApp, gardenH, setU, TREES, REAL, CARDS: Object.keys(PIECE) };\n})();";
const out = path.join(root, "design/art");
const app = path.join(root, "android/app/src/main/assets/garden");

(async () => {
  fs.mkdirSync(app, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { OBJ, SEASONS: SEASONS_MIX, THEMES, TOOTH, objCanvas, gardenScene: gardenMix, theme, donation: donationMix, store: storeMix, sparkleArt, H: HANJI };\n})();" });
  const jobs = await page.evaluate((D) => {
    const A = window.ART, H = A.H, list = [];
    function make(dir, name, w, h, type, draw, q) { const cv = document.createElement("canvas"); cv.width = w; cv.height = h; try { draw(cv); } catch (e) { throw new Error(name + ": " + e.stack); } list.push({ dir, name, data: cv.toDataURL(type, q || 0.86) }); }
    const webp = (name, bw, bh, px, draw) => make("app", name + ".webp", Math.round(bw * px), Math.round(bh * px), "image/webp", (cv) => draw(cv.getContext("2d"), Math.round(bw * px)), 0.88);
    // 한지 정원: 하늘 · 땅은 실제 계절 넷
    H.REAL.forEach((se) => {
      make("app", "sky_" + se + ".jpg", 1170, 2800, "image/jpeg", (cv) => H.skyApp(cv.getContext("2d"), 1170, 2800, se));
      make("app", "strip_" + se + ".webp", 1170, Math.round(1170 * D.stripHeight / 390), "image/webp", (cv) => H.stripApp(cv.getContext("2d"), 1170, cv.height, se, D), 0.86);
      // 나무: 종류 × 계절 × 자람 (0 새싹 · 1 어린 나무 · 2 나무 · 3 큰 나무와 그네)
      H.TREES.forEach((sp) => [0, 1, 2, 3].forEach((st) => webp("tree_" + sp + "_" + se + "_" + st, D.treeBoxW, D.treeBoxH, D.treePx, (c, w) => H.treeApp(c, w, sp, se, st, D))));
      webp("post_" + se, D.postBoxW, D.postBoxH, D.postPx, (c, w) => H.postApp(c, w, "base", se, D));
      [0, 1, 2, 3, 4, 5].forEach((n) => webp("moss_" + se + "_" + n, D.mossBoxW, D.mossBoxH, D.mossPx, (c, w) => H.mossApp(c, w, se, n, D)));
    });
    ["chime", "bell", "lantern", "lantern_lit", "letter"].forEach((part) => webp("post_" + part, D.postBoxW, D.postBoxH, D.postPx, (c, w) => H.postApp(c, w, part, null, D)));
    webp("kite", D.kiteBoxW, D.kiteBoxH, D.kitePx, (c, w) => H.kiteApp(c, w, D));
    webp("kite_folded", D.postBoxW, D.postBoxH, D.postPx, (c, w) => H.kiteFoldApp(c, w, D));
    webp("gaze_pond", D.pondBoxW, D.pondBoxH, 3, (c, w) => H.pondApp(c, w, D));
    H.REAL.concat(["night"]).forEach((k) => [0, 1].forEach((i) => webp("cloud_" + k + "_" + i, 120, 50, 3, (c, w) => H.cloudApp(c, w, k, i))));
    webp("gaze_logs", D.fireBoxW, D.fireBoxH, 4, (c, w) => H.logsApp(c, w, D));
    H.CARDS.forEach((k) => webp("card_" + k, D.cardBoxW, D.cardBoxH, D.cardPx, (c, w) => H.cardApp(c, w, k, D)));
    make("app", "sun.png", 180, 180, "image/png", (cv) => H.sunApp(cv.getContext("2d"), 180));
    make("app", "moon.png", 180, 180, "image/png", (cv) => H.moonApp(cv.getContext("2d"), 180));
    make("app", "moon_full.png", 180, 180, "image/png", (cv) => H.moonFullApp(cv.getContext("2d"), 180));
    make("app", "fiber.png", 320, 320, "image/png", (cv) => H.fiberApp(cv.getContext("2d")));
    // 우연한 순간
    webp("moment_rainbow", 160, 90, 3, (c, w) => H.rainbowApp(c, w));
    webp("moment_aurora", 260, 60, 3, (c, w) => H.auroraApp(c, w));
    webp("moment_snail", 40, 26, 4, (c, w) => H.snailApp(c, w));
    ["tit", "squirrel", "hedgehog", "rabbit", "owl"].forEach((k) => webp("guest_" + k, 40, 40, 4, (c, w) => H.guestApp(c, w, k)));
    [["wing_l", -1], ["wing_r", 1], ["body", 0]].forEach(([n, side]) => webp("fly_" + n, 40, 40, 3, (c, w) => H.flyApp(c, w, side)));
    H.REAL.forEach((se) => webp("wind_" + se, 10, 10, 6, (c, w) => H.windApp(c, w, se)));
    // 보기용: 사계절 · 밤 정원 한 장
    [["spring", {}], ["summer", { lantern: true, letter: true }], ["autumn", { lantern: true }], ["winter", { lantern: true }], ["night", { night: true, lantern: true }]].forEach(([n, o]) =>
      make("art", "hanji_" + n + ".jpg", 780, 1400, "image/jpeg", (cv) => { const c = cv.getContext("2d"); H.setU(2); H.gardenH(c, Object.assign({ season: n === "night" ? "summer" : n, kite: "gaori", ribbons: 3, buds: 3, chime: 2, keep: true }, o)); }));
    A.OBJ.forEach((o) => make("both", "obj_" + o.id + ".png", 512, 512, "image/png", (cv) => A.objCanvas(cv, o, null)));
    A.THEMES.forEach((t) => make("art", "theme_" + t.id + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.theme(cv.getContext("2d"), 1170, 2532, t)));
    make("art", "donation.jpg", 1800, 1200, "image/jpeg", (cv) => A.donation(cv.getContext("2d"), 1800, 1200, null));
    make("art", "store_bg.jpg", 1290, 2796, "image/jpeg", (cv) => A.store(cv.getContext("2d"), 1290, 2796));
    // 응원하기 그림 (하루는 앱이 가운데에 얹는다: 땅 = 높이 × layout.supportGround, 폭 = 너비 × layout.supportHaru)
    make("app", "support.jpg", 1170, 780, "image/jpeg", (cv) => A.donation(cv.getContext("2d"), 1170, 780, null));
    make("app", "sparkle.png", 120, 120, "image/png", (cv) => A.sparkleArt(cv.getContext("2d"), 120));
    make("app", "tooth_line.png", 256, 256, "image/png", (cv) => cv.getContext("2d").drawImage(A.TOOTH.masks[3], 0, 0));
    make("app", "tooth_fill.png", 256, 256, "image/png", (cv) => cv.getContext("2d").drawImage(A.TOOTH.masks[4], 0, 0));
    make("app", "paper.png", 256, 256, "image/png", (cv) => cv.getContext("2d").drawImage(A.TOOTH.paper, 0, 0));
    return list;
  }, D);
  for (const j of jobs) {
    const buf = Buffer.from(j.data.split(",")[1], "base64");
    if (j.dir !== "app") fs.writeFileSync(path.join(out, j.name), buf);
    if (j.dir !== "art") fs.writeFileSync(path.join(app, j.name), buf);
  }
  // 앱이 WebView 로 하루를 그릴 때 쓰는 페이지
  console.log(jobs.length + " files → design/art/ · android assets/garden/");
  await browser.close();
})();
