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
    else if (part === "letter") { var lx = x + 2, ly = bar - 2; paper(ctx, place([[-6, -4], [6, -4], [6, 4], [-6, 4]], lx + 2, ly - 1, 1, -0.18), "#F6EEDC", 1650, { rim: 0.4, sh: 0.9 }); paper(ctx, place([[-6, -4], [6, -4], [0, 0.6]], lx + 2, ly - 1, 1, -0.18), "#EADFC6", 1651, { rim: 0.1, sh: 0.3 }); paper(ctx, blob(lx + 2.3, ly - 1.4, 1.5, 1.5, 1652, 0.05), "#C8553D", 1652, { rim: 0.15, sh: 0.3 }); } }
  function mossApp(ctx, W, se, buds, D) { hj(ctx, W / D.mossBoxW); moss(ctx, D.mossAtX, D.mossAtY, 1, se, buds); }
  function kiteApp(ctx, W, D) { hj(ctx, W / D.kiteBoxW); kite(ctx, D.kiteAtX, D.kiteAtY, 1, "gaori", 0); }
  function cardApp(ctx, W, key, D) { hj(ctx, W / D.cardBoxW); piece(ctx, D.cardBoxW / 2, D.cardAtY - 24, 1, key); }
  function sunApp(ctx, W) { hj(ctx, W / 60); sun(ctx, 30, 30, 1.2, "#F6C979"); }
  function moonApp(ctx, W) { hj(ctx, W / 60); moon(ctx, 30, 30, 1.2); }
  function fiberApp(ctx) { ensureTex(); ctx.drawImage(TEX.fiber, 0, 0); }
  // 우연한 순간 (움직임은 앱이): 무지개 140 × 70 (아래 가운데 70, 66) · 오로라 260 × 60 (가운데 130, 40) · 달팽이 40 × 24 (발 20, 20) · 나비 40 × 40 · 바람 조각 10 × 10
  function rainbowApp(ctx, W) { hj(ctx, W / 140); MOMENT.rainbow(ctx, 70, 66, 1.1); }
  function auroraApp(ctx, W) { hj(ctx, W / 260); MOMENT.aurora(ctx, 130, 40, 1); }
  function snailApp(ctx, W) { hj(ctx, W / 40); MOMENT.snail(ctx, 20, 20, 1); }
  function flyApp(ctx, W, side) { hj(ctx, W / 40); flySprite(ctx, side); }
  function windApp(ctx, W, se) { hj(ctx, W / 10); windPiece(ctx, se); }
  var TREES = ["cherry", "zelkova", "ginkgo", "pine"], REAL = ["spring", "summer", "autumn", "winter"];
