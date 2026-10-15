  /* 모은 것 (1.1.6): 스물넷 절기 한 장 · 열두 달 별자리 한 장. 계절 한 장과 같은 한지 조각 · 같은 자리 (가운데 0, 땅 y 24).
     절기는 그 절기를 알리는 것 하나 (얼음 · 매화 · 개구리 · 해 · 국화 · 팥죽 …), 색은 하나 (많아야 둘).
     별자리는 남빛 한지 한 장 위에 크림빛 별 조각 · 실 같은 선 (별 자리는 data/constellations.csv, CONSTEL 로 받음). */
  function flake(cx, cy, r, w) { var out = []; for (var i = 0; i < 6; i++) { var a = -Math.PI / 2 + i * Math.PI / 3, c = Math.cos(a), s = Math.sin(a);
      out.push(rib([[cx, cy], [cx + c * r, cy + s * r]], w, w * 0.6));
      [0.55].forEach(function (t) { var bx = cx + c * r * t, by = cy + s * r * t; [-1, 1].forEach(function (d) { var b = a + d * 0.75; out.push(rib([[bx, by], [bx + Math.cos(b) * r * 0.32, by + Math.sin(b) * r * 0.32]], w * 0.8, w * 0.5)); }); }); }
    return out; }
  function drop(cx, cy, r) { return [[cx, cy - r * 1.7]].concat(bez([cx, cy - r * 1.7, cx + r * 0.9, cy - r * 0.5, cx + r * 1.05, cy + r * 0.2, cx + r * 0.6, cy + r * 0.75], 6), bez([cx + r * 0.6, cy + r * 0.75, cx, cy + r * 1.2, cx - r * 0.6, cy + r * 0.75, cx - r * 0.6, cy + r * 0.75], 6), bez([cx - r * 0.6, cy + r * 0.75, cx - r * 1.05, cy + r * 0.2, cx - r * 0.9, cy - r * 0.5, cx, cy - r * 1.7], 6)); }
  function disc(cx, cy, r, from, to) { var p = [[cx, cy]]; for (var i = 0; i <= 24; i++) { var a = from + (to - from) * i / 24; p.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]); } return p; }
  function twig(ctx, pts, w0, w1, col) { one(ctx, rib(bez(pts, 10), w0, w1), col || "#7A5A44", 6001, { rim: 0.25, sh: 0.45 }); }
  function grassBlade(x, len, lean, w) { return rib(bez([x, 24, x + lean * 0.2, 24 - len * 0.4, x + lean * 0.7, 24 - len * 0.8, x + lean, 24 - len], 10), w, 0.3); }
  var TERM = {
    // 겨울 끝 · 봄
    sohan: function (ctx) { one(ctx, flake(0, 6, 13, 1.6), "#CFE3EE", 6101, { rim: 0.4 }); one(ctx, blob(0, 6, 2.4, 2.4, 6102, 0.05), "#E9F3F8", 6102, { rim: 0, sh: 0.1 }); },
    daehan: function (ctx) { twig(ctx, [-15, -6, -6, -9, 4, -7, 15, -10], 2.4, 1.2);
      one(ctx, [[-9, -7.4], [-6.4, -7.6], [-7.6, 12]], "#D7E8F1", 6111, { rim: 0.3 }); one(ctx, [[-2, -7.8], [1.4, -7.8], [-0.2, 20]], "#D7E8F1", 6112, { rim: 0.3 }); one(ctx, [[6, -8], [8.6, -8.2], [7.4, 6]], "#D7E8F1", 6113, { rim: 0.3 }); },
    ipchun: function (ctx) { twig(ctx, [-14, 22, -8, 10, 0, 2, 12, -10], 2.2, 1); twig(ctx, [-5, 7, -10, 2, -12, -4, -12, -8], 1.2, 0.7);
      one(ctx, petal5(5, -2, 6.4, 0.2), "#F6E4E6", 6121, { rim: 0.5, light: 0.9 }); one(ctx, blob(5, -2, 1.3, 1.3, 6122, 0.05), "#D8687A", 6122, { rim: 0, sh: 0.1 });
      one(ctx, petal5(-11, -9, 4.4, 0.6), "#F6E4E6", 6123, { rim: 0.5, light: 0.9 }); one(ctx, blob(-11, -9, 1, 1, 6124, 0.05), "#D8687A", 6124, { rim: 0, sh: 0.1 });
      dot(ctx, 12.4, -10.6, 1.3, "#C8553D"); },
    usu: function (ctx) { one(ctx, drop(0, 6, 8), "#A9CFE0", 6131, { tone: 0.12 }); one(ctx, blob(0, 22.6, 12, 1.8, 6132, 0.04), "#C7DFEA", 6132, { rim: 0.2, sh: 0.2 });
      one(ctx, blob(-2.6, 2.4, 1.4, 3, 6133, 0.1, -0.4), "#E6F2F7", 6133, { rim: 0, sh: 0 }); },
    gyeongchip: function (ctx) { one(ctx, blob(0, 15, 12, 8.6, 6141, 0.05), "#8DB46A", 6141, { tone: 0.12 }); one(ctx, [blob(-5.4, 6.6, 3.4, 3.2, 6142, 0.05), blob(5.4, 6.6, 3.4, 3.2, 6143, 0.05)], "#8DB46A", 6142);
      dot(ctx, -5.4, 6.4, 1.2, "#2E2A24"); dot(ctx, 5.4, 6.4, 1.2, "#2E2A24"); thread(ctx, bez([-4, 15, -2, 17, 2, 17, 4, 15]), 0.6, "rgba(46,42,36,.7)");
      one(ctx, [blob(-11, 22, 3.4, 1.6, 6144, 0.1), blob(11, 22, 3.4, 1.6, 6145, 0.1)], "#7CA65C", 6144, { rim: 0.2 }); },
    chunbun: function (ctx) { one(ctx, disc(0, 6, 13, -Math.PI / 2, Math.PI / 2), "#F3C46A", 6151, { rim: 0.4 }); one(ctx, disc(0, 6, 13, Math.PI / 2, Math.PI * 1.5), "#5E6A8E", 6152, { rim: 0.4 });
      dot(ctx, -6, 2, 0.9, "#F6EAC4"); dot(ctx, -4, 11, 0.7, "#F6EAC4"); },
    cheongmyeong: function (ctx) { twig(ctx, [-14, -10, -4, -12, 6, -11, 15, -13], 1.8, 1);
      [[-10, -10.6, 26], [-3, -11.6, 32], [4, -11.2, 28], [10, -12, 22]].forEach(function (q, i) { var x = q[0], y = q[1], L = q[2]; thread(ctx, bez([x, y, x - 1, y + L * 0.4, x + 1.2, y + L * 0.7, x, y + L]), 0.7, "#7E9E5A");
        for (var k = 1; k <= 3; k++) one(ctx, place(leafPts(5, 1.4), x + (k % 2 ? 0.4 : -0.4), y + L * k / 3.4, 1, Math.PI / 2 + (k % 2 ? -0.5 : 0.5)), "#A9C77E", 6161 + i * 3 + k, { rim: 0.2, sh: 0.3 }); }); },
    gogu: function (ctx) { stem(ctx, [0, 24, 0.4, 19, -0.4, 14, 0, 10]); one(ctx, [place(leafPts(10, 4), 0, 11, 1, -2.5), place(leafPts(10, 4), 0, 11, 1, -0.64)], "#8DB46A", 6171, { rim: 0.4 });
      one(ctx, drop(-10, -6, 2.2), "#A9CFE0", 6172, { rim: 0.2 }); one(ctx, drop(9, -10, 2.6), "#A9CFE0", 6173, { rim: 0.2 }); one(ctx, drop(3, -16, 1.8), "#A9CFE0", 6174, { rim: 0.2 }); },
    // 여름
    ipha: function (ctx) { var p = disc(0, 12, 14, -Math.PI / 2 + 0.22, Math.PI * 1.5 - 0.22); one(ctx, place(p, 0, 0, 1, 0).map(function (q) { return [q[0], 12 + (q[1] - 12) * 0.62]; }), "#7FAE5E", 6181, { tone: 0.1 });
      for (var i = 0; i < 6; i++) { var a = -Math.PI / 2 + 0.5 + i * 0.9; thread(ctx, [[0, 12], [Math.cos(a) * 11, 12 + Math.sin(a) * 11 * 0.62]], 0.45, "rgba(80,110,60,.6)"); }
      stem(ctx, [6, 12, 7, 4, 8, -2, 8, -6]); one(ctx, place(leafPts(9, 3.4), 8, -5, 1, -Math.PI / 2), "#EFA3B4", 6182, { rim: 0.4 }); },
    soman: function (ctx) { stem(ctx, [0, 24, 0.6, 14, -0.6, 6, 0, -2]); one(ctx, place(leafPts(11, 2.2), 0, 14, 1, -0.5), "#8DB46A", 6191, { rim: 0.2 });
      var g = []; for (var i = 0; i < 6; i++) { g.push(place(leafPts(4.6, 1.8), -0.6, -2 - i * 2.6, 1, -Math.PI / 2 - 0.5)); g.push(place(leafPts(4.6, 1.8), 0.6, -2 - i * 2.6, 1, -Math.PI / 2 + 0.5)); }
      one(ctx, g, "#C9C46A", 6192, { rim: 0.3 }); for (var j = 0; j < 4; j++) thread(ctx, [[0, -14 - j * 1.4], [(j % 2 ? 4 : -4), -22 - j * 1.6]], 0.35, "rgba(150,140,70,.7)"); },
    mangjong: function (ctx) { one(ctx, blob(0, 22.6, 15, 2, 6201, 0.03), "#A9CFE0", 6201, { rim: 0.3, sh: 0.2 });
      one(ctx, [grassBlade(-3, 22, -9, 1.6), grassBlade(-1, 28, -3, 1.6), grassBlade(1, 26, 4, 1.6), grassBlade(3, 20, 10, 1.6), grassBlade(0, 16, -14, 1.4)], "#7FAE5E", 6202, { rim: 0.3 }); },
    haji: function (ctx) { var rays = []; for (var i = 0; i < 12; i++) rays.push(place(leafPts(6, 2), Math.cos(i * Math.PI / 6) * 11, 6 + Math.sin(i * Math.PI / 6) * 11, 1, i * Math.PI / 6));
      one(ctx, rays, "#F3C46A", 6211, { rim: 0.4 }); one(ctx, blob(0, 6, 9, 9, 6212, 0.03), "#F2B44E", 6212, { tone: 0.14 }); },
    soseo: function (ctx) { one(ctx, rib([[0, 24], [0, 8]], 2.2, 1.8), "#9A7A5E", 6221, { rim: 0.2 }); one(ctx, blob(0, -1, 12.4, 11.6, 6222, 0.03), "#BFD8C8", 6222, { tone: 0.1 });
      for (var i = 0; i < 7; i++) { var a = -Math.PI / 2 - 1.2 + i * 0.4; thread(ctx, [[0, 8], [Math.cos(a) * 11, -1 + Math.sin(a) * 10.4 + 9 * 0]], 0.4, "rgba(90,120,100,.45)"); }
      one(ctx, petal5(-3, -3, 4, 0.3), "#EFA3B4", 6223, { rim: 0.2, sh: 0.2 }); },
    daeseo: function (ctx) { stem(ctx, [0, 24, 0.5, 16, -0.5, 10, 0, 6]); one(ctx, petals(0, 2, 6, 11, 4.2, -Math.PI / 2 - Math.PI / 6), "#F2B6C4", 6231, { rim: 0.45 });
      one(ctx, petals(0, 2, 3, 8, 3.6, -Math.PI / 2), "#E68BA2", 6232, { rim: 0.3 }); one(ctx, blob(0, 2.6, 2.2, 1.8, 6233, 0.05), "#F2D27A", 6233, { rim: 0, sh: 0.1 }); },
    // 가을
    ipchu: function (ctx) { one(ctx, [place(leafPts(14, 3), 0, 4, 1, -2.95), place(leafPts(14, 3), 0, 4, 1, -0.19), place(leafPts(12, 2.6), 0, 7, 1, 2.8), place(leafPts(12, 2.6), 0, 7, 1, 0.34)], "#DCEAF0", 6241, { rim: 0.3, light: 0.95 });
      one(ctx, rib([[0, -1], [0, 21]], 2.6, 1.2), "#C8553D", 6242, { rim: 0.2 }); one(ctx, [blob(-1.6, -2, 1.6, 1.6, 6243, 0.05), blob(1.6, -2, 1.6, 1.6, 6244, 0.05)], "#7A3A2A", 6243, { rim: 0, sh: 0.1 }); },
    cheoseo: function (ctx) { var outer = disc(0, 0, 9, -Math.PI / 2, Math.PI / 2), inner = disc(-4, 0, 8, Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * 0);
      one(ctx, [[0, -9]].concat(bez([0, -9, 7, -8, 10, 0, 0, 9], 10), bez([0, 9, 5, 4, 5, -4, 0, -9], 10)), "#F2E2A8", 6251, { rim: 0.4, light: 0.9 });
      one(ctx, [grassBlade(-6, 18, -6, 1.4), grassBlade(-3, 24, 2, 1.4), grassBlade(2, 14, 8, 1.4)], "#9DB87E", 6252, { rim: 0.2 }); },
    baengno: function (ctx) { one(ctx, [grassBlade(-4, 32, -6, 2), grassBlade(1, 26, 10, 1.8), grassBlade(-1, 20, -14, 1.6)], "#8DB46A", 6261, { rim: 0.3 });
      one(ctx, [blob(-8.6, -6, 1.8, 1.9, 6262, 0.05), blob(8.4, 1.6, 1.6, 1.7, 6263, 0.05), blob(-11, 8, 1.4, 1.5, 6264, 0.05)], "#F4F8FA", 6262, { rim: 0.3, light: 0.98 }); },
    chubun: function (ctx) { stem(ctx, [-2, 24, -1, 12, 1, 2, 6, -6], "#B59A5A"); one(ctx, place(leafPts(13, 2.4), -1.4, 15, 1, -2.4), "#B9A35E", 6280, { rim: 0.2 });
      // 익어 고개 숙인 이삭: 줄기 끝에서 아래로 휘는 길을 따라 낟알이 붙음
      var path = bez([6, -6, 11, -7, 13, -2, 13, 6], 9), g = [];
      path.forEach(function (q, i) { if (i === 0) return; var a = Math.atan2(q[1] - path[i - 1][1], q[0] - path[i - 1][0]); g.push(place(leafPts(3.6, 1.5), q[0], q[1], 1, a + 0.9)); g.push(place(leafPts(3.6, 1.5), q[0], q[1], 1, a - 0.9)); });
      thread(ctx, path, 0.7, "#B59A5A"); one(ctx, g, "#E6B84E", 6271, { rim: 0.3 }); },
    hallo: function (ctx) { stem(ctx, [0, 24, -1, 16, 1, 10, 0, 5]); one(ctx, place(leafPts(8, 3.4), 0, 16, 1, -0.5), "#7E9E5A", 6291, { rim: 0.3 });
      one(ctx, petals(0, 0, 14, 10, 1.6, 0), "#EFC447", 6292, { rim: 0.35 }); one(ctx, petals(0, 0, 10, 6, 1.4, 0.3), "#E3A93A", 6293, { rim: 0.2 }); one(ctx, blob(0, 0, 2, 2, 6294, 0.05), "#C8862E", 6294, { rim: 0, sh: 0.1 }); },
    sanggang: function (ctx) { one(ctx, maple(0, 6, 14, 0.2), "#C8553D", 6301); thread(ctx, [[0, 12], [1.6, 23]], 0.9, "#8A3E2A");
      var r = R(6302); for (var i = 0; i < 14; i++) dot(ctx, -9 + r() * 18, -6 + r() * 18, 0.35 + r() * 0.45, "rgba(250,252,255,.9)"); },
    // 겨울
    ipdong: function (ctx) { one(ctx, blob(0, 13, 11.6, 10.4, 6311, 0.04), "#E8893A", 6311, { tone: 0.14 }); one(ctx, [0, 1, 2, 3].map(function (k) { return place(leafPts(5.6, 2.2), 0, 3.4, 1, -Math.PI / 4 + k * Math.PI / 2); }), "#6E7A40", 6312, { rim: 0.25 });
      one(ctx, rib([[0, 3.4], [0.8, -2.6]], 1.2, 1), "#5A4A34", 6313, { rim: 0 }); one(ctx, blob(-4.4, 9, 1.8, 3.2, 6314, 0.1, 0.5), "#F4B27A", 6314, { rim: 0, sh: 0 }); },
    soseol: function (ctx) { twig(ctx, [-12, 24, -8, 12, -2, 2, 8, -8], 1.8, 0.9); twig(ctx, [-4, 6, 2, 6, 6, 9, 10, 8], 1, 0.6);
      [[6, -14, 3.4], [-8, -6, 2.6], [11, 2, 2.2], [-2, 15, 2]].forEach(function (q, i) { one(ctx, flake(q[0], q[1], q[2], 0.8), "#EEF4F8", 6321 + i, { rim: 0.2, sh: 0.3, light: 0.98 }); }); },
    daeseol: function (ctx) { one(ctx, rib([[0, 24], [0, 16]], 2.6, 2.2), "#8A6E54", 6330, { rim: 0.2 });
      // 눈 덮인 작은 소나무: 세 층 · 층마다 위에 눈
      [[17, 13], [8.4, 10.4], [0.4, 7.6]].forEach(function (t, i) { var y = t[0], w = t[1];
        one(ctx, [[-w, y], [w, y], [w * 0.2, y - 9], [-w * 0.2, y - 9]], "#5E7E62", 6331 + i, { rim: 0.3 });
        one(ctx, [[-w * 0.78, y - 3.6]].concat(bez([-w * 0.78, y - 3.6, -w * 0.4, y - 9.8, w * 0.4, y - 9.8, w * 0.78, y - 3.6], 8), bez([w * 0.78, y - 3.6, w * 0.3, y - 2.2, -w * 0.3, y - 4.4, -w * 0.78, y - 3.6], 8)), "#FBFAF6", 6335 + i, { rim: 0.45, light: 0.98 }); });
      one(ctx, blob(0, 23, 13, 2, 6339, 0.04), "#FBFAF6", 6339, { rim: 0.4, light: 0.98 }); },
    dongji: function (ctx) { one(ctx, [[-14, 8]].concat(bez([-14, 8, -13, 19, -6, 22.6, 0, 22.6], 10), bez([0, 22.6, 6, 22.6, 13, 19, 14, 8], 10)), "#E4DACA", 6341, { rim: 0.5, light: 0.85 });
      one(ctx, blob(0, 8.4, 13, 2.6, 6342, 0.03), "#8E3A34", 6342, { rim: 0, sh: 0.1 });
      one(ctx, [blob(-5, 7.6, 2, 1.6, 6343, 0.06), blob(1, 8.6, 2, 1.6, 6344, 0.06), blob(5.6, 7.4, 1.8, 1.5, 6345, 0.06)], "#FBF3E4", 6343, { rim: 0.2, sh: 0.2, light: 0.95 });
      thread(ctx, bez([-3, 3, -5, -1, -1, -4, -3, -8]), 0.6, "rgba(120,104,88,.45)"); thread(ctx, bez([3, 3, 1, -1, 5, -4, 3, -8]), 0.6, "rgba(120,104,88,.45)"); }
  };
  Object.keys(TERM).forEach(function (k) { PIECE["term_" + k] = TERM[k]; });

  // 별자리 한 장: 남빛 한지 (네모, 살짝 기움) 위에 크림빛 별 조각과 실 같은 선. 별이 많아도 칸 안에 들게 줄여 맞춤
  function starChart(ctx, m) {
    var c = (typeof CONSTEL !== "undefined" && CONSTEL[m]) || { stars: [[0.5, 0.5]], lines: [] };
    var S = 26, x0 = -S / 2, y0 = 22 - S - 2;
    one(ctx, place([[-S / 2, -S / 2], [S / 2, -S / 2 - 0.6], [S / 2 + 0.4, S / 2], [-S / 2 - 0.3, S / 2 + 0.5]], 0, y0 + S / 2, 1, -0.05), "#3E4A6E", 6400 + m, { rim: 0.7, light: 0.55 });
    var xs = c.stars.map(function (p) { return p[0]; }), ys = c.stars.map(function (p) { return p[1]; }), mx = Math.min.apply(null, xs), Mx = Math.max.apply(null, xs), my = Math.min.apply(null, ys), My = Math.max.apply(null, ys);
    var span = Math.max(Mx - mx, My - my, 0.01), k = (S - 8) / span, ox = -((mx + Mx) / 2) * k, oy = y0 + S / 2 - ((my + My) / 2) * k;
    function at(i) { return [ox + c.stars[i][0] * k, oy + c.stars[i][1] * k]; }
    (c.lines.length ? c.lines : c.stars.slice(1).map(function (_, i) { return [i, i + 1]; })).forEach(function (l) { thread(ctx, [at(l[0]), at(l[1])], 0.45, "rgba(246,234,196,.7)"); });
    one(ctx, c.stars.map(function (_, i) { var p = at(i); return star4(p[0], p[1], i % 3 === 0 ? 2.2 : 1.6); }), "#F6E2A0", 6420 + m, { rim: 0.2, sh: 0.3, light: 0.9 });
  }
  for (var mm = 1; mm <= 12; mm++) (function (m) { PIECE["stars_" + m] = function (ctx) { starChart(ctx, m); }; })(mm);
