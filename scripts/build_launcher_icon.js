// 앱 아이콘 (시안 A · 검은 하루): 한지 그림 엔진으로 적응형 아이콘 층 (앞 · 뒤 · 단색 원본) 과 Play 512 · 1024 를 그린다.
// 사용법: NODE_PATH=... node scripts/build_launcher_icon.js OUT_DIR  →  python3 scripts/launcher_icon.py OUT_DIR (앱 res 와 docs/store 로 넣음)
const { chromium } = require("playwright");
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
const hanji = ["hanji_base.js", "hanji_haru.js", "hanji_garden.js", "hanji_cards.js", "hanji_export.js"];
const read = (f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8");
const icon = `
function iconLayer(ctx, W, layer, seed) {
  // 적응형 아이콘 108 × 108 단위: 보이는 원은 가운데 72, 시안 A 를 0.72 배로
  hj(ctx, W / 108);
  if (layer === "bg") { ctx.fillStyle = "#F3EDE0"; ctx.fillRect(0, 0, 108, 108); texFill(ctx, TEX.fiber, 0.5, "source-atop", 0.4); texFill(ctx, TEX.grain, 0.3, "multiply", 0.5); return; }
  ctx.save(); ctx.translate(54, 54); ctx.scale(0.86, 0.86); ctx.translate(-50, -54);
  if (layer !== "mono") moss(ctx, 50, 70, 1.05, "spring", 2); stoneH(ctx, HV, seed, 50, 69, 46, { lineScale: 1.15 }); ctx.restore();
}
function iconDraw(ctx, W, kind, seed) {
  hj(ctx, W / 100);
  var bg = { a: "#F3EDE0", b: "#1C2236", c: "#EFE6D2", d: "#F3EDE0" }[kind];
  ctx.fillStyle = bg; ctx.fillRect(0, 0, 100, 100);
  if (kind === "b") NIGHT = true;
  if (kind === "a") { moss(ctx, 50, 70, 1.05, "spring", 2); stoneH(ctx, HV, seed, 50, 69, 46, { lineScale: 1.15 }); }
  if (kind === "b") { stars(ctx, 6, 91, 100, 46); moon(ctx, 70, 24, 0.75); moss(ctx, 50, 72, 0.95, "summer", 0); stoneH(ctx, HV, seed, 50, 71, 40, { night: true, lineScale: 1.1 }); }
  if (kind === "c") { sun(ctx, 71, 27, 0.62, "#F2B65A"); var hill = []; for (var x = -4; x <= 104; x += 4) hill.push([x, 70 + Math.sin(x * 0.07) * 2]); hill.push([104, 104], [-4, 104]); HV.world(ctx, hill, "#C9B98E", 777, { kind: "layer", top: hill.slice(0, hill.length - 2) }); stoneH(ctx, HV, seed, 46, 71, 40, { lineScale: 1.1 }); }
  if (kind === "d") { var ring = [], inner = [], i; for (i = 0; i <= 64; i++) { var a = i / 64 * 2 * Math.PI, w = 1 + 0.12 * Math.sin(a * 3 + 1); ring.push([50 + Math.cos(a) * 38, 50 + Math.sin(a) * 38]); inner.push([50 + Math.cos(-a) * (38 - 7 * w), 50 + Math.sin(-a) * (38 - 7 * w)]); }
    HV.world(ctx, ring.concat(inner), "#6E8A4A", 4242, {}); stoneH(ctx, HV, seed, 50, 64, 34, { lineScale: 1.05 }); }
  NIGHT = false;
}
`;
const src = files.map(read).join("\n") + "\nvar HANJI = (function () {\n" + hanji.map(read).join("\n") + icon + "\nreturn { iconDraw: iconDraw, iconLayer: iconLayer };\n})();";
(async () => {
  const b = await chromium.launch(); const p = await b.newPage(); await p.setContent("<body></body>");
  await p.addScriptTag({ content: "window.ART=(function(){\n" + src + "\nreturn {H: HANJI};})();" });
  const out = await p.evaluate(() => { const r = {}, S = 4254103021;
    const mk = (n, w, f) => { const cv = document.createElement("canvas"); cv.width = cv.height = w; f(cv.getContext("2d"), w); r[n] = cv.toDataURL("image/png"); };
    mk("fg_432", 432, (c, w) => window.ART.H.iconLayer(c, w, "fg", S));
    mk("mono_src_432", 432, (c, w) => window.ART.H.iconLayer(c, w, "mono", S));
    mk("bg_432", 432, (c, w) => window.ART.H.iconLayer(c, w, "bg", S));
    mk("play_512", 512, (c, w) => window.ART.H.iconDraw(c, w, "a", S));
    mk("play_1024", 1024, (c, w) => window.ART.H.iconDraw(c, w, "a", S));
    return r; });
  for (const [k, v] of Object.entries(out)) fs.writeFileSync(path.join(process.argv[2], k + ".png"), Buffer.from(v.split(",")[1], "base64"));
  await b.close();
})();
