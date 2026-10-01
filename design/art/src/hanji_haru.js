  /* 세상은 한지 (hanjiWorld: 언덕 · 큰 겹), 하루와 가족 돌만 먹선 (haruMaker · stoneH). 앱의 하루는 Haru.kt 가 같은 규칙으로 벡터로 그린다. */
  var INK = "#1E1A17", NIGHT = false;
  // 세상 마감: edge = torn (찢은) | cut (가위로 오린), fiber · shadow 배율, haze = 먼 겹을 하늘색에 섞는 정도
  function hanjiWorld(o) {
    return function (ctx, pts, col, seed, k) { k = k || {}; var L = k.kind === "layer", sh = o.shadow;
      ctx.save(); ctx.shadowColor = "rgba(50,38,28," + ((L ? 0.2 : 0.26) * Math.min(sh, 1.6)) + ")"; ctx.shadowBlur = (L ? 7 : 4) * sh * SHK(); ctx.shadowOffsetY = (L ? 2.4 : 1.6) * sh * SHK();
      if (o.edge === "torn") {
        var torn = offsetRing(pts, L ? 1.4 : 1.1, L ? 0.9 : 0.55, seed);
        ctx.fillStyle = mixHex(col, "#FBF8F0", NIGHT ? 0.22 : 0.72); pathOf(ctx, torn); ctx.fill(); ctx.restore();
        var r = R(seed + 3); ctx.save(); ctx.strokeStyle = NIGHT ? "rgba(200,205,225,.25)" : "rgba(251,248,240,.85)"; ctx.lineWidth = 0.35; ctx.lineCap = "round"; for (var i = 0; i < torn.length; i += 3) { if (r() > 0.45) continue; var p = torn[i], q = torn[(i + 2) % torn.length], a = Math.atan2(q[1] - p[1], q[0] - p[0]) - Math.PI / 2 + (r() - 0.5), l = 0.8 + r() * 2.2; ctx.beginPath(); ctx.moveTo(p[0], p[1]); ctx.lineTo(p[0] + Math.cos(a) * l, p[1] + Math.sin(a) * l); ctx.stroke(); } ctx.restore();
        ctx.save(); ctx.fillStyle = col; pathOf(ctx, offsetRing(pts, -0.2, 0.35, seed + 1)); ctx.fill();
      } else { ctx.fillStyle = col; pathOf(ctx, pts, !L && k.smooth !== false); ctx.fill(); ctx.restore(); ctx.save(); }
      pathOf(ctx, pts, o.edge !== "torn" && !L && k.smooth !== false); ctx.clip(); texFill(ctx, TEX.fiber, (L ? 0.32 : 0.6) * o.fiber, "source-atop", 0.4); texFill(ctx, TEX.grain, 0.35, "multiply", 0.5); ctx.restore(); };
  }
  function hanjiLine(ctx, pts, w) { ctx.save(); ctx.strokeStyle = "rgba(90,74,58,.55)"; ctx.lineWidth = w * 0.8; ctx.lineCap = "round"; ctx.beginPath(); pts.forEach(function (p, i) { i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1]); }); ctx.stroke(); ctx.restore(); }

  // 하루 마감: ink (고른 먹선) | brush (아래로 굵어지는 붓먹) | cutout (검은 한지를 뒤에 덧댐)
  function haruPaper(ctx, pts, col, smooth) { ctx.save(); ctx.fillStyle = col; pathOf(ctx, pts, smooth); ctx.fill(); pathOf(ctx, pts, smooth); ctx.clip(); var bb = bbox(pts), gr = ctx.createLinearGradient(bb[0], bb[1], bb[0] + bb[2] * 0.5, bb[1] + bb[3]); gr.addColorStop(0, "rgba(255,255,255,.16)"); gr.addColorStop(0.55, "rgba(255,255,255,0)"); gr.addColorStop(1, "rgba(40,30,20,.10)"); ctx.fillStyle = gr; ctx.fillRect(bb[0], bb[1], bb[2], bb[3]); texFill(ctx, TEX.fiber, 0.7, "source-atop", 0.4); texFill(ctx, TEX.grain, 0.35, "multiply", 0.5); ctx.restore(); }
  function haruMaker(kind, lw) {
    return {
      body: function (ctx, pts, col, seed, scale) { var smooth = true, w = lw * (scale || 1);
        if (kind === "cutout") { var back = offsetRing(pts, w * 1.1, 0.18, seed + 9); ctx.save(); ctx.shadowColor = "rgba(30,22,16,.3)"; ctx.shadowBlur = 4; ctx.shadowOffsetY = 1.6; ctx.fillStyle = INK; pathOf(ctx, back); ctx.fill(); ctx.restore(); haruPaper(ctx, offsetRing(pts, 0, 0.15, seed + 1), col, true); return; }
        ctx.save(); ctx.shadowColor = "rgba(30,22,16,.22)"; ctx.shadowBlur = 4 * SHK(); ctx.shadowOffsetY = 1.6 * SHK(); ctx.fillStyle = col; pathOf(ctx, pts, smooth); ctx.fill(); ctx.restore(); haruPaper(ctx, pts, col, smooth);
        if (kind === "ink") { ctx.save(); ctx.strokeStyle = INK; ctx.lineWidth = w; ctx.lineJoin = "round"; pathOf(ctx, pts, smooth); ctx.stroke(); ctx.restore(); return; }
        var bb = bbox(pts), cy = bb[1] + bb[3] * 0.4, P = resample(pts, 1); varLine(ctx, P, INK, function (t) { var y = P[Math.floor(t * (P.length - 1))][1]; return w * (0.38 + 1.5 * Math.pow(Math.max(0, (y - cy) / (bb[3] * 0.6)), 1.3)); }, true); },
      eye: function (ctx, c, rr, px, py, pr, scale) { var w = Math.max(1, lw * 0.5) * (scale || 1);
        ctx.save(); if (kind === "cutout") { ctx.fillStyle = INK; ctx.beginPath(); ctx.arc(c[0], c[1], rr + w * 0.9, 0, 7); ctx.fill(); }
        ctx.fillStyle = "#FFFDF7"; ctx.beginPath(); ctx.arc(c[0], c[1], rr, 0, 7); ctx.fill(); if (kind !== "cutout") { ctx.strokeStyle = INK; ctx.lineWidth = w; ctx.stroke(); }
        ctx.fillStyle = INK; ctx.beginPath(); ctx.arc(px, py, pr, 0, 7); ctx.fill(); ctx.fillStyle = "#FFFFFF"; ctx.beginPath(); ctx.arc(px - pr * 0.35, py - pr * 0.38, pr * 0.26, 0, 7); ctx.fill(); ctx.restore(); } };
  }
  function stoneH(ctx, V, seed, cx, gy, width, o) {
    o = o || {}; var t = traitsOf(seed), hs = haruShape(t, cx, gy, 70), bb = bbox(hs.pts), k = width / bb[2], sc = o.lineScale || 1;
    function tr(p) { return [cx + (p[0] - cx) * k, gy + (p[1] - gy) * k]; }
    var pts = hs.pts.map(tr), base = mix(t.base, "#FFFFFF", 0.18); if (o.night) base = mix(base, [70, 78, 110], 0.28); var col = "rgb(" + base.join(",") + ")";
    ctx.save(); ctx.filter = "blur(2px)"; ctx.fillStyle = "rgba(50,38,28,.22)"; ctx.beginPath(); ctx.ellipse(cx + width * 0.06, gy + 1, width * 0.52, 2.4, 0, 0, 7); ctx.fill(); ctx.restore();
    V.haru.body(ctx, pts, col, seed % 997, sc);
    hs.eyes.forEach(function (e) { var c = tr(e.c), rr = e.r * k, pr = rr * t.eye.pupil, lx = t.eye.look.x + (e.idx ? t.eye.spread : -t.eye.spread), ly = t.eye.look.y, ll = Math.hypot(lx, ly); if (ll > 1) { lx /= ll; ly /= ll; } var lim = rr - pr - rr * 0.1; V.haru.eye(ctx, c, rr, c[0] + lx * lim, c[1] + ly * lim + lim * 0.2, pr, sc); });
    if (o.earmuff) earmuffsOn(ctx, V, pts, sc, o.night, seed % 997 + 90);
    if (o.hat) hatOn(ctx, V, pts, hs.eyes.map(function (e) { return tr(e.c); }), sc, seed % 997 + 70);
  }

