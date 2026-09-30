  /* ───────── 소품 다듬기: 형태 음영 · 반사광 · 결을 따르는 붓질 · 번지는 가장자리 ─────────
     도형 하나를 한 색으로 칠하지 않고, 실제 물건이 만들어진 방식(비늘 · 엮은 결 · 접힌 면)대로 조각을 쌓습니다. */
  var LX = -0.66, LY = -0.75; // 빛 방향 (왼쪽 위에서)
  function scaleOf(ctx) { var m = ctx.getTransform(); return Math.hypot(m.a, m.b); }
  function blur(ctx, px) { ctx.filter = "blur(" + Math.max(0, px * scaleOf(ctx)).toFixed(2) + "px)"; }
  function smoothPath(pts) {
    var p = new Path2D(), n = pts.length;
    for (var i = 0; i < n; i++) { var a = pts[i], b = pts[(i + 1) % n], mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2; if (i === 0) p.moveTo(mx, my); else p.quadraticCurveTo(a[0], a[1], mx, my); }
    var a0 = pts[0], b0 = pts[1]; p.quadraticCurveTo(a0[0], a0[1], (a0[0] + b0[0]) / 2, (a0[1] + b0[1]) / 2); p.closePath(); return p;
  }
  function wobble(pts, amt, seed) {
    var r = rng(seed), cx = 0, cy = 0; pts.forEach(function (q) { cx += q[0]; cy += q[1]; }); cx /= pts.length; cy /= pts.length;
    var f1 = r() * 6, f2 = r() * 6;
    return pts.map(function (q, i) { var t = i / pts.length * Math.PI * 2, d = amt * (Math.sin(t * 5 + f1) * 0.6 + Math.sin(t * 13 + f2) * 0.4), dx = q[0] - cx, dy = q[1] - cy, l = Math.hypot(dx, dy) || 1; return [q[0] + dx / l * d, q[1] + dy / l * d]; });
  }
  function jit(pts, a, seed) { var r = rng(seed); return pts.map(function (q) { return [q[0] + (r() - 0.5) * a, q[1] + (r() - 0.5) * a]; }); }
  function cast(ctx, cx, cy, rx, ry, a) {
    a = a == null ? 0.36 : a;
    ctx.save(); blur(ctx, ry * 0.9); ctx.fillStyle = "rgba(52,38,20," + a * 0.8 + ")"; ctx.beginPath(); ctx.ellipse(cx, cy, rx, ry, 0, 0, 7); ctx.fill(); ctx.restore();
    ctx.save(); blur(ctx, ry * 0.3); ctx.fillStyle = "rgba(38,26,12," + a + ")"; ctx.beginPath(); ctx.ellipse(cx, cy - ry * 0.15, rx * 0.62, ry * 0.4, 0, 0, 7); ctx.fill(); ctx.restore();
  }

  // 부피가 있는 면: 물감 얼룩 → 결 → 빛 → 그늘 → 반사광 → 젖은 가장자리 → 색연필
  function form(ctx, path, col, o) {
    o = o || {}; col = C(col);
    var r = rng(o.seed || 5), b = o.box, big = Math.max(b[2], b[3]);
    var lx = b[0] + b[2] * (o.lx == null ? 0.3 : o.lx), ly = b[1] + b[3] * (o.ly == null ? 0.26 : o.ly);
    ctx.save(); ctx.clip(path);
    if (!o.keep) { ctx.fillStyle = css(col); ctx.fillRect(b[0] - 10, b[1] - 10, b[2] + 20, b[3] + 20); }
    ctx.save(); blur(ctx, big * 0.03);
    for (var i = 0; i < (o.mottle == null ? 16 : o.mottle); i++) { var x = b[0] + r() * b[2], y = b[1] + r() * b[3], rr = big * lerp(0.06, 0.2, r()); ctx.fillStyle = css(sh(col, (r() - 0.5) * (o.vary == null ? 0.22 : o.vary)), lerp(0.25, 0.5, r())); ctx.beginPath(); ctx.ellipse(x, y, rr, rr * lerp(0.45, 1, r()), r() * 3, 0, 7); ctx.fill(); }
    ctx.restore();
    if (o.tex) o.tex(ctx, r);
    var g = ctx.createRadialGradient(lx, ly, 0, lx, ly, big * (o.lr || 0.6));
    g.addColorStop(0, "rgba(255,246,226," + (o.hl == null ? 0.42 : o.hl) + ")"); g.addColorStop(1, "rgba(255,246,226,0)");
    ctx.fillStyle = g; ctx.fillRect(b[0] - 10, b[1] - 10, b[2] + 20, b[3] + 20);
    var d = ctx.createRadialGradient(lx, ly, big * 0.15, lx, ly, big * (o.dr || 1.02));
    d.addColorStop(0, "rgba(40,24,10,0)"); d.addColorStop(0.5, "rgba(40,24,10,0)"); d.addColorStop(1, "rgba(40,24,10," + (o.sd == null ? 0.5 : o.sd) + ")");
    ctx.fillStyle = d; ctx.fillRect(b[0] - 10, b[1] - 10, b[2] + 20, b[3] + 20);
    if (o.bounceA !== 0) { ctx.save(); blur(ctx, big * 0.025); ctx.translate(-big * 0.04, -big * 0.04); ctx.lineWidth = big * 0.06; ctx.strokeStyle = css(o.bounce || mix(col, "#F4D8AA", 0.55), o.bounceA == null ? 0.32 : o.bounceA); ctx.stroke(path); ctx.restore(); }
    ctx.save(); blur(ctx, big * 0.01); ctx.lineWidth = big * 0.024; ctx.strokeStyle = css(sh(col, -0.5), o.wet == null ? 0.32 : o.wet); ctx.stroke(path); ctx.restore();
    if (o.after) o.after(ctx, r);
    ctx.restore();
    if (o.pencil !== false) pencil(ctx, path, o.line || sh(col, -0.62), o.seed, o.lw || 1.3);
  }

  // 원통 (다리 · 손잡이 · 줄기): 가로질러 밝음 → 본색 → 그늘 → 반사광
  function rod(ctx, x1, y1, x2, y2, w1, w2, col, o) {
    o = o || {}; col = C(col);
    var dx = x2 - x1, dy = y2 - y1, L = Math.hypot(dx, dy) || 1, ux = dx / L, uy = dy / L, nx = -uy, ny = ux, an = Math.atan2(ny, nx);
    var p = new Path2D();
    p.moveTo(x1 + nx * w1 / 2, y1 + ny * w1 / 2); p.lineTo(x2 + nx * w2 / 2, y2 + ny * w2 / 2);
    p.arc(x2, y2, w2 / 2, an, an - Math.PI, true);
    p.lineTo(x1 - nx * w1 / 2, y1 - ny * w1 / 2);
    p.arc(x1, y1, w1 / 2, an + Math.PI, an, true); p.closePath();
    var s = nx * LX + ny * LY > 0 ? 1 : -1, mx = (x1 + x2) / 2, my = (y1 + y2) / 2, wm = (w1 + w2) / 4;
    var g = ctx.createLinearGradient(mx + nx * wm * s, my + ny * wm * s, mx - nx * wm * s, my - ny * wm * s);
    var hi = o.metal ? 0.55 : 0.3;
    g.addColorStop(0, css(sh(col, -0.08))); g.addColorStop(0.2, css(sh(col, hi))); g.addColorStop(0.42, css(col)); g.addColorStop(0.8, css(sh(col, -0.38))); g.addColorStop(1, css(sh(col, -0.18)));
    ctx.fillStyle = g; ctx.fill(p);
    if (o.grain) {
      ctx.save(); ctx.clip(p); var r = rng(o.seed || 9);
      for (var k = 0; k < 5; k++) { var v = (r() - 0.5) * 0.8; ctx.strokeStyle = css(sh(col, -0.45), lerp(0.12, 0.3, r())); ctx.lineWidth = lerp(0.5, 1.2, r()); ctx.beginPath(); ctx.moveTo(x1 + nx * w1 * v, y1 + ny * w1 * v); ctx.bezierCurveTo(x1 + dx * 0.33 + nx * w1 * (v + 0.08), y1 + dy * 0.33 + ny * w1 * (v + 0.08), x1 + dx * 0.66 + nx * w2 * (v - 0.08), y1 + dy * 0.66 + ny * w2 * (v - 0.08), x2 + nx * w2 * v, y2 + ny * w2 * v); ctx.stroke(); }
      ctx.restore();
    }
    if (o.pencil !== false) pencil(ctx, p, sh(col, -0.6), o.seed || 9, 1);
    return p;
  }
  // 곡선을 따라 원통을 이어 그림 (휜 손잡이)
  function tube(ctx, pts, w, col, o) {
    o = o || {}; var n = o.n || 28, P = [];
    for (var i = 0; i <= n; i++) { var t = i / n, a = 1 - t; P.push([a * a * a * pts[0] + 3 * a * a * t * pts[2] + 3 * a * t * t * pts[4] + t * t * t * pts[6], a * a * a * pts[1] + 3 * a * a * t * pts[3] + 3 * a * t * t * pts[5] + t * t * t * pts[7]]); }
    for (i = 0; i < n; i++) rod(ctx, P[i][0], P[i][1], P[i + 1][0], P[i + 1][1], w, w, col, { pencil: false, metal: o.metal });
    if (o.wrap) { ctx.save(); ctx.lineCap = "round"; for (i = 0; i < n; i += 1) { var q = P[i], q2 = P[i + 1], ddx = q2[0] - q[0], ddy = q2[1] - q[1], l = Math.hypot(ddx, ddy) || 1, nx2 = -ddy / l, ny2 = ddx / l; ctx.strokeStyle = css(sh(col, -0.45), 0.45); ctx.lineWidth = 1.2; ctx.beginPath(); ctx.moveTo(q[0] + nx2 * w / 2, q[1] + ny2 * w / 2); ctx.lineTo(q2[0] - nx2 * w / 2, q2[1] - ny2 * w / 2); ctx.stroke(); } ctx.restore(); }
    return P;
  }

  var DETAIL = {};

  /* 솔방울: 해바라기 씨처럼 나선으로 붙은 비늘. 비늘 끝에 두꺼운 판(비늘 끝판)과 가운데 작은 돌기. 비늘 사이는 깊은 그늘 */
  DETAIL.pinecone = function (c) {
    cast(c, 262, 418, 116, 15, 0.42);
    var cx = 256, yb = 404, H = 250;
    function R(t) { return 98 * Math.pow(Math.sin(Math.PI * Math.min(0.995, 0.06 + t * 0.94)), 0.62) * (1 - 0.32 * t); }
    var core = [], i, t;
    for (i = 0; i <= 50; i++) { t = i / 50; core.push([cx + R(t) * 0.92, yb - t * H]); }
    for (i = 50; i >= 0; i--) { t = i / 50; core.push([cx - R(t) * 0.92, yb - t * H]); }
    form(c, smoothPath(core), "#2E1D10", { box: box(cx - 100, yb - H, 200, H), seed: 130, hl: 0.08, sd: 0.4, pencil: false, mottle: 6, bounceA: 0 });
    var N = 104, list = [];
    for (i = 0; i < N; i++) { t = (i + 0.5) / N; var th = i * 2.39996 + 0.4, z = Math.cos(th); if (z < -0.3) continue; list.push({ t: t, sx: Math.sin(th), z: z }); }
    list.sort(function (a, b) { return (a.z * 1.1 + a.t * 0.9) - (b.z * 1.1 + b.t * 0.9); });
    list.forEach(function (s, k) {
      var rr = R(s.t), px = cx + rr * s.sx * 0.98, py = yb - s.t * H + (1 - s.z) * 5;
      var size = lerp(16, 36, Math.pow(Math.sin(Math.PI * Math.min(1, 0.08 + s.t)), 0.8)), face = 0.3 + 0.7 * Math.max(0, s.z);
      var dirx = s.sx * 0.9, diry = 0.5 + 0.4 * s.z, dl = Math.hypot(dirx, diry); dirx /= dl; diry /= dl;
      var len = size * 1.3, w = size * (0.5 + 0.55 * face);
      c.save(); c.translate(px - dirx * len * 0.6, py - diry * len * 0.6); c.rotate(Math.atan2(diry, dirx));
      var body = svg("M0," + (-w * 0.28) + " C" + len * 0.35 + "," + (-w * 0.64) + " " + len * 0.82 + "," + (-w * 0.62) + " " + len + ",0 C" + len * 0.82 + "," + (w * 0.62) + " " + len * 0.35 + "," + (w * 0.64) + " 0," + (w * 0.28) + " Z");
      var lit = Math.max(0, Math.min(1, 0.5 + 0.5 * (-s.sx * 0.6 + s.z * 0.35 + (s.t - 0.45) * 0.5)));
      var baseC = mix("#3A2515", "#7E5431", lit), tipC = mix("#7C5230", "#D2A46A", lit);
      c.save(); blur(c, 3); c.fillStyle = "rgba(20,10,4,.55)"; c.translate(3, 4); c.fill(body); c.restore();
      var g = c.createLinearGradient(0, 0, len, 0); g.addColorStop(0, css(sh(baseC, -0.45))); g.addColorStop(0.55, css(baseC)); g.addColorStop(1, css(sh(baseC, 0.12)));
      c.fillStyle = g; c.fill(body);
      c.save(); c.clip(body);
      var g2 = c.createRadialGradient(len * 0.66, -w * 0.2, 0, len * 0.76, 0, len * 0.36);
      g2.addColorStop(0, css(sh(tipC, 0.28))); g2.addColorStop(0.65, css(tipC)); g2.addColorStop(1, css(sh(tipC, -0.35)));
      c.fillStyle = g2; c.beginPath(); c.ellipse(len * 0.76, 0, len * 0.28, w * 0.52, 0, 0, 7); c.fill();
      c.strokeStyle = css(sh(tipC, -0.45), 0.5); c.lineWidth = 1; c.beginPath(); c.moveTo(len * 0.52, w * 0.02); c.quadraticCurveTo(len * 0.76, -w * 0.08, len, 0); c.stroke();
      c.strokeStyle = "rgba(255,236,200,.35)"; c.lineWidth = 1; c.beginPath(); c.moveTo(len * 0.58, -w * 0.3); c.quadraticCurveTo(len * 0.8, -w * 0.44, len * 0.96, -w * 0.12); c.stroke();
      c.fillStyle = css(sh(tipC, -0.55), 0.85); c.beginPath(); c.arc(len * 0.78, -w * 0.03, Math.max(1.3, w * 0.07), 0, 7); c.fill();
      c.restore();
      c.strokeStyle = "rgba(30,16,6,.4)"; c.lineWidth = 1; c.stroke(body);
      c.restore();
    });
    rod(c, 254, yb - H + 8, 266, yb - H - 26, 11, 7, "#5E4028", { grain: true, seed: 139 });
  };

  /* 도토리: 윤이 나는 열매 + 작은 비늘이 층층이 덮인 깍정이 */
  DETAIL.acorn = function (c) {
    cast(c, 262, 422, 94, 13, 0.42);
    var cx = 256, pts = [];
    for (var i = 0; i < 120; i++) { var a = i / 120 * Math.PI * 2, x = Math.cos(a), y = Math.sin(a), yy = y > 0 ? y * 1.12 : y * 0.8, xx = x * (y > 0 ? 1 - 0.42 * Math.pow(y, 2.4) : 1); pts.push([cx + xx * 76, 318 + yy * 88]); }
    var nut = smoothPath(wobble(pts, 1.5, 90));
    form(c, nut, "#A56B33", { box: box(180, 248, 152, 170), seed: 91, hl: 0.5, sd: 0.58, lx: 0.3, ly: 0.36, tex: function (x) {
      for (var k = -6; k <= 6; k++) { var u = k / 6.6; x.strokeStyle = css(k % 2 ? "#7A4A20" : "#C99050", 0.18); x.lineWidth = 2.6; x.beginPath(); x.moveTo(cx + u * 74, 262); x.bezierCurveTo(cx + u * 82, 322, cx + u * 52, 392, cx + u * 5, 414); x.stroke(); }
    }, after: function (x) {
      x.save(); blur(x, 7); x.fillStyle = "rgba(255,248,235,.6)"; x.beginPath(); x.ellipse(214, 322, 12, 34, -0.18, 0, 7); x.fill(); x.restore();
      x.fillStyle = "rgba(255,255,255,.75)"; x.beginPath(); x.ellipse(212, 312, 3.5, 11, -0.18, 0, 7); x.fill();
      x.save(); blur(x, 8); x.fillStyle = "rgba(40,20,6,.5)"; x.beginPath(); x.ellipse(cx, 270, 84, 18, 0, 0, 7); x.fill(); x.restore();
    } });
    form(c, ell(cx + 2, 414, 5, 8), "#4A2E16", { box: box(248, 404, 16, 20), pencil: false, mottle: 0, bounceA: 0 });
    var cap = svg("M162,272 C158,224 206,204 256,204 C306,204 354,224 350,272 C332,286 180,286 162,272 Z");
    form(c, cap, "#6A4C2E", { box: box(158, 204, 196, 82), seed: 92, hl: 0.3, sd: 0.55, after: function (x) {
      for (var j = 0; j < 8; j++) {
        var yy = lerp(274, 214, j / 7.5), hw = lerp(92, 36, Math.pow(j / 7.5, 1.2)), n = Math.round(lerp(16, 6, j / 7));
        for (var k = 0; k < n; k++) {
          var u = (k + (j % 2 ? 0.5 : 0)) / n * 2 - 1 + 1 / n; if (Math.abs(u) > 1.02) continue;
          var sx = cx + u * hw, sy = yy + (1 - u * u) * 5, s = lerp(9.5, 6, j / 7) * (0.55 + 0.45 * Math.sqrt(Math.max(0, 1 - u * u)));
          var lit = Math.max(0, Math.min(1, 0.55 - u * 0.4 + (j / 7) * 0.2)), sc = mix("#4A321C", "#B89060", lit);
          x.fillStyle = "rgba(24,12,4,.5)"; x.beginPath(); x.ellipse(sx + 1, sy + s * 0.6, s * 0.95, s * 0.42, 0, 0, 7); x.fill();
          x.fillStyle = css(sc); x.beginPath(); x.moveTo(sx - s * 0.9, sy - s * 0.15); x.quadraticCurveTo(sx, sy - s * 1.15, sx + s * 0.9, sy - s * 0.15); x.quadraticCurveTo(sx + s * 0.25, sy + s * 0.75, sx, sy + s * 0.8); x.quadraticCurveTo(sx - s * 0.25, sy + s * 0.75, sx - s * 0.9, sy - s * 0.15); x.fill();
          x.strokeStyle = css(sh(sc, 0.35), 0.6); x.lineWidth = 0.9; x.beginPath(); x.moveTo(sx - s * 0.5, sy - s * 0.35); x.quadraticCurveTo(sx, sy - s * 0.85, sx + s * 0.5, sy - s * 0.35); x.stroke();
        }
      }
    } });
    var lip = curve([164, 272, 182, 287, 330, 287, 348, 272]);
    c.save(); c.lineCap = "round"; c.lineWidth = 6; c.strokeStyle = css("#8C6A44"); c.stroke(lip); c.lineWidth = 2; c.strokeStyle = "rgba(230,200,150,.55)"; c.translate(0, -1.5); c.stroke(lip); c.restore();
    rod(c, 254, 212, 270, 176, 12, 7, "#5E4028", { grain: true, seed: 93 });
  };

  /* 책 세 권: 비스듬히 본 상자. 둥근 책등 · 헝겊 표지 · 결이 보이는 종이 옆면 · 닳은 모서리 · 금박 띠 */
  function book3(c, x, y, W, T, D, cloth, seed) {
    var dx = D * 0.6, dy = -D * 0.34, bd = 3;
    // 종이 옆면
    var pages = poly([[x + W - 4, y + bd], [x + W - 4 + dx, y + bd + dy], [x + W - 4 + dx, y + T - bd + dy], [x + W - 4, y + T - bd]]);
    form(c, pages, "#EEE3CA", { box: box(x + W - 4, y + dy, dx, T - dy), seed: seed + 1, mottle: 4, hl: 0.15, sd: 0.3, bounceA: 0, pencil: false, after: function (q) {
      for (var i = 0; i < 26; i++) { var f = i / 25, yy = y + bd + f * (T - 2 * bd); q.strokeStyle = i % 3 ? "rgba(140,110,70,.2)" : "rgba(255,255,255,.4)"; q.lineWidth = 0.7; q.beginPath(); q.moveTo(x + W - 4, yy); q.lineTo(x + W - 4 + dx, yy + dy); q.stroke(); }
      q.save(); blur(q, 3); q.fillStyle = "rgba(60,40,20,.35)"; q.fillRect(x + W - 4, y + bd - 2, dx + 4, 5); q.restore();
    } });
    // 표지 두께 (종이 위 · 아래)
    [y, y + T - bd].forEach(function (yy, k) { form(c, poly([[x + W - 2, yy], [x + W - 2 + dx + 2, yy + dy], [x + W - 2 + dx + 2, yy + dy + bd], [x + W - 2, yy + bd]]), sh(cloth, k ? -0.25 : 0), { box: box(x + W - 2, yy + dy, dx + 2, bd - dy), mottle: 0, pencil: false, bounceA: 0, wet: 0.1 }); });
    // 앞표지 (윗면)
    var top = poly(jit([[x - 1, y], [x - 1 + dx + 3, y + dy - 1], [x + W + dx + 3, y + dy - 1], [x + W + 1, y]], 1.2, seed));
    form(c, top, sh(cloth, 0.08), { box: box(x, y + dy, W + dx, -dy), seed: seed + 2, lx: 0.15, ly: 0.2, hl: 0.34, sd: 0.3, tex: function (q, r) {
      for (var i = 0; i < 260; i++) { var px = x + r() * (W + dx), py = y + dy + r() * -dy; q.fillStyle = r() < 0.5 ? "rgba(255,255,255,.08)" : "rgba(0,0,0,.08)"; q.fillRect(px, py, lerp(2, 6, r()), 0.8); }
    }, after: function (q) { q.save(); blur(q, 2.5); q.lineWidth = 4; q.strokeStyle = css(sh(cloth, 0.4), 0.45); q.stroke(top); q.restore(); } });
    // 책등 (둥글게)
    var sp = rrect(x, y, W, T, Math.min(8, T * 0.2));
    c.save(); var g = c.createLinearGradient(0, y, 0, y + T); g.addColorStop(0, css(sh(cloth, 0.3))); g.addColorStop(0.28, css(sh(cloth, 0.05))); g.addColorStop(0.75, css(sh(cloth, -0.32))); g.addColorStop(1, css(sh(cloth, -0.12))); c.fillStyle = g; c.fill(sp); c.restore();
    form(c, sp, cloth, { box: box(x, y, W, T), seed: seed + 3, hl: 0.12, sd: 0.25, mottle: 10, vary: 0.14, bounceA: 0, tex: function (q, r) {
      q.save(); q.globalAlpha = 0.75; var gg = q.createLinearGradient(0, y, 0, y + T); gg.addColorStop(0, css(sh(cloth, 0.28))); gg.addColorStop(0.3, css(sh(cloth, 0.02))); gg.addColorStop(0.78, css(sh(cloth, -0.34))); gg.addColorStop(1, css(sh(cloth, -0.14))); q.fillStyle = gg; q.fillRect(x, y, W, T); q.restore();
      for (var i = 0; i < 200; i++) { q.fillStyle = r() < 0.5 ? "rgba(255,255,255,.07)" : "rgba(0,0,0,.09)"; q.fillRect(x + r() * W, y + r() * T, lerp(2, 5, r()), 0.8); }
    }, after: function (q) {
      [x + W * 0.1, x + W * 0.86].forEach(function (bx) { var gb = q.createLinearGradient(0, y, 0, y + T); gb.addColorStop(0, "#F4D68A"); gb.addColorStop(0.35, "#D9A84A"); gb.addColorStop(1, "#8A6424"); q.fillStyle = gb; q.globalAlpha = 0.85; q.fillRect(bx, y, 5, T); q.fillRect(bx + 8, y, 2, T); q.globalAlpha = 1; });
      var lw = W * 0.34, lx0 = x + W / 2 - lw / 2; q.fillStyle = css(sh(cloth, -0.25), 0.6); q.fillRect(lx0, y + T * 0.28, lw, T * 0.44);
      q.strokeStyle = "rgba(230,190,110,.7)"; q.lineWidth = 1; q.strokeRect(lx0 + 3, y + T * 0.28 + 3, lw - 6, T * 0.44 - 6);
    } });
  }
  DETAIL.books = function (c) {
    cast(c, 262, 420, 170, 16, 0.4);
    book3(c, 124, 362, 226, 50, 74, PAL.olive, 10);
    book3(c, 150, 322, 196, 42, 66, "#A95A28", 20);
    book3(c, 178, 290, 160, 34, 56, "#5E7478", 30);
  };

  /* 나무 의자: 깎아 만든 다리(마디) · 결이 보이는 앉는 판 · 판 밑 그늘 */
  DETAIL.chair = function (c) {
    var W = "#A8733F";
    cast(c, 266, 414, 150, 16, 0.4);
    c.save(); blur(c, 10); c.fillStyle = "rgba(40,26,12,.22)"; c.beginPath(); c.moveTo(186, 402); c.lineTo(352, 392); c.lineTo(330, 376); c.lineTo(176, 386); c.closePath(); c.fill(); c.restore();
    rod(c, 172, 300, 174, 392, 11, 9, sh(W, -0.3), { grain: true, seed: 201 });
    rod(c, 330, 288, 332, 380, 11, 9, sh(W, -0.3), { grain: true, seed: 202 });
    rod(c, 174, 366, 332, 356, 6, 6, sh(W, -0.32), { seed: 203 });
    rod(c, 170, 300, 164, 144, 13, 11, W, { grain: true, seed: 204 });
    rod(c, 330, 288, 330, 132, 13, 11, W, { grain: true, seed: 205 });
    [214, 250, 288].forEach(function (x, i) { rod(c, x - 2, 294 - i * 4, x - 3, 160 - i * 3, 7, 6, sh(W, -0.04), { grain: true, seed: 206 + i }); });
    var rail = svg("M156,148 C212,128 286,120 340,128 L340,158 C286,150 212,158 156,178 Z");
    form(c, rail, W, { box: box(156, 120, 184, 58), seed: 210, hl: 0.35, sd: 0.4, tex: function (q, r) { for (var k = 0; k < 7; k++) { var o2 = lerp(-12, 14, k / 6); q.strokeStyle = css(sh(W, -0.4), lerp(0.15, 0.32, r())); q.lineWidth = 1; q.beginPath(); q.moveTo(156, 162 + o2); q.bezierCurveTo(212, 142 + o2 + (r() - 0.5) * 6, 286, 134 + o2, 340, 142 + o2); q.stroke(); } } });
    var seatTop = poly(jit([[160, 300], [334, 286], [372, 318], [196, 334]], 1.5, 211));
    form(c, seatTop, sh(W, 0.1), { box: box(160, 286, 212, 48), seed: 212, lx: 0.2, ly: 0.1, hl: 0.4, sd: 0.4, tex: function (q, r) {
      for (var k = 0; k < 10; k++) { var f = k / 9; q.strokeStyle = css(sh(W, -0.35), lerp(0.14, 0.3, r())); q.lineWidth = lerp(0.6, 1.4, r()); q.beginPath(); q.moveTo(lerp(160, 196, f), lerp(300, 334, f)); q.bezierCurveTo(lerp(220, 250, f) + (r() - 0.5) * 10, lerp(292, 326, f), lerp(290, 320, f), lerp(286, 322, f) + (r() - 0.5) * 4, lerp(334, 372, f), lerp(286, 318, f)); q.stroke(); }
      q.strokeStyle = css(sh(W, -0.45), 0.4); q.lineWidth = 1.2; q.beginPath(); q.ellipse(262, 306, 12, 4, -0.08, 0, 7); q.stroke();
    } });
    form(c, poly([[196, 334], [372, 318], [372, 336], [196, 352]]), sh(W, -0.22), { box: box(196, 318, 176, 34), seed: 213, mottle: 6, hl: 0.12, bounceA: 0 });
    function leg(x1, y1, x2, y2, s) { rod(c, x1, y1, x2, y2, 14, 11, W, { grain: true, seed: s }); [0.28, 0.62].forEach(function (f) { var mx = lerp(x1, x2, f), my = lerp(y1, y2, f); rod(c, mx - 9, my, mx + 9, my - 1, 5, 5, sh(W, 0.12), { pencil: false }); c.strokeStyle = "rgba(50,30,12,.4)"; c.lineWidth = 1; c.beginPath(); c.moveTo(mx - 8, my + 3); c.lineTo(mx + 8, my + 2); c.stroke(); }); }
    leg(206, 348, 208, 418, 214);
    leg(362, 334, 364, 406, 215);
    rod(c, 208, 392, 364, 382, 6, 6, sh(W, -0.1), { seed: 216 });
  };

  /* 사과 바구니: 세로 살에 가로 줄을 위아래로 엮은 결 · 꼬아 두른 테두리 · 사과 줄무늬와 숨구멍 */
  function apple(c, x, y, r, base, seed) {
    var pts = []; for (var i = 0; i < 90; i++) { var a = i / 90 * Math.PI * 2, cx0 = Math.cos(a), cy0 = Math.sin(a), dip = Math.exp(-Math.pow((a - Math.PI * 1.5) / 0.35, 2)) * 0.12; pts.push([x + cx0 * r * (1.04 - 0.08 * cy0), y + cy0 * r * 0.93 + dip * r]); }
    var p = smoothPath(wobble(pts, r * 0.02, seed));
    form(c, p, base, { box: box(x - r, y - r, r * 2, r * 2), seed: seed, hl: 0.48, sd: 0.55, lx: 0.32, ly: 0.28, tex: function (q, rr) {
      for (var k = -5; k <= 5; k++) { var u = k / 5.5; q.strokeStyle = css(rr() < 0.5 ? "#E8B24A" : sh(base, -0.3), lerp(0.12, 0.3, rr())); q.lineWidth = lerp(1.5, 3.5, rr()); q.beginPath(); q.moveTo(x + u * r * 0.25, y - r * 0.8); q.quadraticCurveTo(x + u * r * 1.15, y, x + u * r * 0.4, y + r * 0.85); q.stroke(); }
      for (k = 0; k < 40; k++) { var a = rr() * 6.28, d = Math.sqrt(rr()) * r * 0.9; q.fillStyle = "rgba(255,236,190," + lerp(0.25, 0.6, rr()) + ")"; q.beginPath(); q.arc(x + Math.cos(a) * d, y + Math.sin(a) * d * 0.9, lerp(0.6, 1.3, rr()), 0, 7); q.fill(); }
    }, after: function (q) {
      q.save(); blur(q, 2); q.fillStyle = "rgba(60,30,10,.55)"; q.beginPath(); q.ellipse(x + 2, y - r * 0.78, r * 0.22, r * 0.08, 0, 0, 7); q.fill(); q.restore();
      q.save(); blur(q, r * 0.12); q.fillStyle = "rgba(255,250,240,.55)"; q.beginPath(); q.ellipse(x - r * 0.4, y - r * 0.38, r * 0.16, r * 0.26, -0.5, 0, 7); q.fill(); q.restore();
      q.fillStyle = "rgba(255,255,255,.7)"; q.beginPath(); q.ellipse(x - r * 0.42, y - r * 0.42, r * 0.05, r * 0.1, -0.5, 0, 7); q.fill();
    } });
  }
  DETAIL.basket = function (c) {
    var S = "#B88A55", cx = 256;
    cast(c, 258, 418, 170, 17, 0.42);
    function RX(y) { return lerp(126, 96, (y - 300) / 112); }
    // 안쪽 (어두움)
    form(c, ell(cx, 300, 124, 22), "#4A3320", { box: box(132, 278, 248, 44), mottle: 4, pencil: false, hl: 0.05, bounceA: 0 });
    function braid(from, to) { for (var k = 0; k < 44; k++) { var a = lerp(from, to, k / 43), bx = cx + Math.cos(a) * 126, by = 300 + Math.sin(a) * 23; var dirA = Math.atan2(Math.cos(a) * 23, -Math.sin(a) * 126) + 0.7; rod(c, bx - Math.cos(dirA) * 9, by - Math.sin(dirA) * 9, bx + Math.cos(dirA) * 9, by + Math.sin(dirA) * 9, 9, 9, sh(S, Math.sin(a) > 0 ? 0 : -0.2), { pencil: false }); } }
    braid(Math.PI * 1.02, Math.PI * 1.98);
    apple(c, 196, 276, 40, "#B83E2A", 151);
    apple(c, 322, 280, 37, "#D08A36", 152);
    apple(c, 262, 262, 43, "#A93627", 153);
    apple(c, 232, 290, 34, "#C2502E", 154);
    apple(c, 294, 294, 32, "#B2452F", 155);
    [[196, 238], [322, 245], [262, 221]].forEach(function (s, i) { rod(c, s[0] + 1, s[1] + 4, s[0] + 5, s[1] - 14, 4, 3, "#5E4028", { pencil: false }); });
    leaf(c, 266, 222, 46, 13, -0.55, PAL.leaf, 158);
    // 몸통: 어두운 바탕 위에 엮기
    var body = svg("M130,300 C134,352 148,396 168,410 Q256,426 344,410 C364,396 378,352 382,300 Q256,326 130,300 Z");
    c.save(); c.clip(body);
    c.fillStyle = "#3A2716"; c.fillRect(120, 290, 272, 140);
    var stakes = 14, rows = 10, j, k;
    function rowY(j, u) { var y0 = lerp(310, 408, j / (rows - 1)); return y0 + lerp(22, 12, j / (rows - 1)) * Math.sqrt(Math.max(0, 1 - u * u)); }
    function uAt(k) { return Math.sin(lerp(-1.35, 1.35, k / stakes)) / Math.sin(1.35); }
    for (j = 0; j < rows; j++) {
      for (k = 0; k < stakes; k++) {
        var u0 = uAt(k), u1 = uAt(k + 1), y0 = lerp(310, 408, j / (rows - 1)), rx = RX(y0);
        var p = new Path2D(); for (var s = 0; s <= 6; s++) { var u = lerp(u0, u1, s / 6); var X = cx + u * rx, Y = rowY(j, u); s ? p.lineTo(X, Y) : p.moveTo(X, Y); }
        var lit = 0.1 - (u0 + u1) * 0.18 - Math.abs(u0 + u1) * 0.12;
        c.lineCap = "round"; c.lineWidth = 9.5; c.strokeStyle = css(sh(S, lit)); c.stroke(p);
        c.save(); c.translate(0, -2.4); c.lineWidth = 3; c.strokeStyle = css(sh(S, lit + 0.3), 0.7); c.stroke(p); c.restore();
        c.save(); c.translate(0, 3.6); c.lineWidth = 1.6; c.strokeStyle = "rgba(40,24,10,.45)"; c.stroke(p); c.restore();
      }
      for (k = 1; k < stakes; k++) {
        if ((j + k) % 2) continue;
        var uu = uAt(k), yy = rowY(j, uu), xx = cx + uu * RX(yy), wS = 7 * (0.55 + 0.45 * Math.sqrt(1 - uu * uu));
        c.save(); blur(c, 1.5); c.fillStyle = "rgba(30,16,6,.5)"; c.fillRect(xx + wS * 0.5, yy - 6, 2.5, 12); c.restore();
        rod(c, xx, yy - 6.5, xx - uu * 0.8, yy + 6.5, wS, wS, sh(S, 0.05 - uu * 0.25), { pencil: false });
      }
    }
    var gl = c.createRadialGradient(190, 320, 10, 200, 330, 230); gl.addColorStop(0, "rgba(255,236,200,.18)"); gl.addColorStop(0.5, "rgba(0,0,0,0)"); gl.addColorStop(1, "rgba(30,16,6,.45)"); c.fillStyle = gl; c.fillRect(120, 290, 272, 140);
    c.restore();
    pencil(c, body, [70, 44, 20], 159, 1.3);
    braid(0.02 * Math.PI, 0.98 * Math.PI);
    tube(c, [142, 302, 150, 166, 362, 166, 370, 302], 11, sh(S, -0.08), { wrap: true });
  };

  /* 가리비 껍데기: 부채꼴 골(하나하나 둥근 원통) · 나이테 같은 성장선 · 분홍과 크림 색 띠 · 귀 두 개 */
  DETAIL.shell = function (c) {
    cast(c, 258, 416, 150, 15, 0.38);
    var hx = 256, hy = 392, R = 158, n = 15, A0 = -Math.PI + 0.36, A1 = -0.36;
    function edge(a) { return R * (0.94 + 0.06 * Math.cos((a + 1.2) * 1.2)); }
    var fan = new Path2D(); fan.moveTo(hx - 16, hy + 2);
    for (var i = 0; i <= n * 10; i++) { var t = i / (n * 10), a = lerp(A0, A1, t), bump = Math.pow(Math.abs(Math.sin(t * n * Math.PI)), 0.6) * 0.045; fan.lineTo(hx + Math.cos(a) * edge(a) * (0.955 + bump), hy + Math.sin(a) * edge(a) * (0.955 + bump) * 0.93); }
    fan.lineTo(hx + 16, hy + 2); fan.closePath();
    var base = C("#EBC0A0");
    [-1, 1].forEach(function (d) { var ear = poly([[hx + d * 10, hy - 2], [hx + d * 60, hy - 30], [hx + d * 58, hy - 4], [hx + d * 14, hy + 12]]); form(c, ear, sh(base, -0.08), { box: box(hx + Math.min(d * 60, d * 10), hy - 30, 50, 42), seed: 176 + d, mottle: 4, hl: 0.25, tex: function (q) { for (var k = 0; k < 6; k++) { q.strokeStyle = "rgba(150,80,55,.3)"; q.lineWidth = 1; q.beginPath(); q.moveTo(hx + d * 12, hy + 8 - k * 2); q.lineTo(hx + d * 58, hy - 6 - k * 4.5); q.stroke(); } } }); });
    c.save(); c.fillStyle = css(base); c.fill(fan); c.restore();
    for (i = 0; i < n; i++) {
      var a0 = lerp(A0, A1, i / n), a1 = lerp(A0, A1, (i + 1) / n), am = (a0 + a1) / 2, rib = new Path2D();
      rib.moveTo(hx, hy);
      for (var s = 0; s <= 10; s++) { var aa = lerp(a0, a1, s / 10), bb = Math.pow(Math.sin(s / 10 * Math.PI), 0.6) * 0.045; rib.lineTo(hx + Math.cos(aa) * edge(aa) * (0.955 + bb), hy + Math.sin(aa) * edge(aa) * (0.955 + bb) * 0.93); }
      rib.closePath();
      var tx = -Math.sin(am), ty = Math.cos(am), lit = tx * LX + ty * LY > 0 ? 1 : -1, midR = R * 0.62;
      var ex = hx + Math.cos(am) * midR, ey = hy + Math.sin(am) * midR * 0.93, hw = midR * (a1 - a0) * 0.5;
      var g = c.createLinearGradient(ex + tx * hw * lit, ey + ty * hw * lit, ex - tx * hw * lit, ey - ty * hw * lit);
      var tone = i % 3 === 1 ? sh(base, -0.06) : base;
      g.addColorStop(0, css(sh(tone, 0.02))); g.addColorStop(0.25, css(sh(tone, 0.3))); g.addColorStop(0.55, css(tone)); g.addColorStop(0.88, css(sh(tone, -0.32))); g.addColorStop(1, css(sh(tone, -0.45)));
      c.fillStyle = g; c.fill(rib);
    }
    form(c, fan, base, { box: box(hx - R, hy - R, R * 2, R), seed: 171, hl: 0.3, sd: 0.4, mottle: 0, lx: 0.35, ly: 0.2, keep: true, after: function (q, r) {
      q.save(); q.globalCompositeOperation = "multiply";
      for (var j = 0; j < 7; j++) { var rr = R * lerp(0.28, 0.98, j / 6); q.strokeStyle = j % 2 ? "rgba(214,120,90,.35)" : "rgba(246,226,205,.5)"; q.lineWidth = R * 0.07; q.beginPath(); q.ellipse(hx, hy, rr, rr * 0.93, 0, A0, A1); q.stroke(); }
      q.restore();
      for (j = 0; j < 22; j++) { var r2 = R * lerp(0.18, 0.96, r()); q.strokeStyle = r() < 0.5 ? "rgba(150,80,55,.22)" : "rgba(255,248,240,.35)"; q.lineWidth = lerp(0.6, 1.4, r()); q.beginPath(); q.ellipse(hx, hy, r2, r2 * 0.93, 0, A0 + 0.05, A1 - 0.05); q.stroke(); }
    } });
  };

  /* 종이배: 공책을 접어 만든 배. 면마다 빛이 다르고, 접힌 등에 밝은 선, 골에 그늘, 옅은 공책 줄 */
  function paperPlane(c, pts, shade, seed, o) {
    o = o || {}; var P = poly(jit(pts, 1.4, seed)), xs = pts.map(function (q) { return q[0]; }), ys = pts.map(function (q) { return q[1]; });
    var bx = Math.min.apply(0, xs), by = Math.min.apply(0, ys), bw = Math.max.apply(0, xs) - bx, bh = Math.max.apply(0, ys) - by;
    form(c, P, sh("#F1EADA", shade), { box: box(bx, by, bw, bh), seed: seed, mottle: 8, vary: 0.06, hl: o.hl == null ? 0.25 : o.hl, sd: o.sd == null ? 0.2 : o.sd, lx: o.lx, ly: o.ly, bounceA: 0.15, wet: 0.14, lw: 1.1, line: [110, 96, 76], tex: function (q, r) {
      var ang = o.ang || 0, ca = Math.cos(ang), sa = Math.sin(ang);
      for (var k = -12; k <= 12; k++) { var off = k * 13 + (o.phase || 0); q.strokeStyle = "rgba(110,140,180,.22)"; q.lineWidth = 1; q.beginPath(); q.moveTo(256 - ca * 300 - sa * off, 300 - sa * 300 + ca * off); q.lineTo(256 + ca * 300 - sa * off, 300 + sa * 300 + ca * off); q.stroke(); }
      if (o.margin) { q.strokeStyle = "rgba(200,90,80,.35)"; q.lineWidth = 1.2; q.beginPath(); q.moveTo(o.margin[0], o.margin[1]); q.lineTo(o.margin[2], o.margin[3]); q.stroke(); }
      for (k = 0; k < 40; k++) { var fx = bx + r() * bw, fy = by + r() * bh; q.strokeStyle = "rgba(160,140,110,.12)"; q.lineWidth = 0.6; q.beginPath(); q.moveTo(fx, fy); q.lineTo(fx + (r() - 0.5) * 10, fy + (r() - 0.5) * 4); q.stroke(); }
    } });
  }
  function crease(c, x1, y1, x2, y2, light) { c.save(); c.lineCap = "round"; if (light) { c.strokeStyle = "rgba(255,253,246,.85)"; c.lineWidth = 1.6; } else { blur(c, 2); c.strokeStyle = "rgba(80,64,40,.35)"; c.lineWidth = 4; } c.beginPath(); c.moveTo(x1, y1); c.lineTo(x2, y2); c.stroke(); c.restore(); }
  DETAIL.boat = function (c) {
    cast(c, 258, 406, 160, 15, 0.38);
    paperPlane(c, [[256, 168], [192, 334], [256, 334]], 0.04, 71, { ang: -1.2, hl: 0.35, lx: 0.3, ly: 0.2 });
    paperPlane(c, [[256, 168], [256, 334], [320, 334]], -0.14, 72, { ang: 1.2, phase: 5, sd: 0.3 });
    crease(c, 256, 172, 256, 332, true);
    paperPlane(c, [[116, 316], [256, 334], [256, 402], [166, 402]], 0, 73, { ang: 0.1, margin: [150, 320, 186, 402], lx: 0.2 });
    paperPlane(c, [[256, 334], [396, 316], [346, 402], [256, 402]], -0.1, 74, { ang: -0.1, phase: 6, sd: 0.3 });
    crease(c, 256, 336, 256, 400, false);
    paperPlane(c, [[116, 316], [204, 338], [166, 402]], -0.06, 75, { ang: 0.9, hl: 0.2 });
    paperPlane(c, [[396, 316], [308, 338], [346, 402]], -0.2, 76, { ang: -0.9, sd: 0.35 });
    crease(c, 118, 317, 254, 333, true); crease(c, 258, 333, 394, 317, true);
  };

  /* 빨간 버섯: 윤이 나는 갓 · 도톰하게 솟은 흰 사마귀 · 갓 밑 주름 · 줄기 턱받이 · 이끼 밑동 */
  DETAIL.mushroom = function (c) {
    cast(c, 258, 420, 130, 15, 0.4);
    var r = rng(40);
    for (var i = 0; i < 16; i++) { var mx = lerp(186, 330, r()), my = 414 + (r() - 0.5) * 8, mr = lerp(10, 20, r()); form(c, ell(mx, my, mr, mr * 0.55), r() < 0.5 ? PAL.moss : PAL.olive, { box: box(mx - mr, my - mr, mr * 2, mr * 2), seed: 400 + i, mottle: 3, lw: 0.8, hl: 0.3 }); }
    var stem = smoothPath(wobble([[230, 300], [236, 340], [232, 380], [222, 404], [226, 418], [256, 424], [288, 418], [292, 404], [282, 380], [278, 340], [282, 300]], 1, 41));
    form(c, stem, "#EFE3C8", { box: box(220, 300, 74, 124), seed: 41, hl: 0.4, sd: 0.5, tex: function (q, rr) { for (var k = 0; k < 16; k++) { var u = lerp(228, 286, rr()); q.strokeStyle = rr() < 0.5 ? "rgba(170,140,100,.25)" : "rgba(255,255,255,.4)"; q.lineWidth = lerp(0.6, 1.4, rr()); q.beginPath(); q.moveTo(u, 300); q.bezierCurveTo(u + (rr() - 0.5) * 6, 340, u + (rr() - 0.5) * 8, 380, u + (u - 256) * 0.2, 418); q.stroke(); } }, after: function (q) { q.save(); blur(q, 8); q.fillStyle = "rgba(60,30,14,.5)"; q.beginPath(); q.ellipse(256, 304, 40, 14, 0, 0, 7); q.fill(); q.restore(); } });
    var ring = new Path2D(); ring.moveTo(228, 322); ring.quadraticCurveTo(256, 316, 284, 322); for (i = 0; i <= 8; i++) { var x = lerp(288, 224, i / 8), y = 346 + (i % 2 ? 6 : 0) + Math.sin(i / 8 * Math.PI) * 4; ring.lineTo(x, y); } ring.closePath();
    form(c, ring, "#F4ECDA", { box: box(224, 316, 64, 38), seed: 42, hl: 0.35, sd: 0.4, mottle: 4, lw: 1 });
    var gills = ell(256, 300, 120, 17);
    form(c, gills, "#E3CFA8", { box: box(136, 283, 240, 34), seed: 43, hl: 0.1, sd: 0.3, mottle: 4, bounceA: 0, after: function (q) { for (var k = 0; k < 60; k++) { var a = Math.PI * (k / 59); q.strokeStyle = "rgba(140,105,60,.35)"; q.lineWidth = 0.9; q.beginPath(); q.moveTo(256 + Math.cos(a) * 30, 302 + Math.sin(a) * 4); q.lineTo(256 + Math.cos(a) * 120, 300 + Math.sin(a) * 17); q.stroke(); } } });
    var capPts = []; for (i = 0; i <= 60; i++) { var t = i / 60, a2 = Math.PI + t * Math.PI; capPts.push([256 + Math.cos(a2) * 126, 298 + Math.sin(a2) * 104 * (0.9 + 0.1 * Math.sin(t * Math.PI))]); }
    for (i = 0; i <= 20; i++) { var t2 = i / 20; capPts.push([lerp(382, 130, t2), 300 + Math.sin(t2 * Math.PI) * 12]); }
    var cap = smoothPath(wobble(capPts, 1.6, 44));
    form(c, cap, "#B83E2A", { box: box(130, 194, 252, 118), seed: 44, hl: 0.55, sd: 0.55, lx: 0.32, ly: 0.2, tex: function (q) { var go = q.createLinearGradient(0, 260, 0, 312); go.addColorStop(0, "rgba(230,120,50,0)"); go.addColorStop(1, "rgba(230,120,50,.45)"); q.fillStyle = go; q.fillRect(120, 240, 272, 80); }, after: function (q, rr) {
      q.save(); blur(q, 8); q.fillStyle = "rgba(255,245,235,.5)"; q.beginPath(); q.ellipse(206, 232, 40, 16, -0.4, 0, 7); q.fill(); q.restore();
      var warts = [[196, 256, 14], [252, 226, 17], [312, 248, 13], [224, 284, 10], [290, 286, 11], [164, 284, 8], [346, 282, 8], [278, 214, 8], [226, 214, 7], [338, 258, 7]];
      warts.forEach(function (w, k) { var fx = 1 - Math.abs(w[0] - 256) / 150, s = w[2], pts2 = []; for (var m = 0; m < 9; m++) { var a = m / 9 * 6.28; pts2.push([w[0] + Math.cos(a) * s * (0.8 + rr() * 0.35) * (0.7 + 0.3 * fx), w[1] + Math.sin(a) * s * 0.62 * (0.8 + rr() * 0.35)]); }
        var wp = smoothPath(pts2);
        q.save(); blur(q, 2.5); q.translate(2.5, 3.5); q.fillStyle = "rgba(70,14,6,.45)"; q.fill(wp); q.restore();
        var gw = q.createRadialGradient(w[0] - s * 0.3, w[1] - s * 0.3, 0, w[0], w[1], s); gw.addColorStop(0, "#FFFDF6"); gw.addColorStop(0.7, "#F1E8D8"); gw.addColorStop(1, "#D8C6AA"); q.fillStyle = gw; q.fill(wp);
        q.strokeStyle = "rgba(150,110,80,.35)"; q.lineWidth = 0.8; q.stroke(wp); });
    } });
    for (i = 0; i < 9; i++) { var bx0 = lerp(196, 322, i / 8) + (r() - 0.5) * 10; if (bx0 > 222 && bx0 < 292) continue; var hgt = lerp(28, 56, r()), lean = (r() - 0.5) * 22, blade = svg("M" + (bx0 - 3) + ",420 Q" + (bx0 + lean * 0.4) + "," + (420 - hgt * 0.6) + " " + (bx0 + lean) + "," + (420 - hgt) + " Q" + (bx0 + lean * 0.3 + 2) + "," + (420 - hgt * 0.5) + " " + (bx0 + 3) + ",420 Z"); var gb = c.createLinearGradient(0, 420, 0, 420 - hgt); gb.addColorStop(0, css(PAL.oliveD)); gb.addColorStop(1, css(PAL.sprout)); c.fillStyle = gb; c.fill(blade); }
  };

  /* 물뿌리개: 금속 원통의 날카로운 반사 띠 · 이음새와 리벳 · 녹슨 얼룩 · 구멍 뚫린 물뿌리개 꼭지 */
  DETAIL.can = function (c) {
    var M = C("#6E8C7A");
    cast(c, 252, 420, 170, 17, 0.4);
    tube(c, [300, 254, 312, 150, 406, 160, 332, 336], 13, sh(M, -0.08), { metal: true });
    rod(c, 196, 386, 92, 240, 26, 12, sh(M, -0.04), { metal: true, seed: 141 });
    var roseP = svg("M72,230 C70,212 94,200 108,212 L114,226 C108,240 84,248 76,242 Z");
    form(c, roseP, sh(M, -0.1), { box: box(70, 200, 46, 48), seed: 142, hl: 0.5, mottle: 4, after: function (q) { q.fillStyle = "rgba(30,40,34,.6)"; for (var k = 0; k < 9; k++) { q.beginPath(); q.arc(82 + (k % 3) * 7, 216 + Math.floor(k / 3) * 7, 1.6, 0, 7); q.fill(); } } });
    var body = svg("M178,256 L178,398 C178,416 206,424 256,424 C306,424 334,416 334,398 L334,256 C334,272 178,272 178,256 Z");
    c.save(); var g = c.createLinearGradient(178, 0, 334, 0);
    [[0, -0.35], [0.1, -0.12], [0.2, 0.55], [0.27, 0.2], [0.55, 0], [0.8, -0.3], [0.92, -0.12], [1, -0.42]].forEach(function (s) { g.addColorStop(s[0], css(sh(M, s[1]))); });
    c.fillStyle = g; c.fill(body); c.restore();
    form(c, body, M, { box: box(178, 256, 156, 168), seed: 143, hl: 0.12, sd: 0.25, mottle: 12, vary: 0.1, bounceA: 0.2, tex: function (q) { q.save(); q.globalAlpha = 0.88; q.fillStyle = g; q.fillRect(170, 250, 170, 180); q.restore(); }, after: function (q, r) {
      [300, 384].forEach(function (yy) { var sp = curve([178, yy, 220, yy + 16, 292, yy + 16, 334, yy]); q.lineWidth = 3; q.strokeStyle = "rgba(255,255,255,.35)"; q.stroke(sp); q.save(); q.translate(0, 3); q.lineWidth = 1.6; q.strokeStyle = "rgba(20,30,24,.45)"; q.stroke(sp); q.restore(); });
      for (var k = 0; k < 5; k++) { var rx0 = 206 + k * 12, ry0 = 312 + k * 2.8; q.fillStyle = "rgba(20,30,24,.5)"; q.beginPath(); q.arc(rx0 + 0.8, ry0 + 0.8, 2.3, 0, 7); q.fill(); q.fillStyle = "rgba(255,255,255,.55)"; q.beginPath(); q.arc(rx0, ry0, 1.6, 0, 7); q.fill(); }
      q.save(); blur(q, 3); for (k = 0; k < 10; k++) { q.fillStyle = "rgba(150,110,60," + lerp(0.08, 0.2, r()) + ")"; q.beginPath(); q.ellipse(190 + r() * 136, 340 + r() * 80, lerp(4, 12, r()), lerp(3, 7, r()), 0, 0, 7); q.fill(); } q.restore();
    } });
    form(c, ell(256, 256, 78, 16), sh(M, 0.12), { box: box(178, 240, 156, 32), seed: 144, mottle: 2, hl: 0.5, bounceA: 0 });
    form(c, ell(256, 257, 62, 10), sh(M, -0.55), { box: box(194, 247, 124, 20), mottle: 0, pencil: false, hl: 0, bounceA: 0 });
    rod(c, 180, 380, 204, 396, 18, 18, sh(M, -0.1), { metal: true, seed: 145 });
  };

  PROPS.forEach(function (p) { if (DETAIL[p.id]) p.draw = DETAIL[p.id]; });
