  /* 앱으로 굽는 한지 정원 그림 (scripts/build_art.js 가 부름). 상자 크기 · 기준점은 tokens.json garden.decor 와 같아야 한다 (D = 그 값).
     하늘 · 땅은 실제 계절 넷, 나무는 종류 넷 × 계절 넷 × 자람 넷, 말뚝은 계절 옷 + 걸리는 것 따로 (흔들림은 앱이), 연은 몸 + 꼬리 (리본 색은 앱이). */
  function hj(ctx, u) { ensureTex(); setU(u); NIGHT = false; ctx.setTransform(u, 0, 0, u, 0, 0); }
  function skyApp(ctx, W, H, se) { hj(ctx, W / SW); var P = WPAL[se], h = H / U;
    var g = ctx.createLinearGradient(0, 0, 0, 620); g.addColorStop(0, P.sky); g.addColorStop(1, P.sky2); ctx.fillStyle = g; ctx.fillRect(0, 0, SW, h);
    texFill(ctx, TEX.fiber, 0.55, "source-over", 0.4); texFill(ctx, TEX.grain, 0.5, "multiply", 0.5);
    cloud(ctx, 46, 268, 0.85, P.cloud, 2500); cloud(ctx, 214, 330, 0.62, P.cloud, 2510); cloud(ctx, 356, 70, 0.8, P.cloud, 2520); }
  function stripApp(ctx, W, H, se, D) { hj(ctx, W / SW); var P = WPAL[se], gy = D.stripLineY;
    var far = ridgeS(gy - 62, 40, 330, 0.011), mid = ridgeS(gy - 24, 20, 340, 0.017), path = []; for (var x = -14; x <= SW + 14; x += 5) path.push([x, gy + 2 + Math.sin(x * 0.05) * 1.2]);
    var fr = ridgeS(gy + 66, 10, 370, 0.022), fr2 = ridgeS(gy + 128, 9, 372, 0.03), down2 = function (top) { return top.concat([[SW + 14, H / U + 30], [-14, H / U + 30]]); };
    [[far, mixHex(P.far, P.sky2, 0.55)], [mid, mixHex(P.mid, P.sky2, 0.25)], [path, P.path], [fr, P.front], [fr2, P.front2]].forEach(function (L, i) { HV.world(ctx, down2(L[0]), L[1], 530 + i, { kind: "layer", top: L[0] }); }); }
  function treeApp(ctx, W, sp, se, stage, D) { hj(ctx, W / D.treeBoxW); tree(ctx, D.treeAtX, D.treeAtY, 1, sp, se, stage, {}); }
  // 말뚝: base = 기둥 + 가로대 + 계절 옷, 나머지는 걸리는 것 하나씩 (같은 상자, 같은 자리)
  function postApp(ctx, W, part, se, D) { hj(ctx, W / D.postBoxW); var x = D.postAtX, y = D.postAtY, bar = y - 96;
    if (part === "base") post(ctx, x, y, 1, { season: se, chime: 0 });
    else if (part === "chime") chime(ctx, D.chimeX, D.chimeY, 1, 1, 0);
    else if (part === "bell") chime(ctx, D.bellX, D.bellY, 1, 0.62, 0);
    else if (part === "lantern" || part === "lantern_lit") lantern(ctx, D.lanternX, D.lanternY, 1, part === "lantern_lit");
    else if (part === "letter") { var lx = x - 6, ly = bar - 2; paper(ctx, place([[-6, -4], [6, -4], [6, 4], [-6, 4]], lx + 2, ly - 1, 1, -0.18), "#F6EEDC", 1650, { rim: 0.4, sh: 0.9 }); paper(ctx, place([[-6, -4], [6, -4], [0, 0.6]], lx + 2, ly - 1, 1, -0.18), "#EADFC6", 1651, { rim: 0.1, sh: 0.3 }); paper(ctx, blob(lx + 2.3, ly - 1.4, 1.5, 1.5, 1652, 0.05), "#C8553D", 1652, { rim: 0.15, sh: 0.3 }); } }
  function mossApp(ctx, W, se, buds, D) { hj(ctx, W / D.mossBoxW); moss(ctx, D.mossAtX, D.mossAtY, 1, se, buds); }
  function kiteApp(ctx, W, D) { hj(ctx, W / D.kiteBoxW); kite(ctx, D.kiteAtX, D.kiteAtY, 1, "gaori", 0); }
  function cardApp(ctx, W, key, D) { hj(ctx, W / D.cardBoxW); piece(ctx, D.cardBoxW / 2, D.cardAtY - 24, 1, key); }
  function sunApp(ctx, W) { hj(ctx, W / 60); sun(ctx, 30, 30, 1.2, "#F6C979"); }
  function moonApp(ctx, W) { hj(ctx, W / 60); moon(ctx, 30, 30, 1.2); }
  // 둥근 달 60 × 60, 가운데 (30, 30) 반지름 24 = 상자 반의 0.8 (Night.moonDisk)
  function moonFullApp(ctx, W) { hj(ctx, W / 60); moonFull(ctx, 30, 30, 24); }
  function fiberApp(ctx) { ensureTex(); ctx.drawImage(TEX.fiber, 0, 0); }
  // 우연한 순간 (움직임은 앱이): 무지개 160 × 90 (아래 가운데 80, 84) · 오로라 260 × 60 (가운데 130, 40) · 달팽이 40 × 26 (발 20, 20) · 나비 40 × 40 · 바람 조각 10 × 10
  function rainbowApp(ctx, W) { hj(ctx, W / 160); MOMENT.rainbow(ctx, 80, 84, 1.1); }
  function auroraApp(ctx, W) { hj(ctx, W / 260); MOMENT.aurora(ctx, 130, 40, 1); }
  function snailApp(ctx, W) { hj(ctx, W / 40); MOMENT.snail(ctx, 20, 20, 1); }
  function flyApp(ctx, W, side) { hj(ctx, W / 40); flySprite(ctx, side); }
  function windApp(ctx, W, se) { hj(ctx, W / 10); windPiece(ctx, se); }
  var TREES = ["cherry", "zelkova", "ginkgo", "pine"], REAL = ["spring", "summer", "autumn", "winter"];
  // 밤의 연: 반으로 접혀 말뚝 왼쪽에 기대 있음 (꼬리는 둘둘 감아 발치에). 말뚝과 같은 상자.
  function kiteFoldApp(ctx, W, D) { hj(ctx, W / D.postBoxW); var x = D.postAtX - 4, y = D.postAtY;
    ctx.save(); ctx.translate(x, y); ctx.rotate(-0.17); ctx.scale(-0.9, 0.9);
    paper(ctx, [[0, -42], [17, -22], [0, -1]], "#F6F0E2", 1861, { tone: 0.06 }); paper(ctx, [[0, -42], [17, -22], [0, -22]], "#D9776A", 1862, { rim: 0.3, sh: 0.3 });
    thread(ctx, [[0, -42], [0, -1]], 0.7, "rgba(90,70,50,.55)"); thread(ctx, [[0, -22], [17, -22]], 0.6, "rgba(90,70,50,.45)");
    var coil = []; for (var t = 0; t <= 1.0001; t += 0.04) { var a = t * Math.PI * 3.2, r = 5.5 - t * 3; coil.push([6 + Math.cos(a) * r, -4 + Math.sin(a) * r * 0.55]); }
    paper(ctx, rib(coil, 1.8, 1.2), "#F4EEDF", 1863, { rim: 0.35, sh: 0.6 }); ctx.restore(); }
  // 돌멍하기의 작은 연못 (봄 · 여름): 한지로 오린 물 한 장, 가장자리 조약돌 셋. 비침 · 물결은 앱이.
  function pondApp(ctx, W, D) { hj(ctx, W / D.pondBoxW); var cx = D.pondBoxW / 2, cy = D.pondBoxH / 2, rx = D.pondBoxW / 2 - 8, ry = D.pondBoxH / 2 - 7, pts = [];
    for (var i = 0; i < 64; i++) { var a = i / 64 * Math.PI * 2; pts.push([cx + Math.cos(a) * rx * (1 + 0.025 * Math.sin(i * 1.7) + 0.015 * Math.sin(i * 3.1)), cy + Math.sin(a) * ry * (1 + 0.05 * Math.sin(i * 2.3))]); }
    paper(ctx, pts, "#C2D5D6", 1870, { rim: 0.7, jit: 0.3, sh: 0.7, fiber: 0.7 });
    paper(ctx, blob(cx, cy + ry * 0.25, rx * 0.86, ry * 0.5, 1871, 0.06), "#D3E1DE", 1871, { rim: 0, sh: 0, fiber: 0.5 });
    [[cx - rx + 10, cy + ry - 4, 7], [cx - rx + 22, cy + ry + 1, 5], [cx + rx - 14, cy - ry + 6, 6]].forEach(function (q, j) { paper(ctx, blob(q[0], q[1], q[2], q[2] * 0.62, 1875 + j, 0.12), ["#A9A192", "#B7AF9F", "#9E978A"][j], 1875 + j, { rim: 0.5, sh: 0.8 }); }); }
  // 돌멍하기의 작은 화톳불 (가을 · 겨울): 엇갈린 장작 둘과 재. 불꽃 · 불티는 앱이.
  function logsApp(ctx, W, D) { hj(ctx, W / D.fireBoxW); var cx = D.fireBoxW / 2, cy = D.fireBoxH - 9;
    paper(ctx, blob(cx, cy + 3, 22, 3.6, 1880, 0.2), "#B8AFA2", 1880, { rim: 0.3, sh: 0.3 });
    [[-0.22, "#7E5E46", 1881], [0.22, "#8E6C50", 1882]].forEach(function (q) { var c = Math.cos(q[0]), s = Math.sin(q[0]); paper(ctx, rib([[cx - c * 20, cy - s * 20], [cx + c * 20, cy + s * 20]], 3.6, 3.2), q[1], q[2], { rim: 0.5, sh: 0.8, tone: 0.12 });
      paper(ctx, blob(cx + c * 20, cy + s * 20, 2.6, 3.2, q[2] + 10, 0.05), "#C9A57E", q[2] + 10, { rim: 0.2, sh: 0.2 }); }); }
  // 돌멍하기 하늘에 천천히 흘러가는 구름 (계절 넷 + 밤), 120 × 50 상자
  function cloudApp(ctx, W, key, i) { hj(ctx, W / 120); if (key === "night") NIGHT = true; var P = WPAL[key]; cloud(ctx, 48, 26, [1, 0.72][i], P.cloud, 2530 + i * 7); NIGHT = false; }
  // 정원 손님 (우연히 놀러 오는 날): 하루와 같은 그림체 = 매끈한 조약돌 몸 하나 + 귀 · 꼬리 정도, 굵은 먹선 (#33281F), 한지 결, 왼쪽 위 빛,
  // 하루와 같은 눈 (흰 눈 · 먹 테 · 까만 눈동자 · 반짝 점). 40 × 40 상자, 발 = (20, 37).
  var GINK = "#33281F", GLINE = 2.1, GEYE = 1.1;
  function gPath(ctx, pts) { var n = pts.length; ctx.beginPath(); for (var i = 0; i < n; i++) { var a = pts[i], b = pts[(i + 1) % n], mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2; i ? ctx.quadraticCurveTo(a[0], a[1], mx, my) : ctx.moveTo(mx, my); } var a0 = pts[0], b0 = pts[1]; ctx.quadraticCurveTo(a0[0], a0[1], (a0[0] + b0[0]) / 2, (a0[1] + b0[1]) / 2); ctx.closePath(); }
  function gOval(cx, cy, rx, ry, rot, squish) { var p = []; for (var i = 0; i < 16; i++) { var t = i / 16 * Math.PI * 2, x = Math.cos(t) * rx, y = Math.sin(t) * ry; if (squish && y > 0) y *= squish; var c = Math.cos(rot || 0), s = Math.sin(rot || 0); p.push([cx + x * c - y * s, cy + x * s + y * c]); } return p; }
  function gPiece(ctx, pts, col, o) { o = o || {}; ctx.save(); gPath(ctx, pts); ctx.fillStyle = col; ctx.fill(); ctx.clip();
    var pat = ctx.createPattern(TEX.fiber, "repeat"); if (pat.setTransform) pat.setTransform(new DOMMatrix().scale(0.12)); ctx.globalAlpha = 0.5; ctx.fillStyle = pat; ctx.fillRect(-5, -5, 50, 50); ctx.globalAlpha = 1;
    var xs = pts.map(function (q) { return q[0]; }), ys = pts.map(function (q) { return q[1]; }), l = Math.min.apply(0, xs), r = Math.max.apply(0, xs), t = Math.min.apply(0, ys), b = Math.max.apply(0, ys);
    var g = ctx.createLinearGradient(l, t, l + (r - l) * 0.5, b); g.addColorStop(0, "rgba(255,255,255,.22)"); g.addColorStop(0.5, "rgba(255,255,255,0)"); g.addColorStop(1, "rgba(0,0,0,.12)"); ctx.fillStyle = g; ctx.fillRect(l, t, r - l, b - t);
    if (o.inner) o.inner(ctx);
    ctx.restore(); if (o.line !== 0) { gPath(ctx, pts); ctx.strokeStyle = GINK; ctx.lineWidth = o.line || GLINE; ctx.lineJoin = "round"; ctx.stroke(); } }
  function gEye(ctx, x, y, r, lx) { ctx.save(); ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fillStyle = "#FFFFFF"; ctx.fill(); ctx.clip(); var pr = r * 0.56, px = x + (lx || 0.25) * (r - pr - r * 0.1), py = y + 0.2 * (r - pr);
    ctx.fillStyle = "#1E1915"; ctx.beginPath(); ctx.arc(px, py, pr, 0, 7); ctx.fill(); ctx.fillStyle = "#FFFFFF"; ctx.beginPath(); ctx.arc(px - pr * 0.36, py - pr * 0.38, pr * 0.28, 0, 7); ctx.fill(); ctx.restore();
    ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.strokeStyle = GINK; ctx.lineWidth = GEYE; ctx.stroke(); }
  function gShadow(ctx, cx, w) { ctx.save(); ctx.fillStyle = "rgba(51,40,31,.13)"; ctx.beginPath(); ctx.ellipse(cx, 37, w / 2, 1.5, 0, 0, 7); ctx.fill(); ctx.restore(); }
  function guestSprite(ctx, kind) { ensureTex();
    if (kind === "tit") {   // 곤줄박이: 동글한 밤색 몸, 위는 검은 모자 (흰 뺨), 작은 부리 · 꼬리
      gShadow(ctx, 20, 20);
      gPiece(ctx, [[8, 22], [2, 18.6], [2.6, 15.6], [9, 18.4]], "#6F7884", { line: 1.8 });
      var body = gOval(20, 24.5, 12, 11, 0, 0.92);
      gPiece(ctx, body, "#C98552", { inner: function (c) { c.fillStyle = "#3B3632"; c.beginPath(); c.ellipse(21, 15, 15, 8.6, 0, 0, 7); c.fill(); c.fillStyle = "#F3E9D6"; c.beginPath(); c.ellipse(24, 21, 6.8, 4.2, -0.15, 0, 7); c.fill(); } });
      gPiece(ctx, [[31.6, 19.4], [36.4, 21], [31.6, 22.6]], "#E3A247", { line: 1.3 });
      gEye(ctx, 21.4, 19.6, 3.5, 0.4); gEye(ctx, 28.4, 19.4, 3.1, 0.4); }
    else if (kind === "squirrel") {   // 다람쥐: 등 뒤 큰 꼬리 하나, 뾰족 귀 둘
      gShadow(ctx, 22, 22);
      gPiece(ctx, [[12, 33], [4, 30], [2, 20], [4, 10], [10, 5], [16, 7], [15, 13], [11, 15], [10, 22], [14, 28]], "#A8693D");
      gPiece(ctx, [[17, 14], [17.5, 6.5], [22.5, 12]], "#BD7D4B", { line: 1.8 }); gPiece(ctx, [[26, 12], [29.5, 5.5], [31, 14]], "#BD7D4B", { line: 1.8 });
      gPiece(ctx, gOval(24, 24, 11, 12.6, 0, 0.9), "#BD7D4B", { inner: function (c) { c.fillStyle = "#EBD2AC"; c.beginPath(); c.ellipse(25, 31, 6.4, 6, 0, 0, 7); c.fill(); } });
      gEye(ctx, 20.6, 20, 3.5, 0.3); gEye(ctx, 28, 19.8, 3.2, 0.3); }
    else if (kind === "hedgehog") {   // 고슴도치: 둥근 언덕 몸, 등에 짧은 가시 몇 줄 (먹선), 연한 얼굴 · 까만 코
      gShadow(ctx, 20, 28);
      gPiece(ctx, gOval(19, 27.5, 14.6, 12, 0, 0.62), "#8A6B4C", { inner: function (c) { c.strokeStyle = "rgba(51,40,31,.7)"; c.lineWidth = 1.1; c.lineCap = "round";
        [[8, 24], [12, 19.5], [16.5, 17.4], [21, 17.6], [11, 27.5], [15.5, 23], [20, 21.6], [7.5, 31], [14.5, 28.6]].forEach(function (q) { c.beginPath(); c.moveTo(q[0] + 1.8, q[1] + 1.6); c.lineTo(q[0], q[1]); c.lineTo(q[0] + 2.6, q[1] - 0.6); c.stroke(); });
        c.fillStyle = "#EBD6B4"; c.beginPath(); c.ellipse(29.4, 29, 7.8, 7.6, 0, 0, 7); c.fill(); } });
      ctx.fillStyle = GINK; ctx.beginPath(); ctx.arc(34.2, 30.6, 1.4, 0, 7); ctx.fill();
      gEye(ctx, 25.2, 26, 3, 0.4); gEye(ctx, 31.2, 25.8, 2.7, 0.4); }
    else if (kind === "rabbit") {   // 토끼: 흰 조약돌 몸, 긴 귀 둘 (안쪽 분홍)
      gShadow(ctx, 20, 22);
      [[15.6, 9, -0.16], [24.4, 9, 0.16]].forEach(function (e) { gPiece(ctx, gOval(e[0], e[1], 3.4, 8.4, e[2]), "#EEE8DC", { line: 1.8, inner: function (c) { c.fillStyle = "#EDB7B1"; c.beginPath(); c.ellipse(e[0], e[1] + 1, 1.5, 5.6, e[2], 0, 7); c.fill(); } }); });
      gPiece(ctx, gOval(20, 26, 12.4, 11, 0, 0.9), "#EEE8DC");
      ctx.fillStyle = "rgba(229,150,140,.45)"; ctx.beginPath(); ctx.ellipse(13.6, 29, 2.2, 1.1, 0, 0, 7); ctx.fill(); ctx.beginPath(); ctx.ellipse(26.4, 29, 2.2, 1.1, 0, 0, 7); ctx.fill();
      gEye(ctx, 16, 24.6, 3.4, 0.2); gEye(ctx, 24, 24.6, 3.4, 0.2); }
    else {   // 부엉이: 키 큰 조약돌 몸, 귀깃 둘, 연한 배, 큰 눈 · 작은 부리
      gShadow(ctx, 20, 20);
      gPiece(ctx, [[11.4, 12], [10.4, 4.4], [16, 9]], "#8E6E4B", { line: 1.8 }); gPiece(ctx, [[28.6, 12], [29.6, 4.4], [24, 9]], "#8E6E4B", { line: 1.8 });
      gPiece(ctx, gOval(20, 22, 11, 14.6, 0, 0.94), "#9A7853", { inner: function (c) { c.fillStyle = "#E6D1AE"; c.beginPath(); c.ellipse(20, 29, 6.6, 7, 0, 0, 7); c.fill(); } });
      gEye(ctx, 15.4, 17.4, 4.2, 0); gEye(ctx, 24.6, 17.4, 4.2, 0);
      gPiece(ctx, [[18.4, 21.4], [21.6, 21.4], [20, 24.6]], "#E3A247", { line: 1.1 }); } }
  function guestApp(ctx, W, kind) { hj(ctx, W / 40); guestSprite(ctx, kind); }
