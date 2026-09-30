  /* ───────── 붓 도구: 과슈 면 · 가장자리 물감 고임 · 색연필 선 · 종이 결 ───────── */
  var PAL = {
    olive: "#5F7236", oliveL: "#8A9A55", oliveD: "#3F4E22", moss: "#6E8440", leaf: "#7E9447", sprout: "#98AD5E",
    stone: "#E7E6DB", paper: "#F3F2EC", clay: "#B5651D", terra: "#B8653A", amber: "#F2B35A", cream: "#F1E7D2",
    wood: "#9C6B3F", woodD: "#6E4526", woodL: "#B98A5A", brass: "#C9A04A", red: "#B2452F", ink: "#3A3226",
    soil: "#4A3A2A", lav: "#8B7BB3", teal: "#6E8C7A", blue: "#7C8C8E"
  };
  function C(c) { return typeof c === "string" ? hex(c) : c; }
  function css(c, a) { c = C(c); return "rgba(" + c[0] + "," + c[1] + "," + c[2] + "," + (a == null ? 1 : a) + ")"; }
  function mix(a, b, t) { a = C(a); b = C(b); return [0, 1, 2].map(function (i) { return Math.round(a[i] + (b[i] - a[i]) * t); }); }
  function sh(c, k) { return shade(C(c), k); }

  function ell(cx, cy, rx, ry, rot) { var p = new Path2D(); p.ellipse(cx, cy, rx, ry, rot || 0, 0, Math.PI * 2); return p; }
  function rrect(x, y, w, h, r) { var p = new Path2D(); p.roundRect ? p.roundRect(x, y, w, h, r) : p.rect(x, y, w, h); return p; }
  function poly(pts) { var p = new Path2D(); pts.forEach(function (q, i) { i ? p.lineTo(q[0], q[1]) : p.moveTo(q[0], q[1]); }); p.closePath(); return p; }
  function svg(d) { return new Path2D(d); }
  function box(x, y, w, h) { return [x, y, w, h]; }

  // 종이 결: 흰 결 · 어두운 결 두 장 (투명한 곳에는 묻지 않도록 source-atop 으로 씀)
  var GRAIN = (function () {
    function make(white) {
      var c = document.createElement("canvas"); c.width = c.height = 256;
      var x = c.getContext("2d"), d = x.createImageData(256, 256), r = rng(white ? 11 : 12);
      for (var i = 0; i < d.data.length; i += 4) { var v = white ? 255 : 30; d.data[i] = v; d.data[i + 1] = v - (white ? 4 : 0); d.data[i + 2] = v - (white ? 12 : 4); d.data[i + 3] = Math.pow(r(), 3) * 70; }
      x.putImageData(d, 0, 0);
      // 종이 섬유
      x.lineCap = "round";
      for (i = 0; i < 70; i++) { var sx = r() * 256, sy = r() * 256, a = r() * Math.PI; x.strokeStyle = white ? "rgba(255,252,240,.18)" : "rgba(40,30,20,.08)"; x.lineWidth = 0.6; x.beginPath(); x.moveTo(sx, sy); x.quadraticCurveTo(sx + Math.cos(a) * 6 + r() * 4, sy + Math.sin(a) * 6, sx + Math.cos(a) * 12, sy + Math.sin(a) * 12); x.stroke(); }
      return c;
    }
    return { w: make(true), d: make(false) };
  })();
  function grain(ctx, x, y, w, h, amt) {
    ctx.save(); ctx.globalCompositeOperation = "source-atop";
    ctx.globalAlpha = amt == null ? 0.9 : amt;
    ctx.fillStyle = ctx.createPattern(GRAIN.w, "repeat"); ctx.fillRect(x, y, w, h);
    ctx.fillStyle = ctx.createPattern(GRAIN.d, "repeat"); ctx.fillRect(x, y, w, h);
    ctx.restore();
  }

  // 과슈 면 하나
  function paint(ctx, path, col, o) {
    o = o || {}; col = C(col);
    var r = rng(o.seed || 7), b = o.box, big = Math.max(b[2], b[3]);
    ctx.save();
    ctx.clip(path);
    if (o.cut) ctx.clip(o.cut, "evenodd");
    ctx.fillStyle = css(col); ctx.fillRect(b[0] - 4, b[1] - 4, b[2] + 8, b[3] + 8);
    // 붓 자국: 비슷한 색의 옅은 타원을 여러 번 겹침
    var n = o.dabs == null ? 46 : o.dabs;
    for (var i = 0; i < n; i++) {
      var x = b[0] + r() * b[2], y = b[1] + r() * b[3], rx = big * lerp(0.07, 0.2, r()), ry = rx * lerp(0.22, 0.5, r());
      ctx.fillStyle = css(sh(col, (r() - 0.5) * (o.vary == null ? 0.16 : o.vary)), lerp(0.14, 0.32, r()));
      ctx.beginPath(); ctx.ellipse(x, y, rx, ry, (o.ang == null ? -0.45 : o.ang) + (r() - 0.5) * 0.8, 0, 7); ctx.fill();
    }
    // 빛: 왼쪽 위 밝게, 오른쪽 아래 어둡게
    var g = ctx.createLinearGradient(b[0] + b[2] * 0.1, b[1], b[0] + b[2] * 0.85, b[1] + b[3]);
    g.addColorStop(0, "rgba(255,247,228," + (o.light == null ? 0.3 : o.light) + ")");
    g.addColorStop(0.48, "rgba(255,247,228,0)");
    g.addColorStop(0.52, "rgba(45,30,15,0)");
    g.addColorStop(1, "rgba(45,30,15," + (o.dark == null ? 0.32 : o.dark) + ")");
    ctx.fillStyle = g; ctx.fillRect(b[0] - 4, b[1] - 4, b[2] + 8, b[3] + 8);
    // 가장자리에 고인 물감
    var ew = o.edge == null ? Math.min(b[2], b[3]) * 0.1 : o.edge;
    ctx.lineJoin = "round";
    [1, 0.62, 0.3].forEach(function (f) { ctx.lineWidth = ew * f * 2; ctx.strokeStyle = css(sh(col, -0.42), o.pool == null ? 0.12 : o.pool); ctx.stroke(path); if (o.cut) ctx.stroke(o.cut); });
    if (o.after) o.after(ctx, r);
    ctx.restore();
    if (o.pencil !== false) pencil(ctx, path, o.line || sh(col, -0.55), o.seed, o.lw);
  }

  // 색연필 선: 두 번, 조금 어긋나게, 군데군데 끊김
  function pencil(ctx, path, col, seed, w) {
    var r = rng((seed || 3) * 7 + 1);
    for (var p = 0; p < 2; p++) {
      ctx.save(); ctx.translate((r() - 0.5) * 1.6, (r() - 0.5) * 1.6);
      ctx.setLineDash([lerp(26, 80, r()), lerp(2, 7, r()), lerp(10, 40, r()), lerp(2, 5, r())]); ctx.lineDashOffset = r() * 60;
      ctx.lineWidth = (w || 1.6) * (p ? 0.7 : 1); ctx.lineCap = "round"; ctx.lineJoin = "round";
      ctx.strokeStyle = css(col, p ? 0.22 : 0.42); ctx.stroke(path); ctx.restore();
    }
  }
  // 굵은 붓 선 (줄기 · 끈 · 손잡이)
  function brush(ctx, path, col, w, o) {
    o = o || {}; col = C(col);
    ctx.save(); ctx.lineCap = "round"; ctx.lineJoin = "round";
    ctx.lineWidth = w; ctx.strokeStyle = css(col); ctx.stroke(path);
    if (w > 3) { ctx.lineWidth = w * 0.35; ctx.strokeStyle = css(sh(col, 0.28), 0.55); ctx.translate(-w * 0.18, -w * 0.18); ctx.stroke(path); }
    ctx.restore();
    if (o.pencil !== false && w > 3) { ctx.save(); ctx.lineWidth = 1; ctx.strokeStyle = css(sh(col, -0.5), 0.3); ctx.translate(w * 0.3, w * 0.3); ctx.setLineDash([30, 6]); ctx.stroke(path); ctx.restore(); }
  }
  function ground(ctx, cx, cy, rx, ry, a) {
    ctx.save(); ctx.translate(cx, cy); ctx.scale(1, ry / rx);
    var g = ctx.createRadialGradient(0, 0, 0, 0, 0, rx);
    g.addColorStop(0, "rgba(52,40,22," + (a == null ? 0.32 : a) + ")"); g.addColorStop(0.55, "rgba(52,40,22," + (a == null ? 0.14 : a * 0.45) + ")"); g.addColorStop(1, "rgba(52,40,22,0)");
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(0, 0, rx, 0, 7); ctx.fill(); ctx.restore();
  }
  function glow(ctx, cx, cy, rad, col, a) {
    var g = ctx.createRadialGradient(cx, cy, 0, cx, cy, rad);
    g.addColorStop(0, css(col, a)); g.addColorStop(0.4, css(col, a * 0.45)); g.addColorStop(1, css(col, 0));
    ctx.fillStyle = g; ctx.fillRect(cx - rad, cy - rad, rad * 2, rad * 2);
  }
  // 반사광 한 줄 (유리 · 금속 · 도자기)
  function gleam(ctx, path, a) { ctx.save(); ctx.lineCap = "round"; ctx.lineWidth = 7; ctx.strokeStyle = "rgba(255,253,245," + (a || 0.55) + ")"; ctx.stroke(path); ctx.lineWidth = 2.5; ctx.strokeStyle = "rgba(255,255,255," + Math.min(1, (a || 0.55) + 0.3) + ")"; ctx.stroke(path); ctx.restore(); }
  function curve(pts) { var p = new Path2D(); p.moveTo(pts[0], pts[1]); for (var i = 2; i < pts.length; i += 6) p.bezierCurveTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], pts[i + 4], pts[i + 5]); return p; }
  function steam(ctx, x, y, h, seed) {
    var r = rng(seed);
    for (var k = 0; k < 3; k++) {
      var sx = x + (k - 1) * 18 + (r() - 0.5) * 6, w = lerp(10, 16, r());
      var p = curve([sx, y, sx - w, y - h * 0.3, sx + w, y - h * 0.55, sx, y - h * (0.8 + r() * 0.2)]);
      var g = ctx.createLinearGradient(0, y, 0, y - h); g.addColorStop(0, "rgba(255,252,244,.0)"); g.addColorStop(0.25, "rgba(255,252,244,.55)"); g.addColorStop(1, "rgba(255,252,244,0)");
      ctx.save(); ctx.lineCap = "round"; ctx.strokeStyle = g; ctx.lineWidth = 9; ctx.stroke(p); ctx.lineWidth = 4; ctx.stroke(p); ctx.restore();
    }
  }
  function leafPath(x, y, len, wid, ang) {
    var c = Math.cos(ang), s = Math.sin(ang);
    function P(u, v) { return [x + u * c - v * s, y + u * s + v * c]; }
    var a = P(len * 0.35, -wid), b = P(len * 0.8, -wid * 0.7), e = P(len, 0), d = P(len * 0.8, wid * 0.7), f = P(len * 0.35, wid);
    var p = new Path2D(); p.moveTo(x, y); p.bezierCurveTo(a[0], a[1], b[0], b[1], e[0], e[1]); p.bezierCurveTo(d[0], d[1], f[0], f[1], x, y); p.closePath();
    return { path: p, box: [Math.min(x, e[0]) - wid, Math.min(y, e[1]) - wid, Math.abs(e[0] - x) + wid * 2, Math.abs(e[1] - y) + wid * 2], mid: [x, y, e[0], e[1]] };
  }
  function leaf(ctx, x, y, len, wid, ang, col, seed) {
    var L = leafPath(x, y, len, wid, ang);
    paint(ctx, L.path, col, { box: L.box, seed: seed, dabs: 10, edge: wid * 0.3, lw: 1.2 });
    ctx.save(); ctx.strokeStyle = css(sh(col, -0.35), 0.45); ctx.lineWidth = 1.2; ctx.beginPath(); ctx.moveTo(L.mid[0], L.mid[1]); ctx.lineTo(lerp(L.mid[0], L.mid[2], 0.85), lerp(L.mid[1], L.mid[3], 0.85)); ctx.stroke(); ctx.restore();
  }

  /* ───────── 소품 20종 (512 × 512 칸, 바닥 y = 420) ───────── */
  var PROPS = [
    { id: "pot", ko: "새싹 화분", en: "Sprout pot", draw: function (c) {
      ground(c, 256, 422, 130, 20);
      paint(c, svg("M168,300 L188,414 Q256,428 324,414 L344,300 Z"), PAL.terra, { box: box(168, 300, 176, 124), seed: 1 });
      paint(c, rrect(148, 262, 216, 46, 12), sh(PAL.terra, 0.08), { box: box(148, 262, 216, 46), seed: 2 });
      paint(c, ell(256, 268, 94, 11), PAL.soil, { box: box(162, 257, 188, 22), seed: 3, dabs: 12, pencil: false, light: 0.1 });
      brush(c, curve([256, 268, 252, 240, 262, 210, 254, 176]), PAL.olive, 7);
      leaf(c, 254, 182, 78, 22, -2.6, PAL.leaf, 4);
      leaf(c, 256, 178, 88, 25, -0.45, PAL.sprout, 5);
      gleam(c, curve([176, 318, 180, 350, 186, 380, 192, 400]), 0.28);
    } },
    { id: "lantern", ko: "종이 등불", en: "Paper lantern", hang: true, draw: function (c) {
      brush(c, curve([256, 0, 256, 40, 256, 80, 256, 118]), PAL.ink, 2.5, { pencil: false });
      glow(c, 256, 232, 220, PAL.amber, 0.42);
      var body = ell(256, 232, 112, 108);
      paint(c, body, "#F3D69B", { box: box(144, 124, 224, 216), seed: 6, light: 0.12, dark: 0.18, vary: 0.1, after: function (x) {
        glow(x, 246, 222, 110, "#FFF4D6", 0.9);
        for (var yy = 138; yy <= 326; yy += 17) { var hw = 112 * Math.sqrt(Math.max(0, 1 - Math.pow((yy - 232) / 108, 2))); x.strokeStyle = "rgba(150,95,40,.35)"; x.lineWidth = 1.6; x.beginPath(); x.moveTo(256 - hw, yy); x.quadraticCurveTo(256, yy + 9, 256 + hw, yy); x.stroke(); }
      } });
      paint(c, rrect(212, 112, 88, 20, 6), PAL.woodD, { box: box(212, 112, 88, 20), seed: 7, dabs: 8 });
      paint(c, rrect(220, 332, 72, 18, 6), PAL.woodD, { box: box(220, 332, 72, 18), seed: 8, dabs: 8 });
      for (var k = -2; k <= 2; k++) brush(c, curve([256 + k * 6, 350, 256 + k * 7, 365, 256 + k * 8, 380, 256 + k * 9, 400]), PAL.red, 3, { pencil: false });
    } },
    { id: "books", ko: "오래된 책 세 권", en: "Three old books", draw: function (c) {
      ground(c, 256, 420, 150, 18);
      function book(x, y, w, h, col, rot, seed) {
        c.save(); c.translate(x + w / 2, y + h / 2); c.rotate(rot);
        paint(c, poly([[-w / 2 + 6, -h / 2], [w / 2 + 14, -h / 2 - 10], [w / 2 + 14, -h / 2 - 2], [w / 2, -h / 2 + 2]]), "#EFE6D2", { box: box(-w / 2, -h / 2 - 12, w + 16, 16), seed: seed + 9, dabs: 6, pencil: false });
        paint(c, poly([[w / 2, -h / 2], [w / 2 + 14, -h / 2 - 10], [w / 2 + 14, h / 2 - 10], [w / 2, h / 2]]), "#E7DCC4", { box: box(w / 2, -h / 2 - 10, 14, h + 10), seed: seed + 5, dabs: 4, lw: 1 });
        paint(c, rrect(-w / 2, -h / 2, w, h, 5), col, { box: box(-w / 2, -h / 2, w, h), seed: seed, after: function (x) {
          x.fillStyle = css(PAL.amber, 0.55); x.fillRect(-w / 2 + 18, -h / 2, 5, h); x.fillRect(w / 2 - 26, -h / 2, 5, h);
          x.fillStyle = css(sh(col, 0.25), 0.5); x.fillRect(-w * 0.18, -h * 0.2, w * 0.36, h * 0.4);
        } });
        c.restore();
      }
      book(136, 358, 240, 56, PAL.olive, 0, 10);
      book(158, 306, 204, 52, PAL.clay, -0.04, 20);
      book(178, 262, 170, 44, PAL.blue, 0.05, 30);
    } },
    { id: "teacup", ko: "따뜻한 찻잔", en: "Warm teacup", draw: function (c) {
      ground(c, 256, 418, 160, 22);
      paint(c, ell(256, 402, 150, 26), "#EDE6D6", { box: box(106, 376, 300, 52), seed: 11, dabs: 18 });
      paint(c, ell(256, 398, 92, 12), "#E2D9C5", { box: box(164, 386, 184, 24), seed: 12, dabs: 4, pencil: false, light: 0 });
      var handle = new Path2D(); handle.ellipse(360, 332, 30, 32, 0, -1.4, 1.5);
      brush(c, handle, "#EDE6D6", 13);
      paint(c, svg("M158,296 C158,380 198,404 256,404 C314,404 354,380 354,296 Z"), "#F1EBDD", { box: box(158, 296, 196, 108), seed: 13, after: function (x) {
        x.fillStyle = css(PAL.olive, 0.85); x.beginPath(); x.moveTo(150, 322); x.quadraticCurveTo(256, 336, 362, 322); x.lineTo(362, 336); x.quadraticCurveTo(256, 350, 150, 336); x.fill();
      } });
      paint(c, ell(256, 296, 98, 18), "#F6F1E6", { box: box(158, 278, 196, 36), seed: 14, dabs: 4 });
      paint(c, ell(256, 299, 86, 13), "#8C5A26", { box: box(170, 286, 172, 26), seed: 15, dabs: 6, pencil: false, light: 0.2, after: function (x) { glow(x, 226, 294, 40, "#E0A55A", 0.6); } });
      gleam(c, curve([176, 318, 178, 340, 186, 362, 198, 378]), 0.45);
      steam(c, 256, 270, 130, 16);
    } },
    { id: "chair", ko: "작은 나무 의자", en: "Tiny wooden chair", draw: function (c) {
      ground(c, 262, 418, 130, 18);
      var W = PAL.woodL;
      brush(c, curve([180, 304, 180, 340, 180, 370, 180, 396]), sh(W, -0.2), 11);
      brush(c, curve([330, 294, 330, 330, 330, 360, 330, 388]), sh(W, -0.2), 11);
      brush(c, curve([180, 304, 178, 250, 176, 200, 176, 148]), W, 12);
      brush(c, curve([330, 294, 330, 240, 330, 190, 330, 140]), W, 12);
      paint(c, svg("M166,150 C220,128 290,122 344,132 L344,160 C290,150 220,156 166,178 Z"), W, { box: box(166, 122, 178, 56), seed: 21 });
      [214, 254, 294].forEach(function (x, i) { brush(c, curve([x, 160 - i * 3, x, 200, x, 250, x, 296 - i * 3]), sh(W, -0.06), 8); });
      paint(c, svg("M160,302 L334,290 L372,322 L198,336 Z"), sh(W, 0.08), { box: box(160, 290, 212, 46), seed: 22 });
      paint(c, svg("M198,336 L372,322 L372,338 L198,352 Z"), sh(W, -0.18), { box: box(198, 322, 174, 30), seed: 23, dabs: 6 });
      brush(c, curve([208, 348, 208, 380, 208, 400, 208, 418]), W, 13);
      brush(c, curve([362, 336, 362, 366, 362, 390, 362, 408]), W, 13);
    } },
    { id: "star", ko: "종이 별", en: "Paper star", hang: true, draw: function (c) {
      brush(c, curve([256, 0, 258, 50, 254, 90, 256, 126]), PAL.ink, 2.5, { pencil: false });
      var cx = 256, cy = 258, R = 128, r0 = 54, base = C("#F1C46C");
      var lx = -0.7, ly = -0.7;
      for (var i = 0; i < 5; i++) {
        var a = -Math.PI / 2 + i * Math.PI * 2 / 5, a1 = a - Math.PI / 5, a2 = a + Math.PI / 5;
        var tip = [cx + Math.cos(a) * R, cy + Math.sin(a) * R], v1 = [cx + Math.cos(a1) * r0, cy + Math.sin(a1) * r0], v2 = [cx + Math.cos(a2) * r0, cy + Math.sin(a2) * r0];
        [[v1, a1], [v2, a2]].forEach(function (pair, k) {
          var mxd = (Math.cos(a) + Math.cos(pair[1])) / 2, myd = (Math.sin(a) + Math.sin(pair[1])) / 2, len = Math.hypot(mxd, myd);
          var lit = (mxd * lx + myd * ly) / len;
          var col = sh(base, lit * 0.2 + (k ? -0.06 : 0.04));
          paint(c, poly([[cx, cy], tip, pair[0]]), col, { box: box(cx - R, cy - R, R * 2, R * 2), seed: 30 + i * 2 + k, dabs: 10, light: 0.08, dark: 0.1, edge: 6, lw: 1.3 });
        });
      }
    } },
    { id: "mushroom", ko: "빨간 버섯", en: "Red mushroom", draw: function (c) {
      ground(c, 256, 420, 120, 16);
      paint(c, svg("M226,420 C232,380 232,340 238,300 L276,300 C282,340 282,380 288,420 Q256,428 226,420 Z"), "#EFE4CC", { box: box(226, 300, 62, 124), seed: 41 });
      paint(c, ell(256, 302, 118, 16), "#DCC9A6", { box: box(138, 286, 236, 32), seed: 42, dabs: 8, pencil: false });
      paint(c, svg("M132,302 C136,196 376,196 380,302 C330,318 182,318 132,302 Z"), PAL.red, { box: box(132, 196, 248, 120), seed: 43, after: function (x) {
        [[196, 262, 16, 10], [256, 232, 20, 12], [314, 256, 15, 10], [228, 290, 10, 6], [292, 292, 11, 6], [166, 290, 8, 5], [344, 290, 8, 5]].forEach(function (d) { x.fillStyle = "rgba(250,244,230,.92)"; x.beginPath(); x.ellipse(d[0], d[1], d[2], d[3], 0, 0, 7); x.fill(); });
      } });
      [[208, 422, -1.9], [220, 424, -1.4], [300, 424, -1.2], [312, 422, -0.8]].forEach(function (b, i) { brush(c, curve([b[0], b[1], b[0] + Math.cos(b[2]) * 10, b[1] - 12, b[0] + Math.cos(b[2]) * 18, b[1] - 22, b[0] + Math.cos(b[2]) * 26, b[1] - 34]), i % 2 ? PAL.leaf : PAL.olive, 4, { pencil: false }); });
    } },
    { id: "globe", ko: "작은 스노볼", en: "Snow globe", draw: function (c) {
      ground(c, 256, 420, 120, 16);
      var glass = ell(256, 268, 112, 112);
      c.save(); c.clip(glass);
      var g = c.createLinearGradient(0, 156, 0, 380); g.addColorStop(0, "#D5E2E4"); g.addColorStop(1, "#EEF2EE"); c.fillStyle = g; c.fillRect(140, 150, 232, 240);
      paint(c, svg("M140,340 C200,318 312,318 372,340 L372,390 L140,390 Z"), "#F6F5F0", { box: box(140, 318, 232, 72), seed: 51, dabs: 6, pencil: false, dark: 0.12 });
      paint(c, poly([[256, 214], [220, 300], [292, 300]]), "#4F6A4A", { box: box(220, 214, 72, 86), seed: 52, dabs: 8 });
      paint(c, poly([[256, 250], [212, 332], [300, 332]]), "#46603F", { box: box(212, 250, 88, 82), seed: 53, dabs: 8 });
      paint(c, rrect(250, 330, 12, 14, 2), PAL.woodD, { box: box(250, 330, 12, 14), dabs: 0, pencil: false });
      var r = rng(54); for (var i = 0; i < 46; i++) { c.fillStyle = "rgba(255,255,255," + lerp(0.6, 1, r()) + ")"; c.beginPath(); c.arc(150 + r() * 212, 165 + r() * 170, lerp(1.4, 3.6, r()), 0, 7); c.fill(); }
      c.restore();
      c.save(); c.lineWidth = 3; c.strokeStyle = "rgba(90,110,110,.35)"; c.stroke(glass); c.restore();
      gleam(c, (function () { var p = new Path2D(); p.arc(256, 268, 92, 3.5, 4.3); return p; })(), 0.6);
      gleam(c, (function () { var p = new Path2D(); p.arc(256, 268, 96, 0.3, 0.6); return p; })(), 0.25);
      paint(c, svg("M168,420 L344,420 L328,370 L184,370 Z"), PAL.wood, { box: box(168, 370, 176, 50), seed: 55 });
      paint(c, rrect(178, 360, 156, 14, 4), PAL.woodD, { box: box(178, 360, 156, 14), seed: 56, dabs: 6 });
    } },
    { id: "moon", ko: "초승달 장식", en: "Crescent moon charm", hang: true, draw: function (c) {
      brush(c, curve([290, 0, 290, 40, 290, 80, 290, 132]), PAL.ink, 2.5, { pencil: false });
      var outer = ell(250, 262, 116, 116), inner = ell(304, 228, 100, 100);
      var cut = new Path2D(); cut.rect(0, 0, 512, 512); cut.ellipse(304, 228, 100, 100, 0, 0, 7);
      glow(c, 220, 280, 170, PAL.amber, 0.22);
      paint(c, outer, "#D9A441", { box: box(134, 146, 232, 232), seed: 61, cut: cut, pencil: false, light: 0.35 });
      c.save(); c.clip(cut, "evenodd"); pencil(c, outer, sh("#D9A441", -0.55), 61); c.restore();
      c.save(); c.clip(outer); pencil(c, inner, sh("#D9A441", -0.55), 62); c.restore();
      paint(c, ell(290, 140, 9, 9), "#E7C07A", { box: box(280, 130, 20, 20), dabs: 0, lw: 1 });
      var st = []; for (var i = 0; i < 10; i++) { var a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 ? 9 : 22; st.push([348 + Math.cos(a) * rr, 364 + Math.sin(a) * rr]); }
      brush(c, curve([330, 262, 340, 290, 346, 320, 348, 342]), PAL.ink, 1.6, { pencil: false });
      paint(c, poly(st), "#F1C46C", { box: box(326, 342, 44, 44), seed: 63, dabs: 4, edge: 3, lw: 1 });
    } },
    { id: "boat", ko: "종이배", en: "Paper boat", draw: function (c) {
      ground(c, 256, 408, 150, 16);
      var P = "#EFE7D5";
      paint(c, poly([[256, 176], [196, 332], [256, 332]]), sh(P, 0.05), { box: box(196, 176, 60, 156), seed: 71, dabs: 10, light: 0.18 });
      paint(c, poly([[256, 176], [256, 332], [316, 332]]), sh(P, -0.1), { box: box(256, 176, 60, 156), seed: 72, dabs: 10 });
      paint(c, poly([[118, 318], [256, 332], [394, 318], [346, 400], [166, 400]]), P, { box: box(118, 318, 276, 82), seed: 73, after: function (x) {
        x.strokeStyle = "rgba(120,100,70,.28)"; x.lineWidth = 1.4; x.beginPath(); x.moveTo(166, 400); x.lineTo(206, 334); x.moveTo(346, 400); x.lineTo(306, 334); x.stroke();
      } });
      paint(c, poly([[118, 318], [206, 334], [166, 400]]), sh(P, -0.08), { box: box(118, 318, 88, 82), seed: 74, dabs: 4, lw: 1.2 });
      paint(c, poly([[394, 318], [306, 334], [346, 400]]), sh(P, -0.16), { box: box(306, 318, 88, 82), seed: 75, dabs: 4, lw: 1.2 });
    } },
    { id: "bell", ko: "놋쇠 방울", en: "Brass bell", hang: true, draw: function (c) {
      var loop = new Path2D(); loop.ellipse(256, 118, 22, 28, 0, Math.PI * 0.95, Math.PI * 2.05);
      brush(c, curve([256, 0, 256, 30, 256, 60, 256, 90]), PAL.red, 5, { pencil: false });
      brush(c, loop, PAL.red, 5, { pencil: false });
      paint(c, rrect(236, 134, 40, 26, 8), sh(PAL.brass, -0.1), { box: box(236, 134, 40, 26), seed: 80, dabs: 4 });
      paint(c, svg("M256,152 C212,152 198,192 196,252 C194,318 176,352 150,370 L362,370 C336,352 318,318 316,252 C314,192 300,152 256,152 Z"), PAL.brass, { box: box(150, 152, 212, 218), seed: 81, light: 0.42, dark: 0.4, after: function (x) {
        x.fillStyle = "rgba(120,80,20,.28)"; x.fillRect(150, 318, 212, 8);
      } });
      gleam(c, curve([222, 178, 212, 220, 210, 280, 196, 340]), 0.6);
      paint(c, ell(256, 372, 110, 15), sh(PAL.brass, -0.22), { box: box(146, 357, 220, 30), seed: 82, dabs: 6 });
      paint(c, ell(256, 392, 17, 17), sh(PAL.brass, -0.35), { box: box(239, 375, 34, 34), seed: 83, dabs: 3 });
    } },
    { id: "acorn", ko: "도토리", en: "Acorn", draw: function (c) {
      ground(c, 262, 420, 100, 16);
      paint(c, svg("M184,262 C180,340 214,410 258,418 C302,410 336,340 332,262 Z"), "#A8703A", { box: box(180, 262, 156, 156), seed: 91, light: 0.35, dark: 0.35 });
      gleam(c, curve([204, 292, 204, 320, 212, 348, 226, 370]), 0.45);
      paint(c, svg("M166,272 C166,208 350,208 350,272 C340,290 176,290 166,272 Z"), "#7A5A36", { box: box(166, 208, 184, 84), seed: 92, after: function (x) {
        for (var yy = 222; yy < 292; yy += 11) for (var xx = 160 + (yy % 22 ? 0 : 6); xx < 360; xx += 12) { x.strokeStyle = "rgba(60,40,20,.4)"; x.lineWidth = 1.3; x.beginPath(); x.arc(xx, yy, 6, 0.2, Math.PI - 0.2); x.stroke(); x.strokeStyle = "rgba(230,200,150,.25)"; x.beginPath(); x.arc(xx, yy - 2, 5, 3.6, 5.8); x.stroke(); }
      } });
      brush(c, curve([256, 214, 256, 196, 262, 184, 272, 176]), PAL.woodD, 8);
    } },
    { id: "lavender", ko: "라벤더 한 병", en: "Lavender jar", draw: function (c) {
      ground(c, 256, 420, 110, 16);
      var jar = svg("M200,262 L312,262 C312,280 330,290 330,320 L330,396 C330,414 316,420 300,420 L212,420 C196,420 182,414 182,396 L182,320 C182,290 200,280 200,262 Z");
      c.save(); c.fillStyle = "rgba(210,226,222,.45)"; c.fill(jar); c.restore();
      var r = rng(101);
      var stems = [[-44, 118], [-22, 92], [0, 80], [22, 96], [46, 124], [-8, 104], [12, 110]];
      stems.forEach(function (s, i) {
        var top = [256 + s[0] * 1.5, s[1]], p = curve([256 + s[0] * 0.2, 400, 256 + s[0] * 0.5, 320, top[0], 220, top[0], top[1]]);
        brush(c, p, i % 2 ? PAL.olive : PAL.moss, 3.5, { pencil: false });
        for (var k = 0; k < 11; k++) { var yy = top[1] + k * 7, sx = top[0] + (k % 2 ? 4 : -4); c.fillStyle = css(k % 3 ? PAL.lav : "#A597C9", 0.95); c.beginPath(); c.ellipse(sx, yy, 5.5, 7.5, k % 2 ? 0.5 : -0.5, 0, 7); c.fill(); c.fillStyle = "rgba(60,40,90,.22)"; c.beginPath(); c.ellipse(sx + 1.5, yy + 2, 3, 4, 0, 0, 7); c.fill(); }
      });
      c.save(); c.clip(jar); var g = c.createLinearGradient(0, 330, 0, 420); g.addColorStop(0, "rgba(170,200,196,.45)"); g.addColorStop(1, "rgba(140,175,170,.55)"); c.fillStyle = g; c.fillRect(170, 334, 172, 90);
      c.strokeStyle = "rgba(255,255,255,.6)"; c.lineWidth = 2; c.beginPath(); c.moveTo(184, 334); c.quadraticCurveTo(256, 340, 330, 334); c.stroke(); c.restore();
      c.save(); c.lineWidth = 2.4; c.strokeStyle = "rgba(90,115,110,.45)"; c.stroke(jar); c.restore();
      paint(c, rrect(196, 252, 120, 16, 6), "#DCE6E2", { box: box(196, 252, 120, 16), dabs: 2, lw: 1.2, light: 0.4 });
      brush(c, curve([204, 282, 200, 310, 196, 350, 198, 396]), "#FFFFFF", 5, { pencil: false });
      c.save(); c.globalAlpha = 0.5; brush(c, curve([316, 300, 318, 330, 318, 360, 316, 390]), "#FFFFFF", 3, { pencil: false }); c.restore();
      brush(c, curve([204, 272, 230, 290, 280, 290, 308, 272]), PAL.red, 3, { pencil: false });
    } },
    { id: "candle", ko: "촛대와 초", en: "Candle", draw: function (c) {
      ground(c, 256, 420, 140, 16);
      glow(c, 256, 176, 190, PAL.amber, 0.38);
      var ring = new Path2D(); ring.ellipse(380, 394, 22, 16, 0, 0, 7);
      brush(c, ring, sh(PAL.brass, -0.1), 8);
      paint(c, ell(256, 400, 128, 22), PAL.brass, { box: box(128, 378, 256, 44), seed: 111, light: 0.4 });
      paint(c, ell(256, 394, 60, 10), sh(PAL.brass, -0.2), { box: box(196, 384, 120, 20), dabs: 3, pencil: false });
      paint(c, svg("M212,236 L212,392 Q256,402 300,392 L300,236 Z"), "#F2EAD8", { box: box(212, 228, 88, 172), seed: 112, light: 0.25, after: function (x) {
        x.fillStyle = "#F7F1E4"; x.beginPath(); x.moveTo(268, 236); x.quadraticCurveTo(272, 262, 266, 280); x.quadraticCurveTo(262, 292, 258, 280); x.quadraticCurveTo(254, 258, 256, 236); x.fill();
        glow(x, 256, 236, 60, "#FFE3A6", 0.5);
      } });
      paint(c, ell(256, 236, 44, 8), "#F7F0E1", { box: box(212, 228, 88, 16), dabs: 2, lw: 1 });
      brush(c, curve([256, 236, 256, 230, 257, 224, 256, 218]), PAL.ink, 2.5, { pencil: false });
      var fl = svg("M256,154 C240,184 238,204 256,218 C274,204 272,184 256,154 Z");
      c.save(); c.fillStyle = "#F4B24A"; c.fill(fl); c.fillStyle = "#FFE8A8"; c.fill(svg("M256,176 C248,194 248,206 256,214 C264,206 264,194 256,176 Z")); c.restore();
      glow(c, 256, 196, 50, "#FFF2C8", 0.55);
    } },
    { id: "watch", ko: "회중시계", en: "Pocket watch", draw: function (c) {
      ground(c, 262, 420, 140, 18);
      var r = rng(121), chain = new Path2D(); for (var i = 0; i < 14; i++) { var t = i / 13, x = lerp(256, 110, t) - Math.sin(t * Math.PI) * 30, y = lerp(110, 60, t) + Math.sin(t * Math.PI) * 50; chain.moveTo(x + 6, y); chain.ellipse(x, y, 6, 4, t * 1.5 + (i % 2) * 1.2, 0, 7); }
      c.save(); c.lineWidth = 2.6; c.strokeStyle = css(sh(PAL.brass, -0.2)); c.stroke(chain); c.restore();
      var ring = new Path2D(); ring.ellipse(256, 124, 20, 18, 0, 0, 7); brush(c, ring, PAL.brass, 6);
      paint(c, rrect(238, 138, 36, 26, 6), sh(PAL.brass, -0.1), { box: box(238, 138, 36, 26), dabs: 4 });
      paint(c, ell(256, 290, 126, 126), PAL.brass, { box: box(130, 164, 252, 252), seed: 122, light: 0.45, dark: 0.4 });
      paint(c, ell(256, 290, 106, 106), sh(PAL.brass, -0.15), { box: box(150, 184, 212, 212), seed: 123, dabs: 10, pencil: false });
      paint(c, ell(256, 290, 98, 98), "#F4EDDC", { box: box(158, 192, 196, 196), seed: 124, light: 0.2, dark: 0.18, after: function (x) {
        for (var k = 0; k < 60; k++) { var a = k / 60 * Math.PI * 2, r1 = k % 5 ? 88 : 80; x.strokeStyle = css(PAL.ink, k % 5 ? 0.35 : 0.7); x.lineWidth = k % 5 ? 1.2 : 3; x.beginPath(); x.moveTo(256 + Math.cos(a) * r1, 290 + Math.sin(a) * r1); x.lineTo(256 + Math.cos(a) * 92, 290 + Math.sin(a) * 92); x.stroke(); }
      } });
      c.save(); c.lineCap = "round"; c.strokeStyle = css(PAL.ink, 0.9);
      c.lineWidth = 6; c.beginPath(); c.moveTo(256, 290); c.lineTo(256 + Math.cos(-2.62) * 50, 290 + Math.sin(-2.62) * 50); c.stroke();
      c.lineWidth = 4; c.beginPath(); c.moveTo(256, 290); c.lineTo(256 + Math.cos(-0.52) * 76, 290 + Math.sin(-0.52) * 76); c.stroke();
      c.lineWidth = 1.6; c.strokeStyle = css(PAL.red, 0.9); c.beginPath(); c.moveTo(256, 290); c.lineTo(256 + Math.cos(1.2) * 82, 290 + Math.sin(1.2) * 82); c.stroke();
      c.fillStyle = css(PAL.ink); c.beginPath(); c.arc(256, 290, 7, 0, 7); c.fill(); c.restore();
      gleam(c, (function () { var p = new Path2D(); p.arc(256, 290, 84, 3.6, 4.4); return p; })(), 0.5);
    } },
    { id: "pinecone", ko: "솔방울", en: "Pinecone", draw: function (c) {
      ground(c, 256, 420, 110, 16);
      var base = C("#8A5A33"), cx = 256;
      paint(c, svg("M256,160 C330,170 356,260 340,320 C326,370 290,404 256,406 C222,404 186,370 172,320 C156,260 182,170 256,160 Z"), sh(base, -0.35), { box: box(160, 160, 192, 246), seed: 129, dabs: 10, pencil: false });
      var rows = 11;
      for (var i = 0; i < rows; i++) {
        var t = i / (rows - 1), y = lerp(166, 394, t), hw = Math.sin(lerp(0.35, 2.9, t)) * 92 + 6, n = Math.max(3, Math.round(hw / 22)), s = lerp(0.6, 1, Math.sin(t * Math.PI));
        for (var k = 0; k < n; k++) {
          var u = (k + (i % 2 ? 0.5 : 0)) / n * 2 - 1 + 1 / n; if (Math.abs(u) > 1) continue;
          var x = cx + u * hw, sw = 26 * s * Math.sqrt(1 - u * u * 0.6), shh = 20 * s;
          var p = svg("M" + (x - sw) + "," + (y - shh * 0.3) + " Q" + x + "," + (y - shh * 1.4) + " " + (x + sw) + "," + (y - shh * 0.3) + " Q" + (x + sw * 0.6) + "," + (y + shh) + " " + x + "," + (y + shh * 0.9) + " Q" + (x - sw * 0.6) + "," + (y + shh) + " " + (x - sw) + "," + (y - shh * 0.3) + " Z");
          var lit = -u * 0.12 + (1 - t) * 0.08;
          paint(c, p, sh(base, lit + (k % 2 ? -0.04 : 0.03)), { box: box(x - sw, y - shh * 1.2, sw * 2, shh * 2.2), seed: 130 + i * 9 + k, dabs: 4, edge: 4, lw: 1.1, dark: 0.35, after: function (xx) { xx.strokeStyle = "rgba(230,190,140,.5)"; xx.lineWidth = 2; xx.beginPath(); xx.moveTo(x - sw * 0.5, y + shh * 0.6); xx.quadraticCurveTo(x, y + shh * 1.05, x + sw * 0.5, y + shh * 0.6); xx.stroke(); } });
        }
      }
      brush(c, curve([256, 170, 254, 150, 260, 138, 270, 128]), PAL.woodD, 7);
    } },
    { id: "can", ko: "물뿌리개", en: "Watering can", draw: function (c) {
      ground(c, 256, 420, 160, 18);
      var M = PAL.teal;
      brush(c, curve([300, 250, 318, 150, 400, 170, 330, 336]), sh(M, -0.1), 12);
      paint(c, svg("M186,372 L96,236 L112,226 L204,344 Z"), sh(M, -0.05), { box: box(96, 226, 108, 146), seed: 141, dabs: 8 });
      paint(c, ell(96, 226, 20, 12, -0.9), sh(M, -0.2), { box: box(76, 212, 40, 28), dabs: 3, after: function (x) { x.fillStyle = "rgba(40,50,40,.45)"; for (var k = 0; k < 6; k++) { x.beginPath(); x.arc(90 + (k % 3) * 6, 222 + Math.floor(k / 3) * 7, 1.5, 0, 7); x.fill(); } } });
      paint(c, svg("M180,256 L180,398 C180,414 200,420 256,420 C312,420 332,414 332,398 L332,256 Z"), M, { box: box(180, 244, 152, 176), seed: 142, light: 0.36, after: function (x) { x.fillStyle = "rgba(40,60,50,.18)"; x.fillRect(180, 360, 152, 10); } });
      paint(c, ell(256, 256, 76, 16), sh(M, 0.1), { box: box(180, 240, 152, 32), seed: 143, dabs: 4 });
      paint(c, ell(256, 256, 58, 10), sh(M, -0.45), { box: box(198, 246, 116, 20), dabs: 2, pencil: false });
      gleam(c, curve([198, 280, 196, 320, 198, 360, 202, 392]), 0.45);
    } },
    { id: "basket", ko: "사과 바구니", en: "Basket of apples", draw: function (c) {
      ground(c, 256, 420, 160, 18);
      paint(c, ell(256, 300, 122, 22), sh(PAL.woodL, -0.3), { box: box(134, 278, 244, 44), dabs: 4, pencil: false });
      [[196, 272, 38, PAL.red], [262, 262, 42, "#C2562F"], [322, 276, 36, "#D98B3A"], [232, 288, 34, "#B2452F"], [292, 292, 32, PAL.red]].forEach(function (a, i) {
        paint(c, ell(a[0], a[1], a[2], a[2] * 0.92), a[3], { box: box(a[0] - a[2], a[1] - a[2], a[2] * 2, a[2] * 2), seed: 150 + i, light: 0.4, dark: 0.4 });
        gleam(c, (function () { var p = new Path2D(); p.arc(a[0], a[1], a[2] * 0.66, 3.6, 4.3); return p; })(), 0.4);
        if (i < 3) brush(c, curve([a[0], a[1] - a[2] * 0.85, a[0] + 2, a[1] - a[2] - 6, a[0] + 4, a[1] - a[2] - 10, a[0] + 6, a[1] - a[2] - 16]), PAL.woodD, 3, { pencil: false });
      });
      leaf(c, 268, 224, 44, 12, -0.6, PAL.leaf, 158);
      paint(c, svg("M134,300 L166,410 C176,420 336,420 346,410 L378,300 Z"), PAL.woodL, { box: box(134, 300, 244, 120), seed: 159, after: function (x) {
        for (var yy = 312; yy < 420; yy += 14) { x.strokeStyle = "rgba(90,60,30,.4)"; x.lineWidth = 2; x.beginPath(); x.moveTo(130, yy); x.quadraticCurveTo(256, yy + 8, 382, yy); x.stroke(); }
        for (var xx = 150; xx < 370; xx += 16) { x.strokeStyle = "rgba(90,60,30,.22)"; x.lineWidth = 1.4; x.beginPath(); x.moveTo(xx, 300); x.lineTo(256 + (xx - 256) * 0.82, 420); x.stroke(); }
      } });
      paint(c, rrect(128, 292, 256, 18, 9), sh(PAL.woodL, -0.08), { box: box(128, 292, 256, 18), seed: 160, dabs: 8 });
      brush(c, curve([150, 300, 170, 190, 342, 190, 362, 300]), sh(PAL.woodL, -0.15), 9);
    } },
    { id: "shell", ko: "조개껍데기", en: "Seashell", draw: function (c) {
      ground(c, 256, 418, 140, 16);
      var cx = 256, cy = 398, R = 156, p = new Path2D(); p.moveTo(cx - 18, cy);
      var n = 9;
      for (var i = 0; i <= n * 8; i++) { var t = i / (n * 8), a = lerp(-Math.PI + 0.35, -0.35, t), rr = R * (0.9 + 0.1 * Math.abs(Math.sin(t * n * Math.PI))); p.lineTo(cx + Math.cos(a) * rr, cy + 20 + Math.sin(a) * rr * 0.95); }
      p.lineTo(cx + 18, cy); p.closePath();
      paint(c, p, "#EBC3A6", { box: box(cx - R, cy - R, R * 2, R + 20), seed: 171, light: 0.35, after: function (x) {
        for (var k = 0; k <= n; k++) { var a = lerp(-Math.PI + 0.35, -0.35, k / n); x.strokeStyle = "rgba(160,95,65,.35)"; x.lineWidth = 3; x.beginPath(); x.moveTo(cx, cy + 6); x.lineTo(cx + Math.cos(a) * R, cy + 20 + Math.sin(a) * R); x.stroke(); }
        for (var j = 1; j < 5; j++) { x.strokeStyle = "rgba(255,245,235,.35)"; x.lineWidth = 2; x.beginPath(); x.ellipse(cx, cy + 20, R * j / 5, R * j / 5 * 0.95, 0, -Math.PI + 0.35, -0.35); x.stroke(); }
      } });
      paint(c, svg("M218,392 L294,392 L306,420 L206,420 Z"), "#E0AE8C", { box: box(206, 392, 100, 28), seed: 172, dabs: 4 });
    } },
    { id: "hourglass", ko: "모래시계", en: "Hourglass", draw: function (c) {
      ground(c, 256, 420, 130, 16);
      brush(c, curve([176, 136, 176, 220, 176, 300, 176, 392]), sh(PAL.wood, -0.25), 9);
      var gl = svg("M198,136 C198,218 250,238 252,263 C250,288 198,308 198,392 L314,392 C314,308 262,288 260,263 C262,238 314,218 314,136 Z");
      c.save(); c.fillStyle = "rgba(214,228,224,.4)"; c.fill(gl); c.clip(gl);
      var S = "#D9B070";
      paint(c, svg("M190,196 Q256,212 322,196 L322,270 L190,270 Z"), S, { box: box(190, 196, 132, 74), seed: 181, dabs: 10, pencil: false });
      paint(c, svg("M190,392 L190,372 C220,350 240,318 256,316 C272,318 292,350 322,372 L322,392 Z"), S, { box: box(190, 316, 132, 76), seed: 182, dabs: 10, pencil: false });
      c.fillStyle = css(S); c.fillRect(254.5, 262, 3, 56);
      c.restore();
      c.save(); c.lineWidth = 2.2; c.strokeStyle = "rgba(90,110,105,.5)"; c.stroke(gl); c.restore();
      gleam(c, curve([210, 150, 212, 190, 226, 220, 240, 240]), 0.55);
      gleam(c, curve([214, 380, 212, 350, 222, 318, 236, 298]), 0.35);
      brush(c, curve([336, 136, 336, 220, 336, 300, 336, 392]), PAL.wood, 10);
      paint(c, rrect(158, 112, 196, 26, 8), PAL.wood, { box: box(158, 112, 196, 26), seed: 183 });
      paint(c, rrect(158, 390, 196, 28, 8), PAL.wood, { box: box(158, 390, 196, 28), seed: 184 });
    } }
  ];

  function drawProp(ctx, id, x, y, size) {
    var p = PROPS.filter(function (q) { return q.id === id; })[0];
    ctx.save(); ctx.translate(x - size / 2, p.hang ? y : y - size * (420 / 512)); ctx.scale(size / 512, size / 512); p.draw(ctx); ctx.restore();
  }
  function propCanvas(cv, p) {
    var ctx = cv.getContext("2d"); ctx.clearRect(0, 0, cv.width, cv.height);
    ctx.save(); ctx.scale(cv.width / 512, cv.height / 512); p.draw(ctx); ctx.restore();
    grain(ctx, 0, 0, cv.width, cv.height, 0.85);
  }
