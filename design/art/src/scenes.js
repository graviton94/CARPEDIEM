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
