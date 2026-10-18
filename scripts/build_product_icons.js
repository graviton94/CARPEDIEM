// Play 일회성 제품 (응원하기) 아이콘 셋: support_tea · support_coffee · support_cake, 512 × 512 PNG.
// 앱 안의 그림과 같은 한지 조각 (design/art/src hanji_cards.js 의 tea · coffee · cake, 붓 손선) 을 한지 바탕 가운데에 크게.
// 사용법: node scripts/build_product_icons.js  → docs/store/products/*.png
const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const read = (f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
const hanji = ["hanji_base.js", "hanji_haru.js", "hanji_garden.js", "hanji_cards.js", "hanji_collect.js", "hanji_hand.js", "hanji_export.js"];
// 조각 하나: 원점 = 그림의 땅 가운데 (땅 y + 24), 김 · 장식까지 위아래 약 33 단위. 52 단위 정사각형 가운데에
const icon = `
  function iconApp(ctx, W, key) {
    ensureTex(); ctx.fillStyle = "#F3ECDF"; ctx.fillRect(0, 0, W, W);
    var pat = ctx.createPattern(TEX.fiber, "repeat"); ctx.globalAlpha = 0.55; ctx.fillStyle = pat; ctx.fillRect(0, 0, W, W); ctx.globalAlpha = 1;
    var g = ctx.createRadialGradient(W / 2, W * 0.55, W * 0.1, W / 2, W * 0.55, W * 0.62); g.addColorStop(0, "rgba(255,250,240,.55)"); g.addColorStop(1, "rgba(150,120,90,.16)"); ctx.fillStyle = g; ctx.fillRect(0, 0, W, W);
    ctx.save(); setU(W / 52); hj(ctx, W / 52); setHand(true, 3); try { piece(ctx, 26, 18.5, 1, key); } finally { setHand(false); } ctx.restore();
  }`;
const CONSTEL = {};
const src = files.map(read).join("\n") + "\nvar HANJI = (function () {\nvar CONSTEL = " + JSON.stringify(CONSTEL) + ";\n" + hanji.map(read).join("\n") + icon + "\nreturn { iconApp };\n})();";
const out = path.join(root, "docs/store/products");

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { H: HANJI };\n})();" });
  const imgs = await page.evaluate(() => ["tea", "coffee", "cake"].map((k) => {
    const cv = document.createElement("canvas"); cv.width = 512; cv.height = 512;
    window.ART.H.iconApp(cv.getContext("2d"), 512, k);
    return [k, cv.toDataURL("image/png")];
  }));
  imgs.forEach(([k, d]) => fs.writeFileSync(path.join(out, "support_" + k + ".png"), Buffer.from(d.split(",")[1], "base64")));
  await browser.close();
  console.log("support_tea · support_coffee · support_cake → docs/store/products/");
})();
