  /* ⑤ 계절 한 장: 한지 카드 한 장에 큰 오림 그림 하나 + 붉은 낙관. 나무 종류 × 계절 16장.
     카드 좌표: 아래 가운데 = (0, 0), 폭 60 · 높이 84, 그림 자리 가운데 (0, -48). */
  var CW = 60, CH = 84, CY = -48;
  var DISC = { spring: "#F5D6DD", summer: "#D3E7DC", autumn: "#F3D9A8", winter: "#D8E3EC" }, SEAL = { spring: "春", summer: "夏", autumn: "秋", winter: "冬" };
  function petal5(cx, cy, r, rot) { var tpl = [[0, 0], [r * 0.32, -r * 0.34], [r * 0.7, -r * 0.4], [r * 0.98, -r * 0.2], [r * 0.84, 0], [r * 0.98, r * 0.2], [r * 0.7, r * 0.4], [r * 0.32, r * 0.34]]; var out = []; for (var i = 0; i < 5; i++) out.push(place(tpl, cx, cy, 1, rot + i * Math.PI * 2 / 5)); return out; }
  function cherryFlower(ctx, cx, cy, r, rot, seed, col) { paper(ctx, petal5(cx, cy, r, rot), col || "#F6C3CF", seed, { tone: 0.12, rim: 0.7 }); paper(ctx, petal5(cx, cy, r * 0.42, rot + 0.6), "#E68AA0", seed + 1, { rim: 0, sh: 0.2 });
    for (var i = 0; i < 7; i++) { var a = rot + i * 0.9, l = r * (0.45 + (i % 2) * 0.12); thread(ctx, [[cx, cy], [cx + Math.cos(a) * l, cy + Math.sin(a) * l]], 0.45, "#C9607A"); ctx.fillStyle = "#F2C04E"; ctx.beginPath(); ctx.arc(cx + Math.cos(a) * l, cy + Math.sin(a) * l, 0.75, 0, 7); ctx.fill(); } }
  function serr(len, wid, teeth) { var p = []; for (var i = 0; i <= 20; i++) { var t = i / 20, w = Math.sin(t * Math.PI) * wid * (1 - 0.25 * t) * (1 + (i % 2 ? 0.14 : -0.06) * (teeth ? 1 : 0)); p.push([t * len, -w]); } for (var j = 19; j > 0; j--) { var u = j / 20, w2 = Math.sin(u * Math.PI) * wid * (1 - 0.25 * u) * (1 + (j % 2 ? 0.14 : -0.06) * (teeth ? 1 : 0)); p.push([u * len, w2]); } return p; }
  function veinLeaf(ctx, x, y, len, wid, rot, col, seed, teeth) { paper(ctx, place(serr(len, wid, teeth), x, y, 1, rot), col, seed, { tone: 0.14 }); var c = Math.cos(rot), s = Math.sin(rot); var vc = mixHex(col, "#FFF6E6", 0.45); thread(ctx, [[x + c * len * 0.04, y + s * len * 0.04], [x + c * len * 0.92, y + s * len * 0.92]], 0.6, vc);
    for (var i = 1; i < 5; i++) { var t = i / 5.4, px = x + c * len * t, py = y + s * len * t, w = wid * 0.62 * Math.sin(t * Math.PI); [-1, 1].forEach(function (d) { var a = rot + d * 0.85; thread(ctx, [[px, py], [px + Math.cos(a) * w * 1.25, py + Math.sin(a) * w * 1.25]], 0.45, vc); }); }
    thread(ctx, [[x, y], [x - c * len * 0.16, y - s * len * 0.16]], 0.9, mixHex(col, "#3A2A1A", 0.4)); }
  function snowflake(ctx, cx, cy, r, seed) { var arms = []; for (var i = 0; i < 6; i++) { var a = i * Math.PI / 3 - Math.PI / 2, ca = Math.cos(a), sa = Math.sin(a); arms.push(rib([[cx, cy], [cx + ca * r, cy + sa * r]], 1.5, 1.1)); [0.45, 0.72].forEach(function (t, k) { var px = cx + ca * r * t, py = cy + sa * r * t, l = r * (k ? 0.22 : 0.3); [-1, 1].forEach(function (d) { var b = a + d * 0.8; arms.push(rib([[px, py], [px + Math.cos(b) * l, py + Math.sin(b) * l]], 1.1, 0.8)); }); }); } arms.push(blob(cx, cy, r * 0.18, r * 0.18, seed + 9, 0.02)); paper(ctx, arms, "#FFFFFF", seed, { rim: 0.4, sh: 0.7 }); }
  function bark(ctx, pts, w0, w1, col, seed) { paper(ctx, rib(pts, w0, w1), col, seed, { rim: 0.45, sh: 0.8, tone: 0.14 }); }
  function snowCaps(ctx, list, seed) { paper(ctx, list.map(function (q, i) { return blob(q[0], q[1], q[2], q[3] || 1.6, seed + i, 0.22, q[4] || 0); }), "#FFFFFF", seed, { rim: 0.45, sh: 0.4 }); }

  var CARD = {
    cherry_spring: ["벚꽃 가지", function (ctx) { bark(ctx, bz(0, 0, 1, [-26, -24, -12, -36, 4, -50, 24, -70]), 3.4, 1.4, BARK.cherry, 3001); bark(ctx, bz(0, 0, 1, [-6, -40, -10, -50, -16, -56, -20, -64]), 1.6, 0.8, BARK.cherry, 3002);
      [[-21, -64, -0.6], [21, -72, 0.4]].forEach(function (q, i) { paper(ctx, place([[0, 0], [2.6, -1.8], [5.6, 0], [2.6, 1.8]], q[0], q[1], 1, q[2] - Math.PI / 2), "#E68AA0", 3003 + i, { rim: 0.3, sh: 0.4 }); });
      cherryFlower(ctx, 0, -46, 11.5, 0.3, 3010); cherryFlower(ctx, 14, -62, 8, -0.4, 3020, "#F8D3DC"); cherryFlower(ctx, -16, -32, 7, 0.9, 3030, "#F8D3DC");
      [[18, -32, 0.5], [-20, -48, -0.8], [6, -24, 1.4]].forEach(function (q, i) { paper(ctx, place([[0, 0], [2.4, -1.6], [4.6, -0.6], [4, 0], [4.6, 0.6], [2.4, 1.6]], q[0], q[1], 1, q[2]), "#F8D3DC", 3040 + i, { rim: 0.3, sh: 0.6 }); }); }],
    cherry_summer: ["버찌 한 쌍", function (ctx) { veinLeaf(ctx, 3, -72, 26, 7.5, -0.35, "#6F9A52", 3101, true);
      thread(ctx, bz(0, 0, 1, [2, -70, -2, -60, -8, -50, -9, -40]), 1, "#5E7A44"); thread(ctx, bz(0, 0, 1, [2, -70, 5, -60, 9, -48, 9, -36]), 1, "#5E7A44");
      [[-9, -34, 8.6], [9.5, -30, 8.6]].forEach(function (q, i) { paper(ctx, blob(q[0], q[1], q[2], q[2] * 0.94, 3110 + i, 0.05), i ? "#C0303C" : "#A82432", 3110 + i, { tone: 0.22 }); paper(ctx, blob(q[0] - q[2] * 0.35, q[1] - q[2] * 0.42, q[2] * 0.3, q[2] * 0.16, 3115 + i, 0.05, -0.6), "#F2A6A8", 3115 + i, { rim: 0, sh: 0 }); }); }],
    cherry_autumn: ["물든 벚잎", function (ctx) { veinLeaf(ctx, -18, -30, 34, 11, -0.95, "#E9A34E", 3201, true); veinLeaf(ctx, -12, -26, 36, 12, -0.45, "#D45A3C", 3202, true); paper(ctx, place(serr(10, 3.4, true), 16, -66, 1, 2.6), "#C8473A", 3203, { rim: 0.3, sh: 0.8 }); }],
    cherry_winter: ["꽃눈 가지와 눈송이", function (ctx) { bark(ctx, bz(0, 0, 1, [-26, -30, -10, -38, 6, -44, 26, -56]), 3, 1.4, BARK.cherry, 3301); bark(ctx, bz(0, 0, 1, [0, -42, 2, -48, 6, -54, 8, -60]), 1.5, 0.8, BARK.cherry, 3302);
      [[-16, -34, -1.9], [-2, -41, -1.4], [12, -49, -1.2], [8, -60, -1.5], [22, -55, -0.9]].forEach(function (q, i) { paper(ctx, place([[0, 0], [2.4, -2.6], [6.6, -0.4], [8.4, 0], [6.6, 0.4], [2.4, 2.6]], q[0], q[1], 1, q[2]), i % 2 ? "#B06A70" : "#9C5A5E", 3310 + i, { rim: 0.35, sh: 0.6, tone: 0.18 }); });
      snowCaps(ctx, [[-14, -36.6, 6, 1.6, -0.35], [6, -46.6, 6, 1.5, -0.4]], 3320); snowflake(ctx, -14, -62, 9, 3330); }],
    zelkova_spring: ["새순", function (ctx) { paper(ctx, blob(0, -22, 7, 4.6, 3401, 0.1), "#9A7A55", 3401, { tone: 0.2 }); bark(ctx, bz(0, 0, 1, [0, -24, 1, -34, -1, -42, 0, -50]), 2.2, 1.6, "#7FA35A", 3402);
      veinLeaf(ctx, 0, -48, 22, 7, -2.55, "#A9C77E", 3403, true); veinLeaf(ctx, 0, -50, 24, 7.6, -0.55, "#C2DA92", 3404, true); paper(ctx, place(serr(9, 2.6, false), 0, -50, 1, -1.62), "#D8E8A8", 3405, { rim: 0.3, sh: 0.5 }); }],
    zelkova_summer: ["매미", function (ctx) { bark(ctx, [[-4, -14], [-2, -82]], 15, 13, "#8A6E54", 3501); for (var i = 0; i < 5; i++) thread(ctx, [[-9 + (i % 2) * 6, -20 - i * 12], [-7 + (i % 2) * 6, -27 - i * 12]], 0.6, "rgba(60,44,30,.45)");
      paper(ctx, blob(-2, -44, 5, 11, 3502, 0.05), "#5B4A3A", 3502, { tone: 0.18 });
      [-1, 1].forEach(function (d, i) { var w = place(serr(30, 6.2, false), -2 + d * 1.6, -56, 1, Math.PI / 2 - d * 0.24); paper(ctx, w, "#E8F0EE", 3503 + i, { rim: 0.5, sh: 0.7, fiber: 1.6, light: 0.9 }); thread(ctx, [[-2 + d * 1.6, -56], [-2 + d * 8.6, -28]], 0.45, "rgba(80,90,90,.45)"); thread(ctx, [[-2 + d * 4, -46], [-2 + d * 9.6, -38]], 0.4, "rgba(80,90,90,.35)"); });
      paper(ctx, blob(-2, -57, 6, 4.6, 3506, 0.05), "#4A3E32", 3506, { tone: 0.12 }); paper(ctx, blob(-2, -62.5, 7, 3, 3507, 0.05), "#3E342A", 3507, {});
      [[-8.2, -63], [4.2, -63]].forEach(function (q, i) { paper(ctx, blob(q[0], q[1], 2.1, 2.1, 3508 + i, 0.03), "#2A2420", 3508 + i, { rim: 0.2, sh: 0.2 }); }); }],
    zelkova_autumn: ["단풍 든 느티잎", function (ctx) { [[-1.95, "#E9A34E"], [-1.2, "#C4622E"], [-0.45, "#A4542E"]].forEach(function (q, i) { veinLeaf(ctx, -2, -24, 32 - i * 2, 8.4, q[0], q[1], 3601 + i, true); }); paper(ctx, blob(-2, -24, 2, 2, 3605, 0.05), "#6E5238", 3605, { rim: 0.2 }); }],
    zelkova_winter: ["까치", function (ctx) { bark(ctx, bz(0, 0, 1, [-28, -30, -10, -32, 10, -30, 28, -36]), 2.6, 1.6, BARK.zelkova, 3701); snowCaps(ctx, [[-16, -32.6, 7, 1.4], [16, -34, 6, 1.3]], 3702);
      paper(ctx, [[2, -46], [26, -60], [28, -57], [6, -42]], "#2C3446", 3703, { rim: 0.4, sh: 0.6 }); paper(ctx, [[4, -45], [24, -57], [25, -55.6], [6, -43.6]], "#4D6A9A", 3704, { rim: 0, sh: 0 });
      paper(ctx, blob(-4, -44, 11, 7.6, 3705, 0.05, -0.35), "#232126", 3705, { tone: 0.1 }); paper(ctx, blob(-6, -40.4, 8, 4.4, 3706, 0.05, -0.3), "#F7F3EA", 3706, { rim: 0.3, sh: 0.4 });
      paper(ctx, place(serr(14, 3.4, false), -10, -49, 1, 0.25), "#2C3446", 3707, { rim: 0.3, sh: 0.5 }); paper(ctx, place(serr(7, 1.8, false), -6, -48.6, 1, 0.25), "#F7F3EA", 3708, { rim: 0, sh: 0 });
      paper(ctx, blob(-15, -52, 5.6, 5.2, 3709, 0.04), "#232126", 3709, {}); paper(ctx, [[-20, -53], [-26.6, -52], [-20, -50.6]], "#232126", 3710, { rim: 0.2, sh: 0.3 });
      ctx.fillStyle = "#FFFFFF"; ctx.beginPath(); ctx.arc(-16.6, -53.4, 1.1, 0, 7); ctx.fill(); ctx.fillStyle = "#141210"; ctx.beginPath(); ctx.arc(-16.8, -53.4, 0.7, 0, 7); ctx.fill();
      thread(ctx, [[-4, -37.6], [-5, -33]], 0.8, "#3A3430"); thread(ctx, [[1, -38], [1, -33]], 0.8, "#3A3430"); }],
    ginkgo_spring: ["연둣빛 은행잎", function (ctx) { bark(ctx, [[-2, -22], [0, -34]], 2.6, 2, "#8A7A62", 3801); paper(ctx, fanLeaf(-6, -40, 20, -0.5), "#B7D07A", 3802, { tone: 0.12 }); paper(ctx, fanLeaf(7, -42, 18, 0.45), "#CFE29A", 3803, { tone: 0.12 }); paper(ctx, fanLeaf(0, -46, 12, 0), "#DCEBB0", 3804, { rim: 0.5 }); }],
    ginkgo_summer: ["은행잎과 푸른 열매", function (ctx) { paper(ctx, fanLeaf(-4, -36, 28, -0.25), "#6F9A50", 3901, { tone: 0.14 }); for (var i = 0; i < 6; i++) { var a = -Math.PI / 2 - 0.25 - 0.95 + i * 0.38; thread(ctx, [[-4 + Math.cos(a + Math.PI / 2 + 0.25) * 0, -44], [-4 + Math.cos(a) * 24, -44 + Math.sin(a) * 22]], 0.4, "rgba(220,235,200,.5)"); }
      thread(ctx, bz(0, 0, 1, [-2, -30, 6, -34, 12, -32, 14, -26]), 0.9, "#5E7A44"); thread(ctx, bz(0, 0, 1, [-2, -30, 8, -28, 18, -24, 20, -18]), 0.9, "#5E7A44");
      [[14, -22, 5.2], [20, -14, 5]].forEach(function (q, i) { paper(ctx, blob(q[0], q[1], q[2], q[2] * 1.08, 3910 + i, 0.05), i ? "#A9BE6A" : "#94AE5A", 3910 + i, { tone: 0.2 }); }); }],
    ginkgo_autumn: ["노란 은행잎", function (ctx) { paper(ctx, fanLeaf(-6, -30, 15, -0.62), "#E9B43A", 4001, { tone: 0.12 }); paper(ctx, fanLeaf(6, -31, 15, 0.58), "#F4CC4E", 4002, { tone: 0.12 }); paper(ctx, fanLeaf(0, -33, 17, 0), "#F7D865", 4003, { tone: 0.12 }); paper(ctx, fanLeaf(14, -64, 7, 2.4), "#F0C43C", 4004, { rim: 0.4, sh: 1 }); }],
    ginkgo_winter: ["참새", function (ctx) { bark(ctx, bz(0, 0, 1, [-28, -28, -10, -31, 10, -31, 28, -27]), 2.6, 1.8, BARK.ginkgo, 4101); paper(ctx, fanLeaf(20, -27, 7, 2.9), "#E9B43A", 4102, { rim: 0.35, sh: 0.6 }); snowCaps(ctx, [[-18, -31.4, 6, 1.4], [8, -32.6, 5, 1.3]], 4103);
      paper(ctx, [[8, -38], [22, -34], [21, -31], [8, -35]], "#7A5A40", 4104, { rim: 0.3, sh: 0.5 });
      paper(ctx, blob(-1, -44, 13, 11, 4105, 0.05), "#A87E58", 4105, { tone: 0.14 }); paper(ctx, blob(-4, -39, 9, 6, 4106, 0.05), "#EFE4D2", 4106, { rim: 0.3, sh: 0.3 });
      paper(ctx, place(serr(16, 4.6, false), -2, -47, 1, 0.3), "#8A6242", 4107, { rim: 0.3, sh: 0.5 }); for (var i = 0; i < 3; i++) thread(ctx, [[2 + i * 3.6, -46 + i * 1.2], [5 + i * 3.6, -43 + i * 1.2]], 0.6, "#F3E8D4");
      paper(ctx, blob(-9, -53, 6.4, 5, 4108, 0.04), "#8A5A3A", 4108, {}); paper(ctx, blob(-11, -49.6, 4.2, 2.6, 4109, 0.05), "#F7F3EA", 4109, { rim: 0, sh: 0.1 }); paper(ctx, blob(-11.6, -49.4, 1.3, 1.3, 4110, 0.05), "#2A2420", 4110, { rim: 0, sh: 0 });
      paper(ctx, [[-14, -53], [-18.6, -52], [-14, -51]], "#3A3430", 4111, { rim: 0, sh: 0.2 }); ctx.fillStyle = "#141210"; ctx.beginPath(); ctx.arc(-11.4, -54.6, 0.95, 0, 7); ctx.fill(); ctx.fillStyle = "#FFFFFF"; ctx.beginPath(); ctx.arc(-11.7, -54.9, 0.35, 0, 7); ctx.fill();
      thread(ctx, [[-3, -33.4], [-4, -30.6]], 0.8, "#6A5444"); thread(ctx, [[2, -33.4], [2, -30.6]], 0.8, "#6A5444"); }],
    pine_spring: ["송화", function (ctx) { var nd = []; for (var i = 0; i < 13; i++) { var a = -Math.PI / 2 + (i - 6) * 0.26, l = 20 - Math.abs(i - 6) * 1.2; nd.push(rib([[0, -34], [Math.cos(a) * l * 0.55, -34 + Math.sin(a) * l * 0.55 - 1], [Math.cos(a) * l, -34 + Math.sin(a) * l]], 1.2, 0.5)); } bark(ctx, [[0, -16], [0, -36]], 3.4, 3, BARK.pine, 4201); paper(ctx, nd, "#5C8259", 4202, { rim: 0.25, sh: 0.6 });
      bark(ctx, [[0, -40], [0.2, -50], [0.4, -60]], 4.6, 2.2, "#D6DFA0", 4203); var r = R(4204); for (var k = 0; k < 14; k++) { var a2 = (k / 14) * Math.PI * 2, rr = 4 + (k % 2) * 1.4; paper(ctx, blob(Math.cos(a2) * rr, -42 + Math.sin(a2) * 2.6 - (k % 3), 1.8, 2.8, 4205 + k, 0.05, a2), k % 2 ? "#E6CF66" : "#D9BE52", 4205 + k, { rim: 0.2, sh: 0.4 }); }
      ctx.save(); ctx.fillStyle = "rgba(232,208,96,.75)"; for (var d = 0; d < 16; d++) { ctx.beginPath(); ctx.arc((r() - 0.5) * 36, -54 - r() * 22, 0.4 + r() * 0.5, 0, 7); ctx.fill(); } ctx.restore(); }],
    pine_summer: ["솔잎과 푸른 솔방울", function (ctx) { bark(ctx, bz(0, 0, 1, [-26, -62, -12, -60, 4, -62, 20, -66]), 3, 1.8, BARK.pine, 4301); var nd = []; for (var i = 0; i < 7; i++) { var px = -20 + i * 6.4, py = -61.6 - i * 0.4; [-1, 1].forEach(function (d) { var a = -Math.PI / 2 + d * 0.5 + (i - 3) * 0.06; nd.push(rib([[px, py], [px + Math.cos(a) * 13, py + Math.sin(a) * 12]], 1, 0.45)); }); } paper(ctx, nd, "#4F7A52", 4302, { rim: 0.25, sh: 0.5 });
      thread(ctx, [[-2, -60], [-1, -54]], 1.2, "#7A5034"); pineCone(ctx, -1, -30, 1.15, "#7F9A58", "#A8BC78", false, 0); }],
    pine_autumn: ["여문 솔방울", function (ctx) { pineCone(ctx, 1, -26, 1.55, "#9A6A42", "#C8A070", true, 0.12); }],
    pine_winter: ["소나무 아래 학", function (ctx) { bark(ctx, bz(0, 0, 1, [28, -76, 14, -70, 0, -72, -20, -66]), 3, 1.8, BARK.pine, 4401);
      paper(ctx, [blob(-14, -68, 12, 4.4, 4402, 0.12), blob(4, -72, 10, 4, 4403, 0.12), blob(20, -76, 9, 3.6, 4404, 0.12)], "#4A6E4E", 4402, { sh: 0.8 }); snowCaps(ctx, [[-14, -71.6, 10, 1.8], [4, -75.4, 8, 1.7], [20, -79, 7, 1.6]], 4405);
      thread(ctx, [[-1, -34], [-2, -20]], 0.9, "#3A3430"); thread(ctx, [[3, -34], [5, -26], [2, -24]], 0.9, "#3A3430");
      paper(ctx, [[8, -42], [20, -36], [19, -32], [6, -36]], "#232126", 4410, { rim: 0.3, sh: 0.5 });
      paper(ctx, blob(1, -40, 12, 6.6, 4411, 0.05, -0.2), "#FBF8F0", 4411, { tone: 0.12 }); paper(ctx, place(serr(14, 3.2, false), 0, -42, 1, 0.12), "#EAE6DE", 4412, { rim: 0.2, sh: 0.4 });
      paper(ctx, rib(bz(0, 0, 1, [-8, -42, -12, -48, -6, -54, -9, -62]), 2.8, 2), "#232126", 4413, { rim: 0.3, sh: 0.5 });
      paper(ctx, blob(-9.6, -63.6, 3.2, 2.6, 4414, 0.04), "#F7F3EA", 4414, { rim: 0.2, sh: 0.3 }); paper(ctx, blob(-9.4, -65.6, 1.6, 1.1, 4415, 0.04), "#C8303A", 4415, { rim: 0, sh: 0 });
      paper(ctx, [[-12.4, -63.6], [-21, -61], [-12, -62.2]], "#B9A27A", 4416, { rim: 0, sh: 0.2 }); ctx.fillStyle = "#141210"; ctx.beginPath(); ctx.arc(-10.6, -64, 0.6, 0, 7); ctx.fill(); }]
  };
  // 한 장: (x, y) = 카드 아래 가운데, s = 배율, rot = 기울기 (발치에 기대 세울 때)
  function seasonCard(ctx, x, y, s, key, rot) { var se = key.split("_")[1];
    ctx.save(); ctx.translate(x, y); ctx.rotate(rot || 0); ctx.scale(s, s);
    var card = [[-CW / 2, 0], [CW / 2, 0], [CW / 2, -CH], [-CW / 2, -CH]];
    paper(ctx, resample(card, 2), "#F7F1E3", 3000 + key.length, { rim: 1.3, sh: 1.1, fiber: 1.2 });
    paper(ctx, blob(0, CY, 25, 25, 3900 + key.length, 0.03), DISC[se], 3950 + key.length, { rim: 0.9, sh: 0.25, light: 0.55 });
    ctx.save(); multi(ctx, [resample(card, 2)]); ctx.clip(); CARD[key][1](ctx); ctx.restore();
    var sx = CW / 2 - 9, sy = -9; paper(ctx, [[sx - 4.5, sy - 4.5], [sx + 4.5, sy - 4.5], [sx + 4.5, sy + 4.5], [sx - 4.5, sy + 4.5]], "#C8473A", 3990, { rim: 0.2, sh: 0.2, fiber: 0.4 });
    ctx.fillStyle = "#FBF3E8"; ctx.font = "7px 'WenQuanYi Zen Hei', sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "middle"; ctx.fillText(SEAL[se], sx, sy + 0.3);
    ctx.restore(); }
  Object.keys(CARD).forEach(function (k) { KEEPH[k] = function (ctx, x, y, s) { seasonCard(ctx, x, y, s * 0.42, k, -0.1); }; });

  /* 은행나무: 다른 나무처럼 둥글고 풍성하게. 가장자리에 부채잎 몇 장만 */
  function ginkgoCrown(ctx, cx, cy, lk, cols, seed) {
    var L = [[-30, 0, 30, 22], [30, -2, 30, 22], [0, -16, 40, 28], [-16, -34, 24, 16], [18, -32, 24, 16], [-46, 12, 16, 12], [46, 10, 16, 12], [0, 10, 34, 14]];
    crown(ctx, L, cols, seed, lk, cx, cy);
    [[-58, 8, -1.1], [58, 4, 1.0], [-30, -44, -0.5], [34, -40, 0.6], [-6, -46, -0.1], [-50, 22, -1.6], [50, 22, 1.6]].forEach(function (q, i) { paper(ctx, fanLeaf(cx + q[0] * 1.06 * lk, cy + q[1] * 1.04 * lk, 8.5 * lk, q[2]), cols[i % 2 ? 2 : 1], seed + 60 + i, { rim: 0.45, sh: 0.7 }); });
  }

  /* 겨울 귀마개: 정수리 위로 넘어가는 띠 + 양옆의 털 방울 */
  function hspanAt(pts, y) { var xs = []; for (var i = 0; i < pts.length; i++) { var a = pts[i], b = pts[(i + 1) % pts.length]; if ((a[1] - y) * (b[1] - y) <= 0 && a[1] !== b[1]) xs.push(a[0] + (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1])); } if (xs.length < 2) return null; return [Math.min.apply(null, xs), Math.max.apply(null, xs)]; }
  function earmuffsOn(ctx, V, pts, sc, night, seed) {
    var bb = bbox(pts), my = bb[1] + bb[3] * 0.44, hs = hspanAt(pts, my), r = Math.max(4.2, bb[2] * 0.12), L = [hs[0] + r * 0.35, my], Rr = [hs[1] - r * 0.35, my];
    var band = []; for (var i = 0; i <= 24; i++) { var t = i / 24, x = L[0] + (Rr[0] - L[0]) * t, sp = spanAt(pts, Math.min(bb[0] + bb[2] - 0.5, Math.max(bb[0] + 0.5, x))), ty = sp ? sp[0] : bb[1], e = Math.pow(Math.abs(2 * t - 1), 6); band.push([x, (ty - 1.8) * (1 - e) + my * e]); }
    var col = night ? "#A85A58" : "#C8553D", fur = night ? "#D6CCBE" : "#F4E9D8";
    V.haru.body(ctx, resample(rib(band, 2.8, 2.8), 1), col, seed, sc * 0.6);
    [L, Rr].forEach(function (c, i) { var p = []; for (var k = 0; k < 30; k++) { var a = k / 30 * Math.PI * 2, rr = r * (1 + 0.035 * Math.sin(a * 9 + i)); p.push([c[0] + Math.cos(a) * rr, c[1] + Math.sin(a) * rr * 1.05]); } V.haru.body(ctx, p, fur, seed + 1 + i, sc * 0.65); paper(ctx, blob(c[0] + (i ? -0.8 : 0.8), c[1] - r * 0.15, r * 0.5, r * 0.45, seed + 5 + i, 0.15), night ? "#E6DED2" : "#FFFBF4", seed + 5 + i, { rim: 0, sh: 0 }); });
  }
