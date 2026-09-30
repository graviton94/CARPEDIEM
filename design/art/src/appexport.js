  /* ───────── 앱용 그림 굽기 ─────────
     앱은 움직이는 것(해 · 달 · 하루의 눈동자와 깜빡임)을 직접 그리므로, 그림을 부분별로 굽는다.
     sky: 종이 + 하늘빛 (해 없음) · strip: 땅빛 + 땅 한 줄 + 양 끝 눈금 + 풀 (투명) · sun · moon · sparkle · 종이 결 타일 */
  var APP = { unit: 390, stripLineY: 40, stripHeight: 200, pathStart: 26, pathEnd: 364, haruArtWidth: 70 };  // design/tokens.json garden.layout 과 같은 값
  function skyArt(ctx, W, H, key) { setU(W / APP.unit); var S = SEASONS_MIX[key]; paper(ctx, W, H, "#F6F1E6"); wash(ctx, -60, -120, 510, 700, S.sky, { seed: 501, press: 0.4, smudge: 0.32, fade: "down" }); }
  function stripArt(ctx, W, H, key) {
    setU(W / APP.unit); var S = SEASONS_MIX[key], gy = APP.stripLineY;
    wash(ctx, -60, gy - 30, 510, APP.stripHeight - gy + 60, S.band, { seed: 502, press: 0.42, smudge: 0.34, fade: "up", len: 50, wid: 10 });
    groundLine(ctx, 14, 376, gy + 2, 3.2, 507, [60, 300, 344]);
    [APP.pathStart, APP.pathEnd].forEach(function (x, i) { ink(ctx, [[x, gy - 5], [x, gy + 9]], 2.4, CRAYON_LINE, { open: true, press: 0.8, seed: 610 + i }); });
  }
  function discArt(ctx, W, col, seed) { setU(W / 60); var p = blob(30, 30, 22, 22, seed, 0.04); FILL.mix(ctx, p, col, { seed: seed }); ink(ctx, p, 2.6, CRAYON_LINE, { press: 0.74, passes: 2, seed: seed + 1 }); }
  function sparkleArt(ctx, W) { setU(W / 40); FILL.mix(ctx, star4(20, 20, 17), "#F2C04E", { seed: 970 }); }
  function toothArt(ctx, which) { ctx.drawImage(which, 0, 0); }
  // 하루: 몸(눈 없음)과 눈 흰자를 따로. 좌표는 200 × 200 칸, 땅은 y = 170
  function exportHaru(seed, px, sprout) {
    var t = traitsOf(seed >>> 0), a = document.createElement("canvas"), b = document.createElement("canvas");
    a.width = a.height = b.width = b.height = px;
    setU(px / 200); var hs = drawHaruMix(a.getContext("2d"), t, 100, 170, APP.haruArtWidth, { sprout: !!sprout, parts: "body" });
    setU(px / 200); drawHaruMix(b.getContext("2d"), t, 100, 170, APP.haruArtWidth, { parts: "eyes" });
    var bb = bbox(hs.pts);
    return JSON.stringify({ body: a.toDataURL("image/png"), eyes: b.toDataURL("image/png"), box: 200, ground: 170, stone: t.stone.id,
      eye: hs.eyes.map(function (e) { return { x: e.c[0], y: e.c[1], r: e.r }; }), pupil: t.eye.pupil, spread: t.eye.spread, lookX: t.eye.look.x, lookY: t.eye.look.y,
      top: [hs.top[0], hs.top[1]], bbox: bb });
  }
