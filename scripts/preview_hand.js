// 한지 + 크레용 손그림 (1.1.5 그림체) 비교용: 같은 그림을 지금 / 손그림 한지로 나란히 구워 한 장으로. 앱에는 쓰지 않음.
// node scripts/preview_hand.js <출력.png>
const { chromium } = require("playwright");
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const read = (f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
const hanji = ["hanji_base.js", "hanji_haru.js", "hanji_garden.js", "hanji_cards.js", "hanji_hand.js", "hanji_export.js"];
const extra = `
  function drawCell(ctx, kind, key, on) { HAND = on; try {
    if (kind === "guest") guestSprite(ctx, key);
    else if (kind === "card") { ctx.translate(20, 6); ctx.scale(0.62, 0.62); PIECE[key](ctx); }
    else if (kind === "hat") { ctx.translate(20, 8); ctx.scale(0.9, 0.9); partyHat(ctx); }
    else if (kind === "cake") { ctx.translate(20, 10); ctx.scale(0.8, 0.8); partyCake(ctx); }
    else if (kind === "feather") handFeather(ctx);
    else if (kind === "star") handStar(ctx);
  } finally { HAND = false; } }
`;
const src = files.map(read).join("\n") + "\nvar HANJI = (function () {\n" + hanji.map(read).join("\n") + extra + "\nreturn { drawCell, ensureTex, setU };\n})();";
(async () => {
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { H: HANJI };\n})();" });
  const items = [["guest", "tit"], ["guest", "squirrel"], ["guest", "hedgehog"], ["guest", "rabbit"], ["guest", "owl"], ["card", "tea"], ["card", "coffee"], ["card", "cake"], ["card", "pebble"], ["card", "slip"],
    ["card", "cherry_spring"], ["card", "zelkova_summer"], ["card", "ginkgo_autumn"], ["card", "cherry_winter"], ["card", "pine_summer"], ["hat", ""], ["cake", ""], ["feather", ""], ["star", ""]];
  const data = await page.evaluate((items) => {
    const H = window.ART.H, S = 5, cell = 40 * S, perRow = 5;
    const rows = Math.ceil(items.length / perRow);
    const cv = document.createElement("canvas"); cv.width = perRow * cell * 2 + 40; cv.height = rows * cell + 50; const g = cv.getContext("2d");
    g.fillStyle = "#F6F1E6"; g.fillRect(0, 0, cv.width, cv.height);
    g.fillStyle = "#33281F"; g.font = "bold 24px sans-serif"; g.fillText("지금", 20, 34); g.fillText("손그림 한지 (제안)", perRow * cell + 40, 34);
    items.forEach(([k, key], i) => [false, true].forEach((on) => {
      const x = (on ? perRow * cell + 30 : 10) + (i % perRow) * cell, y = 50 + Math.floor(i / perRow) * cell;
      g.save(); g.fillStyle = (Math.floor(i / perRow) + i) % 2 ? "#EFE3CB" : "#F3EAD7"; g.fillRect(x, y, cell, cell); g.translate(x, y); g.scale(S, S); H.ensureTex(); H.setU(S);
      H.drawCell(g, k, key, on); g.restore();
    }));
    return cv.toDataURL("image/png");
  }, items);
  fs.writeFileSync(process.argv[2] || "hand.png", Buffer.from(data.split(",")[1], "base64"));
  await browser.close();
})();
