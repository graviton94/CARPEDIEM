  /* ⑤ 계절 한 장: 한 장에 한지 조각 하나, 색 하나 (많아야 둘), 바탕 · 테두리 없이. 나무 종류 × 실제 계절 16가지.
     봄 = 꽃, 여름 = 풀 · 작은 나무 · 여름 열매, 가을 = 낙엽 · 가을 열매, 겨울 = 눈사람. 좌표: 가운데 (0, 0), 아래 끝 = y 24 (땅). */
  function petal5(cx, cy, r, rot) { var tpl = [[0, 0], [r * 0.32, -r * 0.34], [r * 0.7, -r * 0.4], [r * 0.98, -r * 0.2], [r * 0.84, 0], [r * 0.98, r * 0.2], [r * 0.7, r * 0.4], [r * 0.32, r * 0.34]]; var out = []; for (var i = 0; i < 5; i++) out.push(place(tpl, cx, cy, 1, rot + i * Math.PI * 2 / 5)); return out; }
  function serr(len, wid, teeth) { var p = []; for (var i = 0; i <= 20; i++) { var t = i / 20, w = Math.sin(t * Math.PI) * wid * (1 - 0.25 * t) * (1 + (i % 2 ? 0.14 : -0.06) * (teeth ? 1 : 0)); p.push([t * len, -w]); } for (var j = 19; j > 0; j--) { var u = j / 20, w2 = Math.sin(u * Math.PI) * wid * (1 - 0.25 * u) * (1 + (j % 2 ? 0.14 : -0.06) * (teeth ? 1 : 0)); p.push([u * len, w2]); } return p; }
  function one(ctx, pts, col, seed, o) { paper(ctx, pts, col, seed, Object.assign({ rim: 0.45, sh: 0.55, fiber: 0.8 }, o || {})); }
  function midrib(ctx, x, y, len, rot, col) { var c = Math.cos(rot), s = Math.sin(rot); thread(ctx, [[x + c * len * 0.08, y + s * len * 0.08], [x + c * len * 0.85, y + s * len * 0.85]], 0.5, mixHex(col, "#FFF8EC", 0.4)); }
  function dot(ctx, x, y, r, col) { ctx.fillStyle = col; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill(); }
  function petals(cx, cy, n, len, wid, rot) { var out = []; for (var i = 0; i < n; i++) out.push(place(leafPts(len, wid), cx, cy, 1, rot + i * Math.PI * 2 / n)); return out; }
  function stem(ctx, pts, col) { one(ctx, rib(bez(pts, 10), 1.3, 1), col || "#7E9E5A", 5001, { rim: 0.25, sh: 0.4 }); }
  function snowman(ctx, x, y, k, seed) { one(ctx, blob(x, y + 13 * k, 11 * k, 10 * k, seed, 0.04), "#FBFAF6", seed, { rim: 0.6, sh: 0.8, light: 0.95 }); one(ctx, blob(x, y - 2 * k, 7.6 * k, 7.2 * k, seed + 1, 0.04), "#FBFAF6", seed + 1, { rim: 0.6, sh: 0.7, light: 0.95 });
    dot(ctx, x - 2.4 * k, y - 3 * k, 0.9 * k, "#3A332C"); dot(ctx, x + 2.4 * k, y - 3 * k, 0.9 * k, "#3A332C"); }
  var PIECE = {
    // 봄: 꽃
    cherry_spring: function (ctx) { stem(ctx, [0, 24, 1, 18, -1, 12, 0, 6]); one(ctx, petal5(0, -2, 12, -0.3), "#F3C2CD", 5101, { rim: 0.5 }); one(ctx, blob(0, -2, 2.2, 2.2, 5102, 0.05), "#E38FA3", 5102, { rim: 0, sh: 0.1 }); },
    zelkova_spring: function (ctx) { stem(ctx, [0, 24, 1, 18, -1, 12, 0, 6]); one(ctx, petals(0, -2, 4, 11, 3.6, -Math.PI / 4), "#F2CB4A", 5111, { rim: 0.5 }); one(ctx, blob(0, -2, 2, 2, 5112, 0.05), "#D9A72E", 5112, { rim: 0, sh: 0.1 }); },
    ginkgo_spring: function (ctx) { stem(ctx, [0, 24, -2, 16, 2, 10, 0, 4]); one(ctx, place(leafPts(10, 4), 0, 16, 1, -2.6), "#8DB06A", 5121, { rim: 0.3 }); one(ctx, petals(0, -3, 5, 9.5, 4.6, -Math.PI / 2), "#A895D4", 5122, { rim: 0.5 }); one(ctx, blob(0, -3, 1.8, 1.8, 5123, 0.05), "#F6E7A0", 5123, { rim: 0, sh: 0.1 }); },
    pine_spring: function (ctx) { stem(ctx, [0, 24, 1, 18, -1, 12, 0, 6]); one(ctx, petals(0, -2, 5, 12, 6, -Math.PI / 2), "#E58AB0", 5131, { rim: 0.5 }); for (var i = 0; i < 3; i++) { var a = -Math.PI / 2 + (i - 1) * 0.35; thread(ctx, [[0, -2], [Math.cos(a) * 9, -2 + Math.sin(a) * 9]], 0.5, "#C2507E"); dot(ctx, Math.cos(a) * 9, -2 + Math.sin(a) * 9, 0.8, "#C2507E"); } },
    // 여름: 풀 · 작은 나무 · 여름 열매
    cherry_summer: function (ctx) { thread(ctx, bez([0, -16, -2, -10, -5, -2, -6, 8]), 0.8, "#6E8A50"); thread(ctx, bez([0, -16, 2, -10, 5, 0, 6, 10]), 0.8, "#6E8A50"); one(ctx, [blob(-6, 13, 5.6, 5.6, 5141, 0.04), blob(6.4, 15, 5.6, 5.6, 5142, 0.04)], "#B83A44", 5141); },
    zelkova_summer: function (ctx) { stem(ctx, [0, 24, 1, 16, -1, 8, 0, 2]); one(ctx, [0, 1, 2].map(function (k) { var ang = -Math.PI / 2 + k * Math.PI * 2 / 3; return heart(Math.cos(ang) * 6.4, 1 + Math.sin(ang) * 6.4, 5.6, ang - Math.PI / 2); }), "#7FAE5E", 5151, { rim: 0.45 }); one(ctx, blob(0, 1, 1.4, 1.4, 5152, 0.05), "#6A9550", 5152, { rim: 0, sh: 0 }); },
    ginkgo_summer: function (ctx) { one(ctx, rib([[0, 24], [0.4, 6]], 3, 2.2), "#8A6E54", 5161, { rim: 0.3 }); one(ctx, blob(0, -4, 13, 12, 5162, 0.08), "#6F9A58", 5162, { tone: 0.1 }); one(ctx, blob(-3.6, -7.4, 6.4, 4.6, 5163, 0.1), "#8FB672", 5163, { rim: 0.3, sh: 0.3 }); },
    pine_summer: function (ctx) { var R0 = 13;
      function wedge(r) { var p = [[-r, 8]]; for (var i = 0; i <= 20; i++) { var a = i / 20 * Math.PI; p.push([-Math.cos(a) * r, 8 + Math.sin(a) * r]); } return p; }
      one(ctx, wedge(R0), "#5E8A4E", 5171); one(ctx, wedge(R0 - 2.4), "#E0605A", 5173, { rim: 0, sh: 0.1 });   // 껍질 · 속 두 색
      [[-4.6, 12], [0, 14.6], [4.6, 12]].forEach(function (q) { dot(ctx, q[0], q[1], 0.9, "#3A2A26"); }); },
    // 가을: 낙엽 · 가을 열매
    cherry_autumn: function (ctx) { one(ctx, maple(0, 4, 15, 0.12), "#D2563C", 5181); thread(ctx, [[0, 10], [1.4, 22]], 0.9, "#8A3E2A"); },
    zelkova_autumn: function (ctx) { one(ctx, blob(0, 10, 12.5, 11, 5191, 0.05), "#E8893A", 5191, { tone: 0.14 }); one(ctx, [0, 1, 2, 3].map(function (k) { return place(leafPts(6.4, 2.4), 0, -0.6, 1, -Math.PI / 4 + k * Math.PI / 2 - Math.PI / 2 * 0.0); }), "#6E7A40", 5192, { rim: 0.25 }); thread(ctx, [[0, -0.6], [0.6, -4.6]], 1, "#5A4A34"); },
    ginkgo_autumn: function (ctx) { one(ctx, fanLeaf(0, 22, 18, 0), "#EFC447", 5201); },
    pine_autumn: function (ctx) { var p = []; for (var i = 0; i < 40; i++) { var t = i / 40 * Math.PI * 2, x = Math.sin(t) * 12, y = -Math.cos(t) * 13; if (y < 0) { x *= 0.86 + 0.14 * (1 + y / 13); y *= 0.9; } p.push([x, 9 + y]); } one(ctx, p, "#8A5636", 5211, { tone: 0.18 }); one(ctx, [[-11.6, 15], [11.6, 15]].concat(bez([11.6, 15, 9, 22.6, -9, 22.6, -11.6, 15], 10)), "#D9B98C", 5212, { rim: 0, sh: 0.1 }); one(ctx, [[-1.2, -2.4], [1.2, -2.4], [0.4, -5.6], [-0.4, -5.6]], "#5A3A24", 5213, { rim: 0, sh: 0.1 }); one(ctx, blob(-4.6, 4, 2.6, 4, 5214, 0.1, 0.4), "#A8704A", 5214, { rim: 0, sh: 0 }); },
    // 겨울: 눈사람
    cherry_winter: function (ctx) { snowman(ctx, 0, 0, 1, 5221); one(ctx, [[-7.4, -6.4]].concat(bez([-7.4, -6.4, -7, -14, 7, -14, 7.4, -6.4], 10)), "#C8553D", 5223, { rim: 0.3 }); one(ctx, blob(0, -13.6, 2.4, 2.4, 5224, 0.1), "#FBF6EC", 5224, { rim: 0.2 }); },
    zelkova_winter: function (ctx) { snowman(ctx, 0, 0, 1, 5231); one(ctx, [rib([[-9, 10], [-13, 5], [-14, 3]], 1.2, 0.7), rib([[9, 10], [13, 6], [14, 4]], 1.2, 0.7)], "#7E5E48", 5233, { rim: 0.2 }); dot(ctx, 0, 9, 0.9, "#3A332C"); dot(ctx, 0, 14, 0.9, "#3A332C"); },
    ginkgo_winter: function (ctx) { snowman(ctx, 0, 0, 1, 5241); one(ctx, [[0.4, -1.2], [7, 0], [0.4, 0.8]], "#E9883A", 5243, { rim: 0, sh: 0.2 }); dot(ctx, 0, 9, 0.9, "#3A332C"); dot(ctx, 0, 14, 0.9, "#3A332C"); },
    pine_winter: function (ctx) { snowman(ctx, -6, 0, 1, 5251); snowman(ctx, 9, 8, 0.6, 5253); one(ctx, place(leafPts(9, 2.4), -6, -8.6, 1, -0.3), "#5E7E62", 5255, { rim: 0.2 }); }
  };
  function piece(ctx, x, y, s, key) { ctx.save(); ctx.translate(x, y); ctx.scale(s, s); PIECE[key](ctx); ctx.restore(); }
  // 정원 보기용 (gardenH): 나무 발치에 작게 놓임 (아래 끝이 땅)
  Object.keys(PIECE).forEach(function (k) { KEEPH[k] = function (ctx, x, y, s) { piece(ctx, x, y - 24 * s * 0.5, s * 0.5, k); }; });

  /* 은행나무: 다른 나무처럼 둥글고 풍성하게. 가장자리에 부채잎 몇 장만 */
  function ginkgoCrown(ctx, cx, cy, lk, cols, seed) {
    var L = [[-30, 0, 30, 22], [30, -2, 30, 22], [0, -16, 40, 28], [-16, -34, 24, 16], [18, -32, 24, 16], [-46, 12, 16, 12], [46, 10, 16, 12], [0, 10, 34, 14]];
    crown(ctx, L, cols, seed, lk, cx, cy);
    [[-30, -44, -0.5], [34, -40, 0.6], [-6, -46, -0.1]].forEach(function (q, i) { paper(ctx, fanLeaf(cx + q[0] * 0.92 * lk, cy + q[1] * 0.92 * lk, 7 * lk, q[2]), cols[i % 2 ? 2 : 1], seed + 60 + i, { rim: 0.45, sh: 0.7 }); });
  }

