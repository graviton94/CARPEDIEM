// 자동 생성 파일 — scripts/build_art.js
window.exportHaru = (function () {
  /* ───────── 난수 (앱에서는 SplitMix64, 여기서는 같은 성질의 mulberry32) ───────── */
  function rng(seed) {
    var a = seed >>> 0;
    return function () { a = (a + 0x6D2B79F5) >>> 0; var t = a; t = Math.imul(t ^ (t >>> 15), t | 1); t ^= t + Math.imul(t ^ (t >>> 7), t | 61); return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
  }
  function lerp(a, b, t) { return a + (b - a) * t; }
  function hex(h) { h = h.replace("#", ""); return [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]; }
  function rgba(c, a) { return "rgba(" + c[0] + "," + c[1] + "," + c[2] + "," + a + ")"; }
  function shade(c, k) { return c.map(function (v) { return Math.max(0, Math.min(255, Math.round(k > 0 ? v + (255 - v) * k : v * (1 + k)))); }); }

  /* ───────── 돌 종류 ───────── */
  var STONES = [
    { id: "basalt", name: "검은 현무암", base: ["#2E2E30", "#3A3A3C", "#27282B"], pattern: "fine", gloss: 0.55, desc: "물에 오래 닳은 검은 돌" },
    { id: "granite", name: "회색 화강암", base: ["#8E8A82", "#9A968D", "#7F7B74"], pattern: "salt", gloss: 0.2, desc: "희고 검은 알갱이가 섞인 돌" },
    { id: "pinkgranite", name: "분홍 화강암", base: ["#B39A8E", "#A88E82", "#BCA497"], pattern: "salt", gloss: 0.2, desc: "분홍빛 알갱이가 섞인 돌" },
    { id: "sand", name: "모래빛 사암", base: ["#C8B9A0", "#BFAE93", "#D2C4AC"], pattern: "grain", gloss: 0.1, desc: "고운 모래가 굳은 따뜻한 돌" },
    { id: "ochre", name: "황토 사암", base: ["#B98E5A", "#AE8352", "#C49A66"], pattern: "grain", gloss: 0.1, desc: "흙빛이 도는 부드러운 돌" },
    { id: "speckle", name: "점박이 돌", base: ["#A7A399", "#9C988E"], pattern: "speckle", gloss: 0.25, desc: "검은 점이 톡톡 박힌 돌" },
    { id: "slate", name: "청회색 점판암", base: ["#5C6670", "#65707A", "#56606A"], pattern: "layers", gloss: 0.35, desc: "얇은 결이 겹친 푸른 회색 돌" },
    { id: "gneiss", name: "줄무늬 편마암", base: ["#7C776E", "#86817A", "#6F6A62"], pattern: "layers", gloss: 0.25, desc: "밝고 어두운 결이 번갈아 흐르는 돌" },
    { id: "jasper", name: "붉은 벽옥", base: ["#8B3E2C", "#7E3727", "#96472F"], pattern: "jasper", gloss: 0.45, desc: "붉은빛이 도는 단단한 돌" },
    { id: "serpentine", name: "초록 사문석", base: ["#4E6A55", "#57735D", "#465F4C"], pattern: "mottle", gloss: 0.5, desc: "숲처럼 얼룩진 초록 돌" },
    { id: "jade", name: "옥빛 돌", base: ["#8FA890", "#9BB39B", "#859E86"], pattern: "mottle", gloss: 0.5, desc: "옅은 물빛 초록 돌" },
    { id: "marble", name: "흰 대리석", base: ["#E4E1D8", "#DCD8CD", "#EAE7DF"], pattern: "marble", gloss: 0.35, desc: "회색 실금이 흐르는 흰 돌" },
    { id: "quartz", name: "석영 줄무늬 돌", base: ["#3B3B3E", "#7F7B74", "#5C6670"], pattern: "vein", gloss: 0.4, desc: "하얀 석영 줄이 지나가는 돌" },
    { id: "ring", name: "띠 두른 돌", base: ["#2E2E30", "#5C6670", "#8B3E2C"], pattern: "ring", gloss: 0.5, desc: "흰 줄 한 가닥이 몸을 한 바퀴 두른 돌" }
  ];

  /* ───────── 특징 뽑기 (순서 고정: 두 플랫폼 결과가 같도록) ───────── */
  function traitsOf(seed) {
    var r = rng(seed);
    var stone = STONES[Math.floor(r() * STONES.length)];
    var base = hex(stone.base[Math.floor(r() * stone.base.length)]);
    base = shade(base, (r() - 0.5) * 0.12);
    var size = lerp(0.64, 1.06, r());
    var aspect = lerp(0.6, 0.92, r()), rot = (r() - 0.5) * 0.9;
    var harm = []; for (var k = 2; k <= 6; k++) harm.push({ k: k, a: (r() * 0.07) / (k - 1) * 1.6, p: r() * Math.PI * 2 });
    var flat = lerp(0.1, 0.3, r());
    var eyeSize = lerp(0.15, 0.23, r()), oddRatio = lerp(0.68, 1, r());
    var ex = lerp(-0.3, 0.3, r());
    var ey = lerp(-0.22, 0.02, r()), gap = lerp(0.9, 1.35, r()), tilt = (r() - 0.5) * 0.35;
    var spread = lerp(0, 0.6, r()), look = { x: (r() - 0.5) * 1.2, y: (r() - 0.5) * 0.8 };
    var pupil = lerp(0.5, 0.64, r()), texSeed = Math.floor(r() * 4294967296);
    return { seed: seed, stone: stone, base: base, size: size, aspect: aspect, rot: rot, harm: harm, flat: flat,
      eye: { size: eyeSize, ratio: oddRatio, x: ex, y: ey, gap: gap, tilt: tilt, spread: spread, look: look, pupil: pupil }, texSeed: texSeed };
  }

  /* ───────── 그리기 ───────── */
  function outline(t, cx, cy, W) {
    var pts = [], w = W * 0.42 * t.size, h = w * t.aspect, n = 180;
    for (var i = 0; i < n; i++) {
      var th = i / n * Math.PI * 2, r = 1;
      t.harm.forEach(function (hm) { r += hm.a * Math.cos(hm.k * th + hm.p); });
      var x = Math.cos(th) * w * r, y = Math.sin(th) * h * r;
      if (y > h * 0.35) y = h * 0.35 + (y - h * 0.35) * (1 - t.flat);
      var c = Math.cos(t.rot), s = Math.sin(t.rot);
      pts.push([cx + x * c - y * s, cy + x * s + y * c]);
    }
    return { pts: pts, w: w, h: h };
  }
  function pathOf(ctx, pts) {
    ctx.beginPath();
    for (var i = 0; i < pts.length; i++) {
      var p0 = pts[i], p1 = pts[(i + 1) % pts.length], mx = (p0[0] + p1[0]) / 2, my = (p0[1] + p1[1]) / 2;
      if (i === 0) ctx.moveTo(mx, my); else ctx.quadraticCurveTo(p0[0], p0[1], mx, my);
    }
    var p = pts[0], q = pts[1]; ctx.quadraticCurveTo(p[0], p[1], (p[0] + q[0]) / 2, (p[1] + q[1]) / 2);
    ctx.closePath();
  }
  function local(t, cx, cy, x, y) { var c = Math.cos(t.rot), s = Math.sin(t.rot); return [cx + x * c - y * s, cy + x * s + y * c]; }

  function drawPebble(cv, t, opt) {
    opt = opt || {};
    var ctx = cv.getContext("2d"), W = cv.width, H = cv.height;
    ctx.clearRect(0, 0, W, H);
    var cx = W / 2, cy = H * 0.54, o = outline(t, cx, cy, W), r = rng(t.texSeed), base = t.base;
    var dark = document.documentElement.matches("[data-theme=dark]") || (matchMedia("(prefers-color-scheme: dark)").matches && !document.documentElement.matches("[data-theme=light]"));

    // 바닥 그림자
    ctx.save();
    ctx.filter = "blur(" + (W * 0.02) + "px)";
    ctx.fillStyle = dark ? "rgba(0,0,0,.55)" : "rgba(40,36,20,.28)";
    ctx.beginPath(); ctx.ellipse(cx + o.w * 0.06, cy + o.h * 0.78, o.w * 0.95, o.h * 0.2, 0, 0, 7); ctx.fill();
    ctx.restore();

    // 몸
    pathOf(ctx, o.pts);
    ctx.fillStyle = rgba(base, 1); ctx.fill();
    ctx.save(); pathOf(ctx, o.pts); ctx.clip();
    // 얼룩
    for (var i = 0; i < 14; i++) {
      var mx = cx + (r() - 0.5) * o.w * 2, my = cy + (r() - 0.5) * o.h * 2, mr = o.w * lerp(0.2, 0.7, r()), k = (r() - 0.5) * 0.28;
      var g = ctx.createRadialGradient(mx, my, 0, mx, my, mr); g.addColorStop(0, rgba(shade(base, k), 0.45)); g.addColorStop(1, rgba(shade(base, k), 0));
      ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    }
    var pat = t.stone.pattern, j, n;
    function dots(count, rmin, rmax, colFn) { for (j = 0; j < count; j++) { var x = cx + (r() - 0.5) * o.w * 2.2, y = cy + (r() - 0.5) * o.h * 2.2; ctx.fillStyle = colFn(); ctx.beginPath(); ctx.arc(x, y, lerp(rmin, rmax, r()) * W / 520, 0, 7); ctx.fill(); } }
    if (pat === "fine") dots(900, 0.3, 0.9, function () { return r() < 0.5 ? "rgba(255,255,255," + lerp(0.03, 0.09, r()) + ")" : "rgba(0,0,0," + lerp(0.05, 0.14, r()) + ")"; });
    if (pat === "salt") { dots(1600, 0.4, 1.6, function () { var v = r(); return v < 0.45 ? "rgba(20,20,20," + lerp(0.25, 0.6, r()) + ")" : v < 0.8 ? "rgba(245,242,235," + lerp(0.25, 0.6, r()) + ")" : "rgba(160,120,100,.35)"; }); }
    if (pat === "grain") dots(2600, 0.3, 0.8, function () { return r() < 0.5 ? "rgba(255,248,230," + lerp(0.05, 0.18, r()) + ")" : "rgba(90,70,40," + lerp(0.05, 0.14, r()) + ")"; });
    if (pat === "speckle") { dots(700, 0.3, 0.8, function () { return "rgba(0,0,0,.08)"; }); dots(110, 1.2, 3.4, function () { return "rgba(25,25,25," + lerp(0.55, 0.85, r()) + ")"; }); }
    if (pat === "layers") { for (j = 0; j < 22; j++) { var yy = cy - o.h * 1.2 + j * o.h * 0.12 + (r() - 0.5) * 6; ctx.strokeStyle = r() < 0.5 ? "rgba(255,255,255,.07)" : "rgba(0,0,0,.1)"; ctx.lineWidth = lerp(1, 4, r()) * W / 520; ctx.beginPath(); ctx.moveTo(cx - o.w * 1.3, yy); ctx.bezierCurveTo(cx - o.w * 0.4, yy + (r() - 0.5) * 18, cx + o.w * 0.4, yy + (r() - 0.5) * 18, cx + o.w * 1.3, yy + (r() - 0.5) * 10); ctx.stroke(); } dots(500, 0.3, 0.8, function () { return "rgba(0,0,0,.08)"; }); }
    if (pat === "jasper") { dots(40, 6, 18, function () { return rgba(shade(base, -0.3), lerp(0.15, 0.3, r())); }); dots(400, 0.4, 1.2, function () { return "rgba(255,210,180," + lerp(0.08, 0.22, r()) + ")"; }); }
    if (pat === "mottle") { dots(30, 8, 22, function () { return rgba(r() < 0.5 ? [120, 150, 110] : [40, 60, 45], lerp(0.12, 0.28, r())); }); dots(500, 0.3, 0.9, function () { return "rgba(0,0,0,.08)"; }); }
    if (pat === "marble") { dots(500, 0.3, 0.8, function () { return "rgba(0,0,0,.05)"; }); for (j = 0; j < 5; j++) { var my0 = cy + (r() - 0.5) * o.h * 1.6; ctx.strokeStyle = "rgba(90,90,95," + lerp(0.12, 0.3, r()) + ")"; ctx.lineWidth = lerp(0.6, 2, r()) * W / 520; ctx.beginPath(); ctx.moveTo(cx - o.w * 1.3, my0); ctx.bezierCurveTo(cx - o.w * 0.4, my0 + (r() - 0.5) * o.h, cx + o.w * 0.4, my0 + (r() - 0.5) * o.h, cx + o.w * 1.3, my0 + (r() - 0.5) * o.h * 0.8); ctx.stroke(); } }
    if (pat === "vein" || pat === "ring") {
      dots(700, 0.3, 0.9, function () { return "rgba(0,0,0,.08)"; });
      var veins = pat === "ring" ? 1 : 1 + Math.floor(r() * 2);
      for (j = 0; j < veins; j++) {
        ctx.save(); ctx.strokeStyle = "rgba(244,240,230,.88)"; ctx.lineCap = "round";
        if (pat === "ring") {
          var ang = (r() - 0.5) * 0.8, off = (r() - 0.5) * o.h * 0.4;
          ctx.translate(cx, cy + off); ctx.rotate(t.rot + ang);
          ctx.lineWidth = o.h * lerp(0.07, 0.11, r()); ctx.beginPath(); ctx.ellipse(0, 0, o.w * 1.3, o.h * 0.22, 0, 0, 7); ctx.stroke();
        } else {
          var y0 = cy + (r() - 0.5) * o.h * 1.2, a1 = (r() - 0.5) * o.h, a2 = (r() - 0.5) * o.h;
          ctx.lineWidth = lerp(3, 9, r()) * W / 520; ctx.beginPath(); ctx.moveTo(cx - o.w * 1.3, y0); ctx.bezierCurveTo(cx - o.w * 0.3, y0 + a1, cx + o.w * 0.3, y0 + a2, cx + o.w * 1.3, y0 + (r() - 0.5) * o.h * 0.6); ctx.stroke();
          ctx.lineWidth *= 0.3; ctx.strokeStyle = "rgba(244,240,230,.5)"; ctx.beginPath(); ctx.moveTo(cx - o.w * 1.3, y0 + 10); ctx.bezierCurveTo(cx - o.w * 0.3, y0 + a1 + 12, cx + o.w * 0.3, y0 + a2 + 8, cx + o.w * 1.3, y0 + 12); ctx.stroke();
        }
        ctx.restore();
      }
    }
    // 빛: 왼쪽 위 하이라이트, 오른쪽 아래 그늘
    var lx = cx - o.w * 0.45, ly = cy - o.h * 0.6;
    var gl = ctx.createRadialGradient(lx, ly, 0, lx, ly, o.w * 1.3); gl.addColorStop(0, "rgba(255,255,255," + (0.16 + t.stone.gloss * 0.12) + ")"); gl.addColorStop(1, "rgba(255,255,255,0)");
    ctx.fillStyle = gl; ctx.fillRect(0, 0, W, H);
    var gd = ctx.createRadialGradient(cx + o.w * 0.5, cy + o.h * 0.8, 0, cx + o.w * 0.5, cy + o.h * 0.8, o.w * 1.4); gd.addColorStop(0, "rgba(0,0,0,.34)"); gd.addColorStop(1, "rgba(0,0,0,0)");
    ctx.fillStyle = gd; ctx.fillRect(0, 0, W, H);
    // 가장자리 어둡게
    pathOf(ctx, o.pts); ctx.lineWidth = W * 0.035; ctx.strokeStyle = "rgba(0,0,0,.16)"; ctx.filter = "blur(" + W * 0.01 + "px)"; ctx.stroke(); ctx.filter = "none";
    // 반사광
    if (t.stone.gloss > 0.3) { ctx.save(); ctx.filter = "blur(" + W * 0.012 + "px)"; ctx.fillStyle = "rgba(255,255,255," + t.stone.gloss * 0.35 + ")"; ctx.beginPath(); ctx.ellipse(cx - o.w * 0.45, cy - o.h * 0.5, o.w * 0.16, o.h * 0.07, -0.5, 0, 7); ctx.fill(); ctx.restore(); }
    ctx.restore();

    // 계절 잎 소품
    if (opt.sprout) {
      var top = local(t, cx, cy, o.w * 0.05, -o.h * 0.98), s = W / 520;
      ctx.save(); ctx.translate(top[0], top[1]); ctx.rotate(t.rot * 0.6);
      ctx.strokeStyle = "#5F7236"; ctx.lineWidth = 5 * s; ctx.lineCap = "round"; ctx.beginPath(); ctx.moveTo(0, 6 * s); ctx.quadraticCurveTo(2 * s, -14 * s, 0, -30 * s); ctx.stroke();
      ctx.fillStyle = "#7E9447"; ctx.beginPath(); ctx.moveTo(0, -26 * s); ctx.bezierCurveTo(-26 * s, -44 * s, -40 * s, -20 * s, -30 * s, -8 * s); ctx.bezierCurveTo(-16 * s, -6 * s, -6 * s, -14 * s, 0, -26 * s); ctx.fill();
      ctx.fillStyle = "#98AD5E"; ctx.beginPath(); ctx.moveTo(0, -30 * s); ctx.bezierCurveTo(22 * s, -54 * s, 42 * s, -34 * s, 34 * s, -20 * s); ctx.bezierCurveTo(20 * s, -16 * s, 8 * s, -22 * s, 0, -30 * s); ctx.fill();
      ctx.restore();
    }

    // 눈
    var e = t.eye, er = o.w * e.size, blink = opt.blink || 0;
    var centers = [-1, 1].map(function (sd) { return local(t, cx, cy, o.w * e.x + sd * er * e.gap, o.h * e.y + sd * er * e.tilt * 2); });
    var look = opt.look || e.look;
    centers.forEach(function (c, idx) {
      var rr = er * (idx === 0 ? 1 : e.ratio);
      ctx.save();
      ctx.shadowColor = "rgba(0,0,0,.35)"; ctx.shadowBlur = rr * 0.35; ctx.shadowOffsetY = rr * 0.12;
      var ge = ctx.createRadialGradient(c[0] - rr * 0.3, c[1] - rr * 0.35, rr * 0.1, c[0], c[1], rr);
      ge.addColorStop(0, "#FFFFFF"); ge.addColorStop(0.75, "#F2F2F0"); ge.addColorStop(1, "#D8D8D4");
      ctx.fillStyle = ge; ctx.beginPath(); ctx.ellipse(c[0], c[1], rr, rr * (1 - blink * 0.92), 0, 0, 7); ctx.fill();
      ctx.restore();
      ctx.strokeStyle = "rgba(0,0,0,.18)"; ctx.lineWidth = Math.max(1, rr * 0.05); ctx.beginPath(); ctx.ellipse(c[0], c[1], rr, rr * (1 - blink * 0.92), 0, 0, 7); ctx.stroke();
      if (blink < 0.6) {
        var pr = rr * e.pupil, lx2 = look.x, ly2 = look.y;
        if (!opt.look) { lx2 += idx === 0 ? -e.spread : e.spread; }
        var len = Math.hypot(lx2, ly2), lim = rr - pr - rr * 0.06;
        if (len > 1) { lx2 /= len; ly2 /= len; }
        var px = c[0] + lx2 * lim, py = c[1] + ly2 * lim + lim * 0.18;
        ctx.save(); ctx.beginPath(); ctx.ellipse(c[0], c[1], rr, rr * (1 - blink * 0.92), 0, 0, 7); ctx.clip();
        ctx.fillStyle = "#141414"; ctx.beginPath(); ctx.arc(px, py, pr, 0, 7); ctx.fill();
        ctx.restore();
      }
      ctx.fillStyle = "rgba(255,255,255,.85)"; ctx.beginPath(); ctx.ellipse(c[0] - rr * 0.38, c[1] - rr * 0.42, rr * 0.2, rr * 0.12, -0.6, 0, 7); ctx.fill();
    });
  }


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

  /* ───────── 정원에 놓이는 것 12가지 (손그림) ─────────
     기준점 (x, y) = 땅에 닿는 가운데, s = 크기 배율. 기본 높이 약 100 단위. */
  function loc(x, y, s) {
    return {
      b: function (cx, cy, rx, ry, seed, wob, rot) { return blob(x + cx * s, y + cy * s, rx * s, ry * s, seed, wob, rot); },
      p: function (pts) { return pts.map(function (q) { return [x + q[0] * s, y + q[1] * s]; }); },
      c: function (arr) { var o = []; for (var i = 0; i < arr.length; i += 2) o.push(x + arr[i] * s, y + arr[i + 1] * s); return bez(o, 20); },
      n: function (v) { return v * s; }
    };
  }
  var OBJ = [
    { id: "cairn", ko: "쌓은 돌", when: "함께한 지 100일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x + L.n(4), y + L.n(1), L.n(46), L.n(6), "#3B3325", 0.3);
      pastel(c, L.b(0, -14, 36, 15, 11, 0.08), "#A9A293", { seed: 11, lw: L.n(1) });
      pastel(c, L.b(3, -38, 25, 11, 12, 0.1), "#BEB6A6", { seed: 12, lw: L.n(1) });
      pastel(c, L.b(-2, -55, 15, 8, 13, 0.1), "#CFC6B3", { seed: 13, lw: L.n(0.9) });
    } },
    { id: "moss", ko: "이끼 방석", when: "시작한 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(46), L.n(5), "#3B3325", 0.25);
      pastel(c, L.b(0, -9, 44, 12, 21, 0.14), "#8C9A5B", { seed: 21, ang: -0.3, lw: L.n(1) });
      pastel(c, L.b(-10, -15, 24, 5, 22, 0.1), "#A9B679", { seed: 22, line: false, shade: false, press: 0.45 });
    } },
    { id: "pine", ko: "작은 소나무", when: "함께한 지 1년", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(40), L.n(5), "#3B3325", 0.25);
      scribble(c, L.c([0, 0, -10, -22, 12, -44, -4, -70]), "#6E5238", 31, L.n(5.5), { press: 0.8 });
      scribble(c, L.c([2, -3, -7, -24, 14, -44, -2, -68]), "#4A3524", 32, L.n(1.3));
      scribble(c, L.c([2, -38, 14, -44, 22, -46, 30, -50]), "#6E5238", 36, L.n(3));
      [[-22, -52, 34, 11, "#50663F"], [26, -54, 26, 9, "#587048"], [-6, -74, 30, 10, "#5E7A4C"], [2, -92, 18, 7, "#6C8658"]].forEach(function (p, i) {
        pastel(c, L.b(p[0], p[1], p[2], p[3], 33 + i, 0.28), p[4], { seed: 33 + i, ang: -0.12, spread: 0.3, len: L.n(6), wid: L.n(1.6), lw: L.n(0.9) });
      });
    } },
    { id: "flower", ko: "들꽃 한 송이", when: "봄이 온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      scribble(c, L.c([0, 0, 5, -24, -4, -46, -2, -72]), "#6F8A4A", 41, L.n(1.8));
      pastel(c, L.b(8, -30, 12, 4, 42, 0.1, -0.5), "#86A05C", { seed: 42, lw: L.n(0.7) });
      pastel(c, L.b(-9, -44, 10, 3.5, 43, 0.1, 0.5), "#86A05C", { seed: 43, lw: L.n(0.7) });
      for (var i = 0; i < 5; i++) { var a = -Math.PI / 2 + i * Math.PI * 2 / 5; pastel(c, L.b(-2 + Math.cos(a) * 9, -76 + Math.sin(a) * 8, 8, 5, 44 + i, 0.12, a), "#F4ECD8", { seed: 44 + i, dark: 0.12, lw: L.n(0.7), lineA: 0.8 }); }
      pastel(c, L.b(-2, -76, 4, 4, 49, 0.1), "#D9A441", { seed: 49, lw: L.n(0.6) });
    } },
    { id: "pond", ko: "작은 물웅덩이", when: "여름이 온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      pastel(c, L.b(0, -6, 54, 12, 51, 0.1), "#9DB4B5", { seed: 51, ang: -0.04, spread: 0.2, light: 0.28, lw: L.n(0.9) });
      scribble(c, L.b(-10, -8, 16, 3.5, 52, 0.05).slice(0, 40), "#F6F4EC", 52, L.n(1.2), { alpha: 0.9 });
      pastel(c, L.b(22, -8, 11, 4, 53, 0.12), "#7F9A58", { seed: 53, lw: L.n(0.7) });
      [[-58, 0, -20, -1.4], [-53, -1, -14, 0.6], [56, -2, -12, 1.2]].forEach(function (g, i) { scribble(c, L.c([g[0], g[1], g[0] + g[3], g[1] + g[2] * 0.4, g[0] + g[3] * 2, g[1] + g[2] * 0.7, g[0] + g[3] * 4, g[1] + g[2]]), "#5E6B3A", 54 + i, L.n(0.9)); });
    } },
    { id: "teacup", ko: "따뜻한 차 한 잔", when: "함께한 지 7일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y, L.n(44), L.n(6), "#3B3325", 0.25);
      pastel(c, L.b(0, -4, 40, 6.5, 61, 0.05), "#E6DDCB", { seed: 61, lw: L.n(0.8) });
      scribble(c, L.c([22, -30, 38, -30, 38, -14, 20, -14]), INK, 62, L.n(1.6));
      pastel(c, organic([[-26, -34], [26, -34], [22, -16], [12, -7], [-12, -7], [-22, -16]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.8), 63, L.n(3)), "#F1EADB", { seed: 63, lw: L.n(1) });
      pastel(c, L.b(0, -33, 23, 4, 64, 0.05), "#8E6536", { seed: 64, shade: false, line: false, press: 0.8 });
      scribble(c, L.c([-4, -42, -10, -52, 2, -60, -6, -74]), "#B9B1A2", 65, L.n(1.1), { alpha: 0.8 });
      scribble(c, L.c([8, -42, 2, -54, 14, -62, 6, -78]), "#B9B1A2", 66, L.n(1.1), { alpha: 0.7 });
    } },
    { id: "candle", ko: "작은 촛불", when: "생일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y - L.n(58), L.n(40), L.n(40), "#F2B35A", 0.28);
      pastel(c, L.b(0, -6, 30, 8, 71, 0.1), "#A9A293", { seed: 71, lw: L.n(0.9) });
      pastel(c, organic([[-9, -10], [9, -10], [9, -50], [-9, -50]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.6), 72, L.n(3)), "#F2EAD8", { seed: 72, lw: L.n(0.9) });
      scribble(c, L.c([0, -50, 1, -53, -1, -55, 0, -57]), INK, 73, L.n(1));
      pastel(c, L.b(0, -64, 4.5, 8, 74, 0.1), "#F0A84A", { seed: 74, shade: false, press: 0.9, line: false, smudge: 0.8 });
      pastel(c, L.b(0, -62, 2, 4, 75, 0.1), "#FFE9B0", { seed: 75, shade: false, press: 0.95, line: false, smudge: 0.9 });
    } },
    { id: "leaf", ko: "떨어진 잎", when: "가을이 온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      var pts = []; for (var i = 0; i <= 40; i++) { var t = i / 40, a = t * Math.PI * 2, rx = Math.cos(a) * 30, ry = Math.sin(a) * 11 * (1 - 0.5 * Math.cos(a)); pts.push([rx, ry - 8]); }
      var P = organic(pts.map(function (q) { var cc = Math.cos(-0.25), ss = Math.sin(-0.25); return [x + (q[0] * cc - q[1] * ss) * s, y + (q[0] * ss + q[1] * cc) * s]; }), L.n(0.8), 81, L.n(3));
      smudge(c, x + L.n(3), y, L.n(32), L.n(4), "#3B3325", 0.22);
      pastel(c, P, "#C0773C", { seed: 81, ang: 0.2, lw: L.n(0.9) });
      scribble(c, L.c([-30, 0, -10, -6, 10, -10, 30, -16]), "#7A4424", 82, L.n(0.9));
      for (var k = 0; k < 4; k++) { var bx = -18 + k * 12; scribble(c, L.c([bx, -4 - k * 2.2, bx + 3, -10 - k * 2, bx + 6, -13 - k * 2, bx + 8, -16 - k * 2]), "#7A4424", 83 + k, L.n(0.6)); }
    } },
    { id: "feather", ko: "깃털 하나", when: "처음 문장을 넘긴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      var vane = []; for (var i = 0; i <= 40; i++) { var t = i / 40, a = t * Math.PI * 2; vane.push([Math.cos(a) * 38, Math.sin(a) * 8 * (1 + 0.3 * Math.cos(a))]); }
      var rot = -0.35, cc = Math.cos(rot), ss = Math.sin(rot);
      function T(q) { return [x + (q[0] * cc - q[1] * ss) * s, y - L.n(12) + (q[0] * ss + q[1] * cc) * s]; }
      smudge(c, x, y, L.n(38), L.n(4), "#3B3325", 0.18);
      pastel(c, organic(vane.map(T), L.n(0.6), 91, L.n(3)), "#DCD7CB", { seed: 91, ang: -0.9, dark: 0.18, lw: L.n(0.8) });
      scribble(c, [T([-46, 2]), T([-20, 0.5]), T([10, -0.5]), T([38, 0])], "#8A8174", 92, L.n(1.2));
      for (var k = 0; k < 8; k++) { var ux = -28 + k * 8; scribble(c, [T([ux, 0]), T([ux + 6, -6])], "#A39A8C", 93 + k, L.n(0.6), { alpha: 0.7 }); scribble(c, [T([ux, 0]), T([ux + 6, 6])], "#A39A8C", 103 + k, L.n(0.6), { alpha: 0.7 }); }
    } },
    { id: "snail", ko: "느린 달팽이", when: "오랜만에 돌아온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(40), L.n(4), "#3B3325", 0.22);
      pastel(c, organic([[-30, -2], [30, -2], [40, -8], [38, -20], [30, -20], [26, -12], [-24, -10]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.8), 111, L.n(3)), "#BFB6A3", { seed: 111, lw: L.n(0.9) });
      scribble(c, L.c([32, -20, 31, -26, 30, -30, 28, -34]), INK, 112, L.n(0.9)); scribble(c, L.c([36, -20, 38, -26, 39, -30, 41, -33]), INK, 113, L.n(0.9));
      pastel(c, L.b(28, -35, 1.8, 1.8, 114, 0.05), "#3A332A", { line: false, shade: false, press: 0.9 }); pastel(c, L.b(41, -34, 1.8, 1.8, 115, 0.05), "#3A332A", { line: false, shade: false, press: 0.9 });
      pastel(c, L.b(-4, -26, 21, 19, 116, 0.06), "#C99B63", { seed: 116, lw: L.n(1) });
      var sp = []; for (var i = 0; i <= 60; i++) { var t = i / 60, a = t * Math.PI * 4.2, rr = lerp(15, 2, t); sp.push([x + (-4 + Math.cos(a) * rr) * s, y + (-26 + Math.sin(a) * rr * 0.92) * s]); }
      scribble(c, sp, "#7A5634", 117, L.n(1.1));
    } },
    { id: "dandelion", ko: "민들레 홀씨", when: "새해 첫날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      scribble(c, L.c([0, 0, 4, -26, -3, -54, 2, -80]), "#7E9453", 121, L.n(1.5));
      pastel(c, L.b(2, -82, 3, 3, 122, 0.1), "#B8AE8C", { line: false, press: 0.9 });
      for (var i = 0; i < 18; i++) { var a = i / 18 * Math.PI * 2, r0 = 19 + (i % 3) * 2; scribble(c, [[x + L.n(2), y - L.n(82)], [x + L.n(2 + Math.cos(a) * r0 * 0.6), y + L.n(-82 + Math.sin(a) * r0 * 0.6)], [x + L.n(2 + Math.cos(a) * r0), y + L.n(-82 + Math.sin(a) * r0)]], "#A8A194", 123 + i, L.n(0.5), { alpha: 0.75, passes: 1 }); pastel(c, L.b(2 + Math.cos(a) * r0, -82 + Math.sin(a) * r0, 2.2, 2.2, 150 + i, 0.2), "#EDE9DF", { line: false, shade: false, press: 0.8 }); }
    } },
    { id: "acorn", ko: "도토리", when: "인생의 계절이 바뀐 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x + L.n(2), y + L.n(1), L.n(22), L.n(4), "#3B3325", 0.25);
      pastel(c, L.b(0, -21, 15, 20, 131, 0.06), "#B07A45", { seed: 131, lw: L.n(0.9) });
      pastel(c, L.b(0, -39, 18, 8, 132, 0.08), "#7A5B3A", { seed: 132, lw: L.n(0.9) });
      scribble(c, L.c([0, -46, 1, -50, 3, -53, 6, -55]), "#4A3524", 133, L.n(1.8));
    } }
  ];
  function drawObj(c, id, x, y, s) { OBJ.filter(function (o) { return o.id === id; })[0].draw(c, x, y, s); }

  /* ───────── 장면: 하루의 정원 (종이 위 파스텔) ─────────
     화면 단위 390 × 845. 하늘은 대부분 종이 그대로 비워 두고(글자 자리), 아래 1/4에 낮은 둔덕과 하루. */
  var SEASONS = {
    spring: { ko: "봄", sky: "#DCE6D2", sky2: "#EFE3D6", sun: "#F3D39A", hill: "#C3CDAE", ground: "#A9BC84", tuft: "#6F8A4A", items: [["flower", 62, 742, 1.0], ["moss", 318, 752, 0.9], ["dandelion", 352, 736, 0.75]], part: "petal" },
    summer: { ko: "여름", sky: "#CFE0DE", sky2: "#E9EBD9", sun: "#F4DFA8", hill: "#AFC39C", ground: "#8FAA6A", tuft: "#5A7440", items: [["pine", 66, 740, 1.05], ["pond", 316, 766, 0.95], ["teacup", 330, 742, 0.6]], part: "none" },
    autumn: { ko: "가을", sky: "#F1DCC0", sky2: "#F2E7D6", sun: "#EDB072", hill: "#D6C09A", ground: "#C9A46A", tuft: "#8C6A3A", items: [["snail", 318, 752, 0.95], ["acorn", 70, 748, 1.1], ["leaf", 112, 770, 0.8], ["leaf", 262, 782, 0.6]], part: "leaf" },
    winter: { ko: "겨울", sky: "#E1E5E8", sky2: "#F0EFEA", sun: "#F4EEDF", hill: "#D9DEE0", ground: "#F4F3EE", tuft: "#9AA2A4", items: [["cairn", 70, 748, 1.0], ["candle", 322, 750, 0.9], ["feather", 250, 780, 0.55]], part: "snow" }
  };
  function groundPts(y0, amp, seed, W, H) { var r = rng(seed), f = [r() * 6, r() * 6], pts = []; for (var x = -12; x <= W + 12; x += 6) { var u = x / W; pts.push([x, y0 - amp * (Math.sin(u * 2.2 + f[0]) * 0.6 + Math.sin(u * 5 + f[1]) * 0.25)]); } return pts; }
  function gardenScene(ctx, W, H, key, seed, o) {
    o = o || {}; var S = SEASONS[key]; setU(W / 390); var h = H / U;
    paper(ctx, W, H, "#F4EFE4");
    wash(ctx, -60, -80, 510, 420, S.sky, { seed: 501, press: 0.42, len: 70, wid: 14, smudge: 0.34, fade: "down" });
    wash(ctx, -60, 200, 510, 460, S.sky2, { seed: 502, press: 0.32, len: 70, wid: 12, smudge: 0.22, dense: 0.8 });
    pastel(ctx, blob(66, h * 0.66, 26, 26, 503, 0.03), S.sun, { seed: 503, shade: false, press: 0.5, line: false, len: 14, wid: 4, smudge: 0.35 });
    // 먼 언덕
    var hp = groundPts(h * 0.72, 14, 504, 390, h); var hill = hp.concat([[402, h * 0.8], [-12, h * 0.8]]);
    pastel(ctx, hill, S.hill, { seed: 504, ang: -0.1, spread: 0.8, len: 20, wid: 6, dense: 0.7, press: 0.42, shade: false, line: false, smudge: 0.55 });
    scribble(ctx, hp, sh(S.hill, -0.35), 505, 0.9, { alpha: 0.45 });
    // 둔덕
    var gp = groundPts(h * 0.8, 10, 506, 390, h); var gnd = gp.concat([[440, gp[gp.length - 1][1]], [440, h + 60], [-50, h + 60], [-50, gp[0][1]]]);
    pastel(ctx, gnd, S.ground, { seed: 506, ang: -0.3, spread: 1.2, len: 16, wid: 5, dense: 0.8, press: 0.5, dark: key === "winter" ? 0.08 : 0.16, light: 0.12, line: false, smudge: 0.75 });
    scribble(ctx, gp, INK, 507, 1, { alpha: key === "winter" ? 0.35 : 0.5, passes: 1 });
    var r = rng(508);
    for (var i = 0; i < 26; i++) { var tx = r() * 390, ty = gp[Math.max(0, Math.min(gp.length - 1, Math.round((tx + 12) / 6)))][1] + lerp(4, 70, r()), tl = lerp(5, 11, r()), tlean = (r() - 0.5) * 5; scribble(ctx, bez([tx, ty, tx + tlean * 0.3, ty - tl * 0.4, tx + tlean * 0.7, ty - tl * 0.7, tx + tlean, ty - tl]), S.tuft, 520 + i, 0.8, { alpha: 0.75, passes: 1 }); }
    var items = o.items || S.items;
    items.filter(function (it) { return it[2] < 760; }).forEach(function (it) { drawObj(ctx, it[0], it[1], it[2], it[3]); });
    if (seed != null) drawHaru(ctx, traitsOf(seed), 195, o.haruY || 772, o.haruW || 104, { sprout: key === "spring" });
    items.filter(function (it) { return it[2] >= 760; }).forEach(function (it) { drawObj(ctx, it[0], it[1], it[2], it[3]); });
    // 계절 입자 (아주 조금)
    var pr = rng(530);
    for (i = 0; i < 12; i++) {
      var px = pr() * 390, py = lerp(40, h * 0.7, pr());
      if (S.part === "petal") pastel(ctx, blob(px, py, 3.6, 2.2, 540 + i, 0.1, pr() * 3), "#EBC6C2", { line: false, shade: false, press: 0.75, smudge: 0.5 });
      if (S.part === "leaf" && i < 6) drawObj(ctx, "leaf", px, py, 0.32);
      if (S.part === "snow") for (var k = 0; k < 3; k++) pastel(ctx, blob(pr() * 390, lerp(30, h * 0.9, pr()), 2.4, 2.4, 560 + i * 3 + k, 0.1), "#FFFFFF", { line: false, shade: false, press: 0.9, smudge: 0.7 });
    }
  }

  var THEMES = [
    { id: "olive", ko: "올리브", a: "#DCE2C8", b: "#EFE6D0", p: "#F4EFE4", ink: "#1F2119", sub: "#5E604B" },
    { id: "dawn", ko: "새벽 분홍", a: "#F1D9D4", b: "#E2D7E6", p: "#F6F0EA", ink: "#2A2126", sub: "#6A5A63" },
    { id: "sea", ko: "바다 안개", a: "#D5E5E3", b: "#E9EEE6", p: "#F2F2EC", ink: "#18252A", sub: "#4E6468" },
    { id: "amber", ko: "가을 호박", a: "#F2D7B0", b: "#F2E8D6", p: "#F6EFE3", ink: "#2A1E12", sub: "#6E5236" },
    { id: "snow", ko: "눈과 하늘", a: "#DDE6EC", b: "#F2F2EE", p: "#F6F6F2", ink: "#1C2328", sub: "#56636C" },
    { id: "night", ko: "밤 남색", a: "#39406A", b: "#2A2F4C", p: "#23273D", ink: "#ECEBF5", sub: "#B4B5D0", night: true }
  ];
  function theme(ctx, W, H, T) {
    setU(W / 390); var h = H / U;
    paper(ctx, W, H, T.p);
    wash(ctx, -60, -120, 510, h * 0.8, T.a, { seed: 601 + T.id.length, press: T.night ? 0.7 : 0.42, len: 80, wid: 16, smudge: T.night ? 0.7 : 0.34, fade: "down" });
    wash(ctx, -60, h * 0.35, 510, h * 0.8, T.b, { seed: 611 + T.id.length, press: T.night ? 0.6 : 0.35, len: 80, wid: 16, smudge: T.night ? 0.6 : 0.26, fade: "up" });
    if (T.night) { var r = rng(620); for (var i = 0; i < 26; i++) pastel(ctx, blob(r() * 390, r() * h * 0.7, lerp(1, 2.2, r()), lerp(1, 2.2, r()), 630 + i, 0.1), "#FFF6DA", { line: false, shade: false, press: 0.9, smudge: 0.8 }); }
  }

  // 후원 감사 그림: 600 × 400 단위. 해 질 녘, 하루 곁에 촛불과 차 한 잔
  function donation(ctx, W, H, seed) {
    setU(W / 600);
    paper(ctx, W, H, "#F4EDE2");
    wash(ctx, -60, -80, 720, 300, "#F2D6B4", { seed: 701, press: 0.45, len: 90, wid: 16, smudge: 0.36, fade: "down" });
    wash(ctx, -60, 110, 720, 200, "#E6D6E0", { seed: 702, press: 0.32, len: 90, wid: 14, smudge: 0.24 });
    pastel(ctx, blob(420, 170, 34, 34, 703, 0.03), "#EFB47A", { seed: 703, shade: false, press: 0.55, line: false, len: 14, wid: 4, smudge: 0.4 });
    var hp = groundPts(262, 8, 704, 600, 400); pastel(ctx, hp.concat([[612, 300], [-12, 300]]), "#D3BFA2", { seed: 704, ang: -0.1, spread: 0.8, len: 22, wid: 6, dense: 0.7, press: 0.42, shade: false, line: false, smudge: 0.55 });
    var gp = groundPts(290, 6, 705, 600, 400); pastel(ctx, gp.concat([[660, 290], [660, 460], [-60, 460], [-60, 290]]), "#B9A27A", { seed: 705, ang: -0.3, spread: 1.2, len: 16, wid: 5, dense: 0.8, press: 0.5, line: false, smudge: 0.75 });
    scribble(ctx, gp, INK, 706, 1, { alpha: 0.5, passes: 1 });
    drawObj(ctx, "candle", 196, 344, 1.05);
    drawObj(ctx, "teacup", 408, 346, 0.9);
    if (seed != null) drawHaru(ctx, traitsOf(seed), 300, 350, 110);
  }

  // 스토어 홍보 배경
  function store(ctx, W, H) {
    setU(W / 390); var h = H / U;
    paper(ctx, W, H, "#F4EFE4");
    wash(ctx, -60, -120, 510, h * 0.75, "#E3E6D2", { seed: 801, press: 0.4, len: 80, wid: 16, smudge: 0.3, fade: "down" });
    var gp = groundPts(h * 0.88, 8, 802, 390, h); pastel(ctx, gp.concat([[440, h * 0.88], [440, h + 60], [-50, h + 60], [-50, h * 0.88]]), "#A9BC84", { seed: 802, ang: -0.3, spread: 1.2, len: 16, wid: 5, dense: 0.8, press: 0.5, line: false, smudge: 0.75 });
    scribble(ctx, gp, INK, 803, 1, { alpha: 0.5, passes: 1 });
    drawObj(ctx, "cairn", 330, h * 0.9, 0.7);
  }

  // 정원에 놓이는 것 한 칸 (투명 배경용)
  function objCanvas(cv, o, bg) {
    var ctx = cv.getContext("2d"); ctx.clearRect(0, 0, cv.width, cv.height); setU(cv.width / 200);
    if (bg) paper(ctx, cv.width, cv.height, bg);
    o.draw(ctx, 100, 168, 1.3);
  }

  /* ───────── 손그림 방향 시안 5가지 ─────────
     같은 장면(하루 · 쌓은 돌 · 달팽이 · 땅 한 줄 · 반짝이)을 선과 채움만 바꿔 그립니다. */
  var LINE = "#2B2420";

  // 굵기가 변하는 선. o.press < 1 이면 종이 결에 걸러져 크레용처럼 거칠어짐
  function ink(ctx, pts, w, col, o) {
    o = o || {}; var closed = !o.open, r = rng(o.seed || 5), P = pts.slice();
    if (closed) P.push(P[0], P[1] || P[0]);
    var bb = bbox(pts), ph = r() * 9, amp = o.vary == null ? 0.3 : o.vary;
    layer(ctx, bb, function (x) {
      x.lineCap = "round"; x.lineJoin = "round"; x.strokeStyle = css(col || LINE, o.alpha == null ? 1 : o.alpha);
      for (var pass = 0; pass < (o.passes || 1); pass++) {
        var ox = pass ? (r() - 0.5) * w * 0.5 : 0, oy = pass ? (r() - 0.5) * w * 0.5 : 0;
        for (var i = 0; i < P.length - 1; i++) {
          var t = i / P.length, pr = 1 + amp * (Math.sin(t * 17 + ph) * 0.6 + Math.sin(t * 5.3 + ph * 2) * 0.4);
          if (o.gaps && Math.sin(t * 23 + ph) > 0.93) continue;
          if (o.taper && !closed) pr *= Math.min(1, Math.min(t, 1 - t) * 6 + 0.25);
          x.lineWidth = w * pr; x.beginPath(); x.moveTo(P[i][0] + ox, P[i][1] + oy); x.lineTo(P[i + 1][0] + ox, P[i + 1][1] + oy); x.stroke();
        }
      }
    }, o.press == null ? 1 : o.press, { pad: w * 2 });
  }
  function star4(cx, cy, R) { var pts = []; for (var i = 0; i < 64; i++) { var a = i / 64 * Math.PI * 2, k = Math.pow(Math.abs(Math.cos(a * 2)), 3.2), rr = R * (0.28 + 0.72 * k); pts.push([cx + Math.cos(a) * rr, cy + Math.sin(a) * rr]); } return pts; }

  /* 채우는 방식 */
  var FILL = {
    // 단색 + 셀 그림자(오른쪽 아래 한 조각) + 작은 반짝임
    flat: function (ctx, pts, col, o) {
      var path = smoothPath(pts), bb = bbox(pts), d = Math.min(bb[2], bb[3]) * 0.16, cut = new Path2D();
      cut.rect(bb[0] - 50, bb[1] - 50, bb[2] + 100, bb[3] + 100); var sp = pts.map(function (q) { return [q[0] - d, q[1] - d]; }); cut.addPath(smoothPath(sp));
      layer(ctx, bb, function (x) {
        x.fillStyle = css(col); x.fill(path);
        if (o.cel !== false) { x.save(); x.clip(path); x.clip(cut, "evenodd"); x.fillStyle = css(sh(col, -0.16)); x.fillRect(bb[0] - 10, bb[1] - 10, bb[2] + 20, bb[3] + 20); x.restore(); }
        if (o.shine) { x.save(); x.clip(path); x.fillStyle = "rgba(255,255,255,.55)"; x.beginPath(); x.ellipse(bb[0] + bb[2] * 0.3, bb[1] + bb[3] * 0.28, bb[2] * 0.1, bb[3] * 0.06, -0.6, 0, 7); x.fill(); x.restore(); }
      }, 1, { pad: 2 });
    },
    // 지금의 파스텔 붓질을 선 안에만
    pastel: function (ctx, pts, col, o) { pastel(ctx, pts, col, { clip: true, line: false, seed: o.seed, press: 0.72, dark: 0.16, light: 0.14, smudge: 0.65, dense: 1.1 }); },
    // 크레용: 선 밖으로 조금 어긋나고, 비스듬히 문질러 종이가 비침
    crayon: function (ctx, pts, col, o) {
      var off = pts.map(function (q) { return [q[0] + 2.2, q[1] + 1.6]; }), path = smoothPath(off), bb = bbox(off), r = rng(o.seed || 3), sp = Math.max(2.2, Math.min(bb[2], bb[3]) * 0.07);
      layer(ctx, bb, function (x) {
        x.save(); x.clip(path); x.lineCap = "round"; x.lineJoin = "round"; x.strokeStyle = css(col, o.alpha == null ? 0.9 : o.alpha); x.lineWidth = sp * (o.alpha == null ? 1.6 : 0.9);
        x.beginPath(); var a = -0.75, ca = Math.cos(a), sa = Math.sin(a), diag = bb[2] + bb[3], cx = bb[0] + bb[2] / 2, cy = bb[1] + bb[3] / 2, first = true;
        for (var k = -diag; k < diag; k += sp) { var x1 = cx + ca * -diag - sa * k, y1 = cy + sa * -diag + ca * k, x2 = cx + ca * diag - sa * k, y2 = cy + sa * diag + ca * k; if (first) { x.moveTo(x1, y1); first = false; } x.lineTo(x1 + (r() - 0.5) * 2, y1 + (r() - 0.5) * 2); x.lineTo(x2 + (r() - 0.5) * 2, y2 + (r() - 0.5) * 2); }
        x.stroke(); x.restore();
      }, o.press || 0.72, { pad: 6 });
    },
    // 물감: 선보다 살짝 번지고, 가장자리에 물감이 고이고, 가운데는 옅게
    water: function (ctx, pts, col, o) {
      var off = pts.map(function (q) { return [q[0] + 2.5, q[1] + 2]; }), path = smoothPath(off), bb = bbox(off), r = rng(o.seed || 4);
      layer(ctx, bb, function (x) {
        x.filter = "blur(" + (1.2 * U).toFixed(1) + "px)"; x.fillStyle = css(col, 0.62); x.fill(path);
        x.save(); x.clip(path); x.lineWidth = 5; x.strokeStyle = css(sh(col, -0.25), 0.55); x.stroke(path);
        x.globalCompositeOperation = "destination-out"; x.filter = "blur(" + (bb[2] * 0.08 * U).toFixed(1) + "px)"; x.fillStyle = "rgba(0,0,0,.45)"; x.beginPath(); x.ellipse(bb[0] + bb[2] * 0.36, bb[1] + bb[3] * 0.32, bb[2] * 0.2, bb[3] * 0.16, -0.4, 0, 7); x.fill();
        x.restore(); x.filter = "none";
      }, 0.88, { pad: 10 });
    },
    // 2 + 3: 선에서 살짝 어긋난 파스텔 + 그 위에 비스듬히 문지른 크레용 결 (종이가 비치는 흰 반점 없음)
    mix: function (ctx, pts, col, o) {
      var off = pts.map(function (q) { return [q[0] + 1.8, q[1] + 1.3]; });
      pastel(ctx, off, col, { clip: true, line: false, seed: o.seed, press: 0.8, dark: 0.14, light: 0.12, smudge: 0.8, dense: 1.1 });
      FILL.crayon(ctx, pts, sh(col, -0.14), { seed: (o.seed || 1) + 7, alpha: 0.32, press: 0.6 });
    },
    // 색연필: 같은 방향 빗금, 그늘 쪽만 한 번 더 교차
    hatch: function (ctx, pts, col, o) {
      var path = smoothPath(pts), bb = bbox(pts), r = rng(o.seed || 6), sp = 2.4;
      layer(ctx, bb, function (x) {
        x.save(); x.clip(path); x.lineCap = "round";
        function lines(a, alphaFn) { var ca = Math.cos(a), sa = Math.sin(a), diag = bb[2] + bb[3], cx = bb[0] + bb[2] / 2, cy = bb[1] + bb[3] / 2; for (var k = -diag; k < diag; k += sp * lerp(0.8, 1.3, r())) { var mx = cx - sa * k, my = cy + ca * k, al = alphaFn(mx, my); if (al <= 0) continue; x.strokeStyle = css(col, al); x.lineWidth = lerp(0.8, 1.3, r()); x.beginPath(); x.moveTo(mx - ca * diag, my - sa * diag); x.lineTo(mx + ca * diag, my + sa * diag); x.stroke(); } }
        function shadeAt(px, py) { return 0.5 + 0.45 * (((px - bb[0]) / bb[2] - 0.5) * 0.7 + ((py - bb[1]) / bb[3] - 0.5) * 0.9); }
        lines(-0.9, function () { return lerp(0.5, 0.85, r()); });
        lines(0.6, function (px, py) { return shadeAt(px, py) > 0.55 ? 0.5 : 0; });
        x.restore();
      }, 0.7, { pad: 4 });
    }
  };

  var STYLES = [
    { id: "bold", ko: "굵은 선 카툰", tag: "레퍼런스", paper: "#FBF8F1", fill: "flat", line: function (c, p, w, o) { ink(c, p, w, LINE, Object.assign({ vary: 0.22 }, o)); }, w: 4.2, kawaii: true, lighten: 0.3,
      say: "굵고 조금 흔들리는 짙은 선, 단색 면, 반짝이는 큰 눈과 작은 미소. 가장 귀엽고 멀리서도 잘 보입니다.", good: "작은 위젯에서도 또렷함", care: "눈이 커지면서 조약돌 눈(뽁뽁이 눈)의 개성이 약해짐" },
    { id: "boldpastel", ko: "굵은 선 + 파스텔", tag: "", paper: "#F4EFE4", fill: "pastel", line: function (c, p, w, o) { ink(c, p, w, LINE, Object.assign({ vary: 0.28, press: 0.9, passes: 1 }, o)); }, w: 3.8, lighten: 0.22,
      say: "레퍼런스의 굵은 선에 지금의 파스텔 붓질을 채웠습니다. 선은 종이 결에 살짝 걸려 거칠고, 하루의 뽁뽁이 눈은 그대로입니다.", good: "귀여움과 손맛, 조용함이 함께 있음", care: "파스텔 채움은 그리는 데 시간이 걸려 앱에서 한 번 그려 저장해 둬야 함" },
    { id: "crayon", ko: "크레용", tag: "", paper: "#FAF6EC", fill: "crayon", line: function (c, p, w, o) { ink(c, p, w, "#3A2E26", Object.assign({ vary: 0.35, press: 0.66, passes: 2 }, o)); }, w: 3.4, lighten: 0.25,
      say: "크레용으로 문지른 듯 거친 선, 선 밖으로 조금 어긋난 빗금 채움. 어린 시절 그림장처럼 따뜻합니다.", good: "가장 손으로 그린 티가 남", care: "거친 결이 작게 보면 지저분해 보일 수 있음" },
    { id: "ink", ko: "펜 + 수채", tag: "", paper: "#F7F4EC", fill: "water", line: function (c, p, w, o) { ink(c, p, w, "#2A2A30", Object.assign({ vary: 0.5, gaps: true, taper: true }, o)); }, w: 2.4, lighten: 0.15,
      say: "붓펜처럼 굵기가 변하는 선에 물감이 선 밖으로 번집니다. 가운데는 옅고 가장자리에 물감이 고입니다.", good: "가장 차분하고 여백이 많음", care: "밝은 돌은 물감이 옅어 존재감이 약함" },
    { id: "pencil", ko: "색연필 스케치", tag: "", paper: "#F5F1E8", fill: "hatch", line: function (c, p, w, o) { pencil(c, p, "#3E3832", o.seed, w, { open: o.open, passes: 3 }); }, w: 1.3, lighten: 0.1,
      say: "색연필 빗금으로만 칠하고 그늘 쪽만 한 번 더 교차했습니다. 선은 여러 번 가볍게 긋습니다.", good: "섬세하고 어른스러운 느낌", care: "작은 크기에서는 흐리게 보임" }
  ];

  var MIX = { id: "mix", ko: "크레용 파스텔", tag: "2 + 3", paper: "#F6F1E6", fill: "mix", line: function (c, p, w, o) { ink(c, p, w, "#33281F", Object.assign({ vary: 0.32, press: 0.74, passes: 2 }, o)); }, w: 3.7, lighten: 0.24,
    say: "2안의 굵은 선 · 파스텔 · 뽁뽁이 눈에, 3안의 크레용 선과 어긋난 채움을 더했습니다.", good: "손맛이 가장 크게 살아 있으면서도 깨끗하고 조용함", care: "선이 거칠어서 아주 작은 크기(아이콘)에는 선을 조금 더 굵게" };

  /* 장면: 단위 300 × 400 (카드) 또는 390 × 845 (전화) */
  function haruShape(t, cx, gy, width) {
    var o = outline(t, 0, 0, width / 0.42), bb = bbox(o.pts), dy = gy - (bb[1] + bb[3]);
    var pts = o.pts.map(function (q) { return [q[0] + cx, q[1] + dy]; }), e = t.eye, er = o.w * e.size;
    var eyes = [-1, 1].map(function (sd, idx) { return { c: local(t, cx, dy, o.w * e.x + sd * er * e.gap, o.h * e.y + sd * er * e.tilt * 2), r: er * (idx === 0 ? 1 : e.ratio), idx: idx }; });
    var top = pts.reduce(function (m, q) { return q[1] < m[1] ? q : m; }, [0, 1e9]);
    return { pts: pts, o: o, eyes: eyes, top: top, cy: dy };
  }
  function drawConcept(ctx, W, H, st, seed, lay) {
    lay = lay || {}; var uw = lay.uw || 300; setU(W / uw); var uh = H / U, gy = lay.gy || uh * 0.8, w = st.w * (lay.lw || 1), f = FILL[st.fill];
    paper(ctx, W, H, st.paper);
    if (lay.sky) { wash(ctx, -60, -80, uw + 120, uh * 0.5, lay.sky, { seed: 901, press: 0.35, smudge: 0.25, fade: "down" }); }
    // 해
    if (!lay.noSun) { var sun = blob(uw * (lay.sunX || 0.84), uh * (lay.sunY || 0.2), 16 * (lay.k || 1), 16 * (lay.k || 1), 902, 0.04);
    f(ctx, sun, "#F3C66A", { seed: 902, cel: false });
    if (st.id !== "bold") st.line(ctx, sun, w * 0.6, { seed: 903 }); else ink(ctx, sun, w * 0.8, LINE, { seed: 903, vary: 0.2 }); }
    var k = lay.k || 1, t = traitsOf(seed), base = mix(t.base, "#FFFFFF", st.lighten);
    if (!lay.solo) {
    // 쌓은 돌
    [[-0, 13, 30, 12, "#B8B1A2"], [2, 33, 21, 10, "#C9C1B1"], [-1, 49, 13, 7, "#D8D0C0"]].forEach(function (s, i) { var p = blob(uw * 0.18 + s[0] * k, gy - s[1] * k, s[2] * k, s[3] * k, 910 + i, 0.08); f(ctx, p, s[4], { seed: 910 + i }); st.line(ctx, p, w * 0.8, { seed: 915 + i }); });
    // 달팽이
    var sx = uw * 0.8, sb = organic([[-24, -1], [22, -1], [30, -6], [28, -16], [22, -16], [18, -9], [-18, -8]].map(function (q) { return [sx + q[0] * k, gy + q[1] * k]; }), 0.6 * k, 920, 3);
    f(ctx, sb, "#CFC6B2", { seed: 920 }); st.line(ctx, sb, w * 0.8, { seed: 921 });
    [[22, -16, 20, -27], [27, -16, 31, -26]].forEach(function (a, i) { st.line(ctx, [[sx + a[0] * k, gy + a[1] * k], [sx + (a[0] + a[2]) / 2 * k, gy + (a[1] + a[3]) / 2 * k - 1], [sx + a[2] * k, gy + a[3] * k]], w * 0.55, { open: true, seed: 922 + i }); var d = blob(sx + a[2] * k, gy + a[3] * k, 2 * k, 2 * k, 924 + i, 0.05); FILL.flat(ctx, d, LINE, { cel: false }); });
    var shell = blob(sx - 3 * k, gy - 20 * k, 15 * k, 14 * k, 926, 0.05); f(ctx, shell, "#D9A866", { seed: 926 }); st.line(ctx, shell, w * 0.8, { seed: 927 });
    var sp = []; for (var i = 0; i <= 50; i++) { var tt = i / 50, a2 = tt * Math.PI * 3.8, rr = lerp(11, 1.5, tt) * k; sp.push([sx - 3 * k + Math.cos(a2) * rr, gy - 20 * k + Math.sin(a2) * rr * 0.93]); }
    st.line(ctx, sp, w * 0.5, { open: true, seed: 928 });
    }
    // 하루
    var hs = haruShape(t, uw * 0.5, gy + 1, (lay.haruW || 74) * k / Math.pow(t.size, 0.85));
    smudge(ctx, uw * 0.5 + 6 * k, gy + 2, hs.o.w * 1.05, 5 * k, "#3B3325", 0.22);
    f(ctx, hs.pts, base, { seed: 930, shine: true });
    st.line(ctx, hs.pts, w, { seed: 931 });
    // 새싹
    var tx = hs.top[0], ty = hs.top[1] + 2;
    st.line(ctx, bez([tx, ty, tx + 1, ty - 6 * k, tx - 1, ty - 10 * k, tx + 1, ty - 15 * k]), w * 0.6, { open: true, seed: 932 });
    [[-1, -0.4], [1, 0.4]].forEach(function (d, i) { var lp = blob(tx + d[0] * 8 * k, ty - 17 * k, 8 * k, 4 * k, 933 + i, 0.08, d[1]); f(ctx, lp, i ? "#A7BC6E" : "#8EA85A", { seed: 933 + i, cel: false }); st.line(ctx, lp, w * 0.55, { seed: 935 + i }); });
    // 눈
    hs.eyes.forEach(function (ey) {
      var c = ey.c, rr = ey.r;
      if (st.kawaii) {
        var R = rr * 1.25, p = blob(c[0], c[1], R, R, 940 + ey.idx, 0.02);
        FILL.flat(ctx, p, "#1E1A17", { cel: false });
        FILL.flat(ctx, blob(c[0] - R * 0.32, c[1] - R * 0.3, R * 0.34, R * 0.34, 942 + ey.idx, 0.02), "#FFFFFF", { cel: false });
        FILL.flat(ctx, blob(c[0] + R * 0.3, c[1] + R * 0.34, R * 0.14, R * 0.14, 944 + ey.idx, 0.02), "#FFFFFF", { cel: false });
      } else {
        var pe = blob(c[0], c[1], rr, rr, 940 + ey.idx, 0.03);
        if (st.fill === "hatch" || st.fill === "water") layer(ctx, bbox(pe), function (x) { x.fillStyle = css(st.paper); x.fill(smoothPath(pe)); }, 1); else f(ctx, pe, "#FFFFFF", { seed: 946 + ey.idx, cel: false });
        st.line(ctx, pe, w * 0.6, { seed: 948 + ey.idx });
        var pr = rr * t.eye.pupil, lk = t.eye.look, lx = lk.x + (ey.idx ? t.eye.spread : -t.eye.spread), ly = lk.y, ll = Math.hypot(lx, ly); if (ll > 1) { lx /= ll; ly /= ll; }
        var lim = rr - pr - rr * 0.1, pp = blob(c[0] + lx * lim, c[1] + ly * lim + lim * 0.2, pr, pr, 950 + ey.idx, 0.04);
        if (st.fill === "hatch") { FILL.hatch(ctx, pp, "#1E1A17", { seed: 952 }); FILL.hatch(ctx, pp, "#1E1A17", { seed: 953 }); } else FILL.flat(ctx, pp, "#1E1A17", { cel: false });
        if (st.fill !== "hatch") FILL.flat(ctx, blob(c[0] + lx * lim - pr * 0.35, c[1] + ly * lim + lim * 0.2 - pr * 0.38, pr * 0.24, pr * 0.24, 954 + ey.idx, 0.02), "#FFFFFF", { cel: false });
      }
    });
    if (st.kawaii) { var mc = [hs.eyes[0].c[0] * 0.5 + hs.eyes[1].c[0] * 0.5, Math.max(hs.eyes[0].c[1], hs.eyes[1].c[1]) + hs.eyes[0].r * 1.1]; ink(ctx, bez([mc[0] - 5 * k, mc[1], mc[0] - 3 * k, mc[1] + 4 * k, mc[0] + 3 * k, mc[1] + 4 * k, mc[0] + 5 * k, mc[1]]), w * 0.55, LINE, { open: true, seed: 956 }); }
    // 땅 한 줄과 풀
    var ground = []; for (var gx = uw * 0.04; gx <= uw * 0.96; gx += 4) ground.push([gx, gy + 2 + Math.sin(gx * 0.05) * 1.2]);
    st.line(ctx, ground, w * 0.9, { open: true, seed: 960 });
    [uw * 0.08, uw * 0.32, uw * 0.66, uw * 0.93].forEach(function (x0, i) { [-1, 0, 1].forEach(function (d) { st.line(ctx, [[x0 + d * 3 * k, gy + 1], [x0 + d * 5 * k, gy - (d ? 7 : 10) * k]], w * 0.5, { open: true, seed: 961 + i * 3 + d }); }); });
    // 반짝이: 하루 둘레에
    var hb = bbox(hs.pts);
    [[hb[0] - 12 * k, hb[1] + 8 * k, 9 * k], [hb[0] + hb[2] + 10 * k, hb[1] - 2 * k, 7 * k]].forEach(function (s2, i) { f(ctx, star4(s2[0], s2[1], s2[2]), "#F2C04E", { seed: 970 + i, cel: false }); });
  }

  /* ───────── 크레용 파스텔 (확정 그림체) ─────────
     선이 있는 면: 선에서 살짝 어긋난 파스텔 + 옅은 크레용 결 + 굵은 크레용 선(짙은 갈색, 두 번, 종이 결에 걸림).
     짙은 선(줄기 · 나선 · 잎맥): 굵은 크레용 선. 옅은 선(김 · 물결 · 홀씨): 가는 연필 그대로.
     선이 없는 면(하늘 · 땅 · 불꽃 · 그림자)은 파스텔 그대로. */
  var CRAYON_LINE = "#33281F";
  var pastelBase = pastel, scribbleBase = scribble;
  function luma(c) { c = C(c); return c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11; }
  pastel = function (ctx, pts, col, o) {
    o = o || {};
    if (o.line === false) return pastelBase(ctx, pts, col, o);
    FILL.mix(ctx, pts, col, { seed: o.seed });
    ink(ctx, pts, Math.max(1.8, (o.lw || 1) * 2.6), CRAYON_LINE, { vary: 0.32, press: 0.74, passes: 2, seed: (o.seed || 1) + 3 });
  };
  scribble = function (ctx, pts, col, seed, w, o) {
    o = o || {}; w = w || 1;
    if ((o.alpha != null && o.alpha < 0.9) || luma(col) > 150) return scribbleBase(ctx, pts, col, seed, w, o);
    var isDark = luma(col) < 90, lw = w < 2 ? w * (isDark ? 1.9 : 1.3) : w * 1.15, dark = isDark ? CRAYON_LINE : sh(col, -0.25);
    ink(ctx, pts, lw, dark, { open: true, vary: 0.3, press: 0.76, passes: 2, seed: seed, taper: w >= 2 });
  };

  // 하루 (크레용 파스텔)
  function drawHaruMix(ctx, t, cx, gy, width, opt) {
    opt = opt || {}; var k = width / 74, w = 3.7 * k * (opt.lw || 1), base = mix(t.base, "#FFFFFF", 0.24);
    var hs = haruShape(t, cx, gy + 1, width / Math.pow(t.size, 0.85)), body = opt.parts !== "eyes", eyes = opt.parts !== "body";
    if (body) {
    smudge(ctx, cx + 6 * k, gy + 2, hs.o.w * 1.05, 5 * k, "#3B3325", 0.22);
    FILL.mix(ctx, hs.pts, base, { seed: 930 });
    // 돌 무늬는 몇 번만 스치듯 (선 안쪽)
    var pat = t.stone.pattern, r = rng(t.texSeed), path = smoothPath(hs.pts), o = hs.o, cy = hs.cy, i;
    layer(ctx, bbox(hs.pts), function (x) {
      x.save(); x.clip(path); x.lineCap = "round";
      if (pat === "salt" || pat === "speckle") for (i = 0; i < (pat === "speckle" ? 14 : 22); i++) { x.fillStyle = r() < 0.6 ? css(sh(base, -0.45), 0.55) : css("#FFFFFF", 0.5); x.beginPath(); x.arc(cx + (r() - 0.5) * o.w * 1.8, cy + (r() - 0.5) * o.h * 1.6, o.w * lerp(0.012, pat === "speckle" ? 0.035 : 0.02, r()), 0, 7); x.fill(); }
      if (pat === "layers" || pat === "marble") for (i = 0; i < 4; i++) { var yy = cy + (r() - 0.5) * o.h * 1.3; x.strokeStyle = css(pat === "marble" ? "#7A7A7E" : sh(base, 0.3), 0.45); x.lineWidth = o.w * 0.014; x.beginPath(); x.moveTo(cx - o.w * 1.2, yy); x.bezierCurveTo(cx - o.w * 0.3, yy + (r() - 0.5) * o.h * 0.5, cx + o.w * 0.3, yy + (r() - 0.5) * o.h * 0.5, cx + o.w * 1.2, yy + (r() - 0.5) * o.h * 0.3); x.stroke(); }
      if (pat === "vein" || pat === "ring") { var y0 = cy + (r() - 0.5) * o.h * 0.5; x.strokeStyle = "rgba(248,244,234,.9)"; x.lineWidth = o.h * 0.08; x.beginPath(); x.moveTo(cx - o.w * 1.3, y0); x.bezierCurveTo(cx - o.w * 0.3, y0 + o.h * 0.25, cx + o.w * 0.3, y0 - o.h * 0.2, cx + o.w * 1.3, y0 + o.h * 0.1); x.stroke(); }
      x.restore();
    }, 0.7);
    ink(ctx, hs.pts, w, CRAYON_LINE, { vary: 0.32, press: 0.74, passes: 2, seed: 931 });
    if (opt.sprout) {
      var tx = hs.top[0], ty = hs.top[1] + 2 * k;
      ink(ctx, bez([tx, ty, tx + 1 * k, ty - 6 * k, tx - 1 * k, ty - 10 * k, tx + 1 * k, ty - 15 * k]), w * 0.6, CRAYON_LINE, { open: true, press: 0.76, passes: 2, seed: 932 });
      [[-1, -0.4], [1, 0.4]].forEach(function (d, i) { var lp = blob(tx + d[0] * 8 * k, ty - 17 * k, 8 * k, 4 * k, 933 + i, 0.08, d[1]); FILL.mix(ctx, lp, i ? "#A7BC6E" : "#8EA85A", { seed: 933 + i }); ink(ctx, lp, w * 0.55, CRAYON_LINE, { press: 0.76, passes: 2, seed: 935 + i }); });
    }
    }
    if (!eyes) return hs;
    var look = opt.look || t.eye.look;
    hs.eyes.forEach(function (ey) {
      var c = ey.c, rr = ey.r * (1 - (opt.blink || 0) * 0.85), pe = blob(c[0], c[1], ey.r, rr, 940 + ey.idx, 0.03);
      pastelBase(ctx, pe, "#FFFFFF", { clip: true, line: false, press: 0.9, shade: false, smudge: 0.95, seed: 946 + ey.idx });
      ink(ctx, pe, w * 0.6, CRAYON_LINE, { press: 0.8, passes: 2, seed: 948 + ey.idx });
      if (!opt.parts && (opt.blink || 0) < 0.6) {
        var pr = ey.r * t.eye.pupil, lx = look.x + (opt.look ? 0 : (ey.idx ? t.eye.spread : -t.eye.spread)), ly = look.y, ll = Math.hypot(lx, ly); if (ll > 1) { lx /= ll; ly /= ll; }
        var lim = ey.r - pr - ey.r * 0.1, px = c[0] + lx * lim, py = c[1] + ly * lim + lim * 0.2;
        FILL.flat(ctx, blob(px, py, pr, pr, 950 + ey.idx, 0.04), "#1E1A17", { cel: false });
        FILL.flat(ctx, blob(px - pr * 0.35, py - pr * 0.38, pr * 0.24, pr * 0.24, 954 + ey.idx, 0.02), "#FFFFFF", { cel: false });
      }
    });
    if (opt.sparkle && !opt.parts) { var hb = bbox(hs.pts); [[hb[0] - 12 * k, hb[1] + 8 * k, 9 * k], [hb[0] + hb[2] + 10 * k, hb[1] - 2 * k, 7 * k]].forEach(function (s2, i) { FILL.mix(ctx, star4(s2[0], s2[1], s2[2]), "#F2C04E", { seed: 970 + i }); }); }
    return hs;
  }

  function sunMix(ctx, x, y, r) { var p = blob(x, y, r, r, 902, 0.04); FILL.mix(ctx, p, "#F3C66A", { seed: 902 }); ink(ctx, p, Math.max(2, r * 0.13), CRAYON_LINE, { press: 0.74, passes: 2, seed: 903 }); }
  function groundLine(ctx, x0, x1, gy, w, seed, tufts) {
    var g = []; for (var gx = x0; gx <= x1; gx += 4) g.push([gx, gy + Math.sin(gx * 0.045 + seed) * 1.3]);
    ink(ctx, g, w, CRAYON_LINE, { open: true, press: 0.76, passes: 2, seed: seed, vary: 0.3 });
    (tufts || []).forEach(function (tx, i) { [-1, 0, 1].forEach(function (d) { ink(ctx, [[tx + d * 3, gy + 1], [tx + d * 4.5, gy - (d ? 7 : 10.5)]], w * 0.55, CRAYON_LINE, { open: true, press: 0.8, seed: seed + 10 + i * 3 + d }); }); });
  }

  /* 장면 (390 × 845): 하늘은 종이 그대로 비워 두고, 아래에 땅 한 줄 */
  var SEASONS_MIX = {
    spring: { ko: "봄", sky: "#DCE8D2", band: "#DCE5C4", sun: 0, items: [["flower", 62, 700, 1.0], ["moss", 318, 704, 0.85], ["dandelion", 352, 700, 0.75]], part: "petal" },
    summer: { ko: "여름", sky: "#D2E4E2", band: "#D0DEBA", items: [["pine", 64, 700, 1.0], ["pond", 318, 706, 0.85]], part: "none" },
    autumn: { ko: "가을", sky: "#F3DEC2", band: "#EBD8B4", items: [["acorn", 66, 700, 1.1], ["snail", 322, 702, 0.9], ["leaf", 116, 704, 0.7]], part: "leaf" },
    winter: { ko: "겨울", sky: "#E3E8EC", band: "#F2F2EE", items: [["cairn", 66, 702, 0.95], ["candle", 326, 702, 0.85]], part: "snow" }
  };
  function gardenMix(ctx, W, H, key, seed, o) {
    o = o || {}; var S = SEASONS_MIX[key]; setU(W / 390); var h = H / U, gy = o.gy || 700;
    paper(ctx, W, H, "#F6F1E6");
    wash(ctx, -60, -120, 510, h * 0.62, S.sky, { seed: 501, press: 0.4, smudge: 0.32, fade: "down" });
    wash(ctx, -60, gy - 30, 510, h - gy + 120, S.band, { seed: 502, press: 0.42, smudge: 0.34, fade: "up", len: 50, wid: 10 });
    sunMix(ctx, 316, gy - 250, 20);
    groundLine(ctx, 14, 376, gy + 2, 3.3, 507, [30, 128, 262, 364]);
    var items = o.items || S.items;
    items.forEach(function (it) { drawObj(ctx, it[0], it[1], it[2], it[3]); });
    if (seed != null) drawHaruMix(ctx, traitsOf(seed), 195, gy, o.haruW || 96, { sprout: key === "spring", sparkle: true });
    var pr = rng(530), i;
    for (i = 0; i < 10; i++) {
      var px = pr() * 390, py = lerp(60, gy - 200, pr());
      if (S.part === "petal") pastelBase(ctx, blob(px, py, 4, 2.4, 540 + i, 0.1, pr() * 3), "#EBC6C2", { line: false, shade: false, press: 0.8, smudge: 0.6 });
      if (S.part === "leaf" && i < 5) { var lp = blob(px, py, 7, 3, 560 + i, 0.1, pr() * 3); FILL.mix(ctx, lp, pr() < 0.5 ? "#C8793A" : "#D9A441", { seed: 560 + i }); ink(ctx, lp, 1.6, CRAYON_LINE, { press: 0.8, seed: 565 + i }); }
      if (S.part === "snow") for (var k2 = 0; k2 < 3; k2++) pastelBase(ctx, blob(pr() * 390, lerp(30, gy - 20, pr()), 2.6, 2.6, 570 + i * 3 + k2, 0.1), "#FFFFFF", { line: false, shade: false, press: 0.95, smudge: 0.8 });
    }
  }
  function donationMix(ctx, W, H, seed) {
    setU(W / 600); var gy = 330;
    paper(ctx, W, H, "#F6F0E6");
    wash(ctx, -60, -80, 720, 320, "#F4D8B6", { seed: 701, press: 0.42, smudge: 0.34, fade: "down" });
    wash(ctx, -60, 150, 720, 190, "#E8DAE2", { seed: 702, press: 0.32, smudge: 0.24 });
    sunMix(ctx, 470, 150, 26);
    groundLine(ctx, 40, 560, gy + 2, 3.4, 706, [70, 250, 360, 530]);
    drawObj(ctx, "candle", 186, gy, 1.05);
    drawObj(ctx, "teacup", 420, gy, 0.9);
    if (seed != null) drawHaruMix(ctx, traitsOf(seed), 300, gy, 84, { sparkle: true });
  }
  function storeMix(ctx, W, H) {
    setU(W / 390); var h = H / U, gy = h * 0.86;
    paper(ctx, W, H, "#F6F1E6");
    wash(ctx, -60, -120, 510, h * 0.7, "#E3E8D2", { seed: 801, press: 0.4, smudge: 0.3, fade: "down" });
    groundLine(ctx, 14, 376, gy, 3.3, 803, [40, 180, 300]);
    drawObj(ctx, "cairn", 330, gy - 2, 0.75);
  }
  function themeMix(ctx, W, H, T) { theme(ctx, W, H, T); }

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

return exportHaru;
})();
