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
