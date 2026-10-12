  /* ───────── 종이 · 파스텔 · 연필 ─────────
     매끈한 도형이나 그러데이션을 쓰지 않습니다. 짧은 붓질 수백 개를 겹치고, 종이 결(튀어나온 곳)에만 색이 묻게 합니다.
     밝고 어두움은 붓질의 밀도로 만듭니다. 선은 필압이 변하는 연필로 두 번 긋고, 군데군데 끊깁니다.
     좌표는 모두 '단위'로 쓰고, U(단위당 픽셀)를 곱해 그립니다. */
  var U = 1;
  function setU(v) { U = v; }
  function C(c) { return typeof c === "string" ? hex(c) : c; }
  function css(c, a) { c = C(c); return "rgba(" + c[0] + "," + c[1] + "," + c[2] + "," + (a == null ? 1 : a) + ")"; }
  function mix(a, b, t) { a = C(a); b = C(b); return [0, 1, 2].map(function (i) { return Math.round(a[i] + (b[i] - a[i]) * t); }); }
  function sh(c, k) { return shade(C(c), k); }
  var INK = "#4A4238";

  // 종이 결 (이어 붙여도 이음새가 없는 256 타일)
  var TOOTH = (function () {
    var N = 256, Hh = new Float32Array(N * N), r = rng(4242), i;
    function octave(cell, amp) {
      var g = N / cell, grid = new Float32Array(g * g);
      for (var k = 0; k < g * g; k++) grid[k] = r();
      for (var y = 0; y < N; y++) for (var x = 0; x < N; x++) {
        var gx = x / cell, gy = y / cell, x0 = Math.floor(gx), y0 = Math.floor(gy), fx = gx - x0, fy = gy - y0;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        var a = grid[(y0 % g) * g + (x0 % g)], b = grid[(y0 % g) * g + ((x0 + 1) % g)], c = grid[((y0 + 1) % g) * g + (x0 % g)], d = grid[((y0 + 1) % g) * g + ((x0 + 1) % g)];
        Hh[y * N + x] += amp * (a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy);
      }
    }
    octave(32, 0.2); octave(8, 0.3); octave(4, 0.28); octave(2, 0.22);
    var mn = 9, mx = -9; for (i = 0; i < N * N; i++) { Hh[i] += 0.08 * r(); mn = Math.min(mn, Hh[i]); mx = Math.max(mx, Hh[i]); }
    for (i = 0; i < N * N; i++) Hh[i] = (Hh[i] - mn) / (mx - mn);
    function mask(cov) {
      var c = document.createElement("canvas"); c.width = c.height = N; var x = c.getContext("2d"), d = x.createImageData(N, N), t0 = 1 - cov;
      for (var k = 0; k < N * N; k++) { var v = (Hh[k] - t0 + 0.2) / 0.16; v = v < 0 ? 0 : v > 1 ? 1 : v; d.data[k * 4 + 3] = v * 255; }
      x.putImageData(d, 0, 0); return c;
    }
    var paper = document.createElement("canvas"); paper.width = paper.height = N;
    var px = paper.getContext("2d"), pd = px.createImageData(N, N);
    for (i = 0; i < N * N; i++) { var v = 205 + Hh[i] * 50; pd.data[i * 4] = v; pd.data[i * 4 + 1] = v - 2; pd.data[i * 4 + 2] = v - 6; pd.data[i * 4 + 3] = 255; }
    px.putImageData(pd, 0, 0);
    return { masks: [0.3, 0.45, 0.6, 0.75, 0.9].map(mask), paper: paper };
  })();
  function toothPattern(ctx, which, ox, oy) {
    var p = ctx.createPattern(which, "repeat"), k = Math.max(0.7, U * 0.55);
    if (p.setTransform) p.setTransform(new DOMMatrix().translate(ox, oy).scale(k));
    return p;
  }
  var PT = document.createElement("canvas").getContext("2d");

  // 한 겹: 따로 그린 뒤 종이 결로 걸러서 얹음
  function layer(ctx, bb, draw, press, o) {
    o = o || {};
    var pad = o.pad == null ? 8 : o.pad, x0 = Math.floor((bb[0] - pad) * U), y0 = Math.floor((bb[1] - pad) * U), w = Math.ceil((bb[2] + pad * 2) * U) + 2, h = Math.ceil((bb[3] + pad * 2) * U) + 2;
    if (w < 2 || h < 2) return;
    var L = document.createElement("canvas"); L.width = w; L.height = h; var lx = L.getContext("2d");
    lx.setTransform(U, 0, 0, U, -x0, -y0);
    draw(lx);
    lx.setTransform(1, 0, 0, 1, 0, 0);
    if (press < 1) { lx.globalCompositeOperation = "destination-in"; lx.fillStyle = toothPattern(lx, TOOTH.masks[Math.max(0, Math.min(4, Math.round(press * 4)))], -x0, -y0); lx.fillRect(0, 0, w, h); }
    ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = o.alpha == null ? 1 : o.alpha; if (o.op) ctx.globalCompositeOperation = o.op; ctx.drawImage(L, x0, y0); ctx.restore();
  }

  function bbox(pts) { var a = 1e9, b = 1e9, c = -1e9, d = -1e9; pts.forEach(function (q) { a = Math.min(a, q[0]); b = Math.min(b, q[1]); c = Math.max(c, q[0]); d = Math.max(d, q[1]); }); return [a, b, c - a, d - b]; }
  function smoothPath(pts) {
    var p = new Path2D(), n = pts.length;
    for (var i = 0; i < n; i++) { var a = pts[i], b = pts[(i + 1) % n], mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2; if (i === 0) p.moveTo(mx, my); else p.quadraticCurveTo(a[0], a[1], mx, my); }
    var a0 = pts[0], b0 = pts[1]; p.quadraticCurveTo(a0[0], a0[1], (a0[0] + b0[0]) / 2, (a0[1] + b0[1]) / 2); p.closePath(); return p;
  }
  // 손으로 그린 듯 흔들리는 윤곽
  function blob(cx, cy, rx, ry, seed, wob, rot) {
    var r = rng(seed || 1), f = [r() * 6, r() * 6, r() * 6, r() * 6], n = Math.max(28, Math.round((rx + ry) * 0.9)), pts = [], c = Math.cos(rot || 0), s = Math.sin(rot || 0);
    for (var i = 0; i < n; i++) {
      var t = i / n * Math.PI * 2, k = 1 + (wob == null ? 0.06 : wob) * (Math.sin(t * 2 + f[0]) * 0.55 + Math.sin(t * 3 + f[1]) * 0.35 + Math.sin(t * 5 + f[2]) * 0.2) + (r() - 0.5) * 0.012;
      var x = Math.cos(t) * rx * k, y = Math.sin(t) * ry * k; pts.push([cx + x * c - y * s, cy + x * s + y * c]);
    }
    return pts;
  }
  function organic(pts, amt, seed, step) {
    step = step || 3; var r = rng(seed || 2), out = [], n = pts.length;
    for (var i = 0; i < n; i++) { var a = pts[i], b = pts[(i + 1) % n], L = Math.hypot(b[0] - a[0], b[1] - a[1]), m = Math.max(1, Math.round(L / step)); for (var k = 0; k < m; k++) { var t = k / m; out.push([lerp(a[0], b[0], t), lerp(a[1], b[1], t)]); } }
    var f = [r() * 6, r() * 6, r() * 6, r() * 6], N = out.length;
    return out.map(function (q, i) { var t = i / N * Math.PI * 2; return [q[0] + amt * (Math.sin(t * 3 + f[0]) * 0.5 + Math.sin(t * 8 + f[1]) * 0.3), q[1] + amt * (Math.sin(t * 4 + f[2]) * 0.5 + Math.sin(t * 9 + f[3]) * 0.3)]; });
  }
  function bez(p, n) { var out = []; n = n || 24; for (var s = 0; s + 7 < p.length; s += 6) for (var i = s ? 1 : 0; i <= n; i++) { var t = i / n, a = 1 - t; out.push([a * a * a * p[s] + 3 * a * a * t * p[s + 2] + 3 * a * t * t * p[s + 4] + t * t * t * p[s + 6], a * a * a * p[s + 1] + 3 * a * a * t * p[s + 3] + 3 * a * t * t * p[s + 5] + t * t * t * p[s + 7]]); } return out; }

  // 파스텔로 칠하기
  function pastel(ctx, pts, col, o) {
    o = o || {}; col = C(col);
    var path = smoothPath(pts), bb = bbox(pts), r = rng(o.seed || 1);
    var ang = o.ang == null ? -0.62 : o.ang, len = o.len || Math.max(3, Math.min(bb[2], bb[3]) * 0.22), wid = o.wid || Math.max(1.1, len * 0.24);
    var n = Math.round(bb[2] * bb[3] / (len * wid) * 1.7 * (o.dense || 1));
    if (o.smudge !== 0) layer(ctx, bb, function (x) { x.filter = "blur(" + (wid * U * 0.9).toFixed(1) + "px)"; x.fillStyle = css(col, o.smudge || 0.5); x.fill(path); x.filter = "none"; }, 0.95, { pad: wid * 3 });
    var cx = bb[0] + bb[2] / 2, cy = bb[1] + bb[3] / 2;
    layer(ctx, bb, function (x) {
      x.lineCap = "round";
      if (o.clip) { x.save(); x.clip(path); }
      for (var i = 0; i < n; i++) {
        var px = bb[0] + r() * bb[2], py = bb[1] + r() * bb[3];
        if (!PT.isPointInPath(path, px, py)) continue;
        var s = Math.max(0, Math.min(1, 0.5 + 0.42 * (((px - cx) / (bb[2] / 2 || 1)) * 0.62 + ((py - cy) / (bb[3] / 2 || 1)) * 0.78))), v = r(), c2;
        if (o.shade !== false && v < s * 0.6) c2 = sh(col, -(o.dark == null ? 0.24 : o.dark) * lerp(0.5, 1.2, r()));
        else if (o.shade !== false && v > 0.78 + s * 0.22) c2 = sh(col, o.light == null ? 0.2 : o.light);
        else c2 = sh(col, (r() - 0.5) * 0.08);
        var a = ang + (r() - 0.5) * (o.spread || 0.5), l = len * lerp(0.5, 1.25, r());
        x.strokeStyle = css(c2, lerp(0.35, 0.8, r())); x.lineWidth = wid * lerp(0.6, 1.3, r());
        x.beginPath(); x.moveTo(px - Math.cos(a) * l / 2, py - Math.sin(a) * l / 2); x.quadraticCurveTo(px + (r() - 0.5) * wid, py + (r() - 0.5) * wid, px + Math.cos(a) * l / 2, py + Math.sin(a) * l / 2); x.stroke();
      }
      if (o.clip) x.restore();
    }, o.press == null ? 0.6 : o.press, { pad: len });
    if (o.line !== false) pencil(ctx, pts, o.lineCol || INK, o.seed, o.lw, { alpha: o.lineA });
  }

  // 연필 선: 필압이 변하고, 두 번 긋고, 끊기거나 조금 겹침
  function pencil(ctx, pts, col, seed, w, o) {
    o = o || {}; col = C(col);
    var r = rng((seed || 1) * 13 + 7), closed = !o.open, lw = w || 1, P = pts, bb = bbox(pts), A = o.alpha == null ? 1 : o.alpha;
    layer(ctx, bb, function (x) {
      x.lineCap = "round";
      for (var pass = 0; pass < (o.passes || 2); pass++) {
        var ox = (r() - 0.5) * lw * 1.4, oy = (r() - 0.5) * lw * 1.4, start = closed ? Math.floor(r() * P.length) : 0, cnt = closed ? Math.floor(P.length * lerp(0.82, 1.06, r())) : P.length - 1, ph = r() * 10;
        for (var k = 0; k < cnt; k++) {
          var i = (start + k) % P.length, j = (i + 1) % P.length; if (!closed && j === 0) break;
          var pr = 0.6 + 0.4 * Math.sin(k * 0.13 + ph) * Math.sin(k * 0.041 + ph * 2);
          if (pr < 0.28) continue;
          x.strokeStyle = css(col, (pass ? 0.35 : 0.62) * pr * A); x.lineWidth = lw * (0.65 + 0.6 * pr);
          x.beginPath(); x.moveTo(P[i][0] + ox, P[i][1] + oy); x.lineTo(P[j][0] + ox, P[j][1] + oy); x.stroke();
        }
      }
    }, o.press == null ? 0.7 : o.press, { pad: 4 });
  }
  function scribble(ctx, pts, col, seed, w, o) { o = o || {}; o.open = true; pencil(ctx, pts, col, seed, w, o); }
  function smudge(ctx, cx, cy, rx, ry, col, a) { layer(ctx, [cx - rx, cy - ry, rx * 2, ry * 2], function (x) { x.filter = "blur(" + (ry * U * 0.6).toFixed(1) + "px)"; x.fillStyle = css(col, a); x.beginPath(); x.ellipse(cx, cy, rx, ry, 0, 0, 7); x.fill(); x.filter = "none"; }, 0.85, { pad: ry * 2 }); }

  // 종이 바탕
  function paper(ctx, W, H, col) {
    ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.fillStyle = css(col); ctx.fillRect(0, 0, W, H);
    ctx.globalCompositeOperation = "multiply"; ctx.globalAlpha = 0.28; ctx.fillStyle = toothPattern(ctx, TOOTH.paper, 0, 0); ctx.fillRect(0, 0, W, H); ctx.restore();
  }
  // 넓게 문지른 파스텔 (하늘 · 먼 언덕): 가장자리로 갈수록 붓질이 드물어져 경계가 보이지 않음
  // fade: "down" 위가 진하고 아래로 사라짐, "up" 반대, "both" 가운데가 진함
  function wash(ctx, x, y, w, h, col, o) {
    o = o || {}; col = C(col);
    var r = rng(o.seed || 3), len = o.len || 70, wid = o.wid || 13, fade = o.fade || "both";
    function k(py) { var t = (py - y) / h; t = t < 0 ? 0 : t > 1 ? 1 : t; return fade === "down" ? 1 - t : fade === "up" ? t : Math.sin(t * Math.PI); }
    var bb = [x, y, w, h];
    layer(ctx, bb, function (c) {
      var g = c.createLinearGradient(0, y, 0, y + h);
      for (var i = 0; i <= 10; i++) g.addColorStop(i / 10, css(col, (o.smudge == null ? 0.3 : o.smudge) * k(y + h * i / 10)));
      c.filter = "blur(" + (wid * U).toFixed(1) + "px)"; c.fillStyle = g; c.fillRect(x - wid, y, w + wid * 2, h); c.filter = "none";
    }, 0.95, { pad: wid * 3 });
    var n = Math.round(w * h / (len * wid) * 1.5 * (o.dense || 1));
    layer(ctx, bb, function (c) {
      c.lineCap = "round";
      for (var i = 0; i < n; i++) {
        var px = x + r() * w, py = y + r() * h; if (r() > k(py)) continue;
        var a = (o.ang == null ? -0.08 : o.ang) + (r() - 0.5) * 0.25, l = len * lerp(0.4, 1.2, r());
        c.strokeStyle = css(sh(col, (r() - 0.5) * 0.1), lerp(0.25, 0.6, r())); c.lineWidth = wid * lerp(0.5, 1.2, r());
        c.beginPath(); c.moveTo(px - Math.cos(a) * l / 2, py - Math.sin(a) * l / 2); c.quadraticCurveTo(px, py + (r() - 0.5) * wid, px + Math.cos(a) * l / 2, py + Math.sin(a) * l / 2); c.stroke();
      }
    }, o.press || 0.4, { pad: len });
  }

  /* ───────── 하루: 같은 번호 · 같은 특징, 손그림으로 ───────── */
  function drawHaru(ctx, t, cx, groundY, width, opt) {
    opt = opt || {};
    var Wt = width / 0.42 / t.size * t.size, o = outline(t, 0, 0, Wt), bb = bbox(o.pts), dy = groundY - (bb[1] + bb[3]);
    var pts = o.pts.map(function (q) { return [q[0] + cx, q[1] + dy]; }), cy = dy, base = t.base;
    smudge(ctx, cx + o.w * 0.08, groundY + 1, o.w * 1.05, o.h * 0.16, "#3B3325", 0.35);
    pastel(ctx, pts, base, { seed: t.texSeed % 1000 + 3, len: o.w * 0.13, wid: o.w * 0.04, press: 0.72, clip: true, dense: 1.3, dark: 0.3, light: 0.22, lw: Math.max(1, o.w * 0.012) });
    var pat = t.stone.pattern, r = rng(t.texSeed), path = smoothPath(pts), i;
    // 무늬는 몇 번만 스치듯
    layer(ctx, bbox(pts), function (x) {
      x.save(); x.clip(path); x.lineCap = "round";
      if (pat === "salt" || pat === "speckle" || pat === "grain" || pat === "fine") { var nn = pat === "speckle" ? 26 : 40; for (i = 0; i < nn; i++) { var px = cx + (r() - 0.5) * o.w * 2, py = cy + (r() - 0.5) * o.h * 2; x.fillStyle = r() < 0.5 ? css(sh(base, -0.45), 0.6) : css(sh(base, 0.4), 0.5); x.beginPath(); x.arc(px, py, o.w * lerp(0.008, pat === "speckle" ? 0.03 : 0.018, r()), 0, 7); x.fill(); } }
      if (pat === "layers" || pat === "marble") for (i = 0; i < 5; i++) { var yy = cy + (r() - 0.5) * o.h * 1.4; x.strokeStyle = css(pat === "marble" ? "#6E6E72" : sh(base, 0.35), 0.4); x.lineWidth = o.w * 0.012; x.beginPath(); x.moveTo(cx - o.w * 1.2, yy); x.bezierCurveTo(cx - o.w * 0.3, yy + (r() - 0.5) * o.h * 0.5, cx + o.w * 0.3, yy + (r() - 0.5) * o.h * 0.5, cx + o.w * 1.2, yy + (r() - 0.5) * o.h * 0.3); x.stroke(); }
      if (pat === "vein" || pat === "ring") { var y0 = cy + (r() - 0.5) * o.h * 0.6; x.strokeStyle = "rgba(246,242,232,.85)"; x.lineWidth = o.h * 0.09; x.beginPath(); x.moveTo(cx - o.w * 1.3, y0); x.bezierCurveTo(cx - o.w * 0.3, y0 + o.h * 0.25, cx + o.w * 0.3, y0 - o.h * 0.2, cx + o.w * 1.3, y0 + o.h * 0.1); x.stroke(); }
      if (pat === "mottle" || pat === "jasper") for (i = 0; i < 9; i++) { x.fillStyle = css(sh(base, (r() - 0.5) * 0.5), 0.35); x.beginPath(); x.ellipse(cx + (r() - 0.5) * o.w * 1.6, cy + (r() - 0.5) * o.h * 1.4, o.w * lerp(0.08, 0.18, r()), o.h * lerp(0.06, 0.14, r()), r() * 3, 0, 7); x.fill(); }
      x.restore();
    }, 0.6);
    if (opt.sprout) { var top = pts.reduce(function (m, q) { return q[1] < m[1] ? q : m; }, [0, 1e9]), tx = top[0], ty = top[1] + o.h * 0.04, sz = o.w * 0.5;
      scribble(ctx, bez([tx, ty, tx + sz * 0.05, ty - sz * 0.2, tx - sz * 0.04, ty - sz * 0.35, tx + sz * 0.02, ty - sz * 0.5]), "#5F7236", 97, Math.max(1, sz * 0.05));
      pastel(ctx, blob(tx - sz * 0.2, ty - sz * 0.5, sz * 0.2, sz * 0.09, 98, 0.08, 0.35), "#8FA65A", { seed: 98, lw: Math.max(0.7, sz * 0.02) });
      pastel(ctx, blob(tx + sz * 0.22, ty - sz * 0.56, sz * 0.24, sz * 0.1, 99, 0.08, -0.4), "#A3B86B", { seed: 99, lw: Math.max(0.7, sz * 0.02) }); }
    // 눈: 흰 파스텔 + 연필 테두리 + 까만 눈동자
    var e = t.eye, er = o.w * e.size, look = opt.look || e.look, blink = opt.blink || 0;
    [-1, 1].forEach(function (sd, idx) {
      var c = local(t, cx, cy, o.w * e.x + sd * er * e.gap, o.h * e.y + sd * er * e.tilt * 2), rr = er * (idx === 0 ? 1 : e.ratio);
      var eyePts = blob(c[0], c[1], rr, rr * (1 - blink * 0.9), t.texSeed + idx, 0.04);
      smudge(ctx, c[0] + rr * 0.15, c[1] + rr * 0.25, rr * 1.05, rr * 0.9, "#1E1A14", 0.25);
      pastel(ctx, eyePts, "#FBF9F2", { seed: 70 + idx, clip: true, len: rr * 0.5, wid: rr * 0.16, press: 0.85, dark: 0.1, light: 0, smudge: 0.9, lw: Math.max(0.8, rr * 0.07) });
      if (blink < 0.6) {
        var pr = rr * e.pupil, lx = look.x + (opt.look ? 0 : (idx === 0 ? -e.spread : e.spread)), ly = look.y, len = Math.hypot(lx, ly); if (len > 1) { lx /= len; ly /= len; }
        var lim = rr - pr - rr * 0.08, px = c[0] + lx * lim, py = c[1] + ly * lim + lim * 0.2;
        pastel(ctx, blob(px, py, pr, pr, 90 + idx, 0.05), "#1C1A17", { seed: 80 + idx, clip: true, len: pr * 0.6, wid: pr * 0.25, press: 0.9, shade: false, smudge: 0.95, line: false });
        smudge(ctx, px - pr * 0.35, py - pr * 0.38, pr * 0.2, pr * 0.16, "#FFFFFF", 0.9);
      }
    });
  }
