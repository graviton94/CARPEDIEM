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
    if (seed != null) drawHaruMix(ctx, traitsOf(seed), 195, gy, o.haruW || 96, { sprout: key === "spring" });
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
    if (seed != null) drawHaruMix(ctx, traitsOf(seed), 300, gy, 84, {});
  }
  function storeMix(ctx, W, H) {
    setU(W / 390); var h = H / U, gy = h * 0.86;
    paper(ctx, W, H, "#F6F1E6");
    wash(ctx, -60, -120, 510, h * 0.7, "#E3E8D2", { seed: 801, press: 0.4, smudge: 0.3, fade: "down" });
    groundLine(ctx, 14, 376, gy, 3.3, 803, [40, 180, 300]);
    drawObj(ctx, "cairn", 330, gy - 2, 0.75);
  }
  function themeMix(ctx, W, H, T) { theme(ctx, W, H, T); }
