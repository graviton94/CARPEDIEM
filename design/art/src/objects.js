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
      smudge(c, x + L.n(2), y + L.n(1), L.n(26), L.n(4), "#3B3325", 0.25);
      [[0, -9, 22, 9, "#A9A293"], [2, -25, 17, 7.5, "#BEB6A6"], [-1, -38, 12.5, 6, "#CFC6B3"], [1, -48, 8, 4.5, "#B5AE9E"]].forEach(function (p, i) {
        pastel(c, L.b(p[0], p[1], p[2], p[3], 11 + i, 0.1), p[4], { seed: 11 + i, lw: L.n(0.9) }); });
      // 맨 위에 작은 새싹 하나
      scribble(c, L.c([1, -52, 2, -58, 0, -63, 1, -68]), "#6F8A4A", 15, L.n(1.3));
      pastel(c, L.b(7, -66, 6, 2.6, 16, 0.1, -0.5), "#86A05C", { seed: 16, lw: L.n(0.6) });
      pastel(c, L.b(-5, -63, 5, 2.2, 17, 0.1, 0.5), "#86A05C", { seed: 17, lw: L.n(0.6) });
    } },
    { id: "moss", ko: "이끼 방석", when: "시작한 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      // 하루가 앉는 방석: 몽글몽글한 이끼 언덕 (윗선이 작은 구름처럼), 사이사이 가는 싹과 꽃봉오리
      smudge(c, x, y + L.n(1), L.n(52), L.n(4), "#3B3325", 0.2);
      var top = [], n = 7; for (var i = 0; i <= n * 10; i++) { var t = i / (n * 10), xx = -50 + 100 * t, env = Math.sin(t * Math.PI), bump = Math.abs(Math.sin(t * Math.PI * n)) * 1.6 * env; top.push([xx, -1 - 9 * Math.pow(env, 0.7) - bump]); }
      var shape = top.concat([[46, 0], [-46, 0]]).map(function (q) { return [x + q[0] * s, y + q[1] * s]; });
      pastel(c, organic(shape, L.n(0.4), 21, L.n(2)), "#93A862", { seed: 21, ang: -0.3, lw: L.n(0.9) });
      for (var k = 0; k < 12; k++) { var t = (k + 0.5) / 12, r = (k * 37 % 100) / 100; pastel(c, L.b(-42 + 84 * t, -3 - 6 * Math.sin(t * Math.PI) * r, 1.4, 1.1, 22 + k, 0.1), "#B8C98E", { line: false, shade: false, press: 0.7 }); }
      [[-30, 7, -6, "#F4ECD8"], [26, 9, -8, "#E9A3B4"]].forEach(function (q, i) {
        scribble(c, L.c([q[0], q[2], q[0] + 1, q[2] - q[1] * 0.4, q[0] - 1, q[2] - q[1] * 0.7, q[0] + 0.5, q[2] - q[1]]), "#6F8A4A", 40 + i, L.n(0.9));
        pastel(c, L.b(q[0] + 0.5, q[2] - 1 - q[1], 1.8, 2, 45 + i, 0.1), q[3], { seed: 45 + i, lw: L.n(0.5) }); });
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
      pastel(c, L.b(4, -5, 40, 8, 51, 0.1), "#9DB4B5", { seed: 51, ang: -0.04, spread: 0.2, light: 0.28, lw: L.n(0.9) });
      scribble(c, L.b(0, -7, 12, 2.5, 52, 0.05).slice(0, 40), "#F6F4EC", 52, L.n(1.1), { alpha: 0.9 });
      pastel(c, L.b(26, -7, 8, 3, 53, 0.12), "#7F9A58", { seed: 53, lw: L.n(0.7) });
      // 웅덩이 곁 부들 두 줄기
      [[-30, -76, -34], [-22, -60, -19]].forEach(function (r, i) {
        scribble(c, L.c([r[0], -3, r[0] - 1, r[1] * 0.4, r[2] + 1, r[1] * 0.75, r[2], r[1]]), "#6F8A4A", 54 + i, L.n(1.4));
        pastel(c, L.b(r[2], r[1] + 8, 2.8, 8, 56 + i, 0.08), "#8A5E38", { seed: 56 + i, lw: L.n(0.7) });
        scribble(c, L.c([r[2], r[1] - 1, r[2], r[1] - 3, r[2] + 0.3, r[1] - 5, r[2], r[1] - 7]), "#6F8A4A", 58 + i, L.n(0.7)); });
      scribble(c, L.c([-26, -3, -14, -20, -10, -34, -6, -40]), "#86A05C", 60, L.n(1));
    } },
    { id: "teacup", ko: "따뜻한 차 한 잔", when: "함께한 지 7일", draw: function (c, x, y, s) { var L = loc(x, y, s);
      // 가는 나무 받침 위의 작은 찻잔, 길게 피어오르는 김 두 줄기
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      smudge(c, x, y + L.n(1), L.n(18), L.n(4), "#3B3325", 0.22);
      scribble(c, L.c([-7, 0, -6, -10, -6, -20, -5, -30]), "#8A6A48", 61, L.n(1.8), { press: 0.8 });
      scribble(c, L.c([7, 0, 6, -10, 6, -20, 5, -30]), "#8A6A48", 62, L.n(1.8), { press: 0.8 });
      scribble(c, L.c([-12, -31, -4, -32, 4, -32, 12, -31]), "#8A6A48", 63, L.n(2.2), { press: 0.8 });
      pastel(c, organic(A([[-9, -46], [9, -46], [7, -37], [3, -33], [-3, -33], [-7, -37]]), L.n(0.4), 64, L.n(2)), "#F1EADB", { seed: 64, lw: L.n(0.9) });
      scribble(c, L.c([8, -44, 14, -44, 14, -38, 7, -38]), INK, 65, L.n(1));
      pastel(c, L.b(0, -45, 7, 1.6, 66, 0.05), "#B5652D", { seed: 66, shade: false, line: false, press: 0.8 });
      scribble(c, L.c([-2, -50, -8, -62, 4, -72, -3, -88]), "#B9B1A2", 67, L.n(1.1), { alpha: 0.85 });
      scribble(c, L.c([3, -50, -1, -60, 9, -70, 3, -82]), "#B9B1A2", 68, L.n(1), { alpha: 0.7 });
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
      // 가는 가지에 단풍잎 셋, 하나는 막 떨어지는 중
      smudge(c, x, y + L.n(1), L.n(14), L.n(3), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, 2, -22, -2, -44, 1, -66]), "#7A5634", 81, L.n(1.8), { press: 0.8 });
      scribble(c, L.c([0, -40, 6, -44, 10, -48, 14, -52]), "#7A5634", 82, L.n(1));
      scribble(c, L.c([1, -54, -4, -58, -8, -61, -12, -64]), "#7A5634", 83, L.n(1));
      [[17, -56, 0.6, "#C0773C"], [-15, -67, -0.5, "#D98C4A"], [2, -71, 0.1, "#B5652D"], [22, -22, 1.4, "#C9864A"]].forEach(function (q, i) {
        pastel(c, L.b(q[0], q[1], 7, 4.2, 84 + i, 0.16, q[2]), q[3], { seed: 84 + i, lw: L.n(0.7) }); });
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
      // 키 큰 풀잎 곁의 작은 달팽이
      smudge(c, x, y + L.n(1), L.n(26), L.n(3), "#3B3325", 0.22);
      scribble(c, L.c([-14, 0, -12, -30, -18, -56, -10, -80]), "#6F8A4A", 118, L.n(1.6));
      scribble(c, L.c([-8, 0, -4, -22, 2, -40, 10, -52]), "#86A05C", 119, L.n(1.3));
      var k = 0.62, ox = 10;
      pastel(c, organic([[-30, -2], [30, -2], [40, -8], [38, -20], [30, -20], [26, -12], [-24, -10]].map(function (q) { return [x + (ox + q[0] * k) * s, y + q[1] * k * s]; }), L.n(0.6), 111, L.n(2.5)), "#BFB6A3", { seed: 111, lw: L.n(0.8) });
      scribble(c, L.c([ox + 20, -12, ox + 19, -16, ox + 19, -19, ox + 17, -21]), INK, 112, L.n(0.8)); scribble(c, L.c([ox + 23, -12, ox + 24, -16, ox + 25, -19, ox + 26, -21]), INK, 113, L.n(0.8));
      pastel(c, L.b(ox - 2, -16, 13, 12, 116, 0.06), "#C99B63", { seed: 116, lw: L.n(0.9) });
      var sp = []; for (var i = 0; i <= 60; i++) { var t = i / 60, a = t * Math.PI * 4.2, rr = lerp(9.5, 1.3, t); sp.push([x + (ox - 2 + Math.cos(a) * rr) * s, y + (-16 + Math.sin(a) * rr * 0.92) * s]); }
      scribble(c, sp, "#7A5634", 117, L.n(1));
    } },
    { id: "dandelion", ko: "민들레 홀씨", when: "새해 첫날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      scribble(c, L.c([0, 0, 4, -26, -3, -54, 2, -80]), "#7E9453", 121, L.n(1.5));
      pastel(c, L.b(2, -82, 3, 3, 122, 0.1), "#B8AE8C", { line: false, press: 0.9 });
      for (var i = 0; i < 18; i++) { var a = i / 18 * Math.PI * 2, r0 = 19 + (i % 3) * 2; scribble(c, [[x + L.n(2), y - L.n(82)], [x + L.n(2 + Math.cos(a) * r0 * 0.6), y + L.n(-82 + Math.sin(a) * r0 * 0.6)], [x + L.n(2 + Math.cos(a) * r0), y + L.n(-82 + Math.sin(a) * r0)]], "#A8A194", 123 + i, L.n(0.5), { alpha: 0.75, passes: 1 }); pastel(c, L.b(2 + Math.cos(a) * r0, -82 + Math.sin(a) * r0, 2.2, 2.2, 150 + i, 0.2), "#EDE9DF", { line: false, shade: false, press: 0.8 }); }
    } },
    { id: "acorn", ko: "도토리", when: "인생의 계절이 바뀐 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      // 휘어진 가는 참나무 가지 끝에 매달린 도토리 하나, 잎 두 장
      smudge(c, x, y + L.n(1), L.n(14), L.n(3), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, 2, -30, -2, -62, 6, -86]), "#7A5634", 131, L.n(2), { press: 0.8 });
      scribble(c, L.c([6, -86, 14, -90, 22, -88, 28, -80]), "#7A5634", 132, L.n(1.4));
      [[-6, -70, 9, 3.4, 0.7], [14, -92, 8, 3, -0.3]].forEach(function (q, i) { pastel(c, L.b(q[0], q[1], q[2], q[3], 133 + i, 0.3, q[4]), "#7F9A58", { seed: 133 + i, lw: L.n(0.6) }); });
      scribble(c, L.c([28, -80, 28, -77, 28, -74, 28, -71]), "#7A5634", 135, L.n(0.9));
      pastel(c, L.b(28, -61, 6.5, 8.5, 136, 0.05), "#B07A45", { seed: 136, lw: L.n(0.8) });
      pastel(c, L.b(28, -69, 7.5, 3.2, 137, 0.08), "#7A5B3A", { seed: 137, lw: L.n(0.7) });
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
      // 물 없이 배만 (앱이 물웅덩이 위에 띄움). 작은 돛대에 깃발 하나
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      pastel(c, organic(A([[-30, -16], [30, -16], [20, -5], [-20, -5]]), L.n(0.6), 172, L.n(3)), "#F4EEDF", { seed: 172, lw: L.n(1) });
      pastel(c, organic(A([[-15, -16], [15, -16], [1, -42]]), L.n(0.6), 173, L.n(3)), "#EAE2CF", { seed: 173, lw: L.n(1) });
      scribble(c, L.c([1, -42, 1, -32, 0, -24, 0, -16]), "#B9B1A2", 174, L.n(0.8), { alpha: 0.8 });
      scribble(c, L.c([1, -42, 1, -48, 1, -54, 1, -60]), "#8A6A48", 176, L.n(1.2));
      pastel(c, organic(A([[1, -60], [14, -56], [1, -52]]), L.n(0.4), 177, L.n(2)), "#E9A43A", { seed: 177, lw: L.n(0.6) });
    } },
    { id: "kite", ko: "하늘의 연", when: "한 줄을 100일 이어 쓴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      // 연만 (앱이 하늘에 띄우고 땅까지 실을 그음). 가운데 = 캔버스 가운데
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + (v[1] - 52) * s]; }); }
      pastel(c, organic(A([[0, -24], [20, 0], [0, 26], [-20, 0]]), L.n(0.6), 183, L.n(3)), "#D98C7A", { seed: 183, lw: L.n(1) });
      scribble(c, A([[0, -24], [0, 26]]), INK, 184, L.n(0.7), { alpha: 0.7 });
      scribble(c, A([[-20, 0], [20, 0]]), INK, 185, L.n(0.7), { alpha: 0.7 });
      scribble(c, bez(A([[0, 26], [8, 36], [-4, 44], [6, 56]]).flat(), 20), "#8A8174", 186, L.n(0.7), { alpha: 0.85, passes: 1 });
      [[5, 36, "#E9A43A"], [-1, 46, "#B8C98E"]].forEach(function (b, i) { var q = A([[b[0], b[1]]])[0]; pastel(c, blob(q[0], q[1], L.n(3.6), L.n(2.2), 187 + i, 0.1, 0.4), b[2], { seed: 187 + i, lw: L.n(0.5) }); });
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
    } },
    // 처음 문장을 넘긴 날: 꽂아 둔 한 장 (넘긴 책장 깃발 + 리본)
    { id: "bookmark", ko: "책갈피", when: "처음 문장을 넘긴 날", draw: function (c, x, y, s) { var L = loc(x, y, s);
      // 바람에 넘어온 한 장이 키 큰 풀줄기에 살짝 걸림 (가볍게 넘긴 한 장)
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      smudge(c, x, y + L.n(1), L.n(14), L.n(3), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, -2, -30, 4, -60, 12, -88]), "#6F8A4A", 201, L.n(1.6));
      scribble(c, L.c([2, -2, 6, -18, 4, -32, 10, -44]), "#86A05C", 202, L.n(1.1));
      // 한 장: 작고, 아래 귀퉁이가 바람에 살짝 말림
      var cc = Math.cos(0.55), ss = Math.sin(0.55);
      function T(q) { return [x + (q[0] * cc - q[1] * ss + 9) * s, y + (q[0] * ss + q[1] * cc - 62) * s]; }
      var page = [[-8, -10], [8, -10], [8.5, 4], [6, 8.5], [2, 10], [-8, 10]];
      pastel(c, organic(page.map(T), L.n(0.3), 203, L.n(1.6)), "#F8F3E6", { seed: 203, lw: L.n(0.7), lineA: 0.85 });
      scribble(c, [T([8.5, 4]), T([4, 4.5]), T([2, 10])], "#CFC5B0", 204, L.n(0.6), { passes: 1 });
      [-5, -1.5, 2].forEach(function (yy, i) { scribble(c, [T([-5, yy]), T([0, yy - 0.2]), T([5 - i * 1.5, yy])], "#C9C0AD", 205 + i, L.n(0.55), { alpha: 0.8, passes: 1 }); });
    } },
    // 종이배가 물웅덩이 없이 혼자일 때 아래에 까는 물
    { id: "puddle", ko: "", when: "", draw: function (c, x, y, s) { var L = loc(x, y, s);
      pastel(c, L.b(0, -4, 48, 8, 171, 0.1), "#9DB4B5", { seed: 171, ang: -0.04, spread: 0.2, light: 0.28, lw: L.n(0.8) });
      scribble(c, L.c([-36, -3, -26, -5, -16, -2, -6, -4]), "#F6F4EC", 175, L.n(1), { alpha: 0.9 });
    } },
    // 생일이 아닌 날의 촛불: 불을 끈 초
    { id: "candle_off", ko: "", when: "", draw: function (c, x, y, s) { var L = loc(x, y, s);
      pastel(c, L.b(0, -6, 30, 8, 71, 0.1), "#A9A293", { seed: 71, lw: L.n(0.9) });
      pastel(c, organic([[-9, -10], [9, -10], [9, -50], [-9, -50]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.6), 72, L.n(3)), "#F2EAD8", { seed: 72, lw: L.n(0.9) });
      scribble(c, L.c([0, -50, 1, -53, -1, -55, 0, -57]), INK, 73, L.n(1));
    } },
    // 바람개비 · 풍경을 앱이 움직일 수 있게 나눈 조각 (같은 칸 · 같은 자리)
    { id: "pinwheel_stick", ko: "", when: "", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(18), L.n(4), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, 1, -20, -1, -42, 0, -64]), "#8A6A48", 161, L.n(2.2), { press: 0.8 });
    } },
    { id: "pinwheel_blades", ko: "", when: "", draw: function (c, x, y, s) { var L = loc(x, y, s);
      function A(q) { return q.map(function (v) { return [x + v[0] * s, y + v[1] * s]; }); }
      var cx = 0, cy = -66, R = 24, cols = ["#E9A43A", "#9DB4B5", "#D98C7A", "#B8C98E"];
      for (var i = 0; i < 4; i++) { var a = i * Math.PI / 2 + 0.35;
        var tri = [[cx, cy], [cx + Math.cos(a) * R, cy + Math.sin(a) * R], [cx + Math.cos(a + 1.05) * R * 0.72, cy + Math.sin(a + 1.05) * R * 0.72]];
        pastel(c, organic(A(tri), L.n(0.5), 162 + i, L.n(2.5)), cols[i], { seed: 162 + i, lw: L.n(0.8) }); }
      pastel(c, L.b(cx, cy, 3.2, 3.2, 167, 0.05), "#6E5238", { seed: 167, lw: L.n(0.6) });
    } },
    { id: "windchime_pole", ko: "", when: "", draw: function (c, x, y, s) { var L = loc(x, y, s);
      smudge(c, x, y + L.n(1), L.n(16), L.n(4), "#3B3325", 0.22);
      scribble(c, L.c([0, 0, 1, -30, -1, -62, 0, -92]), "#8A6A48", 191, L.n(2.2), { press: 0.8 });
      scribble(c, L.c([0, -90, 8, -92, 16, -91, 24, -89]), "#8A6A48", 192, L.n(2), { press: 0.8 });
    } },
    { id: "windchime_hang", ko: "", when: "", draw: function (c, x, y, s) { var L = loc(x, y, s);
      scribble(c, L.c([22, -89, 22, -84, 22, -80, 22, -76]), "#8A8174", 193, L.n(0.8), { alpha: 0.9, passes: 1 });
      pastel(c, organic([[14, -64], [30, -64], [28, -76], [22, -79], [16, -76]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.5), 194, L.n(2.5)), "#9DB4B5", { seed: 194, lw: L.n(0.8) });
      scribble(c, L.c([22, -64, 22, -58, 22, -54, 22, -50]), "#8A8174", 195, L.n(0.7), { alpha: 0.9, passes: 1 });
      pastel(c, organic([[18, -50], [26, -50], [27, -34], [17, -34]].map(function (q) { return [x + q[0] * s, y + q[1] * s]; }), L.n(0.5), 196, L.n(2.5)), "#F4EEDF", { seed: 196, lw: L.n(0.7) });
    } }
  ];
  function drawObj(c, id, x, y, s) { OBJ.filter(function (o) { return o.id === id; })[0].draw(c, x, y, s); }
