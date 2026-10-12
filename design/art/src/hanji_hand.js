  /* 손그림 한지 (1.1.5): 세상의 물건 · 손님은 한지 조각 위에 크레용으로 한 번 더 칠하고, 그 색보다 진한 크레용 선으로 손으로 따라 그린 듯.
     HAND = true 일 때 one() (계절 조각 · 소품 · 생일) 과 손님 조각이 이 방식으로. 하늘 · 땅 · 나무 같은 세상 바탕은 그대로 한지.
     모양은 손으로 오린 듯 살짝 흔들림 (organic), 칠은 결 따라 짧은 크레용 획 (pastel), 선은 두 번 그은 연필 (pencil). */
  var HAND = false;
  function setHand(v) { HAND = !!v; }
  function handOne(ctx, pts, col, seed, o) {
    o = o || {};
    var polys = typeof pts[0][0] === "number" ? [pts] : pts;
    polys = polys.map(function (p, i) { return p.length > 4 ? organic(p, o.wob == null ? 0.28 : o.wob, seed + i * 7, 1.1) : p; });
    paper(ctx, polys, col, seed, Object.assign({ rim: 0.5, sh: 0.6 }, o, { tone: 0, raw: true }));
    LAYER_CTM = true;
    try { polys.forEach(function (p, i) {
      if (o.crayon !== false && p.length > 4) pastel(ctx, p, sh(col, 0.02), { seed: seed + i, clip: true, line: false, smudge: 0, press: 0.42, dark: 0.2, light: 0.16, spread: 0.7 });
      // 무늬 (머리 · 배 · 뺨): 크레용 위에 덧댄 한지처럼, 위에 옅은 크레용 결만 한 번 더
      if (o.inner) { ctx.save(); multi(ctx, [p]); ctx.clip(); o.inner(ctx); texFill(ctx, TEX.fiber, 0.35, "source-atop", 0.4); ctx.restore(); }
      if (o.ink !== false) pencil(ctx, p, sh(col, -0.5), seed + i, o.lw || 0.5, { alpha: 0.85 });
    }); } finally { LAYER_CTM = false; }
  }
  // 옛 크레용 꾸밈 (obj_*) 을 같은 모양 그대로 손그림 한지로: 칠 (pastel) 을 한지 조각 + 크레용으로 바꿔 그림. 상자 200, 땅 (100, 168), 배율 1.3 (objCanvas 와 같음)
  function toHex(c) { if (typeof c === "string") return c; return "#" + c.slice(0, 3).map(function (v) { return ("0" + Math.round(v).toString(16)).slice(-2); }).join(""); }
  function objHand(cv, o) { ensureTex(); var ctx = cv.getContext("2d"); ctx.clearRect(0, 0, cv.width, cv.height); setU(cv.width / 200);
    ctx.setTransform(U, 0, 0, U, 0, 0); LAYER_CTM = true; var n = 0;
    PASTEL_HOOK = function (c, pts, col, po) { var h = PASTEL_HOOK; PASTEL_HOOK = null; try { handOne(c, pts, toHex(C(col)), 8100 + (n++) * 17, { rim: 0.6, sh: 0.7, lw: 0.6, ink: !(po && po.line === false) }); } finally { PASTEL_HOOK = h; } };
    // 먹선은 하루에게만: 옛 그림의 짙은 연필 테두리는 가늘고 따뜻한 갈색 크레용 선으로
    PENCIL_HOOK = function (c, pts, col, seed, w, po) { var x = C(col), dark = x[0] + x[1] + x[2] < 260; return pencil(c, pts, dark ? "#7A6550" : col, seed, (w || 1) * (dark ? 0.55 : 0.8), po); };
    var line0 = CRAYON_LINE; CRAYON_LINE = "#7A6550"; INK_SCALE = 0.5;
    try { o.draw(ctx, 100, 168, 1.3); } finally { CRAYON_LINE = line0; INK_SCALE = 1; PASTEL_HOOK = null; PENCIL_HOOK = null; LAYER_CTM = false; ctx.setTransform(1, 0, 0, 1, 0, 0); } }
  // 먹 점 눈 (손님): 작은 먹 점 + 빛 한 점
  function handEye(ctx, x, y, r) { ctx.fillStyle = "#2A221C"; ctx.beginPath(); ctx.arc(x, y + r * 0.1, r * 0.4, 0, 7); ctx.fill(); ctx.fillStyle = "rgba(255,255,255,.85)"; ctx.beginPath(); ctx.arc(x - r * 0.13, y - r * 0.06, r * 0.12, 0, 7); ctx.fill(); }
  function handFeather(ctx) { var pts = place(leafPts(26, 5.4), 7, 32, 1, -0.75); handOne(ctx, pts, "#E9E2D6", 7101, { light: 0.9 });
    LAYER_CTM = true; try { pencil(ctx, [[7, 32], [25, 13]], "#8A7A66", 7102, 0.6, { open: true }); } finally { LAYER_CTM = false; } }
  function handStar(ctx) { var p = []; for (var i = 0; i < 8; i++) { var a = -Math.PI / 2 + i * Math.PI / 4, r = i % 2 ? 4.2 : 11; p.push([20 + Math.cos(a) * r, 20 + Math.sin(a) * r]); } handOne(ctx, p, "#F2D27A", 7201, { wob: 0.18 }); }
