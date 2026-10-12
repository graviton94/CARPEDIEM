  /* 정원 꾸밈 (자리 여섯): 깊은 겹 한지 조각 (그림자 깊이 DEPTH) + 먹선 하루.
     좌표: (x, y) = 그 물건이 땅에 닿는 점, s = 배율, 단위 = 정원 단위 (화면 폭 390). */
  var DEPTH = 2.3, NARROW = 1;
  function orient(p) { return area(p) < 0 ? p.slice().reverse() : p; }
  function multi(ctx, polys) { ctx.beginPath(); polys.forEach(function (p) { p.forEach(function (q, i) { i ? ctx.lineTo(q[0], q[1]) : ctx.moveTo(q[0], q[1]); }); ctx.closePath(); }); }
  // 한지 한 조각 (또는 한 번에 오린 여러 조각). o.rim = 찢긴 흰 결 폭, o.sh = 그림자 배율, o.fiber, o.light = 흰 결 밝기
  function paper(ctx, polys, col, seed, o) {
    o = o || {}; if (NIGHT && !o.glow) col = mixHex(col, "#2E3858", 0.42); if (typeof polys[0][0] === "number") polys = [polys]; if (!o.eo) polys = polys.map(orient);
    var rim = o.rim == null ? 1.1 : o.rim, jit = o.jit == null ? Math.min(0.55, rim * 0.5) : o.jit, sh = (o.sh == null ? 1 : o.sh) * DEPTH / 2.3 * 2.3;
    var torn = polys.map(function (p, i) { return offsetRing(p, rim, jit, seed + i * 13); });
    ctx.save(); ctx.shadowColor = "rgba(46,34,24," + Math.min(0.42, 0.18 * sh) + ")"; ctx.shadowBlur = (2 + 2.2 * sh) * SHK(); ctx.shadowOffsetY = (0.5 + 0.8 * sh) * SHK(); ctx.shadowOffsetX = 0.2 * sh * SHK();
    var rule = o.eo ? "evenodd" : "nonzero"; if (o.eo) torn = polys;
    ctx.fillStyle = mixHex(col, NIGHT ? "#5A6282" : "#FBF8F0", NIGHT ? 0.3 : (o.light == null ? 0.7 : o.light)); multi(ctx, torn); ctx.fill(rule); ctx.restore();
    if (rim >= 0.8) { var r = R(seed + 3); ctx.save(); ctx.strokeStyle = NIGHT ? "rgba(200,205,225,.22)" : "rgba(251,248,240,.8)"; ctx.lineWidth = 0.32; ctx.lineCap = "round"; torn.forEach(function (T) { for (var i = 0; i < T.length; i += 3) { if (r() > 0.4) continue; var p = T[i], q = T[(i + 2) % T.length], a = Math.atan2(q[1] - p[1], q[0] - p[0]) - Math.PI / 2 + (r() - 0.5), l = 0.6 + r() * 1.8; ctx.beginPath(); ctx.moveTo(p[0], p[1]); ctx.lineTo(p[0] + Math.cos(a) * l, p[1] + Math.sin(a) * l); ctx.stroke(); } }); ctx.restore(); }
    ctx.save(); ctx.fillStyle = col; multi(ctx, o.eo ? polys : polys.map(function (p, i) { return offsetRing(p, -0.15, Math.min(0.3, jit), seed + 1 + i * 13); })); ctx.fill(rule); multi(ctx, polys); ctx.clip(rule);
    if (o.tone) { var bb = bbox([].concat.apply([], polys)), g = ctx.createLinearGradient(0, bb[1], 0, bb[1] + bb[3]); g.addColorStop(0, "rgba(255,255,255," + o.tone + ")"); g.addColorStop(0.5, "rgba(255,255,255,0)"); g.addColorStop(1, "rgba(40,28,18," + o.tone * 0.8 + ")"); ctx.fillStyle = g; ctx.fillRect(bb[0], bb[1], bb[2], bb[3]); }
    texFill(ctx, TEX.fiber, 0.5 * (o.fiber == null ? 1 : o.fiber), "source-atop", 0.4); texFill(ctx, TEX.grain, 0.35, "multiply", 0.5); ctx.restore();
  }
  // 띠 (가지 · 줄기 · 끈): 굵기가 w0 → w1 로 변하는 종이 띠
  function rib(pts, w0, w1) { var n = pts.length, L = [], Rr = []; for (var i = 0; i < n; i++) { var a = pts[Math.max(0, i - 1)], b = pts[Math.min(n - 1, i + 1)], tx = b[0] - a[0], ty = b[1] - a[1], l = Math.hypot(tx, ty) || 1, nx = -ty / l, ny = tx / l, w = (w0 + (w1 - w0) * i / (n - 1)) / 2; L.push([pts[i][0] + nx * w, pts[i][1] + ny * w]); Rr.push([pts[i][0] - nx * w, pts[i][1] - ny * w]); } return L.concat(Rr.reverse()); }
  function thread(ctx, pts, w, col) { ctx.save(); ctx.strokeStyle = col || (NIGHT ? "rgba(220,214,200,.4)" : "rgba(80,66,52,.6)"); ctx.lineWidth = w || 0.5; ctx.lineCap = "round"; ctx.beginPath(); pts.forEach(function (p, i) { i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1]); }); ctx.stroke(); ctx.restore(); }
  function T(x, y, s) { return function (pts) { return pts.map(function (q) { return [x + q[0] * s, y + q[1] * s]; }); }; }
  function bz(x, y, s, a) { var o = []; for (var i = 0; i < a.length; i += 2) { o.push(x + a[i] * s); o.push(y + a[i + 1] * s); } return bez(o); }
  function B(x, y, s, cx, cy, rx, ry, seed, wob, rot) { return blob(x + cx * s, y + cy * s, rx * s, ry * s, seed, wob, rot); }
  function poly(x, y, s, arr) { return T(x, y, s)(arr); }
  function leafPts(len, wid) { var p = []; for (var i = 0; i <= 12; i++) { var t = i / 12; p.push([t * len, -Math.sin(t * Math.PI) * wid * (1 - 0.3 * t)]); } for (var j = 11; j > 0; j--) { var u = j / 12; p.push([u * len, Math.sin(u * Math.PI) * wid * (1 - 0.3 * u)]); } return p; }
  function place(pts, x, y, s, rot) { var c = Math.cos(rot), sn = Math.sin(rot); return pts.map(function (q) { return [x + (q[0] * c - q[1] * sn) * s, y + (q[0] * sn + q[1] * c) * s]; }); }
  function maple(cx, cy, r, rot) { var p = [], lob = [[-90, 1], [-30, 0.92], [30, 0.92], [90, 0.62], [150, 0.18], [210, 0.18], [270 - 180 + 180, 0.62]]; var ang = [-90, -38, 14, 62, 90, 118, 166, 218];
    var tips = [[-90, 1], [-18, 0.95], [-162, 0.95], [44, 0.7], [-224 + 360, 0.7]]; var pts = []; for (var i = 0; i < 72; i++) { var a = -Math.PI / 2 + i / 72 * Math.PI * 2, d = a * 180 / Math.PI, k = 0.32; tips.forEach(function (t) { var dd = Math.abs(((d - t[0]) % 360 + 540) % 360 - 180); k = Math.max(k, t[1] * Math.max(0, 1 - dd / 30)); }); if (Math.abs(((d - 90) % 360 + 540) % 360 - 180) < 6) k = Math.max(k, 0.5); pts.push([Math.cos(a) * r * k, Math.sin(a) * r * k]); } return place(pts, cx, cy, 1, rot); }
  function fanLeaf(cx, cy, r, rot) { var p = [[-0.6, 0], [-0.5, -r * 0.28]]; for (var i = 0; i <= 18; i++) { var a = -Math.PI / 2 - 1.1 + i / 18 * 2.2, rr = r * (1 - (i === 9 ? 0.14 : i === 8 || i === 10 ? 0.05 : 0)) * (1 - 0.06 * Math.abs(i - 9) / 9); p.push([Math.cos(a) * rr, Math.sin(a) * rr - r * 0.28]); } p.push([0.5, -r * 0.28]); p.push([0.6, 0]); p.push([0.5, r * 0.32]); p.push([-0.5, r * 0.32]); return place(p, cx, cy, 1, rot); }
  function heart(cx, cy, r, rot) { var p = []; for (var i = 0; i < 24; i++) { var t = i / 24 * Math.PI * 2, x = 16 * Math.pow(Math.sin(t), 3), y = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)); p.push([x / 16, y / 16]); } return place(p, cx, cy, r, rot); }
  function star4(cx, cy, r) { var p = []; for (var i = 0; i < 8; i++) { var a = i / 8 * Math.PI * 2, rr = i % 2 ? r * 0.36 : r; p.push([cx + Math.cos(a) * rr, cy + Math.sin(a) * rr]); } return p; }

  /* ───────── 색 ───────── */
  var WPAL = {
    spring: { sky: "#F3DCE1", sky2: "#F1EEE6", far: "#D6C6DC", mid: "#CBDBA9", path: "#BDD196", front: "#A9C383", front2: "#97B474", sun: "#F6C979", cloud: "#FCF8F4", ink: "#3E3328" },
    summer: { sky: "#CFE5E2", sky2: "#F0F1E4", far: "#AFC8C0", mid: "#A6C887", path: "#B4CF8A", front: "#8DB46A", front2: "#7CA65C", sun: "#F7D27A", cloud: "#FFFFFF", ink: "#2F3A2E" },
    autumn: { sky: "#F4DFC2", sky2: "#F4EDDF", far: "#D9BFA6", mid: "#E2C28E", path: "#DDBA80", front: "#C99E66", front2: "#B98D58", sun: "#F3C46A", cloud: "#FCF6EE", ink: "#3E3328" },
    winter: { sky: "#E1E8EE", sky2: "#F3F3F1", far: "#C2CEDA", mid: "#EEF2F4", path: "#E6ECF0", front: "#F4F6F7", front2: "#E8EDF1", sun: "#F6E2B4", cloud: "#FFFFFF", ink: "#3A3E44" },
    night: { sky: "#26304A", sky2: "#48506C", far: "#3A4462", mid: "#4A5672", path: "#566180", front: "#41506A", front2: "#37455E", sun: "#F1E4BE", cloud: "#4E5878", ink: "#F1ECE0" }
  };
  var CAN = { // 나무 종류 × 계절 잎 색 (뒤 · 가운데 · 앞)
    cherry: { spring: ["#E79AAE", "#F1B5C4", "#F8D3DC"], summer: ["#5F8A4E", "#76A05C", "#8DB46A"], autumn: ["#B84A34", "#D46A45", "#E58B5A"] },
    zelkova: { spring: ["#97B866", "#B0CC7A", "#C8DD95"], summer: ["#4E7A48", "#628F54", "#7AA562"], autumn: ["#A4542E", "#C47038", "#D9924C"] },
    ginkgo: { spring: ["#A9C46A", "#BFD47E", "#D3E29A"], summer: ["#5E8A46", "#739E52", "#8CB264"], autumn: ["#E2A82A", "#EFC23E", "#F7D865"] },
    pine: { spring: ["#4A6E4E", "#5C8259", "#719766"], summer: ["#3F6446", "#527653", "#668A60"], autumn: ["#466648", "#5A7A55", "#6F8C5E"], winter: ["#43614A", "#557455", "#698863"] }
  };
  var BARK = { cherry: "#6E4A3E", zelkova: "#8A6E54", ginkgo: "#7D6C58", pine: "#99604A" };

  /* ───────── ① 나무 ───────── */
  function branchesOf(seed, x0, y0, len, ang, depth, wid, out) { var r = R(seed); var x1 = x0 + Math.cos(ang) * len, y1 = y0 + Math.sin(ang) * len, mx = (x0 + x1) / 2 + (r() - 0.5) * len * 0.25, my = (y0 + y1) / 2 + (r() - 0.5) * len * 0.15;
    out.push({ pts: bez([x0, y0, mx, my, mx, my, x1, y1], 10), w0: wid, w1: wid * 0.55, end: [x1, y1], ang: ang, depth: depth });
    if (depth > 0) { var n = depth > 2 ? 2 : 2 + (r() < 0.4 ? 1 : 0); for (var i = 0; i < n; i++) { var na = ang + (i - (n - 1) / 2) * (0.55 + r() * 0.25) * NARROW + (r() - 0.5) * 0.2; branchesOf(seed * 7 + i * 31 + 5, x1, y1, len * (0.66 + r() * 0.12), na, depth - 1, wid * 0.58, out); } } return out; }
  function treeShape(sp, stage) { // 줄기 높이 · 퍼짐
    var k = [0.32, 0.6, 1, 1.18][stage];
    return { H: { cherry: 84, zelkova: 86, ginkgo: 92, pine: 112 }[sp] * k, wB: { cherry: 11, zelkova: 13, ginkgo: 10, pine: 11 }[sp] * Math.max(0.45, k), k: k };
  }
  function trunk(ctx, x, y, sp, H, wB, seed) {
    var b = sp === "pine" ? [9, -9, 5] : [2, -2, 1], top = [x + b[2] * H / 100, y - H];
    var spine = bez([x, y, x + b[0] * H / 100, y - H * 0.35, x + b[1] * H / 100, y - H * 0.7, top[0], top[1]], 18);
    var flare = [[x - wB * 0.95, y + 1.5], [x - wB * 0.5, y - 4], [x + wB * 0.5, y - 4], [x + wB * 1.05, y + 1.5]];
    paper(ctx, [rib(spine, wB, wB * 0.45), flare], BARK[sp], seed, { tone: 0.12 });
    if (sp === "cherry") { var r = R(seed + 2); for (var i = 0; i < 5; i++) { var t = 0.15 + i * 0.15, p = spine[Math.floor(t * (spine.length - 1))], w = wB * (1 - t * 0.55) * 0.55; paper(ctx, [[p[0] - w, p[1] - 0.5], [p[0] + w, p[1] - 0.8], [p[0] + w, p[1] + 0.5], [p[0] - w, p[1] + 0.7]], "#8E6656", seed + 9 + i, { rim: 0.2, sh: 0.2 }); } }
    if (sp === "pine") { var r2 = R(seed + 4); for (var j = 0; j < 3; j++) { var t2 = 0.15 + j * 0.2, q = spine[Math.floor(t2 * (spine.length - 1))], w2 = wB * (1 - t2 * 0.55) * 0.4; paper(ctx, blob(q[0] + (r2() - 0.5) * w2, q[1], w2, 1.6, seed + 20 + j, 0.2), "#B37A5E", seed + 20 + j, { rim: 0.25, sh: 0.2 }); } }
    return { spine: spine, top: top };
  }
  function crown(ctx, list, cols, seed, lk, cx, cy) { // 한 장 실루엣 (뒤) + 가운데 결 + 밝은 조각 몇
    var back = list.map(function (c, i) { return blob(cx + c[0] * lk, cy + c[1] * lk, c[2] * lk, c[3] * lk, seed + i * 7, 0.18); });
    paper(ctx, back, cols[0], seed, { sh: 1.2, tone: 0.1 });
    var bb = bbox([].concat.apply([], back)), r = R(seed + 5);
    var mid = list.filter(function (c) { return c[1] <= 2; }).map(function (c, i) { var dx = c[0] * 0.78 - 6 - Math.max(0, c[0]) * 0.18, dy = c[1] * 0.8 - 7 + Math.max(0, c[0]) * 0.06; return blob(cx + dx * lk, cy + dy * lk, c[2] * (0.78 - Math.max(0, c[0]) * 0.004) * lk, c[3] * 0.7 * lk, seed + 100 + i * 7, 0.24); });
    paper(ctx, mid, cols[1], seed + 100, { sh: 0.8, rim: 0.7 });
    var top = list.slice().sort(function (a, b) { return (a[1] + a[0] * 0.3) - (b[1] + b[0] * 0.3); }).slice(0, 2);
    var lite = top.map(function (c, i) { return blob(cx + (c[0] * 0.8 - 8 - i * 6) * lk, cy + (c[1] * 0.8 - 9 + i * 5) * lk, c[2] * (0.38 - i * 0.1) * lk, c[3] * (0.3 - i * 0.06) * lk, seed + 200 + i * 7, 0.26, -0.15); });
    if (lite.length) paper(ctx, lite, cols[2], seed + 200, { sh: 0.5, rim: 0.5 }); }
  function tree(ctx, x, y, s, sp, se, stage, o) {
    o = o || {}; var seed = { cherry: 1100, zelkova: 1200, ginkgo: 1300, pine: 1400 }[sp];
    if (stage === 0) return sprout(ctx, x, y, s, sp, se);
    var g = treeShape(sp, stage), H = g.H * s, wB = g.wB * s, bare = se === "winter" && sp !== "pine", cols = (CAN[sp][se] || CAN[sp].summer);
    // 어린 나무: 지지대 + 끈, 겨울엔 짚 옷
    if (stage === 1) { paper(ctx, rib([[x - 9 * s, y + 1], [x - 7 * s, y - H * 0.78]], 2.4 * s, 2 * s), "#C9A77A", seed + 90, { rim: 0.5, sh: 0.6 }); }
    var tk = trunk(ctx, x, y, sp, H, wB, seed), top = tk.top;
    if (stage === 1) { var ty = y - H * 0.55; paper(ctx, [[x - 9 * s, ty - 1.4 * s], [x - wB * 0.4, ty - 1.2 * s], [x - wB * 0.4, ty + 1 * s], [x - 9 * s, ty + 0.8 * s]], "#D9C092", seed + 91, { rim: 0.3, sh: 0.4 });
      if (se === "winter") { for (var w = 0; w < 4; w++) { var yy = y - H * (0.08 + w * 0.1); paper(ctx, rib([[x - wB * 0.8, yy + 3 * s], [x + wB * 0.8, yy - 2 * s]], 4.2 * s, 4 * s), w % 2 ? "#D8BC7E" : "#CBA86A", seed + 92 + w, { rim: 0.4, sh: 0.5 }); } } }
    var spread = (stage === 3 ? 1.2 : 1) * s, kb = stage === 1 ? 0.55 : 1;
    // 가지 (맨가지 겨울 + 잎 아래로 비치는 큰 가지)
    var arms = { cherry: [-2.35, -1.95, -1.15, -0.75], zelkova: [-2.2, -1.85, -1.3, -0.95], ginkgo: [-1.95, -1.7, -1.4, -1.2], pine: [-2.7, -0.35, -2.4, -0.6] }[sp];
    var lens = { cherry: 46, zelkova: 52, ginkgo: 44, pine: 40 }[sp] * kb * spread;
    if (sp !== "pine") { var segs = []; NARROW = sp === "ginkgo" ? 0.85 : 1; arms.forEach(function (a, i) { branchesOf(seed + i * 101, top[0], top[1] + (i % 2) * 8 * s, lens * (bare ? 0.72 : 0.5) * (i === 1 || i === 2 ? 1 : 0.85), a, bare ? (stage === 3 ? 3 : 2) : 1, wB * 0.5, segs); });
      NARROW = 1; var ps = segs.map(function (b) { return rib(b.pts, Math.max(0.9 * s, b.w0), Math.max(0.6 * s, b.w1)); }); paper(ctx, ps, BARK[sp], seed + 50, { rim: 0.45, sh: 0.7, fiber: 0.5 });
      if (bare) { segs.filter(function (b) { return b.depth >= 2; }).forEach(function (b, i) { var m = b.pts[Math.floor(b.pts.length * 0.55)], e = b.end, ang = Math.atan2(e[1] - m[1], e[0] - m[0]); if (Math.abs(Math.sin(ang)) > 0.62) return; paper(ctx, blob((m[0] + e[0]) / 2, (m[1] + e[1]) / 2 - b.w0 * 0.6, Math.hypot(e[0] - m[0], e[1] - m[1]) * 0.4, 1.2 * s, seed + 300 + i, 0.25, ang), "#FFFFFF", seed + 300 + i, { rim: 0.5, sh: 0.5 }); });
        if (sp === "zelkova" && stage >= 2) magpie(ctx, top[0] + 26 * s, top[1] - 30 * s, s);
        if (sp === "cherry") segs.filter(function (b) { return b.depth === 0; }).forEach(function (b, i) { if (i % 2) return; paper(ctx, blob(b.end[0], b.end[1], 1.4 * s, 1.8 * s, seed + 360 + i, 0.05), "#C27C82", seed + 360 + i, { rim: 0.3, sh: 0.3 }); }); }
    }
    if (!bare) {
      var cx = top[0], cy = top[1] - 16 * s * (stage === 1 ? 0.5 : 1), L;
      if (sp === "cherry") L = [[-30, 4, 28, 20], [30, 4, 28, 20], [0, -10, 38, 26], [-14, -24, 24, 16], [16, -22, 24, 16], [-46, 14, 15, 11], [46, 14, 15, 11], [0, 10, 30, 14]];
      else if (sp === "zelkova") L = [[-38, 0, 30, 18], [38, -2, 30, 18], [0, -14, 44, 24], [-20, -28, 26, 14], [22, -27, 26, 14], [-58, 10, 16, 10], [58, 8, 16, 10], [0, 6, 36, 12]];
      else if (sp === "ginkgo") L = [[0, -62, 13, 18], [-1, -42, 21, 20], [0, -18, 27, 22], [0, 6, 30, 18], [-14, -30, 16, 16], [14, -34, 16, 16], [-16, 8, 18, 13], [17, 6, 18, 13]];
      else L = null;
      if (L) { var lk = (stage === 1 ? 0.5 : 1) * spread; if (sp === "ginkgo") ginkgoCrown(ctx, cx, cy, lk, cols, seed + 400); else crown(ctx, L, cols, seed + 400, lk, cx, cy); }
      else { if (stage === 3 && o.swing !== false) swing(ctx, top[0], top[1], s, sp, seed, y, false); pineTiers(ctx, tk, s * (stage === 1 ? 0.55 : stage === 3 ? 1.15 : 1), se, seed, H); }   // 소나무는 그네 가지가 잎 뒤로
      // 계절 덧칠
      var r = R(seed + 7), lkk = (stage === 1 ? 0.5 : 1) * spread;
      if (sp === "cherry" && se === "spring") { for (var i = 0; i < 7; i++) { var px = cx + (r() - 0.5) * 80 * lkk, py = cy - 30 * lkk + r() * 40 * lkk; paper(ctx, blossom(px, py, 2.2 * s), i % 3 ? "#FFFFFF" : "#F6C3CF", seed + 500 + i, { rim: 0.2, sh: 0.3 }); } }
      if (sp === "cherry" && se === "summer") { [[-20, 4], [12, 8], [30, -6], [-34, -4]].forEach(function (q, i) { var px = cx + q[0] * lkk, py = cy + q[1] * lkk; thread(ctx, [[px, py - 4 * s], [px - 1.6 * s, py], [px - 3 * s, py + 1.5 * s]], 0.5, "#4E6A3A"); thread(ctx, [[px, py - 4 * s], [px + 2 * s, py + 1.5 * s]], 0.5, "#4E6A3A"); paper(ctx, [blob(px - 3 * s, py + 2.5 * s, 2.2 * s, 2.2 * s, 520 + i, 0.04), blob(px + 2.4 * s, py + 2.6 * s, 2.2 * s, 2.2 * s, 530 + i, 0.04)], "#B8303A", seed + 520 + i, { rim: 0.35, sh: 0.5 }); }); }
      if (sp === "pine" && se === "spring") { /* 송홧가루 순은 pineTiers 에서 */ }
    }
    // 발치: 떨어진 것
    var fr = R(seed + 11);
    if (se === "autumn" && sp !== "pine") { for (var f = 0; f < (sp === "ginkgo" ? 9 : 5); f++) { var fx = x + (fr() - 0.5) * 70 * s, fy = y + 1 + fr() * 2; var lp = sp === "ginkgo" ? fanLeaf(fx, fy, 4 * s, fr() * 6) : sp === "zelkova" ? place(leafPts(7 * s, 2.6 * s), fx, fy, 1, fr() * 6) : place(leafPts(6 * s, 3 * s), fx, fy, 1, fr() * 6); paper(ctx, lp, cols[f % 3], seed + 600 + f, { rim: 0.3, sh: 0.4 }); } }
    if (se === "spring" && sp === "cherry") { for (var f2 = 0; f2 < 3; f2++) paper(ctx, blossom(x + (fr() - 0.5) * 60 * s, y + 1.5 + fr() * 2, 1.8 * s), "#F8D3DC", seed + 620 + f2, { rim: 0.2, sh: 0.2 }); }
    if (se === "winter") paper(ctx, blob(x, y + 0.5, 24 * s * g.k + 8 * s, 3.2 * s, seed + 640, 0.25), "#FFFFFF", seed + 640, { rim: 0.6, sh: 0.5 });
    // 큰 나무: 오른쪽 가지에 그네
    if (stage === 3 && o.swing !== false && sp !== "pine") swing(ctx, top[0], top[1], s, sp, seed, y, bare);
    return top;
  }
  function blossom(cx, cy, r) { var p = []; for (var i = 0; i < 20; i++) { var a = i / 20 * Math.PI * 2, k = 0.7 + 0.3 * Math.abs(Math.cos(a * 2.5)); p.push([cx + Math.cos(a) * r * k, cy + Math.sin(a) * r * k]); } return p; }
  function pineTiers(ctx, tk, s, se, seed, H) {
    var sp = tk.spine, cols = CAN.pine[se], top = tk.top;
    var tiers = [[-34, 0.52, 30, 9], [30, 0.66, 28, 8.5], [-22, 0.82, 24, 8], [14, 0.95, 22, 7.5], [0, 1.06, 16, 6.5]];
    tiers.forEach(function (t, i) { var p = sp[Math.min(sp.length - 1, Math.floor(t[1] * (sp.length - 1)))] || top, by = sp[0][1] - H * t[1], bx = p[0] + t[0] * s, rx = t[2] * s, ry = t[3] * s;
      paper(ctx, rib([[p[0], by + 4 * s], [(p[0] + bx) / 2, by + 1 * s], [bx, by]], 3.2 * s, 1.6 * s), BARK.pine, seed + 700 + i, { rim: 0.4, sh: 0.6 });
      var bumps = [-0.62, -0.2, 0.22, 0.62].map(function (u, k) { return blob(bx + u * rx, by - ry * 0.25 - (1 - Math.abs(u)) * ry * 0.35, rx * 0.46, ry * (0.62 + (1 - Math.abs(u)) * 0.3), seed + 710 + i * 5 + k, 0.14); });
      paper(ctx, bumps, cols[0], seed + 710 + i, { sh: 1.1, tone: 0.08 });
      var tops = [-0.3, 0.2].map(function (u, k) { return blob(bx + u * rx, by - ry * 0.85, rx * 0.4, ry * 0.36, seed + 730 + i * 3 + k, 0.18); });
      paper(ctx, tops, cols[1], seed + 730 + i, { sh: 0.6, rim: 0.5 });
      if (se === "winter") paper(ctx, [-0.35, 0.25].map(function (u, k) { return blob(bx + u * rx, by - ry * 1.1, rx * 0.42, ry * 0.26, seed + 740 + i * 3 + k, 0.22); }), "#FFFFFF", seed + 740 + i, { sh: 0.5, rim: 0.6 });
      if (se === "spring" && i >= 3) for (var k = 1; k < 2; k++) paper(ctx, rib([[bx - rx * 0.4 + k * rx * 0.4, by - ry * 1.05], [bx - rx * 0.4 + k * rx * 0.4 + 0.5, by - ry * 1.05 - 4.5 * s]], 1.7 * s, 1.2 * s), "#E6CF66", seed + 750 + i * 3 + k, { rim: 0.25, sh: 0.3 });
      if (false) paper(ctx, blob(bx - rx * 0.3, by - ry * 0.4, rx * 0.2, ry * 0.25, seed + 760 + i, 0.1), "#9A7A48", seed + 760 + i, { rim: 0.3, sh: 0.3 });
    });
  }
  function swing(ctx, tx, ty, s, sp, seed, gy, bare) {
    var by = gy - 74 * s, bx0 = tx + 2 * s, bx1 = tx + 62 * s;
    // 줄기에서 자연스럽게 뻗어 살짝 올라가는 가지 (판자처럼 곧지 않게), 끝으로 가늘게
    paper(ctx, rib(bz(0, 0, 1, [bx0 - 2 * s, by + 10 * s, bx0 + 18 * s, by + 4 * s, bx0 + 40 * s, by - 3 * s, bx1, by - 10 * s]), 4.4 * s, 1.6 * s), BARK[sp], seed + 800, { rim: 0.5, sh: 0.8, tone: 0.1 });
    if (bare) paper(ctx, blob(bx0 + 34 * s, by - 3.4 * s, 20 * s, 1.7 * s, seed + 805, 0.2, -0.08), "#FFFFFF", seed + 805, { rim: 0.4, sh: 0.4 });
    var sy = gy - 20 * s; [bx0 + 34 * s, bx0 + 52 * s].forEach(function (rx, i) { var top = by - 3.2 * s - i * 3.4 * s; thread(ctx, [[rx, top], [rx + 0.4, sy]], 0.9 * s, NIGHT ? "rgba(220,210,190,.55)" : "#8A7356"); paper(ctx, blob(rx, top + 1, 1.8 * s, 1.4 * s, seed + 806 + i, 0.05), "#C9A77A", seed + 806 + i, { rim: 0.2, sh: 0.3 }); });
    paper(ctx, [[bx0 + 30 * s, sy - 2 * s], [bx0 + 56 * s, sy - 2 * s], [bx0 + 57 * s, sy + 2.6 * s], [bx0 + 29 * s, sy + 2.6 * s]], "#B88A58", seed + 808, { rim: 0.6, sh: 1, tone: 0.12 });
    if (bare) paper(ctx, blob(bx0 + 43 * s, sy - 2.6 * s, 12 * s, 1.5 * s, seed + 809, 0.2), "#FFFFFF", seed + 809, { rim: 0.4, sh: 0.3 });
  }
  function magpie(ctx, x, y, s) { // 까치둥지 (겨울 느티)
    var r = R(77); var sticks = []; for (var i = 0; i < 4; i++) { var a = (r() - 0.5) * 0.9, l = 9 + r() * 6, cx = x + (r() - 0.5) * 6 * s, cy = y + (r() - 0.5) * 6 * s; sticks.push(rib([[cx - Math.cos(a) * l * s / 2, cy - Math.sin(a) * l * s / 2], [cx + Math.cos(a) * l * s / 2, cy + Math.sin(a) * l * s / 2]], 1.1 * s, 0.9 * s)); }
    paper(ctx, blob(x, y, 7 * s, 5 * s, 78, 0.2), "#8A7058", 78, { rim: 0.5, sh: 0.8 }); paper(ctx, sticks, "#7E6650", 79, { rim: 0.25, sh: 0.4 }); paper(ctx, blob(x - 1 * s, y - 6.5 * s, 8 * s, 1.8 * s, 80, 0.2), "#FFFFFF", 80, { rim: 0.4, sh: 0.3 });
  }
  function sprout(ctx, x, y, s, sp, se) {
    var leaf = { cherry: "#93B866", zelkova: "#9DBE6C", ginkgo: "#A9C46A", pine: "#5C8259" }[sp];
    paper(ctx, blob(x, y + 0.5, 13 * s, 4 * s, 1500, 0.2), se === "winter" ? "#FFFFFF" : "#9B7A58", 1500, { sh: 0.6 });
    paper(ctx, rib(bz(0, 0, 1, [x, y - 1, x + 1 * s, y - 8 * s, x - 1 * s, y - 14 * s, x + 0.5 * s, y - 20 * s]), 1.8 * s, 1.2 * s), "#7FA35A", 1501, { rim: 0.35, sh: 0.5 });
    if (sp === "pine") { for (var i = 0; i < 7; i++) paper(ctx, rib([[x + 0.5 * s, y - 19 * s], [x + Math.cos(-Math.PI / 2 + (i - 3) * 0.35) * 9 * s, y - 19 * s + Math.sin(-Math.PI / 2 + (i - 3) * 0.35) * 9 * s]], 1.2 * s, 0.6 * s), leaf, 1510 + i, { rim: 0.25, sh: 0.4 }); }
    else if (sp === "ginkgo") { paper(ctx, fanLeaf(x - 1 * s, y - 19 * s, 9 * s, -0.7), leaf, 1520, { rim: 0.4, sh: 0.6 }); paper(ctx, fanLeaf(x + 1.5 * s, y - 20 * s, 8 * s, 0.6), mixHex(leaf, "#FFFFFF", 0.15), 1521, { rim: 0.4, sh: 0.6 }); }
    else { paper(ctx, place(leafPts(11 * s, 4.2 * s), x, y - 19 * s, 1, -2.7), leaf, 1530, { rim: 0.4, sh: 0.6 }); paper(ctx, place(leafPts(12 * s, 4.6 * s), x + 0.5 * s, y - 20 * s, 1, -0.45), mixHex(leaf, "#FFFFFF", 0.15), 1531, { rim: 0.4, sh: 0.6 }); }
    if (se === "winter") paper(ctx, blob(x, y - 1 * s, 10 * s, 3 * s, 1540, 0.2), "#FFFFFF", 1540, { sh: 0.5 });
  }

  /* ───────── ② 말뚝 ───────── */
  // o: season, chime 0/1/2 (풍경 · 작은 종), lantern, lit, letter
  function post(ctx, x, y, s, o) {
    o = o || {}; var H = 100 * s, se = o.season, wood = "#A07A54", bar = y - H + 4 * s;
    if (se === "summer") ivy(ctx, x, y, s, H, true);
    paper(ctx, [[x - 3.2 * s, y + 1], [x + 3.2 * s, y + 1], [x + 2.7 * s, y - H], [x, y - H - 2.4 * s], [x - 2.7 * s, y - H]], wood, 1600, { tone: 0.14 });
    paper(ctx, rib([[x + 9 * s, bar], [x - 34 * s, bar - 1 * s]], 4 * s, 3.4 * s), wood, 1601, { tone: 0.12 });
    for (var g = 0; g < 4; g++) paper(ctx, [[x - 2 * s, y - H * (0.2 + g * 0.18)], [x + 1.5 * s, y - H * (0.2 + g * 0.18) - 0.6 * s], [x + 1.4 * s, y - H * (0.2 + g * 0.18) + 0.3 * s]], mixHex(wood, "#000000", 0.18), 1602 + g, { rim: 0, sh: 0 });
    if (se === "summer") ivy(ctx, x, y, s, H, false);
    if (se === "autumn") { var br = bz(x, y, s, [9, 1, 10, -26, 7, -50, 3, -76]); paper(ctx, rib(br, 2.4 * s, 1 * s), "#7A5A40", 1610, { rim: 0.4, sh: 0.7 });
      [[18, -50, 0.3], [11, -76, -0.4], [5, -58, 0.9]].forEach(function (q, i) { var cx = x + q[0] * s, cy = y + q[1] * s; thread(ctx, [[cx - 3 * s, cy + 3 * s], [x + (q[0] - 7) * s, cy + 6 * s]], 0.5, "#7A5A40"); paper(ctx, maple(cx, cy, 5.4 * s, q[2]), ["#C0473A", "#D96A3E", "#E3913E", "#B8452F", "#D2783C", "#C95A34"][i], 1620 + i, { rim: 0.4, sh: 0.7 }); }); }
    var hx = x - 25 * s;
    if (o.lantern) lantern(ctx, x - 12 * s, bar + 1.5 * s, s, o.lit);
    if (o.chime >= 1) chime(ctx, hx, bar + 1 * s, s, 1, o.sway || 0);
    if (o.chime >= 2 && !o.lantern) chime(ctx, x + 6 * s, bar + 1.5 * s, s, 0.62, -(o.sway || 0));
    if (o.letter) { var lx = x - 6 * s, ly = bar - 2 * s; var env = place([[-6, -4], [6, -4], [6, 4], [-6, 4]], lx + 2 * s, ly - 1 * s, s, -0.18); paper(ctx, env, "#F6EEDC", 1650, { rim: 0.4, sh: 0.9 }); paper(ctx, place([[-6, -4], [6, -4], [0, 0.6]], lx + 2 * s, ly - 1 * s, s, -0.18), "#EADFC6", 1651, { rim: 0.1, sh: 0.3 }); paper(ctx, blob(lx + 2.3 * s, ly - 1.4 * s, 1.5 * s, 1.5 * s, 1652, 0.05), "#C8553D", 1652, { rim: 0.15, sh: 0.3 }); }
    if (se === "spring") bird(ctx, x - 14 * s, bar - 2.2 * s, s, o.birdLook || 0);
    if (se === "winter") { paper(ctx, blob(x - 12 * s, bar - 3 * s, 23 * s, 2.6 * s, 1660, 0.25, 0.02), "#FFFFFF", 1660, { sh: 0.6 }); paper(ctx, blob(x, y - H - 2.6 * s, 4.6 * s, 2.6 * s, 1661, 0.2), "#FFFFFF", 1661, { sh: 0.6 }); paper(ctx, blob(x, y + 0.5, 14 * s, 3.4 * s, 1662, 0.3), "#FFFFFF", 1662, { sh: 0.5 }); }
  }
  function ivy(ctx, x, y, s, H, back) { // 담쟁이 나선: 뒤로 가는 부분은 기둥 앞에 그리기 전에
    var pts = []; for (var t = 0; t <= 1.0001; t += 0.01) { var a = t * Math.PI * 2 * 3.2, yy = y - t * H * 0.9, xx = x + Math.sin(a) * 4.4 * s; pts.push([xx, yy, Math.cos(a) > 0]); }
    var runs = [], cur = []; pts.forEach(function (p) { if (p[2] === !back) cur.push([p[0], p[1]]); else if (cur.length) { runs.push(cur); cur = []; } }); if (cur.length) runs.push(cur);
    paper(ctx, runs.filter(function (r) { return r.length > 2; }).map(function (r) { return rib(r, 1.2 * s, 1.2 * s); }), back ? "#5E7A44" : "#6F8E4E", back ? 1670 : 1671, { rim: 0.25, sh: back ? 0.2 : 0.5 });
    if (!back) for (var i = 0; i < 9; i++) { var p = pts[Math.floor((i + 0.5) / 9 * (pts.length - 1))]; if (!p[2]) continue; paper(ctx, heart(p[0] + (i % 2 ? 3 : -3) * s, p[1], 3.6 * s, (i % 2 ? 0.6 : -0.6) + Math.PI), i % 3 ? "#7FA35A" : "#94B66A", 1680 + i, { rim: 0.35, sh: 0.6 }); }
  }
  function chime(ctx, x, y, s, k, sway) { // 풍경: 종 + 물고기 판
    ctx.save(); ctx.translate(x, y); ctx.rotate(sway || 0); var z = s * k;
    thread(ctx, [[0, 0], [0, 6 * z]], 0.6);
    paper(ctx, [[-4.6 * z, 15 * z], [-3.8 * z, 9 * z], [-2 * z, 6.2 * z], [2 * z, 6.2 * z], [3.8 * z, 9 * z], [4.6 * z, 15 * z]], "#7FA39A", 1690, { rim: 0.4, sh: 0.8, tone: 0.18 });
    paper(ctx, [[-5.2 * z, 14.6 * z], [5.2 * z, 14.6 * z], [5.2 * z, 16 * z], [-5.2 * z, 16 * z]], "#6A8E86", 1691, { rim: 0.2, sh: 0.3 });
    thread(ctx, [[0, 16 * z], [0, 22 * z]], 0.5);
    var fish = [[0, 22], [3, 24], [3.4, 29], [1.6, 33], [3.6, 36], [0, 35], [-3.6, 36], [-1.6, 33], [-3.4, 29], [-3, 24]].map(function (q) { return [q[0] * z, q[1] * z]; });
    paper(ctx, fish, "#D8B66A", 1692, { rim: 0.35, sh: 0.7 }); ctx.fillStyle = "#3A3028"; ctx.beginPath(); ctx.arc(-0.9 * z, 25.6 * z, 0.55 * z, 0, 7); ctx.fill();
    ctx.restore();
  }
  function lantern(ctx, x, y, s, lit) { // 한지 등: 위아래 붉은 갓
    thread(ctx, [[x, y], [x, y + 6 * s]], 0.6);
    if (lit) { ctx.save(); var g = ctx.createRadialGradient(x, y + 16 * s, 0, x, y + 16 * s, 30 * s); g.addColorStop(0, "rgba(255,206,130,.55)"); g.addColorStop(1, "rgba(255,206,130,0)"); ctx.fillStyle = g; ctx.fillRect(x - 32 * s, y - 16 * s, 64 * s, 64 * s); ctx.restore(); }
    paper(ctx, blob(x, y + 16 * s, 6.2 * s, 8.6 * s, 1700, 0.04), lit ? "#F8CF86" : "#F2E6CC", 1700, { rim: 0.5, sh: lit ? 0.3 : 0.8, fiber: 1.2, glow: lit });
    for (var i = -1; i <= 1; i++) thread(ctx, bez([x + i * 3 * s, y + 8.6 * s, x + i * 4.6 * s, y + 13 * s, x + i * 4.6 * s, y + 19 * s, x + i * 3 * s, y + 23.6 * s], 10), 0.4, lit ? "rgba(170,110,50,.5)" : "rgba(120,100,80,.35)");
    paper(ctx, [[-3.6, 6.4], [3.6, 6.4], [4.4, 9], [-4.4, 9]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), "#B8453A", 1701, { rim: 0.3, sh: 0.5 });
    paper(ctx, [[-4.4, 23], [4.4, 23], [3.6, 25.6], [-3.6, 25.6]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), "#B8453A", 1702, { rim: 0.3, sh: 0.5 });
    thread(ctx, [[x, y + 25.6 * s], [x, y + 30 * s]], 0.7, "#B8453A");
  }
  function bird(ctx, x, y, s, look) { // 박새: 회청 등, 흰 볼, 검은 머리, 노란 가슴
    var body = blob(x, y - 4.2 * s, 6.2 * s, 4.2 * s, 1710, 0.05, -0.12);
    paper(ctx, [[x - 4 * s, y - 4 * s], [x - 12 * s, y - 2 * s], [x - 11.4 * s, y - 0.4 * s], [x - 3 * s, y - 2 * s]], "#5E6B70", 1711, { rim: 0.3, sh: 0.6 });
    paper(ctx, body, "#8A9BA2", 1712, { rim: 0.45, sh: 0.9 });
    paper(ctx, blob(x + 1.6 * s, y - 2.6 * s, 3.6 * s, 2.4 * s, 1713, 0.05), "#EED98A", 1713, { rim: 0.2, sh: 0.2 });
    paper(ctx, place(leafPts(8 * s, 2.2 * s), x - 4.6 * s, y - 5.4 * s, 1, 0.12), "#6D7C82", 1714, { rim: 0.25, sh: 0.4 });
    var hx = x + 5.4 * s, hy = y - 8.6 * s; paper(ctx, blob(hx, hy, 3.6 * s, 3.4 * s, 1715, 0.04), "#2E2C2C", 1715, { rim: 0.35, sh: 0.6 });
    paper(ctx, blob(hx + 0.8 * s, hy + 0.6 * s, 1.8 * s, 1.4 * s, 1716, 0.05), "#FBF8F0", 1716, { rim: 0, sh: 0 });
    paper(ctx, [[hx + 3 * s, hy - 0.5 * s], [hx + 6 * s, hy + 0.3 * s + look * s], [hx + 3 * s, hy + 1 * s]], "#3A3430", 1717, { rim: 0, sh: 0.2 });
    ctx.fillStyle = "#FFFFFF"; ctx.beginPath(); ctx.arc(hx + 1.3 * s, hy - 1.1 * s, 0.75 * s, 0, 7); ctx.fill(); ctx.fillStyle = "#141210"; ctx.beginPath(); ctx.arc(hx + 1.5 * s, hy - 1.1 * s, 0.5 * s, 0, 7); ctx.fill();
    thread(ctx, [[x + 0.5 * s, y], [x + 0.2 * s, y + 2 * s]], 0.6, "#5A4A3A"); thread(ctx, [[x + 2.6 * s, y], [x + 2.8 * s, y + 2 * s]], 0.6, "#5A4A3A");
  }

  /* ───────── ③ 연 ───────── */
  var FEEL = ["#F2C04E", "#8DB46A", "#7FA3C8", "#E59A8C", "#B7A0CF", "#E9A43A", "#9DB4B5", "#D98C7A"];
  function kite(ctx, x, y, s, kind, n, ax, ay) { // kind: gaori (가오리연) | bangpae (방패연)
    if (ax != null) thread(ctx, bez([ax, ay, ax - 8, ay - 40, x + 10 * s, y + 70 * s, x, y + (kind === "gaori" ? 20 : 14) * s], 30), 0.55);
    if (kind === "gaori") {
      var tail = bz(x, y, s, [0, 18, 6, 34, -6, 50, 2, 70, 8, 84, -2, 96, 4, 110]);
      paper(ctx, rib(tail, 2.4 * s, 1.4 * s), "#F4EEDF", 1800, { rim: 0.35, sh: 0.6 });
      for (var i = 0; i < n; i++) { var p = tail[Math.floor((i + 1) / (n + 1) * (tail.length - 1))]; paper(ctx, [[p[0], p[1]], [p[0] - 6 * s, p[1] - 3 * s], [p[0] - 6 * s, p[1] + 3 * s]], FEEL[i % FEEL.length], 1810 + i, { rim: 0.3, sh: 0.5 }); paper(ctx, [[p[0], p[1]], [p[0] + 6 * s, p[1] - 3 * s], [p[0] + 6 * s, p[1] + 3 * s]], mixHex(FEEL[i % FEEL.length], "#FFFFFF", 0.25), 1820 + i, { rim: 0.3, sh: 0.5 }); }
      paper(ctx, poly(x, y, s, [[0, -22], [17, -2], [0, 19], [-17, -2]]), "#F6F0E2", 1801, { tone: 0.06 });
      paper(ctx, poly(x, y, s, [[0, -22], [17, -2], [0, -2]]), "#D9776A", 1802, { rim: 0.3, sh: 0.3 }); paper(ctx, poly(x, y, s, [[0, -2], [-17, -2], [0, 19]]), "#D9776A", 1803, { rim: 0.3, sh: 0.3 });
      thread(ctx, [[x, y - 22 * s], [x, y + 19 * s]], 0.6, "rgba(90,70,50,.5)"); thread(ctx, [[x - 17 * s, y - 2 * s], [x + 17 * s, y - 2 * s]], 0.6, "rgba(90,70,50,.5)");
    } else {
      [[-12, 20], [12, 20]].forEach(function (q, j) { var tl = bz(x, y, s, [q[0], q[1], q[0] + (j ? 6 : -6), 36, q[0] + (j ? -4 : 4), 50, q[0] + (j ? 4 : -4), 64]); paper(ctx, rib(tl, 1.6 * s, 1 * s), "#F4EEDF", 1830 + j, { rim: 0.3, sh: 0.5 });
        for (var i = j; i < n; i += 2) { var p = tl[Math.floor((Math.floor(i / 2) + 1) / (Math.ceil(n / 2) + 1) * (tl.length - 1))]; paper(ctx, blob(p[0], p[1], 3.2 * s, 2 * s, 1840 + i, 0.1, 0.3), FEEL[i % FEEL.length], 1840 + i, { rim: 0.3, sh: 0.5 }); } });
      var body = poly(x, y, s, [[-14, -20], [14, -20], [14, 20], [-14, 20]]), hole = blob(x, y + 1 * s, 6 * s, 6 * s, 1851, 0.02).reverse();
      paper(ctx, [body, hole], "#F6F0E2", 1850, { tone: 0.06, eo: true });
      paper(ctx, [[x - 6 * s, y - 20 * s], [x + 6 * s, y - 20 * s]].concat(bez([x + 6 * s, y - 20 * s, x + 6 * s, y - 12 * s, x - 6 * s, y - 12 * s, x - 6 * s, y - 20 * s], 10)), "#D9776A", 1852, { rim: 0.3, sh: 0.3 });
      paper(ctx, poly(x, y, s, [[-14, 14], [14, 14], [14, 20], [-14, 20]]), "#7FA3C8", 1853, { rim: 0.2, sh: 0.2 });
      thread(ctx, [[x - 14 * s, y - 20 * s], [x + 14 * s, y + 20 * s]], 0.5, "rgba(90,70,50,.4)"); thread(ctx, [[x + 14 * s, y - 20 * s], [x - 14 * s, y + 20 * s]], 0.5, "rgba(90,70,50,.4)");
    }
  }

  /* ───────── ④ 하루의 자리 ───────── */
  function moss(ctx, x, y, s, se, buds) {
    var dark = se === "winter" ? "#D9E2E6" : se === "autumn" ? "#8E9A5A" : "#7FA35A", light = se === "winter" ? "#FFFFFF" : se === "autumn" ? "#A9AE6A" : "#9CBD6E";
    paper(ctx, blob(x, y + 0.5, 34 * s, 4.6 * s, 1900, 0.18), dark, 1900, { sh: 0.9 });
    var r = R(1901), tufts = []; for (var i = 0; i < 9; i++) { var tx = x + (-30 + i * 7.5) * s, ty = y - 1.4 * s; tufts.push(blob(tx, ty, (3.6 + r() * 1.6) * s, (2 + r()) * s, 1902 + i, 0.2)); }
    paper(ctx, tufts, light, 1902, { rim: 0.4, sh: 0.4 });
    if (se === "winter") paper(ctx, blob(x + 2 * s, y - 2.8 * s, 26 * s, 2.2 * s, 1915, 0.3), "#FFFFFF", 1915, { rim: 0.6, sh: 0.4 });
    var spots = [-26, 22, -17, 29, 14].slice(0, buds || 0);
    spots.forEach(function (dx, i) { var bx = x + dx * s, by = y - 2.6 * s;
      if (se === "spring") { thread(ctx, [[bx, by + 1], [bx, by - 4 * s]], 0.7, "#6F8E4E"); paper(ctx, blossom(bx, by - 5 * s, 2.6 * s), i % 2 ? "#F4B6C4" : "#FBEAF0", 1920 + i, { rim: 0.35, sh: 0.6 }); paper(ctx, blob(bx, by - 5 * s, 0.9 * s, 0.9 * s, 1925 + i, 0.05), "#F2C04E", 1925 + i, { rim: 0, sh: 0 }); }
      else if (se === "summer") { var bl = []; [-0.45, 0, 0.42].forEach(function (a, k) { var l = (4.6 - Math.abs(a) * 2) * s; bl.push(rib([[bx + (k - 1) * 0.6 * s, by + 1], [bx + Math.sin(a) * l * 0.5, by - l * 0.55], [bx + Math.sin(a) * l, by - l]], 1.2 * s, 0.3 * s)); }); paper(ctx, bl, "#8DB46A", 1930 + i, { rim: 0.25, sh: 0.4 }); }   // 풀 한 포기 (작은 나무처럼 보이지 않게)
      else if (se === "autumn") { paper(ctx, rib([[bx, by + 1], [bx + 0.2 * s, by - 3.6 * s]], 1.8 * s, 1.5 * s), "#F2E8D6", 1945 + i, { rim: 0.2, sh: 0.3 }); paper(ctx, [[bx - 3.6 * s, by - 3.2 * s]].concat(bez([bx - 3.6 * s, by - 3.2 * s, bx - 3 * s, by - 7.4 * s, bx + 3 * s, by - 7.4 * s, bx + 3.6 * s, by - 3.2 * s], 10)), i % 2 ? "#C2603E" : "#B8835A", 1950 + i, { rim: 0.3, sh: 0.6 }); }
      else { paper(ctx, blob(bx, by - 1.4 * s, 3.4 * s, 2.6 * s, 1955 + i, 0.15), "#FFFFFF", 1955 + i, { rim: 0.35, sh: 0.5 }); paper(ctx, blob(bx + 0.4 * s, by - 3.6 * s, 1.2 * s, 1 * s, 1960 + i, 0.05), "#C7787F", 1960 + i, { rim: 0, sh: 0.2 }); } });
  }
  function partyHat(ctx, V, x, y, s, sc) { var p = resample([[x - 7 * s, y], [x, y - 17 * s], [x + 7 * s, y]], 1.5); V.haru.body(ctx, p, "#F4C7D2", 2001, sc);
    ctx.save(); ctx.beginPath(); p.forEach(function (q, i) { i ? ctx.lineTo(q[0], q[1]) : ctx.moveTo(q[0], q[1]); }); ctx.closePath(); ctx.clip(); ctx.fillStyle = "#F2C04E"; for (var i = 0; i < 3; i++) { ctx.beginPath(); ctx.arc(x - 1.5 * s + i * 1.8 * s, y - 4 * s - i * 4.6 * s, 1.3 * s, 0, 7); ctx.fill(); } ctx.restore();
    paper(ctx, blob(x, y - 17.5 * s, 2.4 * s, 2.4 * s, 2002, 0.1), "#F2C04E", 2002, { rim: 0.2, sh: 0.4 }); }
  function cake(ctx, x, y, s) { paper(ctx, poly(x, y, s, [[-9, 0], [9, 0], [9, -9], [-9, -9]]), "#F6EEDC", 2010, { tone: 0.1 }); paper(ctx, poly(x, y, s, [[-9.4, -8.6], [9.4, -8.6], [9.4, -11.4], [-9.4, -11.4]]), "#F4B6C4", 2011, { rim: 0.3, sh: 0.4 }); [[-4.5, -12], [0, -12.6], [4.5, -12]].forEach(function (q, i) { paper(ctx, blob(x + q[0] * s, y + q[1] * s, 1.8 * s, 1.6 * s, 2012 + i, 0.05), "#C8303A", 2012 + i, { rim: 0.2, sh: 0.4 }); }); paper(ctx, rib([[x, y - 13 * s], [x, y - 19 * s]], 1.4 * s, 1.4 * s), "#F2C04E", 2016, { rim: 0.2, sh: 0.3 }); paper(ctx, blob(x, y - 20.6 * s, 1.2 * s, 1.8 * s, 2017, 0.05), "#F6A23C", 2017, { rim: 0, sh: 0 }); }
  function bouquet(ctx, x, y, s) { [[-3, -15, "#F4B6C4"], [2.6, -17, "#F2C04E"], [0, -12, "#FBEAF0"], [4.6, -12.6, "#B7A0CF"]].forEach(function (f, i) { thread(ctx, [[x, y - 5 * s], [x + f[0] * s, y + f[1] * s]], 0.7, "#6F8E4E"); paper(ctx, blossom(x + f[0] * s, y + f[1] * s, 2.8 * s), f[2], 2020 + i, { rim: 0.35, sh: 0.6 }); }); paper(ctx, poly(x, y, s, [[-4.4, -8], [4.4, -8], [1.4, 0], [-1.4, 0]]), "#E9DFC9", 2025, { rim: 0.4, sh: 0.6 }); paper(ctx, poly(x, y, s, [[-2.4, -5.6], [2.4, -5.6], [2.4, -4], [-2.4, -4]]), "#D9776A", 2026, { rim: 0.1, sh: 0.2 }); }

  /* ───────── ⑤ 계절 한 장 (나무 종류 × 계절) ───────── */
  var KEEPH = {
    cherry_spring: function (ctx, x, y, s) { [[-7, -10, 0.3], [6, -14, -0.4], [1, -4, 1]].forEach(function (q, i) { paper(ctx, blossom(x + q[0] * s, y + q[1] * s, 6 * s), i === 1 ? "#F8D3DC" : "#F1B5C4", 2100 + i, { rim: 0.5 }); paper(ctx, blob(x + q[0] * s, y + q[1] * s, 1.4 * s, 1.4 * s, 2105 + i, 0.05), "#E68AA0", 2105 + i, { rim: 0, sh: 0 }); }); },
    cherry_summer: function (ctx, x, y, s) { thread(ctx, bz(x, y, s, [1, -26, -2, -18, -5, -12, -7, -6]), 0.9, "#5E7A44"); thread(ctx, bz(x, y, s, [1, -26, 3, -18, 5, -12, 7, -7]), 0.9, "#5E7A44"); paper(ctx, place(leafPts(10 * s, 3.6 * s), x + 1 * s, y - 26 * s, 1, -0.3), "#76A05C", 2110); paper(ctx, [blob(x - 7 * s, y - 4 * s, 4.4 * s, 4.4 * s, 2111, 0.05), blob(x + 7 * s, y - 5 * s, 4.4 * s, 4.4 * s, 2112, 0.05)], "#B8303A", 2111, { tone: 0.2 }); },
    cherry_autumn: function (ctx, x, y, s) { paper(ctx, place(leafPts(20 * s, 7 * s), x - 8 * s, y - 2 * s, 1, -0.9), "#D46A45", 2120, { tone: 0.1 }); thread(ctx, [[x - 8 * s, y - 2 * s], [x + 3.4 * s, y - 17 * s]], 0.6, "rgba(120,50,30,.5)"); },
    cherry_winter: function (ctx, x, y, s) { paper(ctx, rib(bz(x, y, s, [-14, -2, -6, -8, 4, -14, 14, -22]), 2.6 * s, 1.4 * s), BARK.cherry, 2130); [[-6, -7], [3, -13], [12, -20]].forEach(function (q, i) { paper(ctx, blob(x + q[0] * s, y + (q[1] - 3) * s, 2 * s, 2.8 * s, 2131 + i, 0.05), "#C27C82", 2131 + i, { rim: 0.35 }); paper(ctx, blob(x + q[0] * s, y + (q[1] - 6.4) * s, 2.6 * s, 1.2 * s, 2135 + i, 0.15), "#FFFFFF", 2135 + i, { rim: 0.3, sh: 0.3 }); }); },
    zelkova_spring: function (ctx, x, y, s) { paper(ctx, rib(bz(x, y, s, [0, -2, 1, -10, -1, -18, 0, -24]), 1.8 * s, 1.2 * s), "#7FA35A", 2140, { rim: 0.4 }); paper(ctx, place(leafPts(11 * s, 4 * s), x, y - 20 * s, 1, -2.6), "#B0CC7A", 2141); paper(ctx, place(leafPts(12 * s, 4.2 * s), x, y - 23 * s, 1, -0.5), "#C8DD95", 2142); },
    zelkova_summer: function (ctx, x, y, s) { paper(ctx, blob(x, y - 13 * s, 3.6 * s, 8 * s, 2152, 0.05), "#5B4A3A", 2152, { tone: 0.15 });
      [-1, 1].forEach(function (d, i) { var w = place(leafPts(22 * s, 4.6 * s), x + d * 1.4 * s, y - 21 * s, 1, Math.PI / 2 - d * 0.2); paper(ctx, w, "#E6EEEC", 2150 + i, { rim: 0.4, sh: 0.6, fiber: 1.6, light: 0.85 }); thread(ctx, [[x + d * 1.4 * s, y - 21 * s], [x + d * 5.6 * s, y - 2 * s]], 0.4, "rgba(80,90,90,.45)"); thread(ctx, [[x + d * 3 * s, y - 14 * s], [x + d * 6.4 * s, y - 9 * s]], 0.35, "rgba(80,90,90,.35)"); });
      paper(ctx, blob(x, y - 22 * s, 4.6 * s, 3.6 * s, 2153, 0.05), "#4A3E32", 2153, { tone: 0.1 }); paper(ctx, blob(x, y - 26 * s, 5.4 * s, 2.4 * s, 2154, 0.05), "#3E342A", 2154, {});
      [[-4.8, -26.4], [4.8, -26.4]].forEach(function (q, i) { paper(ctx, blob(x + q[0] * s, y + q[1] * s, 1.6 * s, 1.6 * s, 2155 + i, 0.03), "#2A2420", 2155 + i, { rim: 0.2, sh: 0.2 }); }); },
    zelkova_autumn: function (ctx, x, y, s) { paper(ctx, place(leafPts(22 * s, 7 * s), x - 12 * s, y - 3 * s, 1, -0.75), "#C47038", 2160, { tone: 0.1 }); paper(ctx, place(leafPts(19 * s, 6.2 * s), x + 2 * s, y - 2 * s, 1, -1.75), "#A4542E", 2161, { tone: 0.1 }); },
    zelkova_winter: function (ctx, x, y, s) { paper(ctx, rib(bz(x, y, s, [-16, -6, -6, -10, 6, -12, 16, -16]), 2.2 * s, 1.2 * s), BARK.zelkova, 2170); magpie(ctx, x + 2 * s, y - 16 * s, s * 0.75); },
    ginkgo_spring: function (ctx, x, y, s) { paper(ctx, fanLeaf(x - 4 * s, y - 2 * s, 14 * s, -0.4), "#BFD47E", 2180); paper(ctx, fanLeaf(x + 5 * s, y - 2 * s, 12 * s, 0.45), "#D3E29A", 2181); },
    ginkgo_summer: function (ctx, x, y, s) { paper(ctx, fanLeaf(x, y - 1 * s, 20 * s, 0), "#739E52", 2190, { tone: 0.1 }); },
    ginkgo_autumn: function (ctx, x, y, s) { paper(ctx, fanLeaf(x - 4 * s, y - 1 * s, 16 * s, -0.5), "#EFC23E", 2200, { tone: 0.1 }); paper(ctx, fanLeaf(x + 6 * s, y - 2 * s, 14 * s, 0.6), "#F7D865", 2201, { tone: 0.1 }); },
    ginkgo_winter: function (ctx, x, y, s) { [[-6, -4], [5, -4], [0, -12]].forEach(function (q, i) { paper(ctx, blob(x + q[0] * s, y + q[1] * s, 5.2 * s, 5 * s, 2210 + i, 0.06), "#E3B04E", 2210 + i, { tone: 0.2 }); }); },
    pine_spring: function (ctx, x, y, s) { paper(ctx, rib(bz(x, y, s, [0, 0, 1, -8, -1, -16, 0, -22]), 2.4 * s, 1.6 * s), BARK.pine, 2220); [[0, -26], [-6, -20], [6, -21]].forEach(function (q, i) { paper(ctx, blob(x + q[0] * s, y + q[1] * s, 2.4 * s, 5 * s, 2221 + i, 0.06), "#E6CF66", 2221 + i, { rim: 0.4 }); }); },
    pine_summer: function (ctx, x, y, s) { cone(ctx, x, y, s, "#7F9A58", "#5E7A4C"); },
    pine_autumn: function (ctx, x, y, s) { cone(ctx, x, y, s, "#9A6A42", "#7A5034"); },
    pine_winter: function (ctx, x, y, s) { paper(ctx, rib(bz(x, y, s, [-18, -4, -6, -10, 6, -14, 18, -20]), 2.4 * s, 1.4 * s), BARK.pine, 2240); var nd = []; for (var i = 0; i < 9; i++) { var px = x + (-14 + i * 3.6) * s, py = y + (-6.6 - i * 1.6) * s; nd.push(rib([[px, py], [px - 3.6 * s, py - 8 * s]], 1 * s, 0.6 * s)); nd.push(rib([[px, py], [px + 3.6 * s, py - 8 * s]], 1 * s, 0.6 * s)); } paper(ctx, nd, "#4A6E4E", 2241, { rim: 0.25, sh: 0.5 }); paper(ctx, [blob(x - 8 * s, y - 13 * s, 5 * s, 2 * s, 2242, 0.2, -0.4), blob(x + 2 * s, y - 18 * s, 5.4 * s, 2.2 * s, 2243, 0.2, -0.4), blob(x + 11 * s, y - 23 * s, 4 * s, 1.8 * s, 2244, 0.2, -0.4)], "#FFFFFF", 2242, { rim: 0.5 }); }
  };
  function cone(ctx, x, y, s, a, b) { paper(ctx, blob(x, y - 11 * s, 7.4 * s, 11 * s, 2230, 0.05), a, 2230, { tone: 0.12 }); var sc = []; for (var r = 0; r < 4; r++) for (var c = 0; c < 3; c++) { var cx = x + (c - 1) * 4.2 * s + (r % 2) * 2 * s - 1 * s, cy = y + (-4 - r * 4.6) * s; if (Math.abs(cx - x) > 6 * s - r * 0.4 * s) continue; sc.push(blob(cx, cy, 2 * s, 1.6 * s, 2231 + r * 3 + c, 0.05)); } paper(ctx, sc, b, 2231, { rim: 0.2, sh: 0.3 }); }

  /* ───────── ⑥ 우연한 순간 ───────── */
  var MOMENT = {
    bubbles: function (ctx, x, y, s) { [[0, -20, 7], [12, -34, 5], [-10, -40, 4], [6, -52, 3]].forEach(function (b) { var cx = x + b[0] * s, cy = y + b[1] * s, r = b[2] * s; ctx.save(); var g = ctx.createRadialGradient(cx - r * 0.3, cy - r * 0.3, r * 0.1, cx, cy, r); g.addColorStop(0, "rgba(255,255,255,.08)"); g.addColorStop(0.8, "rgba(200,225,235,.18)"); g.addColorStop(1, "rgba(255,255,255,.7)"); ctx.fillStyle = g; ctx.beginPath(); ctx.arc(cx, cy, r, 0, 7); ctx.fill(); ctx.strokeStyle = "rgba(160,190,210,.6)"; ctx.lineWidth = 0.6; ctx.stroke(); ctx.fillStyle = "rgba(255,255,255,.9)"; ctx.beginPath(); ctx.ellipse(cx - r * 0.4, cy - r * 0.42, r * 0.22, r * 0.12, -0.7, 0, 7); ctx.fill(); ctx.restore(); }); },
    fireflies: function (ctx, x, y, s) { var r = R(31); for (var i = 0; i < 9; i++) { var cx = x + (r() - 0.5) * 70 * s, cy = y - 8 * s - r() * 40 * s; ctx.save(); var g = ctx.createRadialGradient(cx, cy, 0, cx, cy, 6 * s); g.addColorStop(0, "rgba(246,236,150,.9)"); g.addColorStop(0.3, "rgba(230,224,120,.35)"); g.addColorStop(1, "rgba(230,224,120,0)"); ctx.fillStyle = g; ctx.beginPath(); ctx.arc(cx, cy, 6 * s, 0, 7); ctx.fill(); ctx.fillStyle = "#FFF6C2"; ctx.beginPath(); ctx.arc(cx, cy, 0.9 * s, 0, 7); ctx.fill(); ctx.restore(); } },
    // 무지개: 옅은 세 줄, 먼 산 위로 낮게 (구름 없이)
    rainbow: function (ctx, x, y, s) { ctx.save(); ctx.globalAlpha = 0.7; ["#F0B6A4", "#F3DCA2", "#B9D6C6"].forEach(function (c, i) { var R0 = (62 - i * 5) * s, pts = []; for (var t = 0; t <= 1.0001; t += 0.03) pts.push([x + Math.cos(Math.PI + t * Math.PI) * R0, y + Math.sin(Math.PI + t * Math.PI) * R0]); paper(ctx, rib(pts, 4.6 * s, 4.6 * s), c, 2300 + i, { rim: 0.3, sh: 0.25, light: 0.6 }); }); ctx.restore(); },
    butterfly: function (ctx, x, y, s) { var c = "#F2C04E"; paper(ctx, [blob(x - 5 * s, y - 4 * s, 5.4 * s, 4.4 * s, 2310, 0.1, -0.5), blob(x - 4 * s, y + 2.6 * s, 3.4 * s, 2.8 * s, 2311, 0.1, 0.4)], c, 2310, { tone: 0.1 }); paper(ctx, [blob(x + 4 * s, y - 4.6 * s, 4.4 * s, 4 * s, 2312, 0.1, 0.5), blob(x + 3.6 * s, y + 2.2 * s, 2.8 * s, 2.4 * s, 2313, 0.1, -0.4)], mixHex(c, "#FFFFFF", 0.3), 2312, { tone: 0.1 }); paper(ctx, blob(x, y - 1 * s, 1 * s, 4.6 * s, 2314, 0.05), "#4A3E34", 2314, { rim: 0, sh: 0.2 }); thread(ctx, [[x, y - 5 * s], [x - 2 * s, y - 9 * s]], 0.5, "#4A3E34"); thread(ctx, [[x, y - 5 * s], [x + 2 * s, y - 9 * s]], 0.5, "#4A3E34"); },
    snail: function (ctx, x, y, s) { paper(ctx, [[x - 11 * s, y], [x + 9 * s, y]].concat(bez([x + 9 * s, y, x + 12 * s, y, x + 12 * s, y - 6 * s, x + 9 * s, y - 7 * s], 8)).concat([[x + 6 * s, y - 3 * s], [x - 11 * s, y - 2.4 * s]]), "#D8C6A6", 2320, { tone: 0.1 }); thread(ctx, [[x + 9.6 * s, y - 6.6 * s], [x + 11 * s, y - 11 * s]], 0.6, "#8A7356"); thread(ctx, [[x + 8.4 * s, y - 6.6 * s], [x + 8 * s, y - 11 * s]], 0.6, "#8A7356"); paper(ctx, blob(x - 2 * s, y - 7 * s, 7.4 * s, 7 * s, 2321, 0.04), "#C49A6A", 2321, { tone: 0.12 }); var sp = []; for (var t = 0; t < 1; t += 0.02) { var a = t * Math.PI * 4.2, rr = 6.2 * s * (1 - t * 0.85); sp.push([x - 2 * s + Math.cos(a) * rr, y - 7 * s + Math.sin(a) * rr]); } thread(ctx, sp, 0.8, "rgba(110,76,44,.7)"); },
    // 오로라: 하늘 위쪽에 옅게 번진 초록 · 보라 띠 둘
    aurora: function (ctx, x, y, s) { ctx.save(); ctx.filter = "blur(" + (2.4 * SHK()) + "px)"; ctx.globalAlpha = 0.42; ["#8FD0B8", "#B7A0CF"].forEach(function (c, i) { var pts = []; for (var t = 0; t <= 1.0001; t += 0.025) pts.push([x + (t - 0.5) * 220 * s, y - i * 12 * s + Math.sin(t * 5 + i * 1.3) * 10 * s]); paper(ctx, rib(pts, 14 * s, 5 * s), c, 2330 + i, { rim: 0, sh: 0, fiber: 0.4 }); }); ctx.restore(); }
  };

  // 계절 바람: 한 줄을 보낸 순간, 작은 조각 열둘이 한 번 지나감 (봄 꽃잎 · 여름 풀잎 · 가을 잎 · 겨울 눈)
  MOMENT.wind = function (ctx, se, x0, y0, w, h) { var r = R(4401);
    for (var i = 0; i < 12; i++) { var x = x0 + r() * w, y = y0 + r() * h, a = r() * 6.28, k = 0.8 + r() * 0.5;
      if (se === "spring") paper(ctx, place([[0, 0], [2.4, -1.6], [4.6, -0.6], [4, 0], [4.6, 0.6], [2.4, 1.6]], x, y, k, a), i % 3 ? "#F6C9D3" : "#FBE6EC", 4410 + i, { rim: 0.2, sh: 0.35 });
      else if (se === "summer") paper(ctx, place(leafPts(7, 1.4), x, y, k, a), i % 2 ? "#9CC27A" : "#B6D394", 4410 + i, { rim: 0.2, sh: 0.35 });
      else if (se === "autumn") paper(ctx, place(leafPts(6, 2.6), x, y, k, a), ["#D98C4A", "#E9B04E", "#C46B3D"][i % 3], 4410 + i, { rim: 0.2, sh: 0.35 });
      else paper(ctx, blob(x, y, 1.6 * k, 1.6 * k, 4410 + i, 0.1), "#FFFFFF", 4410 + i, { rim: 0.3, sh: 0.3 }); } };
  // 나비 한 쌍: 나무에서 말뚝 쪽으로
  MOMENT.pair = function (ctx, x, y, s) { MOMENT.butterfly(ctx, x, y, s); MOMENT.butterfly(ctx, x + 26 * s, y - 14 * s, s * 0.8); };

  // 나비 한 장 (위에서 본 모습, 머리 = 위). side: -1 왼날개, 1 오른날개, 0 몸통. 상자 40 × 40, 몸통 축 x = 20. 앱이 날개를 접었다 폈다 함
  function flySprite(ctx, side) { var x = 20, y = 21, c = "#F2C04E", c2 = "#E9A43A";
    if (side) { var d = side, fw = [[0, 0], [-4, -6], [-11, -9], [-14, -6], [-12, -1], [-6, 1]].map(function (q) { return [x + q[0] * d, y + q[1]]; }), hw = [[0, 1], [-6, 2], [-9, 6], [-7, 10], [-3, 9], [0, 4]].map(function (q) { return [x + q[0] * d, y + q[1]]; });
      paper(ctx, hw, c2, 2315 + (d > 0 ? 1 : 0), { tone: 0.12, rim: 0.6 }); paper(ctx, fw, c, 2310 + (d > 0 ? 1 : 0), { tone: 0.12, rim: 0.6 });
      return; }
    paper(ctx, blob(x, y + 1.5, 1.1, 6, 2314, 0.05), "#4A3E34", 2314, { rim: 0, sh: 0.3 });
    [-1, 1].forEach(function (d) { var e = [x + d * 4, y - 9]; thread(ctx, bez([x, y - 4, x + d * 1, y - 7, x + d * 2.4, y - 8.6, e[0], e[1]], 8), 0.5, "#4A3E34"); ctx.fillStyle = "#4A3E34"; ctx.beginPath(); ctx.arc(e[0], e[1], 0.8, 0, 7); ctx.fill(); }); }
  // 계절 바람 한 조각 (상자 10 × 10, 가운데)
  function windPiece(ctx, se) { if (se === "spring") paper(ctx, place([[0, 0], [2.4, -1.6], [4.6, -0.6], [4, 0], [4.6, 0.6], [2.4, 1.6]], 2.6, 5, 1, 0), "#F6C9D3", 4410, { rim: 0.2, sh: 0.35 });
    else if (se === "summer") paper(ctx, place(leafPts(7, 1.4), 1.5, 5, 1, 0), "#9CC27A", 4411, { rim: 0.2, sh: 0.35 });
    else if (se === "autumn") paper(ctx, place(leafPts(6, 2.6), 2, 5, 1, 0), "#D98C4A", 4412, { rim: 0.2, sh: 0.35 });
    else paper(ctx, blob(5, 5, 1.8, 1.8, 4413, 0.1), "#FFFFFF", 4413, { rim: 0.3, sh: 0.3 }); }

  /* ───────── 하늘 ───────── */
  function sun(ctx, x, y, s, col) { paper(ctx, blob(x, y, 22 * s, 22 * s, 2400, 0.03), mixHex(col, "#FFFFFF", 0.35), 2400, { sh: 0.6, rim: 1.4 }); paper(ctx, blob(x, y, 17 * s, 17 * s, 2401, 0.04), col, 2401, { sh: 0.4 }); }
  // 달: 초승달 한 장 (두 원의 차를 다각형으로, 오려 낸 가장자리가 생기지 않게)
  function moon(ctx, x, y, s) { var R1 = 17 * s, R2 = 14 * s, cx = x + 8 * s, cy = y - 4 * s, d = Math.hypot(cx - x, cy - y), al = Math.atan2(cy - y, cx - x);
    var t1 = Math.acos((R1 * R1 + d * d - R2 * R2) / (2 * R1 * d)), t2 = Math.acos((R2 * R2 + d * d - R1 * R1) / (2 * R2 * d)), pts = [], i, n = 40;
    for (i = 0; i <= n; i++) { var a1 = al + t1 + (2 * Math.PI - 2 * t1) * i / n; pts.push([x + Math.cos(a1) * R1, y + Math.sin(a1) * R1]); }
    for (i = 1; i < n; i++) { var a2 = al + Math.PI - (Math.PI - t2) + (2 * (Math.PI - t2)) * i / n; pts.push([cx + Math.cos(a2) * R2, cy + Math.sin(a2) * R2]); }
    var e = pts[n], q0 = pts[n + 1]; if (Math.hypot(q0[0] - e[0], q0[1] - e[1]) > Math.hypot(pts[pts.length - 1][0] - e[0], pts[pts.length - 1][1] - e[1])) pts = pts.slice(0, n + 1).concat(pts.slice(n + 1).reverse());
    paper(ctx, pts, "#F1E4BE", 2410, { sh: 0.3, glow: true }); }
  // 둥근 달 (앱이 실제 모양대로 오려 씀, S3): 한지 한 장에 아주 옅은 얼룩 둘
  function moonFull(ctx, x, y, r) { paper(ctx, blob(x, y, r, r, 2412, 0.025), "#F1E4BE", 2412, { sh: 0.3, glow: true });
    paper(ctx, blob(x - r * 0.28, y - r * 0.18, r * 0.24, r * 0.2, 2413, 0.2), "#E6D6AE", 2413, { rim: 0.2, sh: 0.1 });
    paper(ctx, blob(x + r * 0.22, y + r * 0.3, r * 0.17, r * 0.14, 2414, 0.2), "#E6D6AE", 2414, { rim: 0.2, sh: 0.1 }); }
  function cloud(ctx, x, y, s, col, seed) { paper(ctx, [blob(x - 12 * s, y, 18 * s, 7 * s, seed, 0.2), blob(x + 8 * s, y - 4 * s, 16 * s, 8 * s, seed + 1, 0.2), blob(x + 22 * s, y + 1 * s, 12 * s, 5 * s, seed + 2, 0.2)], col, seed, { sh: 0.8 }); paper(ctx, blob(x + 2 * s, y + 3 * s, 26 * s, 3.4 * s, seed + 3, 0.15), mixHex(col, "#9AA4B4", 0.18), seed + 3, { rim: 0.5, sh: 0.3 }); }
  function stars(ctx, n, seed, w, h) { var r = R(seed); for (var i = 0; i < n; i++) { var x = r() * w, y = r() * h, big = r() < 0.18; if (big) paper(ctx, star4(x, y, 2.6), "#F6ECCB", seed + i, { rim: 0.2, sh: 0.2, glow: true }); else { ctx.fillStyle = "rgba(255,248,226," + (0.4 + r() * 0.5) + ")"; ctx.beginPath(); ctx.arc(x, y, 0.5 + r() * 0.7, 0, 7); ctx.fill(); } } }

  /* ───────── 장면 · 시트 ───────── */
  var HV = { world: hanjiWorld({ edge: "torn", fiber: 0.8, shadow: 2.3 }), haru: haruMaker("ink", 2.4), haze: 0.55 };
  function ensureTex() { if (!TEX.grain) { TEX.grain = grainTex(5, "60,45,30", "255,255,255"); TEX.fiber = fiberTex(7); } }
  function ground(ctx, P, w, gy, seed, haze) { var far = ridgeS(gy - 78, 70, 330, 0.011), mid = ridgeS(gy - 26, 30, 340, 0.017), path = []; for (var x = -14; x <= w + 14; x += 5) path.push([x, gy + 3 + Math.sin(x * 0.05) * 1.2]); var fr = ridgeS(gy + 92, 16, 370, 0.022), fr2 = ridgeS(gy + 168, 14, 372, 0.03);
    [[far, mixHex(P.far, P.sky2, haze)], [mid, mixHex(P.mid, P.sky2, haze * 0.45)], [path, P.path], [fr, P.front], [fr2, P.front2]].forEach(function (L, i) { HV.world(ctx, down(L[0]), L[1], 530 + i, { kind: "layer", top: L[0] }); }); }
  // 정원 한 장: o = { season (실제), life (인생의 계절 → 나무 종류), stage, night, chime, lantern, letter, kite, ribbons, buds, moment, keep }
  function gardenH(ctx, o) {
    ensureTex(); var se = o.season, night = !!o.night; NIGHT = night; var P = WPAL[night ? "night" : se], sp = { spring: "cherry", summer: "zelkova", autumn: "ginkgo", winter: "pine" }[o.life || "summer"];
    ctx.save(); ctx.setTransform(U, 0, 0, U, 0, 0);
    var gr = ctx.createLinearGradient(0, 0, 0, SGY); gr.addColorStop(0, P.sky); gr.addColorStop(1, P.sky2); ctx.fillStyle = gr; ctx.fillRect(0, 0, SW, SH);
    texFill(ctx, TEX.fiber, night ? 0.2 : 0.55, "source-over", 0.4); texFill(ctx, TEX.grain, 0.5, "multiply", 0.5);
    if (night) { stars(ctx, 40, 77, SW, 330); moon(ctx, 300, 214, 1); if (o.moment === "aurora") MOMENT.aurora(ctx, SW / 2, 260, 1); }
    else { sun(ctx, 300, 214, 1, P.sun); }
    cloud(ctx, 54, 246, 1, P.cloud, 2500); cloud(ctx, 196, 300, 0.75, P.cloud, 2510); cloud(ctx, 352, 52, 0.9, P.cloud, 2520);
    if (o.moment === "rainbow" && !night) MOMENT.rainbow(ctx, 210, SGY - 40, 1.1);
    ground(ctx, P, SW, SGY, 0, 0.55);
    var POSTX = 352, KX = 300, KY = 268;
    tree(ctx, 70, SGY + 2, 1, sp, se, o.stage == null ? 3 : o.stage, {});
    if (o.keep) KEEPH[sp + "_" + se](ctx, 112, SGY + 4, 0.7);
    post(ctx, POSTX, SGY + 2, 1, { season: se, chime: o.chime == null ? 1 : o.chime, lantern: o.lantern, lit: night && o.lantern, letter: o.letter });
    if (o.kite && !night) kite(ctx, KX, KY, 1, o.kite, o.ribbons || 0, POSTX - 33, SGY - 96);
    moss(ctx, 190, SGY + 2, 1, se, o.buds || 0);
    var so = { night: night }; stoneH(ctx, HV, 2718281, 190, SGY, 48, so); stoneH(ctx, HV, 12345, 250, SGY, 34, so); stoneH(ctx, HV, 31337, 297, SGY, 40, so);
    if (o.moment === "butterfly") MOMENT.butterfly(ctx, 226, SGY - 60, 1);
    if (o.moment === "pair") MOMENT.pair(ctx, 150, SGY - 82, 0.7);
    if (o.moment === "wind") MOMENT.wind(ctx, se, 30, SGY - 150, 330, 110);
    if (o.moment === "bubbles") MOMENT.bubbles(ctx, 204, SGY - 30, 1);
    if (o.moment === "snail") MOMENT.snail(ctx, 140, SGY + 3, 0.9);
    if (o.moment === "fireflies") MOMENT.fireflies(ctx, 200, SGY - 10, 1);
    ctx.textAlign = "center"; ctx.fillStyle = P.ink; ctx.globalAlpha = 0.72; ctx.font = "13px 'Gowun Dodum', sans-serif"; ctx.fillText(night ? "오늘도 수고했어요" : "남은 시간 · 인생의 " + { spring: "봄", summer: "여름", autumn: "가을", winter: "겨울" }[o.life || "summer"], SW / 2, 78); ctx.globalAlpha = 0.95; ctx.font = "700 50px 'Gowun Batang', serif"; ctx.fillText("22,212", SW / 2, 133); ctx.globalAlpha = 0.88; ctx.font = "16px 'Gowun Batang', serif"; ctx.fillText(night ? "잘 자요, 하루" : "오늘 들른 곳 하나는?", SW / 2, 186);
    ctx.restore(); NIGHT = false;
  }
  // 에셋 타일: 한지 종이 + 땅 띠 위에 하나
  function tileH(ctx, w, h, gy, draw, o) { ensureTex(); o = o || {}; NIGHT = !!o.night; ctx.save(); ctx.setTransform(U, 0, 0, U, 0, 0); ctx.fillStyle = o.night ? "#2E3752" : (o.bg || "#F3EEE2"); ctx.fillRect(0, 0, w, h); texFill(ctx, TEX.fiber, o.night ? 0.2 : 0.5, "source-over", 0.4); texFill(ctx, TEX.grain, 0.5, "multiply", 0.5);
    if (gy != null) { var gl = []; for (var x = -14; x <= w + 14; x += 5) gl.push([x, gy + Math.sin(x * 0.05) * 1]); HV.world(ctx, gl.concat([[w + 14, h + 20], [-14, h + 20]]), o.ground || (o.night ? "#41506A" : "#C9DAA6"), 600, { kind: "layer", top: gl }); }
    draw(ctx); ctx.restore(); NIGHT = false; }

  function cc(c) { if (Array.isArray(c)) return c; if (c[0] === "#") return hex(c); var m = c.match(/[\d.]+/g); return [+m[0], +m[1], +m[2]]; }
  function mixHex(a, b, t) { var A = cc(a), B = cc(b); return "rgb(" + [0, 1, 2].map(function (i) { return Math.round(A[i] + (B[i] - A[i]) * t); }).join(",") + ")"; }
  /* 땅에 놓인 것 · 솔방울 · 고깔 · 나비 · 달팽이 */
  // 땅에 닿은 그늘 (놓인 물건 밑)
  function contact(ctx, x, y, w, a) { ctx.save(); ctx.filter = "blur(" + (1.6 * SHK()) + "px)"; ctx.fillStyle = "rgba(50,38,28," + (a || 0.22) + ")"; ctx.beginPath(); ctx.ellipse(x, y + 0.6, w, Math.max(1.2, w * 0.12), 0, 0, 7); ctx.fill(); ctx.restore(); }
  // 다각형을 세로줄 x 에서 잘랐을 때 위 · 아래
  function spanAt(pts, x) { var ys = []; for (var i = 0; i < pts.length; i++) { var a = pts[i], b = pts[(i + 1) % pts.length]; if ((a[0] - x) * (b[0] - x) <= 0 && a[0] !== b[0]) ys.push(a[1] + (b[1] - a[1]) * (x - a[0]) / (b[0] - a[0])); } if (ys.length < 2) return null; return [Math.min.apply(null, ys), Math.max.apply(null, ys)]; }

  /* 솔방울: 비늘이 아래로 겹치며 내려옴. open = 여문 (가을) 은 벌어짐. 땅에 누워 있음 */
  function pineCone(ctx, x, y, s, base, tip, open, rot) {
    var Lh = 22, Wd = open ? 15 : 11.5, rows = 8, pieces = [];
    function P(px, py) { var c = Math.cos(rot), sn = Math.sin(rot); return [x + (px * c - py * sn) * s, y + (px * sn + py * c) * s]; }
    contact(ctx, x, y + 1, 12 * s);
    paper(ctx, [P(-0.6, -Lh - 2), P(0.6, -Lh - 2), P(0.8, -Lh - 5.5), P(-0.4, -Lh - 5.5)], "#7A5034", 2260, { rim: 0.2, sh: 0.3 });
    var sil = []; for (var i = 0; i < 28; i++) { var a = i / 28 * Math.PI * 2, ty = Math.sin(a), wf = Math.pow(Math.max(0, 1 - Math.pow((ty + 0.15) / 1.15, 2)), 0.5); sil.push(P(Math.cos(a) * Wd * 0.5 * (0.6 + 0.4 * wf), -Lh / 2 + ty * Lh / 2)); }
    paper(ctx, sil, mixHex(base, "#000000", 0.35), 2261, { sh: 1, rim: 0.4 });
    for (var r = 0; r < rows; r++) { var t = r / (rows - 1), yy = -Lh + 2.6 + t * (Lh - 3.4), wrow = Wd * 0.5 * Math.sin(Math.PI * (0.18 + t * 0.7)), n = r % 2 ? 3 : 2;
      for (var k = 0; k < n; k++) { var u = n === 1 ? 0 : (k / (n - 1) - 0.5) * 2, px = u * wrow * 0.62 + (r % 2 ? 0 : 0), sw = (open ? 4.6 : 3.8) * (0.8 + 0.4 * Math.sin(Math.PI * (0.15 + t * 0.7))), sh = open ? 4.2 : 3.4, fl = open ? u * 0.5 : u * 0.2;
        var sc = [[0, -sh * 0.5], [sw * 0.55, sh * 0.05], [sw * 0.18 + fl, sh * 0.62], [-sw * 0.18 + fl, sh * 0.62], [-sw * 0.55, sh * 0.05]].map(function (q) { return P(px + q[0], yy + q[1]); });
        paper(ctx, sc, mixHex(base, tip, 0.2 * u * u), 2270 + r * 4 + k, { rim: 0.25, sh: 0.35, tone: 0.18 });
        thread(ctx, [P(px - sw * 0.3 + fl * 0.5, yy + sh * 0.2), P(px + sw * 0.3 + fl * 0.5, yy + sh * 0.2)], 0.45, "rgba(255,240,210,.45)"); } }
  }

  /* 땅에 누운 잔가지: 갈래 하나, 그늘 */
  function twig(ctx, x, y, s, len, ang, col, seed, forks) {
    var c = Math.cos(ang), sn = Math.sin(ang), x0 = x - c * len * s / 2, y0 = y - 2.2 * s - sn * len * s / 2, x1 = x + c * len * s / 2, y1 = y - 2.2 * s + sn * len * s / 2;
    var main = bez([x0, y0, x0 + c * len * s * 0.33, y0 + sn * len * s * 0.33 - 1.4 * s, x0 + c * len * s * 0.66, y0 + sn * len * s * 0.66 + 1 * s, x1, y1], 14), parts = [rib(main, 2.6 * s, 1.1 * s)], ends = [];
    (forks || [[0.45, -0.7, 0.32]]).forEach(function (f) { var p = main[Math.floor(f[0] * (main.length - 1))], a = ang + f[1], l = f[2] * len * s, e = [p[0] + Math.cos(a) * l, p[1] + Math.sin(a) * l]; parts.push(rib([p, [(p[0] + e[0]) / 2, (p[1] + e[1]) / 2 - 0.6 * s], e], 1.5 * s, 0.7 * s)); ends.push(e); });
    contact(ctx, x, y, len * s * 0.5, 0.18); paper(ctx, parts, col, seed, { rim: 0.4, sh: 0.6, tone: 0.12 }); return { main: main, ends: ends };
  }
  function bud(ctx, p, s, ang, col, seed) { var b = place([[0, 0], [2, -1.6], [4.6, -0.3], [6.6, 0], [4.6, 0.3], [2, 1.6]].map(function (q) { return [q[0], q[1] * 1.5]; }), p[0], p[1], s, ang); paper(ctx, b, col, seed, { rim: 0.25, sh: 0.4, tone: 0.15 }); }

  KEEPH.cherry_winter = function (ctx, x, y, s) { var t = twig(ctx, x, y, s, 30, -0.12, BARK.cherry, 2130, [[0.55, -0.75, 0.3]]); [[0.2, -1.1], [0.42, -1.3], [0.78, -1.15]].forEach(function (q, i) { bud(ctx, t.main[Math.floor(q[0] * (t.main.length - 1))], s, q[1], i % 2 ? "#B06A70" : "#9C5A5E", 2131 + i); }); bud(ctx, t.ends[0], s, -0.75 - 0.12, "#B06A70", 2134);
    var m = t.main, a = m[Math.floor(m.length * 0.05)], b = m[Math.floor(m.length * 0.3)], c = m[Math.floor(m.length * 0.62)], d = m[Math.floor(m.length * 0.9)];
    paper(ctx, [rib([[a[0], a[1] - 1.3 * s], [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2 - 1.9 * s], [b[0], b[1] - 1.2 * s]], 1.4 * s, 0.8 * s), rib([[c[0], c[1] - 1.2 * s], [(c[0] + d[0]) / 2, (c[1] + d[1]) / 2 - 1.7 * s], [d[0], d[1] - 1.1 * s]], 1.3 * s, 0.7 * s)], "#FFFFFF", 2135, { rim: 0.3, sh: 0.3 }); };
  KEEPH.zelkova_winter = function (ctx, x, y, s) { var t = twig(ctx, x, y, s, 32, -0.06, BARK.zelkova, 2170, [[0.5, -0.6, 0.36], [0.3, -1.6, 0.22]]); var nx = x + 1 * s, ny = y - 6 * s;
    paper(ctx, [[nx - 8 * s, ny - 4 * s]].concat(bez([nx - 8 * s, ny - 4 * s, nx - 8.6 * s, ny + 6.4 * s, nx + 8.6 * s, ny + 6.4 * s, nx + 8 * s, ny - 4.4 * s], 12)).concat(bez([nx + 8 * s, ny - 4.4 * s, nx + 4 * s, ny - 2.6 * s, nx - 4 * s, ny - 2.6 * s, nx - 8 * s, ny - 4 * s], 8)), "#6E5642", 2171, { rim: 0.5, sh: 0.9, tone: 0.15 });
    var r = R(2172), st = []; for (var i = 0; i < 9; i++) { var a = (r() - 0.5) * 0.7, cxx = nx + (r() - 0.5) * 12 * s, cyy = ny + r() * 4 * s - 0.5 * s, l = (7 + r() * 5) * s; st.push(rib([[cxx - Math.cos(a) * l / 2, cyy - Math.sin(a) * l / 2], [cxx + Math.cos(a) * l / 2, cyy + Math.sin(a) * l / 2]], 0.9 * s, 0.7 * s)); } paper(ctx, st, "#8A7056", 2173, { rim: 0.2, sh: 0.3 });
    paper(ctx, blob(nx - 1.5 * s, ny - 4.6 * s, 5.6 * s, 1.4 * s, 2174, 0.25), "#FFFFFF", 2174, { rim: 0.4, sh: 0.3 });
    var f = place(leafPts(13 * s, 1.8 * s), x + 9 * s, y - 1.4 * s, 1, -0.1); paper(ctx, f, "#2C3340", 2175, { rim: 0.3, sh: 0.4 }); paper(ctx, place(leafPts(4.4 * s, 1.5 * s), x + 9 * s, y - 1.4 * s, 1, -0.1), "#F4F1EA", 2176, { rim: 0, sh: 0 }); };
  KEEPH.pine_winter = function (ctx, x, y, s) { var t = twig(ctx, x, y, s, 30, -0.05, BARK.pine, 2240, []); var nd = [];
    for (var i = 0; i < 9; i++) { var p = t.main[Math.floor((0.08 + i * 0.105) * (t.main.length - 1))]; [-0.42, -0.2].forEach(function (d, k) { var a = -0.05 + d - k * 0.05 - (i % 2) * 0.08, l = (10 + (i % 3) * 1.4) * s; nd.push(rib([p, [p[0] + Math.cos(a) * l * 0.5, p[1] + Math.sin(a) * l * 0.5 - 0.6 * s], [p[0] + Math.cos(a) * l, p[1] + Math.sin(a) * l]], 0.9 * s, 0.35 * s)); }); }
    paper(ctx, nd, "#4A6E4E", 2241, { rim: 0.2, sh: 0.4 }); paper(ctx, [blob(x - 5 * s, y - 6.6 * s, 6.4 * s, 1.7 * s, 2242, 0.25, -0.12), blob(x + 7 * s, y - 7.4 * s, 5 * s, 1.5 * s, 2243, 0.25, -0.12)], "#FFFFFF", 2242, { rim: 0.5, sh: 0.4 }); };
  KEEPH.pine_spring = function (ctx, x, y, s) { var t = twig(ctx, x - 2 * s, y, s, 22, -0.05, BARK.pine, 2220, []), e = t.main[t.main.length - 1], nd = [];
    for (var i = 0; i < 9; i++) { var a = -Math.PI / 2 - 1.2 + i * 0.3; nd.push(rib([e, [e[0] + Math.cos(a) * 9 * s, e[1] + Math.sin(a) * 9 * s]], 0.9 * s, 0.4 * s)); } paper(ctx, nd, "#5C8259", 2221, { rim: 0.2, sh: 0.4 });
    paper(ctx, rib([e, [e[0] + 2 * s, e[1] - 6 * s], [e[0] + 3 * s, e[1] - 12 * s]], 2.4 * s, 1.4 * s), "#C9D58A", 2222, { rim: 0.3, sh: 0.5 });
    var r = R(2223); for (var k = 0; k < 9; k++) { var a2 = Math.PI * (0.6 + r() * 0.8), rr = 2.6 * s + r() * 1.4 * s; paper(ctx, blob(e[0] + Math.cos(a2) * rr, e[1] - 1 * s + Math.sin(a2) * rr * 0.6, 1.3 * s, 2 * s, 2224 + k, 0.05, a2), k % 2 ? "#E6CF66" : "#D9BE52", 2224 + k, { rim: 0.15, sh: 0.3 }); }
    ctx.save(); ctx.fillStyle = "rgba(230,205,90,.7)"; for (var d = 0; d < 12; d++) { ctx.beginPath(); ctx.arc(x + (r() - 0.3) * 30 * s, y + (r() - 0.5) * 2 * s, 0.4 + r() * 0.5, 0, 7); ctx.fill(); } ctx.restore(); };
  KEEPH.pine_summer = function (ctx, x, y, s) { pineCone(ctx, x, y - 4 * s, s * 0.95, "#7F9A58", "#A8BC78", false, -1.3); };
  KEEPH.pine_autumn = function (ctx, x, y, s) { pineCone(ctx, x, y - 5 * s, s, "#9A6A42", "#C8A070", true, -1.25); };
  KEEPH.ginkgo_winter = function (ctx, x, y, s) { contact(ctx, x, y, 14 * s);
    [[-7, -3.4, 0.3], [6, -3.6, -0.4]].forEach(function (q, i) { paper(ctx, blob(x + q[0] * s, y + q[1] * s, 4.8 * s, 3.8 * s, 2210 + i, 0.1, q[2]), i ? "#E9B44E" : "#DFA23E", 2210 + i, { tone: 0.22, rim: 0.5 }); thread(ctx, [[x + (q[0] + 3.6) * s, y + (q[1] - 2.4) * s], [x + (q[0] + 5.2) * s, y + (q[1] - 4) * s]], 0.7, "#7A5A3A"); });
    paper(ctx, blob(x - 0.5 * s, y - 2.6 * s, 3 * s, 2.2 * s, 2213, 0.04, 0.5), "#F3EAD2", 2213, { tone: 0.15, rim: 0.4 }); thread(ctx, [[x - 3 * s, y - 4.2 * s], [x + 2 * s, y - 1 * s]], 0.5, "rgba(140,120,90,.6)"); };
  KEEPH.cherry_autumn = function (ctx, x, y, s) { contact(ctx, x, y, 12 * s); var lp = place(leafPts(20 * s, 6.6 * s), x - 10 * s, y - 3 * s, 1, -0.16); paper(ctx, lp, "#D46A45", 2120, { tone: 0.12 }); thread(ctx, [[x - 10 * s, y - 3 * s], [x + 9 * s, y - 6.2 * s]], 0.6, "rgba(120,50,30,.5)"); for (var i = 1; i < 4; i++) { var px = x - 10 * s + i * 4.8 * s, py = y - 3 * s - i * 0.8 * s; thread(ctx, [[px, py], [px + 2.4 * s, py - 3.2 * s]], 0.45, "rgba(120,50,30,.4)"); thread(ctx, [[px, py], [px + 2.6 * s, py + 2.6 * s]], 0.45, "rgba(120,50,30,.4)"); } thread(ctx, [[x - 10 * s, y - 3 * s], [x - 14 * s, y - 2 * s]], 0.9, "#7A4A30"); };
  ["cherry_spring", "cherry_summer", "zelkova_autumn", "ginkgo_spring", "ginkgo_summer", "ginkgo_autumn"].forEach(function (k) { var f = KEEPH[k]; KEEPH[k] = function (ctx, x, y, s) { contact(ctx, x, y, 12 * s); f(ctx, x, y, s); }; });

  /* 나비: 앞날개 크고 뒷날개 작게, 무늬 점, 끝이 둥근 더듬이 */
  MOMENT.butterfly = function (ctx, x, y, s) { var c = "#F2C04E", c2 = "#E9A43A";
    [[-1, 1], [1, 0.82]].forEach(function (d, i) { var k = d[1], fw = place([[0, 0], [-4, -6], [-11, -9], [-14, -6], [-12, -1], [-6, 1]], x, y, s, 0).map(function (q) { return [x + (q[0] - x) * d[0] * k, q[1]]; }), hw = place([[0, 1], [-6, 2], [-9, 6], [-7, 10], [-3, 9], [0, 4]], x, y, s, 0).map(function (q) { return [x + (q[0] - x) * d[0] * k, q[1]]; });
      paper(ctx, hw, i ? mixHex(c, "#FFFFFF", 0.2) : c2, 2315 + i, { tone: 0.12, rim: 0.6 }); paper(ctx, fw, i ? mixHex(c, "#FFFFFF", 0.25) : c, 2310 + i, { tone: 0.12, rim: 0.6 });
      paper(ctx, blob(x + d[0] * k * 9.5 * s, y - 5.5 * s, 1.6 * s, 1.6 * s, 2318 + i, 0.05), "#8A5A2A", 2318 + i, { rim: 0, sh: 0.1 }); paper(ctx, blob(x + d[0] * k * 5.5 * s, y + 6.5 * s, 1.2 * s, 1.2 * s, 2320 + i, 0.05), "#FBF3DC", 2320 + i, { rim: 0, sh: 0.1 }); });
    paper(ctx, blob(x, y + 1.5 * s, 1.1 * s, 6 * s, 2314, 0.05), "#4A3E34", 2314, { rim: 0, sh: 0.3 });
    [-1, 1].forEach(function (d) { var e = [x + d * 4 * s, y - 9 * s]; thread(ctx, bez([x, y - 4 * s, x + d * 1 * s, y - 7 * s, x + d * 2.4 * s, y - 8.6 * s, e[0], e[1]], 8), 0.5, "#4A3E34"); ctx.fillStyle = "#4A3E34"; ctx.beginPath(); ctx.arc(e[0], e[1], 0.8 * s, 0, 7); ctx.fill(); }); };
  /* 달팽이: 길게 뻗은 몸, 고개 들고, 껍데기는 세 겹 소용돌이 */
  MOMENT.snail = function (ctx, x, y, s) { contact(ctx, x, y, 14 * s);
    var foot = [[x - 13 * s, y]].concat(bez([x - 13 * s, y, x - 4 * s, y - 2.4 * s, x + 4 * s, y - 2.4 * s, x + 8 * s, y - 3 * s], 10)).concat(bez([x + 8 * s, y - 3 * s, x + 10 * s, y - 8 * s, x + 14 * s, y - 9 * s, x + 14.4 * s, y - 5 * s], 10)).concat([[x + 13 * s, y - 1 * s], [x + 6 * s, y + 0.4 * s]]);
    paper(ctx, foot, "#D8C6A6", 2320, { tone: 0.14 });
    [[12.4, -8.6, 11, -15], [13.8, -8, 16, -14]].forEach(function (q) { thread(ctx, bez([x + q[0] * s, y + q[1] * s, x + q[0] * s, y + (q[1] - 3) * s, x + q[2] * s, y + (q[3] + 3) * s, x + q[2] * s, y + q[3] * s], 8), 0.7, "#A08C6A"); ctx.fillStyle = "#4A3E34"; ctx.beginPath(); ctx.arc(x + q[2] * s, y + q[3] * s, 0.9 * s, 0, 7); ctx.fill(); });
    var sx = x - 3 * s, sy = y - 8 * s; paper(ctx, blob(sx, sy, 8.4 * s, 7.8 * s, 2321, 0.03), "#B9875A", 2321, { tone: 0.15 }); paper(ctx, blob(sx + 1.2 * s, sy - 0.4 * s, 5.6 * s, 5.2 * s, 2322, 0.03), "#CFA274", 2322, { rim: 0.4, sh: 0.5 }); paper(ctx, blob(sx + 2 * s, sy - 0.8 * s, 3 * s, 2.8 * s, 2323, 0.03), "#E2BE8E", 2323, { rim: 0.35, sh: 0.4 });
    var sp = []; for (var t = 0; t < 1; t += 0.02) { var a = t * Math.PI * 3.6 + 2.4, rr = 7.6 * s * (1 - t * 0.9); sp.push([sx + 2.2 * s * t + Math.cos(a) * rr, sy - 0.8 * s * t + Math.sin(a) * rr * 0.94]); } thread(ctx, sp, 0.6, "rgba(110,76,44,.55)"); };

  /* 하루가 쓰는 것: 정수리 기울기를 따르는 고깔 (겨울 귀마개는 hanji_cards.js) */
  function hatOn(ctx, V, pts, eyes, sc, seed) {
    var bb = bbox(pts), ex = eyes.reduce(function (a, e) { return a + e[0]; }, 0) / eyes.length, hx = Math.min(bb[0] + bb[2] * 0.78, ex + bb[2] * 0.16);
    var y1 = spanAt(pts, hx - 2)[0], y2 = spanAt(pts, hx + 2)[0], hy = spanAt(pts, hx)[0], ang = Math.atan2(y2 - y1, 4) + 0.22, hw = Math.max(5.5, bb[2] * 0.15), hh = hw * 2.5;
    ctx.save(); ctx.translate(hx, hy + 1.2); ctx.rotate(ang);
    var cone = resample([[-hw, 0], [0, -hh], [hw, 0]], 1.4); V.haru.body(ctx, cone, "#F4C7D2", seed, sc * 0.8);
    ctx.save(); multi(ctx, [cone]); ctx.clip(); ctx.fillStyle = "#9DC4E0"; for (var i = 0; i < 4; i++) { ctx.beginPath(); ctx.moveTo(-hw * 2, -hh * (0.12 + i * 0.25)); ctx.lineTo(hw * 2, -hh * (0.12 + i * 0.25) - hw * 0.9); ctx.lineTo(hw * 2, -hh * (0.12 + i * 0.25) - hw * 0.9 - hh * 0.09); ctx.lineTo(-hw * 2, -hh * (0.12 + i * 0.25) - hh * 0.09); ctx.closePath(); ctx.fill(); } ctx.restore();
    V.haru.body(ctx, cone, "rgba(0,0,0,0)", seed, sc * 0.8);
    var ruff = [], nr = 5; for (var k = 0; k <= nr * 8; k++) { var u = k / (nr * 8), xx = -hw * 1.12 + u * hw * 2.24; ruff.push([xx, -hw * 0.3 - Math.abs(Math.sin(u * nr * Math.PI)) * hw * 0.24]); } ruff.push([hw * 1.12, hw * 0.22]); ruff.push([-hw * 1.12, hw * 0.22]);
    V.haru.body(ctx, resample(ruff, 1), "#FBF8F0", seed + 9, sc * 0.6);
    V.haru.body(ctx, blob(0.4, -hh - hw * 0.25, hw * 0.36, hw * 0.36, seed + 20, 0.12), "#F2C04E", seed + 20, sc * 0.6);
    ctx.restore();
  }
