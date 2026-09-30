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
