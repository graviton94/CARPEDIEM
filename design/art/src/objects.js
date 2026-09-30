  /* ───────── 정원에 놓이는 것 12가지 (손그림) ─────────
     기준점 (x, y) = 땅에 닿는 가운데, s = 크기 배율. 기본 높이 약 100 단위. */
  function loc(x, y, s) {
    return {
      b: function (cx, cy, rx, ry, seed, wob, rot) { return blob(x + cx * s, y + cy * s, rx * s, ry * s, seed, wob, rot); },
      p: function (pts) { return pts.map(function (q) { return [x + q[0] * s, y + q[1] * s]; }); },
      c: function (arr) { var o = []; for (var i = 0; i < arr.length; i += 2) o.push(x + arr[i] * s, y + arr[i + 1] * s); return bez(o, 20); },
      n: function (v) { return v * s; }
    };
  }
  var OBJ = [
    { id: "cairn", ko: "쌓은 돌", when: "함께한 지 100일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x + L.n(4), y + L.n(1), L.n(46), L.n(6), "#3B3325", 0.3);
      pastel(c, L.b(0, -14, 36, 15, 11, 0.08), "#A9A293", { seed: 11, lw: L.n(1) });
      pastel(c, L.b(3, -38, 25, 11, 12, 0.1), "#BEB6A6", { seed: 12, lw: L.n(1) });
      pastel(c, L.b(-2, -55, 15, 8, 13, 0.1), "#CFC6B3", { seed: 13, lw: L.n(0.9) });
    } },
    { id: "moss", ko: "이끼 방석", when: "시작한 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(46), L.n(5), "#3B3325", 0.25);
      pastel(c, L.b(0, -9, 44, 12, 21, 0.14), "#8C9A5B", { seed: 21, ang: -0.3, lw: L.n(1) });
      pastel(c, L.b(-10, -15, 24, 5, 22, 0.1), "#A9B679", { seed: 22, line: false, shade: false, press: 0.45 });
    } },
    { id: "pine", ko: "작은 소나무", when: "함께한 지 1년", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(40), L.n(5), "#3B3325", 0.25);
      scribble(c, L.c([0, 0, -10, -22, 12, -44, -4, -70]), "#6E5238", 31, L.n(5.5), { press: 0.8 });
      scribble(c, L.c([2, -3, -7, -24, 14, -44, -2, -68]), "#4A3524", 32, L.n(1.3));
      scribble(c, L.c([2, -38, 14, -44, 22, -46, 30, -50]), "#6E5238", 36, L.n(3));
      [[-22, -52, 34, 11, "#50663F"], [26, -54, 26, 9, "#587048"], [-6, -74, 30, 10, "#5E7A4C"], [2, -92, 18, 7, "#6C8658"]].forEach(function (p, i) {
        pastel(c, L.b(p[0], p[1], p[2], p[3], 33 + i, 0.28), p[4], { seed: 33 + i, ang: -0.12, spread: 0.3, len: L.n(6), wid: L.n(1.6), lw: L.n(0.9) });
      });
    } },
    { id: "flower", ko: "들꽃 한 송이", when: "봄이 온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      scribble(c, L.c([0, 0, 5, -24, -4, -46, -2, -72]), "#6F8A4A", 41, L.n(1.8));
      pastel(c, L.b(8, -30, 12, 4, 42, 0.1, -0.5), "#86A05C", { seed: 42, lw: L.n(0.7) });
      pastel(c, L.b(-9, -44, 10, 3.5, 43, 0.1, 0.5), "#86A05C", { seed: 43, lw: L.n(0.7) });
      for (var i = 0; i < 5; i++) { var a = -Math.PI / 2 + i * Math.PI * 2 / 5; pastel(c, L.b(-2 + Math.cos(a) * 9, -76 + Math.sin(a) * 8, 8, 5, 44 + i, 0.12, a), "#F4ECD8", { seed: 44 + i, dark: 0.12, lw: L.n(0.7), lineA: 0.8 }); }
      pastel(c, L.b(-2, -76, 4, 4, 49, 0.1), "#D9A441", { seed: 49, lw: L.n(0.6) });
    } },
    { id: "pond", ko: "작은 물웅덩이", when: "여름이 온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      pastel(c, L.b(0, -6, 54, 12, 51, 0.1), "#9DB4B5", { seed: 51, ang: -0.04, spread: 0.2, light: 0.28, lw: L.n(0.9) });
      scribble(c, L.b(-10, -8, 16, 3.5, 52, 0.05).slice(0, 40), "#F6F4EC", 52, L.n(1.2), { alpha: 0.9 });
      pastel(c, L.b(22, -8, 11, 4, 53, 0.12), "#7F9A58", { seed: 53, lw: L.n(0.7) });
      [[-58, 0, -20, -1.4], [-53, -1, -14, 0.6], [56, -2, -12, 1.2]].forEach(function (g, i) { scribble(c, L.c([g[0], g[1], g[0] + g[3], g[1] + g[2] * 0.4, g[0] + g[3] * 2, g[1] + g[2] * 0.7, g[0] + g[3] * 4, g[1] + g[2]]), "#5E6B3A", 54 + i, L.n(0.9)); });
    } },
    { id: "teacup", ko: "따뜻한 차 한 잔", when: "함께한 지 7일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y, L.n(44), L.n(6), "#3B3325", 0.25);
      pastel(c, L.b(0, -4, 40, 6.5, 61, 0.05), "#E6DDCB", { seed: 61, lw: L.n(0.8) });
      scribble(c, L.c([22, -30, 38, -30, 38, -14, 20, -14]), INK, 62, L.n(1.6));
      pastel(c, organic([[-26, -34], [26, -34], [22, -16], [12, -7], [-12, -7], [-22, -16]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.8), 63, L.n(3)), "#F1EADB", { seed: 63, lw: L.n(1) });
      pastel(c, L.b(0, -33, 23, 4, 64, 0.05), "#8E6536", { seed: 64, shade: false, line: false, press: 0.8 });
      scribble(c, L.c([-4, -42, -10, -52, 2, -60, -6, -74]), "#B9B1A2", 65, L.n(1.1), { alpha: 0.8 });
      scribble(c, L.c([8, -42, 2, -54, 14, -62, 6, -78]), "#B9B1A2", 66, L.n(1.1), { alpha: 0.7 });
    } },
    { id: "candle", ko: "작은 촛불", when: "생일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y - L.n(58), L.n(40), L.n(40), "#F2B35A", 0.28);
      pastel(c, L.b(0, -6, 30, 8, 71, 0.1), "#A9A293", { seed: 71, lw: L.n(0.9) });
      pastel(c, organic([[-9, -10], [9, -10], [9, -50], [-9, -50]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.6), 72, L.n(3)), "#F2EAD8", { seed: 72, lw: L.n(0.9) });
      scribble(c, L.c([0, -50, 1, -53, -1, -55, 0, -57]), INK, 73, L.n(1));
      pastel(c, L.b(0, -64, 4.5, 8, 74, 0.1), "#F0A84A", { seed: 74, shade: false, press: 0.9, line: false, smudge: 0.8 });
      pastel(c, L.b(0, -62, 2, 4, 75, 0.1), "#FFE9B0", { seed: 75, shade: false, press: 0.95, line: false, smudge: 0.9 });
    } },
    { id: "leaf", ko: "떨어진 잎", when: "가을이 온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      var pts = []; for (var i = 0; i <= 40; i++) { var t = i / 40, a = t * Math.PI * 2, rx = Math.cos(a) * 30, ry = Math.sin(a) * 11 * (1 - 0.5 * Math.cos(a)); pts.push([rx, ry - 8]); }
      var P = organic(pts.map(function (q) { var cc = Math.cos(-0.25), ss = Math.sin(-0.25); return [x + (q[0] * cc - q[1] * ss) * s, y + (q[0] * ss + q[1] * cc) * s]; }), L.n(0.8), 81, L.n(3));
      smudge(c, x + L.n(3), y, L.n(32), L.n(4), "#3B3325", 0.22);
      pastel(c, P, "#C0773C", { seed: 81, ang: 0.2, lw: L.n(0.9) });
      scribble(c, L.c([-30, 0, -10, -6, 10, -10, 30, -16]), "#7A4424", 82, L.n(0.9));
      for (var k = 0; k < 4; k++) { var bx = -18 + k * 12; scribble(c, L.c([bx, -4 - k * 2.2, bx + 3, -10 - k * 2, bx + 6, -13 - k * 2, bx + 8, -16 - k * 2]), "#7A4424", 83 + k, L.n(0.6)); }
    } },
    { id: "feather", ko: "깃털 하나", when: "처음 문장을 넘긴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      var vane = []; for (var i = 0; i <= 40; i++) { var t = i / 40, a = t * Math.PI * 2; vane.push([Math.cos(a) * 38, Math.sin(a) * 8 * (1 + 0.3 * Math.cos(a))]); }
      var rot = -0.35, cc = Math.cos(rot), ss = Math.sin(rot);
      function T(q) { return [x + (q[0] * cc - q[1] * ss) * s, y - L.n(12) + (q[0] * ss + q[1] * cc) * s]; }
      smudge(c, x, y, L.n(38), L.n(4), "#3B3325", 0.18);
      pastel(c, organic(vane.map(T), L.n(0.6), 91, L.n(3)), "#DCD7CB", { seed: 91, ang: -0.9, dark: 0.18, lw: L.n(0.8) });
      scribble(c, [T([-46, 2]), T([-20, 0.5]), T([10, -0.5]), T([38, 0])], "#8A8174", 92, L.n(1.2));
      for (var k = 0; k < 8; k++) { var ux = -28 + k * 8; scribble(c, [T([ux, 0]), T([ux + 6, -6])], "#A39A8C", 93 + k, L.n(0.6), { alpha: 0.7 }); scribble(c, [T([ux, 0]), T([ux + 6, 6])], "#A39A8C", 103 + k, L.n(0.6), { alpha: 0.7 }); }
    } },
    { id: "snail", ko: "느린 달팽이", when: "오랜만에 돌아온 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(40), L.n(4), "#3B3325", 0.22);
      pastel(c, organic([[-30, -2], [30, -2], [40, -8], [38, -20], [30, -20], [26, -12], [-24, -10]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.8), 111, L.n(3)), "#BFB6A3", { seed: 111, lw: L.n(0.9) });
      scribble(c, L.c([32, -20, 31, -26, 30, -30, 28, -34]), INK, 112, L.n(0.9)); scribble(c, L.c([36, -20, 38, -26, 39, -30, 41, -33]), INK, 113, L.n(0.9));
      pastel(c, L.b(28, -35, 1.8, 1.8, 114, 0.05), "#3A332A", { line: false, shade: false, press: 0.9 }); pastel(c, L.b(41, -34, 1.8, 1.8, 115, 0.05), "#3A332A", { line: false, shade: false, press: 0.9 });
      pastel(c, L.b(-4, -26, 21, 19, 116, 0.06), "#C99B63", { seed: 116, lw: L.n(1) });
      var sp = []; for (var i = 0; i <= 60; i++) { var t = i / 60, a = t * Math.PI * 4.2, rr = lerp(15, 2, t); sp.push([x + (-4 + Math.cos(a) * rr) * s, y + (-26 + Math.sin(a) * rr * 0.92) * s]); }
      scribble(c, sp, "#7A5634", 117, L.n(1.1));
    } },
    { id: "dandelion", ko: "민들레 홀씨", when: "새해 첫날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      scribble(c, L.c([0, 0, 4, -26, -3, -54, 2, -80]), "#7E9453", 121, L.n(1.5));
      pastel(c, L.b(2, -82, 3, 3, 122, 0.1), "#B8AE8C", { line: false, press: 0.9 });
      for (var i = 0; i < 18; i++) { var a = i / 18 * Math.PI * 2, r0 = 19 + (i % 3) * 2; scribble(c, [[x + L.n(2), y - L.n(82)], [x + L.n(2 + Math.cos(a) * r0 * 0.6), y + L.n(-82 + Math.sin(a) * r0 * 0.6)], [x + L.n(2 + Math.cos(a) * r0), y + L.n(-82 + Math.sin(a) * r0)]], "#A8A194", 123 + i, L.n(0.5), { alpha: 0.75, passes: 1 }); pastel(c, L.b(2 + Math.cos(a) * r0, -82 + Math.sin(a) * r0, 2.2, 2.2, 150 + i, 0.2), "#EDE9DF", { line: false, shade: false, press: 0.8 }); }
    } },
    { id: "acorn", ko: "도토리", when: "인생의 계절이 바뀐 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x + L.n(2), y + L.n(1), L.n(22), L.n(4), "#3B3325", 0.25);
      pastel(c, L.b(0, -21, 15, 20, 131, 0.06), "#B07A45", { seed: 131, lw: L.n(0.9) });
      pastel(c, L.b(0, -39, 18, 8, 132, 0.08), "#7A5B3A", { seed: 132, lw: L.n(0.9) });
      scribble(c, L.c([0, -46, 1, -50, 3, -53, 6, -55]), "#4A3524", 133, L.n(1.8));
    } },
    // 오늘의 한 줄을 이어 쓴 날 (7 · 30 · 100일). 바람에 실어 보내는 것들
    { id: "pinwheel", ko: "바람개비", when: "한 줄을 7일 이어 쓴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      smudge(c, x, y + L.n(1), L.n(18), L.n(4), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, 1, -20, -1, -42, 0, -64]), "#8A6A48", 161, L.n(2.2), { press: 0.8 });
      var cx = 0, cy = -66, R = 24, cols = ["#E9A43A", "#9DB4B5", "#D98C7A", "#B8C98E"];
      for (var i = 0; i < 4; i++) { var a = i * Math.PI / 2 + 0.35;
        var tri = [[cx, cy], [cx + Math.cos(a) * R, cy + Math.sin(a) * R], [cx + Math.cos(a + 1.05) * R * 0.72, cy + Math.sin(a + 1.05) * R * 0.72]];
        pastel(c, organic(A(tri), L.n(0.5), 162 + i, L.n(2.5)), cols[i], { seed: 162 + i, lw: L.n(0.8) }); }
      pastel(c, L.b(cx, cy, 3.2, 3.2, 167, 0.05), "#6E5238", { seed: 167, lw: L.n(0.6) });
    } },
    { id: "paperboat", ko: "종이배", when: "한 줄을 30일 이어 쓴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      pastel(c, L.b(0, -4, 50, 9, 171, 0.1), "#9DB4B5", { seed: 171, ang: -0.04, spread: 0.2, light: 0.28, lw: L.n(0.8) });
      pastel(c, organic(A([[-30, -16], [30, -16], [20, -5], [-20, -5]]), L.n(0.6), 172, L.n(3)), "#F4EEDF", { seed: 172, lw: L.n(1) });
      pastel(c, organic(A([[-15, -16], [15, -16], [1, -42]]), L.n(0.6), 173, L.n(3)), "#EAE2CF", { seed: 173, lw: L.n(1) });
      scribble(c, L.c([1, -42, 1, -32, 0, -24, 0, -16]), "#B9B1A2", 174, L.n(0.8), { alpha: 0.8 });
      scribble(c, L.c([-40, -2, -30, -4, -20, -1, -10, -3]), "#F6F4EC", 175, L.n(1), { alpha: 0.9 });
    } },
    { id: "kite", ko: "하늘의 연", when: "한 줄을 100일 이어 쓴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      smudge(c, x - L.n(22), y + L.n(1), L.n(12), L.n(3), "#3B3325", 0.22);
      scribble(c, L.c([-22, 0, -21, -4, -22, -8, -21, -12]), "#6E5238", 181, L.n(2.2));
      scribble(c, L.c([-21, -12, -10, -40, 4, -58, 16, -70]), "#8A8174", 182, L.n(0.7), { alpha: 0.85, passes: 1 });
      pastel(c, organic(A([[20, -122], [38, -96], [16, -70], [0, -98]]), L.n(0.6), 183, L.n(3)), "#D98C7A", { seed: 183, lw: L.n(1) });
      scribble(c, [[x + L.n(20), y - L.n(122)], [x + L.n(16), y - L.n(70)]], INK, 184, L.n(0.7), { alpha: 0.7 });
      scribble(c, [[x + L.n(0), y - L.n(98)], [x + L.n(38), y - L.n(96)]], INK, 185, L.n(0.7), { alpha: 0.7 });
      scribble(c, L.c([16, -70, 24, -60, 12, -52, 22, -40]), "#8A8174", 186, L.n(0.7), { alpha: 0.85, passes: 1 });
      [[21, -60, "#E9A43A"], [15, -50, "#B8C98E"]].forEach(function (b, i) { pastel(c, L.b(b[0], b[1], 3.6, 2.2, 187 + i, 0.1, 0.4), b[2], { seed: 187 + i, lw: L.n(0.5) }); });
    } },
    // 처음 하루와 숨 쉰 날: 작은 풍경 (나무 막대에 매단 종 하나와 바람 종이)
    { id: "windchime", ko: "작은 풍경", when: "처음 하루와 숨 쉰 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(16), L.n(4), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, 1, -30, -1, -62, 0, -92]), "#8A6A48", 191, L.n(2.2), { press: 0.8 });
      scribble(c, L.c([0, -90, 8, -92, 16, -91, 24, -89]), "#8A6A48", 192, L.n(2), { press: 0.8 });
      scribble(c, L.c([22, -89, 22, -84, 22, -80, 22, -76]), "#8A8174", 193, L.n(0.8), { alpha: 0.9, passes: 1 });
      pastel(c, organic([[14, -64], [30, -64], [28, -76], [22, -79], [16, -76]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.5), 194, L.n(2.5)), "#9DB4B5", { seed: 194, lw: L.n(0.8) });
      scribble(c, L.c([22, -64, 22, -58, 22, -54, 22, -50]), "#8A8174", 195, L.n(0.7), { alpha: 0.9, passes: 1 });
      pastel(c, organic([[18, -50], [26, -50], [27, -34], [17, -34]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.5), 196, L.n(2.5)), "#F4EEDF", { seed: 196, lw: L.n(0.7) });
    } }
  ];
  function drawObj(c, id, x, y, s) { OBJ.filter(function (o) { return o.id === id; })[0].draw(c, x, y, s); }
