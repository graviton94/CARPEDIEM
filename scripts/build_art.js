// design/art/src 의 손그림 코드로 정원 그림을 파일로 굽습니다. 그림체: 크레용 파스텔 (mix.js).
// 사용법: npm i playwright (또는 전역 설치) 후  node scripts/build_art.js
// 결과: design/art/obj_*.png (놓이는 것, 투명) · season_*.jpg · theme_*.jpg · donation.jpg · store_bg.jpg
// 하루(조약돌)는 번호마다 달라서 굽지 않습니다. 배경에는 하루와 놓이는 것을 넣지 않습니다.
const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const src = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js"].map((f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8")).join("\n");
const out = path.join(root, "design/art");

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { OBJ, SEASONS: SEASONS_MIX, THEMES, objCanvas, gardenScene: gardenMix, theme, donation: donationMix, store: storeMix };\n})();" });
  const jobs = await page.evaluate(() => {
    const A = window.ART, list = [];
    function make(name, w, h, type, draw) { const cv = document.createElement("canvas"); cv.width = w; cv.height = h; draw(cv); list.push({ name, data: cv.toDataURL(type, 0.86) }); }
    A.OBJ.forEach((o) => make("obj_" + o.id + ".png", 512, 512, "image/png", (cv) => A.objCanvas(cv, o, null)));
    Object.keys(A.SEASONS).forEach((k) => make("season_" + k + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.gardenScene(cv.getContext("2d"), 1170, 2532, k, null, { items: [] })));
    A.THEMES.forEach((t) => make("theme_" + t.id + ".jpg", 1170, 2532, "image/jpeg", (cv) => A.theme(cv.getContext("2d"), 1170, 2532, t)));
    make("donation.jpg", 1800, 1200, "image/jpeg", (cv) => A.donation(cv.getContext("2d"), 1800, 1200, null));
    make("store_bg.jpg", 1290, 2796, "image/jpeg", (cv) => A.store(cv.getContext("2d"), 1290, 2796));
    return list;
  });
  for (const j of jobs) fs.writeFileSync(path.join(out, j.name), Buffer.from(j.data.split(",")[1], "base64"));
  console.log(jobs.length + " files → design/art/");
  await browser.close();
})();
