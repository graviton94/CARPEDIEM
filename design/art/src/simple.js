  /* ───────── 소품: 단순한 형태 ─────────
     물건 하나를 큰 면 2~5개로만 만듭니다. 되풀이되는 작은 무늬(비늘 · 점 · 엮은 결)는 쓰지 않습니다.
     면마다 왼쪽 위가 밝고 오른쪽 아래가 어두운 부드러운 그러데이션, 흐린 반사광 하나, 옅은 그림자만 씁니다. */
  function scaleOf(ctx) { var m = ctx.getTransform(); return Math.hypot(m.a, m.b); }
  function blurPx(ctx, px) { ctx.filter = "blur(" + Math.max(0, px * scaleOf(ctx)).toFixed(2) + "px)"; }
  function soft(c, path, col, b, o) {
    o = o || {}; col = C(col);
    c.save(); c.clip(path);
    var g = c.createLinearGradient(b[0] + b[2] * 0.15, b[1], b[0] + b[2] * 0.75, b[1] + b[3]);
    g.addColorStop(0, css(sh(col, o.hi == null ? 0.16 : o.hi))); g.addColorStop(0.5, css(col)); g.addColorStop(1, css(sh(col, o.lo == null ? -0.2 : o.lo)));
    c.fillStyle = g; c.fillRect(b[0] - 6, b[1] - 6, b[2] + 12, b[3] + 12);
    if (o.gloss !== 0) { c.save(); blurPx(c, Math.max(b[2], b[3]) * 0.08); c.fillStyle = "rgba(255,252,244," + (o.gloss || 0.28) + ")"; c.beginPath(); c.ellipse(b[0] + b[2] * 0.3, b[1] + b[3] * 0.28, b[2] * 0.16, b[3] * 0.14, -0.5, 0, 7); c.fill(); c.restore(); }
    c.save(); blurPx(c, 3); c.lineWidth = 5; c.strokeStyle = css(sh(col, -0.4), 0.12); c.stroke(path); c.restore();
    if (o.after) o.after(c);
    c.restore();
  }
  function S(d) { return new Path2D(d); }
  function E(cx, cy, rx, ry, rot) { return ell(cx, cy, rx, ry, rot); }
  function shadow(c, cx, cy, rx, ry) { c.save(); blurPx(c, ry * 0.8); c.fillStyle = "rgba(60,48,30,.22)"; c.beginPath(); c.ellipse(cx, cy, rx, ry, 0, 0, 7); c.fill(); c.restore(); }
  function line(c, d, col, w) { c.save(); c.lineCap = "round"; c.lineJoin = "round"; c.lineWidth = w; c.strokeStyle = css(col); c.stroke(typeof d === "string" ? S(d) : d); c.restore(); }
  function lightLeaf(c, x, y, len, wid, ang, col) { var L = leafPath(x, y, len, wid, ang); soft(c, L.path, col, L.box, { gloss: 0.18 }); }
  function glowSoft(c, x, y, r, col, a) { glow(c, x, y, r, col, a); }

  var SIMPLE = {
    pot: function (c) {
      shadow(c, 256, 420, 118, 14);
      soft(c, S("M172,300 L190,412 Q256,424 322,412 L340,300 Z"), PAL.terra, box(172, 300, 168, 124));
      soft(c, rrect(152, 266, 208, 42, 14), sh(PAL.terra, 0.08), box(152, 266, 208, 42));
      soft(c, E(256, 272, 90, 9), PAL.soil, box(166, 263, 180, 18), { gloss: 0 });
      line(c, "M256,270 C252,238 262,210 255,180", PAL.olive, 7);
      lightLeaf(c, 255, 186, 74, 22, -2.6, PAL.leaf);
      lightLeaf(c, 256, 182, 84, 25, -0.45, PAL.sprout);
    },
    lantern: function (c) {
      line(c, "M256,0 L256,118", PAL.ink, 2.5);
      glowSoft(c, 256, 232, 210, PAL.amber, 0.35);
      soft(c, E(256, 232, 108, 104), "#F4D9A0", box(148, 128, 216, 208), { hi: 0.2, lo: -0.08, gloss: 0.35, after: function (x) { glow(x, 246, 222, 100, "#FFF4D6", 0.8); [-60, 0, 60].forEach(function (d) { x.strokeStyle = "rgba(170,110,50,.16)"; x.lineWidth = 2; x.beginPath(); x.moveTo(148, 232 + d); x.quadraticCurveTo(256, 246 + d, 364, 232 + d); x.stroke(); }); } });
      soft(c, rrect(214, 112, 84, 20, 8), PAL.woodD, box(214, 112, 84, 20), { gloss: 0 });
      soft(c, rrect(222, 330, 68, 18, 8), PAL.woodD, box(222, 330, 68, 18), { gloss: 0 });
      line(c, "M256,348 L256,388", PAL.red, 5);
    },
    books: function (c) {
      shadow(c, 256, 420, 150, 14);
      [[132, 364, 248, 50, PAL.olive], [152, 318, 212, 46, PAL.clay], [176, 280, 170, 38, "#5E7478"]].forEach(function (k, i) {
        soft(c, rrect(k[0] + k[2] - 14, k[1] + 5, 14, k[3] - 10, 3), "#F1E7D2", box(k[0] + k[2] - 14, k[1] + 5, 14, k[3] - 10), { gloss: 0 });
        soft(c, rrect(k[0], k[1], k[2] - 8, k[3], 10), k[4], box(k[0], k[1], k[2] - 8, k[3]), { after: function (x) { x.fillStyle = "rgba(242,196,110,.75)"; x.fillRect(k[0] + 22, k[1], 5, k[3]); } });
      });
    },
    teacup: function (c) {
      shadow(c, 256, 414, 150, 16);
      soft(c, E(256, 402, 146, 24), "#EDE6D6", box(110, 378, 292, 48), { gloss: 0 });
      line(c, (function () { var p = new Path2D(); p.ellipse(358, 334, 28, 30, 0, -1.4, 1.5); return p; })(), "#E9E1CF", 12);
      soft(c, S("M160,298 C160,380 200,404 256,404 C312,404 352,380 352,298 Z"), "#F1EBDD", box(160, 298, 192, 106), { after: function (x) { x.fillStyle = css(PAL.olive, 0.8); x.beginPath(); x.moveTo(150, 326); x.quadraticCurveTo(256, 340, 362, 326); x.lineTo(362, 338); x.quadraticCurveTo(256, 352, 150, 338); x.fill(); } });
      soft(c, E(256, 298, 96, 17), "#F7F2E8", box(160, 281, 192, 34), { gloss: 0 });
      soft(c, E(256, 300, 84, 12), "#9A6630", box(172, 288, 168, 24), { gloss: 0.2 });
      steam(c, 256, 272, 120, 16);
    },
    chair: function (c) {
      var W = "#AD7A48";
      shadow(c, 256, 418, 128, 14);
      soft(c, rrect(186, 150, 18, 268, 8), sh(W, -0.06), box(186, 150, 18, 268));
      soft(c, rrect(308, 150, 18, 268, 8), sh(W, -0.06), box(308, 150, 18, 268));
      soft(c, rrect(176, 150, 160, 30, 12), W, box(176, 150, 160, 30));
      soft(c, rrect(186, 210, 140, 18, 8), sh(W, -0.04), box(186, 210, 140, 18));
      soft(c, rrect(170, 330, 18, 88, 8), W, box(170, 330, 18, 88));
      soft(c, rrect(324, 330, 18, 88, 8), W, box(324, 330, 18, 88));
      soft(c, rrect(168, 306, 176, 32, 12), sh(W, 0.1), box(168, 306, 176, 32));
    },
    star: function (c) {
      line(c, "M256,0 L256,128", PAL.ink, 2.5);
      var cx = 256, cy = 256, R = 124, r0 = 54, base = C("#F1C46C");
      for (var i = 0; i < 5; i++) {
        var a = -Math.PI / 2 + i * Math.PI * 2 / 5, tip = [cx + Math.cos(a) * R, cy + Math.sin(a) * R];
        [-1, 1].forEach(function (s) { var a2 = a + s * Math.PI / 5, v = [cx + Math.cos(a2) * r0, cy + Math.sin(a2) * r0], mxd = (Math.cos(a) + Math.cos(a2)) / 2, myd = (Math.sin(a) + Math.sin(a2)) / 2, lit = -(mxd * 0.7 + myd * 0.7) / Math.hypot(mxd, myd);
          c.fillStyle = css(sh(base, lit * 0.14 + (s > 0 ? -0.05 : 0.03))); c.beginPath(); c.moveTo(cx, cy); c.lineTo(tip[0], tip[1]); c.lineTo(v[0], v[1]); c.closePath(); c.fill(); });
      }
      c.save(); blurPx(c, 10); c.fillStyle = "rgba(255,250,235,.3)"; c.beginPath(); c.ellipse(222, 214, 34, 24, -0.5, 0, 7); c.fill(); c.restore();
    },
    mushroom: function (c) {
      shadow(c, 256, 420, 104, 13);
      soft(c, S("M230,420 C234,380 234,340 238,302 L274,302 C278,340 278,380 282,420 Q256,428 230,420 Z"), "#F0E6D0", box(230, 302, 52, 126));
      soft(c, S("M134,304 C138,196 374,196 378,304 C330,318 182,318 134,304 Z"), "#C0503A", box(134, 196, 244, 118), { gloss: 0.3, after: function (x) { [[204, 258, 18, 12], [264, 230, 20, 12], [318, 266, 15, 10]].forEach(function (d) { x.fillStyle = "rgba(252,246,234,.95)"; x.beginPath(); x.ellipse(d[0], d[1], d[2], d[3], 0, 0, 7); x.fill(); }); } });
    },
    globe: function (c) {
      shadow(c, 256, 420, 110, 13);
      var g = E(256, 266, 108, 108);
      c.save(); c.clip(g); var sky = c.createLinearGradient(0, 158, 0, 374); sky.addColorStop(0, "#D9E5E6"); sky.addColorStop(1, "#F2F4EF"); c.fillStyle = sky; c.fillRect(148, 158, 216, 216);
      soft(c, S("M148,336 C200,318 312,318 364,336 L364,380 L148,380 Z"), "#FAFAF6", box(148, 318, 216, 62), { gloss: 0 });
      soft(c, S("M256,226 L214,318 L298,318 Z"), "#557058", box(214, 226, 84, 92));
      [[196, 210], [306, 232], [238, 190], [322, 292], [186, 280]].forEach(function (d) { c.fillStyle = "#FFFFFF"; c.beginPath(); c.arc(d[0], d[1], 4, 0, 7); c.fill(); });
      c.restore();
      c.save(); c.lineWidth = 3; c.strokeStyle = "rgba(120,140,140,.3)"; c.stroke(g); c.restore();
      c.save(); blurPx(c, 6); c.strokeStyle = "rgba(255,255,255,.7)"; c.lineWidth = 10; c.lineCap = "round"; c.beginPath(); c.arc(256, 266, 84, 3.6, 4.3); c.stroke(); c.restore();
      soft(c, S("M174,420 L338,420 L324,372 L188,372 Z"), PAL.wood, box(174, 372, 164, 48));
    },
    moon: function (c) {
      line(c, "M292,0 L292,132", PAL.ink, 2.5);
      glowSoft(c, 206, 300, 120, PAL.amber, 0.14);
      var cr = new Path2D(); cr.rect(0, 0, 512, 512); cr.arc(306, 226, 98, 0, Math.PI * 2);
      c.save(); c.clip(cr, "evenodd"); soft(c, E(250, 262, 114, 114), "#DDAA48", box(136, 148, 228, 228), { gloss: 0.3 }); c.restore();
    },
    boat: function (c) {
      shadow(c, 256, 406, 150, 13);
      var P = "#F2EBDC";
      soft(c, poly([[256, 176], [196, 332], [256, 332]]), sh(P, 0.02), box(196, 176, 60, 156), { gloss: 0 });
      soft(c, poly([[256, 176], [256, 332], [316, 332]]), sh(P, -0.12), box(256, 176, 60, 156), { gloss: 0 });
      soft(c, poly([[120, 318], [256, 334], [392, 318], [344, 400], [168, 400]]), P, box(120, 318, 272, 82), { gloss: 0.15 });
    },
    bell: function (c) {
      line(c, "M256,0 L256,100", PAL.red, 5);
      line(c, (function () { var p = new Path2D(); p.ellipse(256, 118, 20, 24, 0, Math.PI * 0.95, Math.PI * 2.05); return p; })(), PAL.red, 5);
      soft(c, S("M256,146 C214,146 200,188 198,250 C196,316 178,350 152,368 L360,368 C334,350 316,316 314,250 C312,188 298,146 256,146 Z"), PAL.brass, box(152, 146, 208, 222), { hi: 0.28, lo: -0.26, gloss: 0.45 });
      soft(c, E(256, 370, 108, 14), sh(PAL.brass, -0.18), box(148, 356, 216, 28), { gloss: 0 });
      soft(c, E(256, 390, 16, 16), sh(PAL.brass, -0.3), box(240, 374, 32, 32), { gloss: 0 });
    },
    acorn: function (c) {
      shadow(c, 258, 420, 86, 12);
      soft(c, S("M186,266 C182,344 214,408 258,418 C302,408 334,344 330,266 Z"), "#B07A42", box(182, 266, 152, 152), { gloss: 0.4 });
      soft(c, S("M168,276 C164,216 348,216 344,276 C328,290 184,290 168,276 Z"), "#7A5B3A", box(164, 216, 184, 74), { gloss: 0.15, after: function (x) { x.strokeStyle = "rgba(255,230,190,.25)"; x.lineWidth = 3; x.beginPath(); x.moveTo(176, 262); x.quadraticCurveTo(256, 274, 336, 262); x.stroke(); } });
      line(c, "M256,222 C256,204 262,192 272,184", PAL.woodD, 9);
    },
    lavender: function (c) {
      shadow(c, 256, 420, 100, 12);
      var jar = S("M202,266 L310,266 C310,284 328,292 328,322 L328,398 C328,414 314,420 298,420 L214,420 C198,420 184,414 184,398 L184,322 C184,292 202,284 202,266 Z");
      [[-40, 120], [-18, 94], [4, 84], [26, 100], [46, 126]].forEach(function (s, i) {
        var tx = 256 + s[0] * 1.4, ty = s[1];
        line(c, "M" + (256 + s[0] * 0.2) + ",400 C" + (256 + s[0] * 0.5) + ",320 " + tx + ",230 " + tx + "," + (ty + 50), i % 2 ? PAL.olive : PAL.moss, 3.5);
        soft(c, E(tx, ty + 26, 9, 32, s[0] * 0.004), i % 2 ? "#9486BF" : PAL.lav, box(tx - 9, ty - 6, 18, 64), { gloss: 0.2 });
      });
      c.save(); c.fillStyle = "rgba(205,224,220,.45)"; c.fill(jar); c.clip(jar); c.fillStyle = "rgba(160,192,188,.4)"; c.fillRect(180, 336, 152, 90); c.restore();
      c.save(); c.lineWidth = 2; c.strokeStyle = "rgba(110,135,130,.4)"; c.stroke(jar); c.restore();
      c.save(); blurPx(c, 3); line(c, "M204,290 C200,320 198,360 200,396", "#FFFFFF", 6); c.restore();
      soft(c, rrect(198, 256, 116, 14, 6), "#DCE6E2", box(198, 256, 116, 14), { gloss: 0 });
    },
    candle: function (c) {
      shadow(c, 256, 418, 130, 13);
      glowSoft(c, 256, 176, 180, PAL.amber, 0.34);
      soft(c, E(256, 400, 124, 20), PAL.brass, box(132, 380, 248, 40), { gloss: 0.3 });
      soft(c, S("M214,238 L214,394 Q256,402 298,394 L298,238 Z"), "#F3EBDA", box(214, 230, 84, 170), { after: function (x) { glow(x, 256, 238, 56, "#FFE3A6", 0.45); } });
      soft(c, E(256, 238, 42, 8), "#F8F2E4", box(214, 230, 84, 16), { gloss: 0 });
      line(c, "M256,238 L256,220", PAL.ink, 2.5);
      c.fillStyle = "#F4B24A"; c.fill(S("M256,156 C240,186 238,206 256,220 C274,206 272,186 256,156 Z"));
      c.fillStyle = "#FFE8A8"; c.fill(S("M256,178 C249,196 249,207 256,215 C263,207 263,196 256,178 Z"));
    },
    watch: function (c) {
      shadow(c, 258, 420, 128, 14);
      line(c, (function () { var p = new Path2D(); p.ellipse(256, 124, 18, 16, 0, 0, 7); return p; })(), PAL.brass, 6);
      soft(c, rrect(240, 138, 32, 24, 8), sh(PAL.brass, -0.08), box(240, 138, 32, 24), { gloss: 0 });
      soft(c, E(256, 290, 122, 122), PAL.brass, box(134, 168, 244, 244), { hi: 0.26, lo: -0.26, gloss: 0.35 });
      soft(c, E(256, 290, 100, 100), "#F5EFE0", box(156, 190, 200, 200), { gloss: 0.2 });
      c.save(); c.lineCap = "round"; c.strokeStyle = css(PAL.ink, 0.5); c.lineWidth = 4;
      [0, 1, 2, 3].forEach(function (k) { var a = k * Math.PI / 2; c.beginPath(); c.moveTo(256 + Math.cos(a) * 78, 290 + Math.sin(a) * 78); c.lineTo(256 + Math.cos(a) * 90, 290 + Math.sin(a) * 90); c.stroke(); });
      c.strokeStyle = css(PAL.ink, 0.85); c.lineWidth = 6; c.beginPath(); c.moveTo(256, 290); c.lineTo(256 + Math.cos(-2.62) * 48, 290 + Math.sin(-2.62) * 48); c.stroke();
      c.lineWidth = 4; c.beginPath(); c.moveTo(256, 290); c.lineTo(256 + Math.cos(-0.52) * 72, 290 + Math.sin(-0.52) * 72); c.stroke();
      c.fillStyle = css(PAL.ink); c.beginPath(); c.arc(256, 290, 6, 0, 7); c.fill(); c.restore();
    },
    pinecone: function (c) {
      shadow(c, 258, 420, 92, 12);
      var body = S("M256,160 C318,172 344,250 336,306 C328,364 294,406 256,410 C218,406 184,364 176,306 C168,250 194,172 256,160 Z");
      soft(c, body, "#8A5C36", box(168, 160, 176, 250), { gloss: 0.2 });
      c.save(); c.clip(body);
      [[196, 0.8], [240, 1], [284, 1], [328, 0.95], [372, 0.8]].forEach(function (r, i) {
        var y = r[0], w = 92 * r[1];
        c.strokeStyle = "rgba(60,36,18,.35)"; c.lineWidth = 4; c.lineCap = "round";
        c.beginPath(); c.moveTo(256 - w, y - 8); c.quadraticCurveTo(256 - w / 2, y + 22, 256, y + 2); c.quadraticCurveTo(256 + w / 2, y + 22, 256 + w, y - 8); c.stroke();
        c.strokeStyle = "rgba(255,226,180,.22)"; c.lineWidth = 3; c.beginPath(); c.moveTo(256 - w, y - 14); c.quadraticCurveTo(256 - w / 2, y + 14, 256, y - 5); c.quadraticCurveTo(256 + w / 2, y + 14, 256 + w, y - 14); c.stroke();
      });
      c.restore();
      line(c, "M256,166 C256,150 262,140 272,132", PAL.woodD, 9);
    },
    can: function (c) {
      var M = "#6E8C7A";
      shadow(c, 252, 420, 150, 14);
      line(c, "M300,254 C316,150 404,168 332,336", sh(M, -0.1), 13);
      soft(c, S("M190,376 L96,240 L114,228 L208,352 Z"), sh(M, -0.04), box(96, 228, 112, 148), { gloss: 0 });
      soft(c, E(98, 230, 20, 12, -0.9), sh(M, -0.14), box(78, 216, 40, 28), { gloss: 0 });
      soft(c, S("M180,258 L180,398 C180,414 204,422 256,422 C308,422 332,414 332,398 L332,258 Z"), M, box(180, 246, 152, 176), { gloss: 0.35 });
      soft(c, E(256, 258, 76, 15), sh(M, 0.1), box(180, 243, 152, 30), { gloss: 0 });
      soft(c, E(256, 259, 60, 10), sh(M, -0.5), box(196, 249, 120, 20), { gloss: 0 });
    },
    basket: function (c) {
      var W = "#C49A64";
      shadow(c, 256, 420, 150, 14);
      [[204, 282, 38, "#C0503A"], [306, 286, 34, "#D9953F"], [256, 268, 42, "#B8443A"]].forEach(function (a) { soft(c, E(a[0], a[1], a[2], a[2] * 0.92), a[3], box(a[0] - a[2], a[1] - a[2], a[2] * 2, a[2] * 2), { gloss: 0.35 }); line(c, "M" + a[0] + "," + (a[1] - a[2] * 0.8) + " L" + (a[0] + 5) + "," + (a[1] - a[2] - 12), PAL.woodD, 4); });
      lightLeaf(c, 262, 228, 42, 12, -0.6, PAL.leaf);
      line(c, "M150,300 C166,188 346,188 362,300", sh(W, -0.12), 10);
      soft(c, S("M136,300 L166,410 C176,420 336,420 346,410 L376,300 Z"), W, box(136, 300, 240, 120), { after: function (x) { [340, 378].forEach(function (y) { x.strokeStyle = "rgba(110,76,40,.3)"; x.lineWidth = 4; x.beginPath(); x.moveTo(130, y); x.quadraticCurveTo(256, y + 10, 382, y); x.stroke(); }); } });
      soft(c, rrect(130, 292, 252, 18, 9), sh(W, -0.05), box(130, 292, 252, 18), { gloss: 0 });
    },
    shell: function (c) {
      shadow(c, 256, 414, 136, 12);
      var hx = 256, hy = 394, R = 150, n = 7, A0 = -Math.PI + 0.36, A1 = -0.36;
      var fan = new Path2D(); fan.moveTo(hx - 14, hy);
      for (var i = 0; i <= n * 12; i++) { var t = i / (n * 12), a = lerp(A0, A1, t), bump = Math.pow(Math.abs(Math.sin(t * n * Math.PI)), 0.7) * 0.05; fan.lineTo(hx + Math.cos(a) * R * (0.95 + bump), hy + Math.sin(a) * R * (0.95 + bump) * 0.92); }
      fan.lineTo(hx + 14, hy); fan.closePath();
      soft(c, fan, "#EDC6A8", box(hx - R, hy - R, R * 2, R), { gloss: 0.3, after: function (x) { for (var k = 1; k < n; k++) { var a = lerp(A0, A1, k / n); x.strokeStyle = "rgba(190,120,90,.3)"; x.lineWidth = 3; x.beginPath(); x.moveTo(hx, hy); x.lineTo(hx + Math.cos(a) * R, hy + Math.sin(a) * R * 0.92); x.stroke(); } } });
      soft(c, S("M224,388 L288,388 L300,414 L212,414 Z"), "#E2B494", box(212, 388, 88, 26), { gloss: 0 });
    },
    hourglass: function (c) {
      shadow(c, 256, 420, 120, 13);
      soft(c, rrect(178, 136, 12, 256, 6), sh(PAL.wood, -0.2), box(178, 136, 12, 256), { gloss: 0 });
      var gl = S("M200,136 C200,218 250,238 252,263 C250,288 200,308 200,390 L312,390 C312,308 262,288 260,263 C262,238 312,218 312,136 Z");
      c.save(); c.fillStyle = "rgba(214,228,224,.45)"; c.fill(gl); c.clip(gl);
      soft(c, S("M190,198 Q256,214 322,198 L322,270 L190,270 Z"), "#DDB676", box(190, 198, 132, 72), { gloss: 0 });
      soft(c, S("M190,392 L190,372 C220,350 240,320 256,318 C272,320 292,350 322,372 L322,392 Z"), "#DDB676", box(190, 318, 132, 74), { gloss: 0 });
      c.fillStyle = "#DDB676"; c.fillRect(254.5, 262, 3, 58); c.restore();
      c.save(); c.lineWidth = 2; c.strokeStyle = "rgba(110,130,125,.4)"; c.stroke(gl); c.restore();
      c.save(); blurPx(c, 3); line(c, "M212,152 C214,190 226,218 240,236", "#FFFFFF", 5); c.restore();
      soft(c, rrect(322, 136, 12, 256, 6), PAL.wood, box(322, 136, 12, 256), { gloss: 0 });
      soft(c, rrect(160, 112, 192, 26, 10), PAL.wood, box(160, 112, 192, 26));
      soft(c, rrect(160, 390, 192, 28, 10), PAL.wood, box(160, 390, 192, 28));
    }
  };
  PROPS.forEach(function (p) { if (SIMPLE[p.id]) p.draw = SIMPLE[p.id]; });
  propCanvas = function (cv, p) {
    var ctx = cv.getContext("2d"); ctx.clearRect(0, 0, cv.width, cv.height);
    ctx.save(); ctx.scale(cv.width / 512, cv.height / 512); p.draw(ctx); ctx.restore();
    grain(ctx, 0, 0, cv.width, cv.height, 0.35);
  };
