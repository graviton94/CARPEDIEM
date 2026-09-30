// design/art/src 의 손그림 코드로 정원 그림을 파일로 굽습니다. 그림체: 크레용 파스텔 (mix.js).
// 사용법: npm i playwright (또는 전역 설치) 후  node scripts/build_art.js
// 결과
//   design/art/                       : 보기용 (obj_*.png · season_*.jpg · theme_*.jpg · donation.jpg · store_bg.jpg)
//   android/app/src/main/assets/garden: 앱용 (sky_* · strip_* · obj_* · sun · moon · sparkle · tooth_* · haru.html/haru.js)
// 하루(조약돌)는 번호마다 달라서 굽지 않습니다. 앱이 haru.html 로 처음 한 번 그려 저장합니다.
const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
const src = files.map((f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8")).join("\n");
const out = path.join(root, "design/art");
const app = path.join(root, "android/app/src/main/assets/garden");

(async () => {
  fs.mkdirSync(app, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { OBJ, SEASONS: SEASONS_MIX, THEMES, TOOTH, objCanvas, gardenScene: gardenMix, theme, donation: donationMix, store: storeMix, skyArt, stripArt, discArt, sparkleArt };\n})();" });
  const jobs = await page.evaluate(() => {
    const A = window.ART, list = [];
    function make(dir, name, w, h, type, draw) { const cv = document.createElement("canvas"); cv.width = w; cv.height = h; draw(cv); list.push({ dir, name, data: cv.toDataURL(type, 0.86) }); }
    A.OBJ.forEach((o) => make("both", "obj_" + o.id + ".png", 512, 512, "image/png", (cv) => A.objCanvas(cv, o, null)));
    Object.keys(A.SEASONS).forEach((k) => {
      make("art", "season_" + k + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.gardenScene(cv.getContext("2d"), 1170, 2532, k, null, { items: [] }));
      make("app", "sky_" + k + ".jpg", 1170, 2800, "image/jpeg", (cv) => A.skyArt(cv.getContext("2d"), 1170, 2800, k));
      make("app", "strip_" + k + ".png", 1170, 600, "image/png", (cv) => A.stripArt(cv.getContext("2d"), 1170, 600, k));
    });
    A.THEMES.forEach((t) => make("art", "theme_" + t.id + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.theme(cv.getContext("2d"), 1170, 2532, t)));
    make("art", "donation.jpg", 1800, 1200, "image/jpeg", (cv) => A.donation(cv.getContext("2d"), 1800, 1200, null));
    make("art", "store_bg.jpg", 1290, 2796, "image/jpeg", (cv) => A.store(cv.getContext("2d"), 1290, 2796));
    make("app", "sun.png", 180, 180, "image/png", (cv) => A.discArt(cv.getContext("2d"), 180, "#F3C66A", 902));
    make("app", "moon.png", 180, 180, "image/png", (cv) => A.discArt(cv.getContext("2d"), 180, "#EFE8D2", 904));
    make("app", "sparkle.png", 120, 120, "image/png", (cv) => A.sparkleArt(cv.getContext("2d"), 120));
    make("app", "tooth_line.png", 256, 256, "image/png", (cv) => cv.getContext("2d").drawImage(A.TOOTH.masks[3], 0, 0));
    make("app", "tooth_fill.png", 256, 256, "image/png", (cv) => cv.getContext("2d").drawImage(A.TOOTH.masks[4], 0, 0));
    make("app", "paper.png", 256, 256, "image/png", (cv) => cv.getContext("2d").drawImage(A.TOOTH.paper, 0, 0));
    return list;
  });
  for (const j of jobs) {
    const buf = Buffer.from(j.data.split(",")[1], "base64");
    if (j.dir !== "app") fs.writeFileSync(path.join(out, j.name), buf);
    if (j.dir !== "art") fs.writeFileSync(path.join(app, j.name), buf);
  }
  // 앱이 WebView 로 하루를 그릴 때 쓰는 페이지
  fs.writeFileSync(path.join(app, "haru.js"), "// 자동 생성 파일 — scripts/build_art.js\nwindow.exportHaru = (function () {\n" + src + "\nreturn exportHaru;\n})();\n");
  fs.writeFileSync(path.join(app, "haru.html"), '<!doctype html><meta charset="utf-8"><body></body><script src="haru.js"></script>\n');
  console.log(jobs.length + " files → design/art/ · android assets/garden/");
  await browser.close();
})();
