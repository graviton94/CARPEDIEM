  /* 손그림 한지 (시안): 하루 = 굵은 먹선, 세상 바탕 = 한지, 나머지 물건 · 손님 = 한지 조각 + 손으로 그은 불규칙한 굵기의 선.
     HAND = true 일 때 paper() 가 handOne 으로. HAND_STYLE 로 선의 결을 고름 (시안 비교용):
       1 가는 손선 (0.35~0.8, 매끈, 조각 색보다 진한 같은 계열)
       2 붓 손선 (0.25~1.3, 굵기 차 크고 가끔 끊김)
       3 붓 손선 + 그늘 쪽에만 아주 옅은 결 몇 획 */
  var HAND = false, HAND_STYLE = 1;
  function setHand(v, style) { HAND = !!v; if (style) HAND_STYLE = style; }
  function smoothNoise(seed) { var r = R(seed), f = [r() * 6, r() * 6, r() * 6]; return function (t) { return 0.5 + 0.28 * Math.sin(t * 6.3 + f[0]) + 0.14 * Math.sin(t * 15.7 + f[1]) + 0.08 * Math.sin(t * 31 + f[2]); }; }
  // 손으로 그은 선: 윤곽을 따라 굵기가 변하고, 손이 살짝 떨려 가장자리에서 조금씩 벗어남
  function handLine(ctx, pts, col, seed, style, k) {
    var P = resample(pts, 0.6), n = P.length, nz = smoothNoise(seed), r = R(seed + 5); k = k || 1;
    var lo = (style === 1 ? 0.35 : 0.2) * k, hi = (style === 1 ? 0.75 : 1.6) * k, gapAt = style === 1 ? -1 : r() * n, gapLen = style === 1 ? 0 : 2 + r() * 4;
    ctx.save(); ctx.strokeStyle = col; ctx.lineCap = "round"; ctx.lineJoin = "round";
    for (var i = 0; i < n; i++) {
      if (gapAt >= 0 && Math.abs(i - gapAt) < gapLen) continue;
      var a = P[i], b = P[(i + 1) % n], t = i / n, w = lo + (hi - lo) * nz(t), j = (nz(t * 2.3 + 0.4) - 0.5) * 0.5;
      ctx.lineWidth = w; ctx.beginPath(); ctx.moveTo(a[0] + j, a[1] + j * 0.6); ctx.lineTo(b[0] + j, b[1] + j * 0.6); ctx.stroke();
    }
    ctx.restore();
  }
  function handOne(ctx, pts, col, seed, o) {
    o = o || {};
    var polys = typeof pts[0][0] === "number" ? [pts] : pts;
    polys = polys.map(function (p, i) { return p.length > 4 ? organic(p, 0.18, seed + i * 7, 1.1) : p; });
    // 여러 장을 한 번에 오린 것 (꽃잎 · 잎): 선을 먼저 긋고 그 위에 한지를 덮어, 바깥 윤곽만 남고 겹친 안쪽 선은 가려짐
    var many = polys.length > 1, ink = mixHex(col, "#2E2218", 0.55);
    function lined(p) { var bx = bbox(p); return o.ink !== false && p.length > 3 && !(Math.min(bx[2], bx[3]) < 3.2 || Math.max(bx[2], bx[3]) < 5); }
    if (many) polys.forEach(function (p, i) { if (lined(p)) { ctx.save(); ctx.scale(1, 1); handLine(ctx, p, ink, seed + i, HAND_STYLE, 2.2); ctx.restore(); } });
    paper(ctx, polys, col, seed, Object.assign({}, o, { raw: true, rim: Math.min(o.rim == null ? 0.5 : o.rim, 0.6) }));
    polys.forEach(function (p, i) {
      if (o.inner) { ctx.save(); multi(ctx, [p]); ctx.clip(); o.inner(ctx); texFill(ctx, TEX.fiber, 0.35, "source-atop", 0.4); ctx.restore(); }
      if (HAND_STYLE === 3 && p.length > 6) { var bb = bbox(p), rr = R(seed + 9); ctx.save(); multi(ctx, [p]); ctx.clip(); ctx.strokeStyle = mixHex(col, "#3A2C20", 0.35); ctx.globalAlpha = 0.22; ctx.lineWidth = 0.35; ctx.lineCap = "round";
        for (var k = 0; k < 7; k++) { var x = bb[0] + bb[2] * (0.55 + rr() * 0.45), y = bb[1] + bb[3] * (0.5 + rr() * 0.5), l = Math.min(bb[2], bb[3]) * 0.22; ctx.beginPath(); ctx.moveTo(x - l / 2, y + l * 0.35); ctx.lineTo(x + l / 2, y - l * 0.35); ctx.stroke(); } ctx.restore(); }
      // 가는 띠 (줄기 · 끈) · 아주 작은 조각 (꽃술 · 점) 은 선 없이: 양쪽 선이 겹쳐 검게 보이지 않게. 여러 장이면 위에서 이미 그음
      if (!many && lined(p)) handLine(ctx, p, ink, seed + i, HAND_STYLE);
    });
  }
  // 먹 점 눈 (손님)
  function handEye(ctx, x, y, r) { ctx.fillStyle = "#2A221C"; ctx.beginPath(); ctx.arc(x, y + r * 0.1, r * 0.4, 0, 7); ctx.fill(); ctx.fillStyle = "rgba(255,255,255,.85)"; ctx.beginPath(); ctx.arc(x - r * 0.13, y - r * 0.06, r * 0.12, 0, 7); ctx.fill(); }
  function handFeather(ctx) { var pts = place(leafPts(26, 5.4), 7, 32, 1, -0.75); paper(ctx, pts, "#E9E2D6", 7101, { light: 0.9 }); thread(ctx, [[7, 32], [25, 13]], 0.6, "rgba(110,94,76,.8)"); }
  function handStar(ctx) { var p = []; for (var i = 0; i < 8; i++) { var a = -Math.PI / 2 + i * Math.PI / 4, r = i % 2 ? 4.2 : 11; p.push([20 + Math.cos(a) * r, 20 + Math.sin(a) * r]); } paper(ctx, p, "#F2D27A", 7201, {}); }
