  /* ───────── 장면: 계절 배경 · 유리병 정원 · 테마 · 후원 · 스토어 ───────── */
  var SEASONS = {
    spring: { ko: "봄", sky: ["#DCE5D6", "#F4F0E2"], far: "#BCC9AE", mid: "#9DB07A", near: "#7F9652", ground: "#6F8744", tree: ["#A9BD7A", "#C6D29A", "#E8C9C4"], trunk: "#7A5A3A", part: "petal" },
    summer: { ko: "여름", sky: ["#CFE0D6", "#EFF0E0"], far: "#A9BFA0", mid: "#7F9F66", near: "#5F7F43", ground: "#557338", tree: ["#5F7F43", "#6E8C4A", "#86A35A"], trunk: "#6A4A30", part: "mote" },
    autumn: { ko: "가을", sky: ["#EAD8BC", "#F5ECDB"], far: "#D2BE9C", mid: "#C29A5E", near: "#A87B40", ground: "#8E6A36", tree: ["#C8792F", "#D9A441", "#B2452F"], trunk: "#5E4028", part: "leaf" },
    winter: { ko: "겨울", sky: ["#D9E1E4", "#F3F3EF"], far: "#C9D3D6", mid: "#DCE3E3", near: "#E9EDEC", ground: "#F1F3F1", tree: ["#4F6A5A", "#5C7766", "#3F5A4C"], trunk: "#5E4A3A", part: "snow" }
  };
  function ridge(W, H, y0, amp, seed, bottom) {
    var r = rng(seed), f = [lerp(1.2, 2, r()), lerp(2.5, 4, r()), lerp(5, 8, r())], ph = [r() * 6, r() * 6, r() * 6], p = new Path2D();
    p.moveTo(-10, bottom);
    for (var x = -10; x <= W + 10; x += W / 80) { var u = x / W; p.lineTo(x, y0 - amp * (0.55 * Math.sin(u * f[0] * Math.PI + ph[0]) + 0.3 * Math.sin(u * f[1] * Math.PI + ph[1]) + 0.15 * Math.sin(u * f[2] * Math.PI + ph[2]))); }
    p.lineTo(W + 10, bottom); p.closePath(); return p;
  }
  function ridgeY(W, y0, amp, seed, x) { var r = rng(seed), f = [lerp(1.2, 2, r()), lerp(2.5, 4, r()), lerp(5, 8, r())], ph = [r() * 6, r() * 6, r() * 6], u = x / W; return y0 - amp * (0.55 * Math.sin(u * f[0] * Math.PI + ph[0]) + 0.3 * Math.sin(u * f[1] * Math.PI + ph[1]) + 0.15 * Math.sin(u * f[2] * Math.PI + ph[2])); }
  function tree(ctx, x, y, s, S, seed, key) {
    var r = rng(seed);
    if (key === "winter") {
      brush(ctx, curve([x, y, x, y - s * 0.2, x, y - s * 0.3, x, y - s * 0.4]), S.trunk, s * 0.08, { pencil: false });
      for (var k = 0; k < 3; k++) { var yy = y - s * (0.3 + k * 0.28), w = s * (0.42 - k * 0.1); paint(ctx, poly([[x, yy - s * 0.42], [x - w, yy], [x + w, yy]]), S.tree[k % 3], { box: box(x - w, yy - s * 0.42, w * 2, s * 0.42), seed: seed + k, dabs: 8, lw: 1.1 });
        paint(ctx, svg("M" + (x - w * 0.55) + "," + (yy - s * 0.2) + " Q" + x + "," + (yy - s * 0.5) + " " + (x + w * 0.5) + "," + (yy - s * 0.22) + " Q" + x + "," + (yy - s * 0.3) + " " + (x - w * 0.55) + "," + (yy - s * 0.2) + " Z"), "#FBFBF7", { box: box(x - w, yy - s * 0.5, w * 2, s * 0.3), seed: seed + 9 + k, dabs: 3, pencil: false, dark: 0.08 }); }
      return;
    }
    brush(ctx, curve([x, y, x - s * 0.02, y - s * 0.3, x + s * 0.02, y - s * 0.5, x, y - s * 0.7]), S.trunk, s * 0.07, { pencil: false });
    paint(ctx, ell(x, y - s * 0.84, s * 0.36, s * 0.3), sh(S.tree[0], -0.18), { box: box(x - s * 0.36, y - s * 1.14, s * 0.72, s * 0.6), seed: seed * 3, dabs: 14, lw: 1.1 });
    for (var i = 0; i < 9; i++) {
      var bx = x + (r() - 0.5) * s * 0.5, by = y - s * lerp(0.7, 1.0, r()), br = s * lerp(0.14, 0.22, r()), col = S.tree[Math.floor(r() * S.tree.length)];
      paint(ctx, ell(bx, by, br, br * 0.9), col, { box: box(bx - br, by - br, br * 2, br * 2), seed: seed * 7 + i, dabs: 12, lw: 1.1, pool: 0.08 });
    }
  }
  function season(ctx, W, H, key, o) {
    o = o || {}; var S = SEASONS[key], u = W / 390, r = rng(900 + key.length);
    var g = ctx.createLinearGradient(0, 0, 0, H * 0.7); g.addColorStop(0, S.sky[0]); g.addColorStop(1, S.sky[1]); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    glow(ctx, W * 0.18, H * 0.12, W * 0.9, key === "winter" ? "#FFFFFF" : PAL.amber, key === "autumn" ? 0.4 : 0.26);
    // 먼 구름: 옅은 붓 자국 몇 겹
    var cr = rng(700 + key.charCodeAt(0));
    for (var i = 0; i < 4; i++) { var cx = cr() * W, cy = H * lerp(0.12, 0.36, cr()), cw = W * lerp(0.18, 0.3, cr());
      for (var k = 0; k < 7; k++) { var dx = (cr() - 0.5) * cw * 1.3, dy = (cr() - 0.5) * cw * 0.12, dw = cw * lerp(0.35, 0.7, cr()); var cg = ctx.createRadialGradient(cx + dx, cy + dy, 0, cx + dx, cy + dy, dw); cg.addColorStop(0, "rgba(255,253,246,.5)"); cg.addColorStop(1, "rgba(255,253,246,0)"); ctx.save(); ctx.translate(cx + dx, cy + dy); ctx.scale(1, 0.28); ctx.translate(-(cx + dx), -(cy + dy)); ctx.fillStyle = cg; ctx.beginPath(); ctx.arc(cx + dx, cy + dy, dw, 0, 7); ctx.fill(); ctx.restore(); } }
    ctx.save(); ctx.globalAlpha = 0.72;
    var far = ridge(W, H, H * 0.55, H * 0.035, 31, H); paint(ctx, far, S.far, { box: box(0, H * 0.48, W, H * 0.52), seed: 920, dabs: 30, pencil: false, light: 0.05, dark: 0.08 });
    ctx.restore();
    for (i = 0; i < 18; i++) { var tx = r() * W, ty = ridgeY(W, H * 0.55, H * 0.035, 31, tx) + 4 * u, ts = lerp(16, 26, r()) * u; ctx.save(); ctx.globalAlpha = 0.55; paint(ctx, ell(tx, ty - ts * 0.4, ts * 0.45, ts * 0.55), mix(S.far, S.mid, 0.6), { box: box(tx - ts, ty - ts, ts * 2, ts), seed: 930 + i, dabs: 4, pencil: false }); ctx.restore(); }
    var mid = ridge(W, H, H * 0.64, H * 0.04, 47, H); paint(ctx, mid, S.mid, { box: box(0, H * 0.58, W, H * 0.42), seed: 940, dabs: 40, lw: 1.2 });
    var trees = o.trees == null ? 4 : o.trees;
    for (i = 0; i < trees; i++) { var tx2 = W * (i < trees / 2 ? lerp(0.04, 0.26, r()) : lerp(0.74, 0.96, r())), ty2 = ridgeY(W, H * 0.64, H * 0.04, 47, tx2) + 6 * u; tree(ctx, tx2, ty2, lerp(80, 120, r()) * u, S, 950 + i * 13, key); }
    var near = ridge(W, H, H * 0.76, H * 0.03, 59, H); paint(ctx, near, S.near, { box: box(0, H * 0.7, W, H * 0.3), seed: 960, dabs: 40, lw: 1.2 });
    var gr = ridge(W, H, H * 0.88, H * 0.015, 71, H); paint(ctx, gr, S.ground, { box: box(0, H * 0.84, W, H * 0.16), seed: 970, dabs: 30, pencil: false });
    // 안개
    var hz = ctx.createLinearGradient(0, H * 0.48, 0, H * 0.7); hz.addColorStop(0, "rgba(255,252,242,0)"); hz.addColorStop(0.5, "rgba(255,252,242,.28)"); hz.addColorStop(1, "rgba(255,252,242,0)"); ctx.fillStyle = hz; ctx.fillRect(0, H * 0.48, W, H * 0.22);
    // 계절 입자
    var pr = rng(980);
    for (i = 0; i < 34; i++) {
      var px = pr() * W, py = pr() * H * 0.9, ps = lerp(3, 7, pr()) * u, a = pr() * 6;
      if (S.part === "petal") { ctx.fillStyle = css(pr() < 0.5 ? "#F1D3D0" : "#FAE8E4", 0.9); ctx.beginPath(); ctx.ellipse(px, py, ps, ps * 0.55, a, 0, 7); ctx.fill(); }
      if (S.part === "leaf" && i < 20) { leaf(ctx, px, py, ps * 3, ps * 1.1, a, pr() < 0.5 ? "#C8792F" : "#D9A441", 990 + i); }
      if (S.part === "snow") { ctx.fillStyle = "rgba(255,255,255,.95)"; ctx.beginPath(); ctx.arc(px, py, ps * 0.5, 0, 7); ctx.fill(); }
      if (S.part === "mote" && i < 16) { glow(ctx, px, py * 0.7, ps * 2.4, "#FFF6D0", 0.7); }
    }
    grain(ctx, 0, 0, W, H, 0.35);
  }

  // 유리병 정원: 400 × 500 칸
  function jar(ctx, W, H, pebbleSeed, key) {
    var s = W / 400; ctx.save(); ctx.scale(s, s);
    var S = SEASONS[key || "spring"];
    ground(ctx, 200, 482, 170, 22, 0.38);
    var body = svg("M72,124 C72,100 92,92 112,92 L288,92 C308,92 328,100 328,124 L328,440 C328,468 306,480 280,480 L120,480 C94,480 72,468 72,440 Z");
    ctx.save(); ctx.fillStyle = "rgba(214,230,224,.38)"; ctx.fill(body); ctx.clip(body);
    // 뒤쪽 유리 그림자
    var bg = ctx.createLinearGradient(72, 0, 328, 0); bg.addColorStop(0, "rgba(255,255,255,.18)"); bg.addColorStop(1, "rgba(60,90,80,.14)"); ctx.fillStyle = bg; ctx.fillRect(72, 92, 256, 388);
    paint(ctx, svg("M60,420 C140,408 260,412 340,418 L340,490 L60,490 Z"), PAL.soil, { box: box(60, 408, 280, 82), seed: 301, dabs: 20, pencil: false, after: function (x, r) { for (var i = 0; i < 60; i++) { x.fillStyle = "rgba(200,170,130," + lerp(0.1, 0.3, r()) + ")"; x.beginPath(); x.arc(72 + r() * 256, 430 + r() * 50, lerp(0.8, 2.4, r()), 0, 7); x.fill(); } } });
    // 이끼
    var mr = rng(302);
    for (var i = 0; i < 22; i++) { var mx = 76 + i * 12 + (mr() - 0.5) * 8, my = 416 + (mr() - 0.5) * 8, mr0 = lerp(16, 26, mr()); paint(ctx, ell(mx, my, mr0, mr0 * 0.62), mr() < 0.5 ? (key === "winter" ? "#7E8F6A" : S.near) : PAL.moss, { box: box(mx - mr0, my - mr0, mr0 * 2, mr0 * 2), seed: 303 + i, dabs: 8, lw: 1, pool: 0.1 }); }
    // 작은 조약돌들
    [[112, 424, 14, "#8E8A82"], [292, 428, 11, "#C8B9A0"], [306, 418, 8, "#5C6670"]].forEach(function (p, k) { paint(ctx, ell(p[0], p[1], p[2], p[2] * 0.7), p[3], { box: box(p[0] - p[2], p[1] - p[2], p[2] * 2, p[2] * 2), seed: 340 + k, dabs: 4, lw: 1 }); });
    // 고사리 한 줄기
    brush(ctx, curve([288, 414, 292, 380, 300, 350, 312, 322]), PAL.olive, 3, { pencil: false });
    for (i = 0; i < 6; i++) { var t = i / 6, fx = lerp(290, 310, t), fy = lerp(404, 330, t); leaf(ctx, fx, fy, 22 - i * 2, 6, -2.4, PAL.leaf, 350 + i); leaf(ctx, fx, fy, 22 - i * 2, 6, -0.5, PAL.sprout, 360 + i); }
    // 하루
    if (pebbleSeed != null) {
      var t0 = traitsOf(pebbleSeed), P = 230, off = document.createElement("canvas"); off.width = off.height = Math.round(P * s * 1.2);
      drawPebble(off, t0, { sprout: key === "spring" });
      var hh = 0.42 * P * t0.size * t0.aspect * (0.35 + 0.65 * (1 - t0.flat));
      ctx.drawImage(off, 196 - P / 2, 420 - P * 0.54 - hh, P, P);
    }
    ctx.restore();
    // 앞 유리
    ctx.save(); ctx.lineWidth = 3; ctx.strokeStyle = "rgba(80,110,100,.4)"; ctx.stroke(body); ctx.restore();
    pencil(ctx, body, [70, 96, 88], 305, 1.4);
    gleam(ctx, curve([94, 140, 90, 240, 90, 340, 96, 440]), 0.5);
    ctx.save(); ctx.globalAlpha = 0.5; gleam(ctx, curve([110, 150, 108, 200, 108, 240, 110, 270]), 0.3); gleam(ctx, curve([310, 160, 312, 260, 312, 360, 308, 430]), 0.25); ctx.restore();
    paint(ctx, rrect(96, 70, 208, 30, 10), "#DCE7E3", { box: box(96, 70, 208, 30), seed: 306, dabs: 4, light: 0.4, lw: 1.3 });
    paint(ctx, svg("M112,26 C112,18 120,14 130,14 L270,14 C280,14 288,18 288,26 L292,74 L108,74 Z"), "#C49A6C", { box: box(108, 14, 184, 60), seed: 307, after: function (x, r) { for (var i = 0; i < 90; i++) { x.fillStyle = "rgba(110,70,40," + lerp(0.15, 0.45, r()) + ")"; x.beginPath(); x.ellipse(112 + r() * 176, 18 + r() * 54, lerp(1, 3, r()), lerp(0.6, 1.5, r()), r() * 3, 0, 7); x.fill(); } } });
    ctx.restore();
  }

  var THEMES = [
    { id: "olive", ko: "올리브", a: "#E7E6DB", b: "#C9D0AE", glow: "#F2B35A", ink: "#1F2119", sub: "#5E604B" },
    { id: "dawn", ko: "새벽 분홍", a: "#F3E6E2", b: "#D8C9DE", glow: "#F6C9A8", ink: "#2A2126", sub: "#6A5A63" },
    { id: "sea", ko: "바다 안개", a: "#E3ECE9", b: "#AFC9C6", glow: "#F4E3BC", ink: "#18252A", sub: "#4E6468" },
    { id: "amber", ko: "가을 호박", a: "#F3E7D2", b: "#DDB27E", glow: "#F2B35A", ink: "#2A1E12", sub: "#6E5236" },
    { id: "snow", ko: "눈과 하늘", a: "#F5F6F3", b: "#CFDDE6", glow: "#FFFFFF", ink: "#1C2328", sub: "#56636C" },
    { id: "night", ko: "밤 남색", a: "#2A3150", b: "#141A2E", glow: "#8E8BC6", ink: "#ECEBF5", sub: "#A7A9C8", night: true }
  ];
  function theme(ctx, W, H, T) {
    var g = ctx.createLinearGradient(0, 0, W * 0.4, H); g.addColorStop(0, T.a); g.addColorStop(1, T.b); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    var r = rng(T.id.length * 97);
    for (var i = 0; i < 9; i++) { var x = r() * W, y = r() * H, rad = W * lerp(0.35, 0.8, r()); glow(ctx, x, y, rad, i % 2 ? T.a : T.b, T.night ? 0.25 : 0.35); }
    glow(ctx, W * 0.12, H * 0.08, W * 1.1, T.glow, T.night ? 0.22 : 0.4);
    // 붓으로 한 번 쓴 듯한 큰 결
    for (i = 0; i < 16; i++) { var bx = r() * W, by = r() * H, bw = W * lerp(0.3, 0.7, r()); ctx.fillStyle = css(r() < 0.5 ? sh(T.b, 0.1) : sh(T.a, -0.03), 0.12); ctx.beginPath(); ctx.ellipse(bx, by, bw, bw * lerp(0.06, 0.14, r()), -0.5 + (r() - 0.5) * 0.3, 0, 7); ctx.fill(); }
    if (T.night) { for (i = 0; i < 70; i++) { var sx = r() * W, sy = r() * H * 0.8, ss = lerp(0.6, 1.8, r()) * W / 390; ctx.fillStyle = "rgba(255,250,230," + lerp(0.3, 0.9, r()) + ")"; ctx.beginPath(); ctx.arc(sx, sy, ss, 0, 7); ctx.fill(); if (ss > 1.5) glow(ctx, sx, sy, ss * 6, "#FFF6D8", 0.25); } }
    grain(ctx, 0, 0, W, H, T.night ? 0.25 : 0.35);
  }

  // 후원 감사 그림: 600 × 400 칸 (창가, 노을빛)
  function donation(ctx, W, H, pebbleSeed) {
    var s = W / 600; ctx.save(); ctx.scale(s, s);
    var wall = ctx.createLinearGradient(0, 0, 600, 400); wall.addColorStop(0, "#EEDFC6"); wall.addColorStop(1, "#D9C3A2"); ctx.fillStyle = wall; ctx.fillRect(0, 0, 600, 400);
    // 창
    var win = rrect(150, 30, 300, 240, 8);
    ctx.save(); ctx.clip(win); var sky = ctx.createLinearGradient(0, 30, 0, 270); sky.addColorStop(0, "#F3D9A6"); sky.addColorStop(0.7, "#F6C37E"); sky.addColorStop(1, "#E9A866"); ctx.fillStyle = sky; ctx.fillRect(150, 30, 300, 240);
    glow(ctx, 300, 250, 170, "#FFF0C8", 0.9);
    paint(ctx, ridge(600, 400, 232, 12, 81, 290), "#C9956A", { box: box(150, 200, 300, 90), seed: 401, dabs: 12, pencil: false, light: 0.1, dark: 0.1 });
    paint(ctx, ridge(600, 400, 250, 8, 83, 290), "#A87B5A", { box: box(150, 230, 300, 60), seed: 402, dabs: 12, pencil: false, light: 0.1, dark: 0.1 });
    ctx.restore();
    // 창틀
    [[150, 30, 300, 12], [150, 258, 300, 12], [150, 30, 12, 240], [438, 30, 12, 240], [294, 30, 12, 240], [150, 144, 300, 10]].forEach(function (f, i) { paint(ctx, rrect(f[0], f[1], f[2], f[3], 3), "#F4EBDD", { box: box(f[0], f[1], f[2], f[3]), seed: 410 + i, dabs: 4, lw: 1.1, light: 0.2, dark: 0.15 }); });
    // 창으로 들어오는 빛
    ctx.save(); ctx.globalCompositeOperation = "soft-light"; var sh0 = ctx.createLinearGradient(300, 270, 300, 400); sh0.addColorStop(0, "rgba(255,230,170,.9)"); sh0.addColorStop(1, "rgba(255,230,170,0)"); ctx.fillStyle = sh0; ctx.beginPath(); ctx.moveTo(160, 270); ctx.lineTo(440, 270); ctx.lineTo(560, 400); ctx.lineTo(90, 400); ctx.closePath(); ctx.fill(); ctx.restore();
    // 창턱
    paint(ctx, svg("M80,300 L520,300 L540,322 L60,322 Z"), PAL.woodL, { box: box(60, 300, 480, 22), seed: 420, dabs: 20 });
    paint(ctx, rrect(60, 322, 480, 20, 3), PAL.wood, { box: box(60, 322, 480, 20), seed: 421, dabs: 14 });
    ground(ctx, 300, 342, 250, 12, 0.25);
    glow(ctx, 300, 250, 260, "#FFE1A0", 0.12);
    drawProp(ctx, "teacup", 172, 316, 140);
    drawProp(ctx, "pot", 452, 314, 120);
    drawProp(ctx, "lantern", 522, -6, 130);
    if (pebbleSeed != null) { var t0 = traitsOf(pebbleSeed), P = 118, off = document.createElement("canvas"); off.width = off.height = Math.round(P * s * 1.2); drawPebble(off, t0, {}); var hh = 0.42 * P * t0.size * t0.aspect * (0.35 + 0.65 * (1 - t0.flat)); ctx.drawImage(off, 300 - P / 2, 314 - P * 0.54 - hh, P, P); }
    ctx.restore();
    grain(ctx, 0, 0, W, H, 0.4);
  }

  // 스토어 홍보 배경: 세로 9:19.5
  function store(ctx, W, H) {
    var g = ctx.createLinearGradient(0, 0, 0, H); g.addColorStop(0, "#EEEDE3"); g.addColorStop(0.55, "#DCDDC8"); g.addColorStop(1, "#B9C49A"); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    glow(ctx, W * 0.5, -H * 0.02, W * 1.1, PAL.amber, 0.35);
    var u = W / 390, r = rng(501);
    for (var i = 0; i < 14; i++) { var x = r() * W, y = H * lerp(0.84, 0.98, r()); leaf(ctx, x, y, lerp(22, 38, r()) * u, lerp(7, 11, r()) * u, -Math.PI / 2 + (r() - 0.5) * 1.6, r() < 0.5 ? PAL.leaf : PAL.olive, 510 + i); }
    grain(ctx, 0, 0, W, H, 0.35);
  }

  // 정원 한 장: 계절 배경 + 선반 + 유리병 + 소품
  function garden(ctx, W, H, key, pebbleSeed) {
    season(ctx, W, H, key, { trees: 4 });
    var u = W / 390;
    // 선반
    var shelfY = H * 0.8;
    ground(ctx, W / 2, shelfY + 4 * u, W * 0.5, 10 * u, 0.3);
    paint(ctx, rrect(-10, shelfY, W + 20, 16 * u, 2), PAL.woodL, { box: box(0, shelfY, W, 16 * u), seed: 601, dabs: 20 });
    paint(ctx, rrect(-10, shelfY + 16 * u, W + 20, 12 * u, 2), PAL.wood, { box: box(0, shelfY + 16 * u, W, 12 * u), seed: 602, dabs: 14 });
    var jw = W * 0.6, jh = jw * 1.25, jx = (W - jw) / 2, jy = shelfY + 6 * u - jh * (482 / 500);
    var off = document.createElement("canvas"); off.width = Math.round(jw); off.height = Math.round(jh);
    jar(off.getContext("2d"), off.width, off.height, pebbleSeed, key);
    ctx.drawImage(off, jx, jy, jw, jh);
    var extra = { spring: ["pot", "books"], summer: ["teacup", "lavender"], autumn: ["acorn", "basket"], winter: ["candle", "globe"] }[key];
    drawProp(ctx, extra[0], W * 0.11, shelfY + 2 * u, 150 * u);
    drawProp(ctx, extra[1], W * 0.89, shelfY + 2 * u, 150 * u);
    drawProp(ctx, { spring: "lantern", summer: "star", autumn: "moon", winter: "bell" }[key], W * 0.17, -4 * u, 140 * u);
    grain(ctx, 0, 0, W, H, 0.2);
  }
