// 디자인 방향 비교용 (앱에는 쓰지 않음): 손님 · 깃털 · 별을 지금 / 한지만 / 한지 + 먹선 (콜라보) 로 나란히 구워 한 장으로.
// node scripts/preview_style.js <출력.png>
const { chromium } = require("playwright");
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const read = (f) => fs.readFileSync(path.join(root, "design/art/src", f), "utf8");
const files = ["engine.js", "pastel.js", "objects.js", "scenes.js", "concepts.js", "mix.js", "appexport.js"];
const hanji = ["hanji_base.js", "hanji_haru.js", "hanji_garden.js", "hanji_cards.js", "hanji_hand.js", "hanji_export.js"];
const extra = `
  var V = {};
  var _piece = gPiece, _eye = gEye;
  function inkPart(ctx, pts, w, a) { ctx.save(); gPath(ctx, pts); ctx.strokeStyle = "rgba(51,40,31," + a + ")"; ctx.lineWidth = w; ctx.lineJoin = "round"; ctx.setLineDash([9, 2.2]); ctx.stroke(); ctx.restore(); }
  V.hanji = { piece: function (ctx, pts, col, o) { o = o || {}; paper(ctx, pts, col, 7000 + pts.length * 13, { rim: 0.5, sh: 0.6 }); if (o.inner) { ctx.save(); gPath(ctx, pts); ctx.clip(); o.inner(ctx); ctx.restore(); } },
    eye: function (ctx, x, y, r) { ctx.fillStyle = "#2A221C"; ctx.beginPath(); ctx.arc(x, y + r * 0.1, r * 0.42, 0, 7); ctx.fill(); ctx.fillStyle = "rgba(255,255,255,.8)"; ctx.beginPath(); ctx.arc(x - r * 0.14, y - r * 0.08, r * 0.12, 0, 7); ctx.fill(); } };
  V.collab = { piece: function (ctx, pts, col, o) { o = o || {}; paper(ctx, pts, col, 7000 + pts.length * 13, { rim: 0.45, sh: 0.55 }); if (o.inner) { ctx.save(); gPath(ctx, pts); ctx.clip(); o.inner(ctx); ctx.restore(); } inkPart(ctx, pts, 1.15, 0.72); },
    eye: function (ctx, x, y, r, lx) { var s = GEYE; GEYE = 0.8; _eye(ctx, x, y, r * 0.8, lx); GEYE = s; } };
  function drawGuest(ctx, kind, style) { if (style === "now") return guestSprite(ctx, kind); gPiece = V[style].piece; gEye = V[style].eye; try { guestSprite(ctx, kind); } finally { gPiece = _piece; gEye = _eye; } }
  function feather(ctx, style) { var pts = place(leafPts(26, 5.4), 7, 30, 1, -0.7);
    if (style === "hanji") paper(ctx, pts, "#E9E2D6", 7100, { rim: 0.6, sh: 0.6, light: 0.9 }); else if (style === "collab") { paper(ctx, pts, "#E9E2D6", 7100, { rim: 0.55, sh: 0.55, light: 0.9 }); inkPart(ctx, pts, 1.1, 0.7); }
    thread(ctx, [[7, 30], [26, 11]], 0.7, "rgba(90,74,58,.7)"); }
  function star(ctx, style) { var p = []; for (var i = 0; i < 8; i++) { var a = -Math.PI / 2 + i * Math.PI / 4, r = i % 2 ? 4 : 11; p.push([20 + Math.cos(a) * r, 20 + Math.sin(a) * r]); }
    if (style === "hanji") paper(ctx, p, "#F2D27A", 7200, { rim: 0.4, sh: 0.4, light: 0.9 }); else { paper(ctx, p, "#F2D27A", 7200, { rim: 0.4, sh: 0.4, light: 0.9 }); inkPart(ctx, p, 1, 0.6); } }
`;
const src = files.map(read).join("\n") + "\nvar HANJI = (function () {\n" + hanji.map(read).join("\n") + extra + "\nreturn { drawGuest, feather, star, hj, ensureTex, setU };\n})();";
(async () => {
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const page = await browser.newPage();
  await page.setContent("<body></body>");
  await page.addScriptTag({ content: "window.ART = (function () {\n" + src + "\nreturn { H: HANJI, OBJ, objCanvas };\n})();" });
  const data = await page.evaluate(() => {
    const H = window.ART.H, S = 6, cell = 40 * S, cols = ["now", "hanji", "collab"], rows = ["tit", "squirrel", "owl", "hedgehog", "feather", "star"];
    const cv = document.createElement("canvas"); cv.width = cell * 3 + 20; cv.height = cell * rows.length + 60; const g = cv.getContext("2d");
    g.fillStyle = "#F6F1E6"; g.fillRect(0, 0, cv.width, cv.height);
    g.fillStyle = "#33281F"; g.font = "bold 26px sans-serif"; ["지금 (먹선 스티커)", "한지만", "한지 + 먹선 (콜라보)"].forEach((t, i) => g.fillText(t, 20 + i * cell, 40));
    rows.forEach((r, ri) => cols.forEach((c, ci) => {
      const x = 10 + ci * cell, y = 60 + ri * cell;
      g.save(); g.fillStyle = ri % 2 ? "#EFE3CB" : "#F2E8D4"; g.fillRect(x, y, cell, cell); g.translate(x, y); g.scale(S, S); H.ensureTex(); H.setU(S);
      if (r === "feather" || r === "star") { if (c === "now") { /* 지금 그림은 따로 붙임 */ } else (r === "feather" ? H.feather : H.star)(g, c); }
      else H.drawGuest(g, r, c);
      g.restore();
    }));
    return cv.toDataURL("image/png");
  });
  fs.writeFileSync(process.argv[2] || "style.png", Buffer.from(data.split(",")[1], "base64"));
  await browser.close();
})();
