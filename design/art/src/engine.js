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

