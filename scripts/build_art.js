// design/art/src 의 그리기 코드로 정원 에셋을 파일로 굽습니다.
// 사용법: npm i playwright (또는 전역 설치) 후  node scripts/build_art.js
// 결과: design/art/*.png (소품 · 유리병, 투명) · design/art/*.jpg (배경)
const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const src = ["engine.js", "paint.js", "scenes.js", "simple.js"].map((f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8")).join("\n");
const out = path.join(root, "design/art");

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { PROPS, SEASONS, THEMES, propCanvas, season, theme, donation, store, jar };\n})();" });
  const jobs = await page.evaluate(() => {
    const A = window.ART, list = [];
    function make(name, w, h, type, draw) { const cv = document.createElement("canvas"); cv.width = w; cv.height = h; draw(cv); list.push({ name, data: cv.toDataURL(type, 0.85) }); }
    A.PROPS.forEach((p) => make("prop_" + p.id + ".png", 512, 512, "image/png", (cv) => A.propCanvas(cv, p)));
    make("jar.png", 600, 750, "image/png", (cv) => A.jar(cv.getContext("2d"), 600, 750, null, "summer"));
    Object.keys(A.SEASONS).forEach((k) => make("season_" + k + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.season(cv.getContext("2d"), 1170, 2532, k)));
    A.THEMES.forEach((t) => make("theme_" + t.id + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.theme(cv.getContext("2d"), 1170, 2532, t)));
    make("donation.jpg", 1800, 1200, "image/jpeg", (cv) => A.donation(cv.getContext("2d"), 1800, 1200, null));
    make("store_bg.jpg", 1290, 2796, "image/jpeg", (cv) => A.store(cv.getContext("2d"), 1290, 2796));
    return list;
  });
  for (const j of jobs) fs.writeFileSync(path.join(out, j.name), Buffer.from(j.data.split(",")[1], "base64"));
  console.log(jobs.length + " files → design/art/");
  await browser.close();
})();
