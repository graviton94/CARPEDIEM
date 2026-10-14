// 손그림 한지 시안 비교: 같은 물건을 지금 / 가는 손선 / 붓 손선 / 붓 손선 + 옅은 결 로 나란히 (앱에는 쓰지 않음)
// node scripts/preview_hand.js <출력.png>
const { chromium } = require("playwright");
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const read = (f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
const hanji = ["hanji_base.js", "hanji_haru.js", "hanji_garden.js", "hanji_cards.js", "hanji_hand.js", "hanji_export.js"];
const extra = `
  function drawCell(ctx, kind, key, style) { setHand(style > 0, style || 1); try {
    if (kind === "guest") guestSprite(ctx, key);
    else if (kind === "card") { ctx.translate(20, 7); ctx.scale(0.6, 0.6); PIECE[key](ctx); }
    else if (kind === "feather") handFeather(ctx);
    else if (kind === "star") handStar(ctx);
  } finally { setHand(false); } }
`;
const src = files.map(read).join("\n") + "\nvar HANJI = (function () {\n" + hanji.map(read).join("\n") + extra + "\nreturn { drawCell, ensureTex, setU };\n})();";
(async () => {
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { H: HANJI };\n})();" });
  const items = [["guest", "squirrel"], ["guest", "owl"], ["guest", "tit"], ["card", "tea"], ["card", "ginkgo_autumn"], ["card", "cherry_winter"], ["card", "cake"], ["star", ""]];
  const data = await page.evaluate((items) => {
    const H = window.ART.H, S = 5, cell = 40 * S, cols = 4, heads = ["지금", "① 가는 손선", "② 붓 손선", "③ 붓 손선 + 옅은 결"];
    const cv = document.createElement("canvas"); cv.width = cols * cell + 20; cv.height = items.length * cell + 50; const g = cv.getContext("2d");
    g.fillStyle = "#F6F1E6"; g.fillRect(0, 0, cv.width, cv.height); g.fillStyle = "#33281F"; g.font = "bold 22px sans-serif";
    heads.forEach((h, i) => g.fillText(h, 18 + i * cell, 34));
    items.forEach(([k, key], ri) => [0, 1, 2, 3].forEach((st) => {
      const x = 10 + st * cell, y = 50 + ri * cell;
      g.save(); g.fillStyle = (ri + st) % 2 ? "#EFE3CB" : "#F3EAD7"; g.fillRect(x, y, cell, cell); g.translate(x, y); g.scale(S, S); H.ensureTex(); H.setU(S);
      H.drawCell(g, k, key, st); g.restore();
    }));
    return cv.toDataURL("image/png");
  }, items);
  fs.writeFileSync(process.argv[2] || "hand.png", Buffer.from(data.split(",")[1], "base64"));
  await browser.close();
})();
