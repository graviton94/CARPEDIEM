  /* 한지 그림의 바탕: 찢긴 가장자리 · 한지 섬유 · 낟알 결 · 겹치는 언덕. 좌표 = 정원 단위 (화면 폭 390), U = 단위당 px (pastel.js).
     그림자 흐림 · 거리는 px 라서 SH() (= U / 2) 를 곱해 어느 크기로 구워도 같은 깊이로 보이게. */
  var SW = 390, SH = 700, SGY = 470;
  function SHK() { return U / 2; }
  function R(seed) { return rng(seed); }
  function resample(pts, step) { var out = [], n = pts.length; for (var i = 0; i < n; i++) { var a = pts[i], b = pts[(i + 1) % n], L = Math.hypot(b[0] - a[0], b[1] - a[1]), m = Math.max(1, Math.round(L / step)); for (var k = 0; k < m; k++) { var t = k / m; out.push([a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]); } } return out; }
  function area(pts) { var s = 0; for (var i = 0; i < pts.length; i++) { var a = pts[i], b = pts[(i + 1) % pts.length]; s += a[0] * b[1] - b[0] * a[1]; } return s / 2; }
  function offsetRing(pts, d, jit, seed) { var r = R(seed), P = resample(pts, 1.2), n = P.length, sg = area(P) > 0 ? 1 : -1; return P.map(function (p, i) { var a = P[(i - 1 + n) % n], b = P[(i + 1) % n], tx = b[0] - a[0], ty = b[1] - a[1], l = Math.hypot(tx, ty) || 1, nx = ty / l * sg, ny = -tx / l * sg, dd = d + (r() - 0.5) * 2 * jit; return [p[0] + nx * dd, p[1] + ny * dd]; }); }
  function pathOf(ctx, pts, smoothIt) { ctx.beginPath(); if (smoothIt) { var n = pts.length; for (var i = 0; i < n; i++) { var a = pts[i], b = pts[(i + 1) % n], mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2; i ? ctx.quadraticCurveTo(a[0], a[1], mx, my) : ctx.moveTo(mx, my); } var a0 = pts[0], b0 = pts[1]; ctx.quadraticCurveTo(a0[0], a0[1], (a0[0] + b0[0]) / 2, (a0[1] + b0[1]) / 2); } else pts.forEach(function (p, i) { i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1]); }); ctx.closePath(); }
  function ridgeS(y0, amp, seed, freq, step) { var r = R(seed), f = [r() * 9, r() * 9, r() * 9], pts = []; for (var x = -14; x <= SW + 14; x += step || 5) pts.push([x, y0 - amp * (0.55 * Math.sin(x * freq + f[0]) + 0.3 * Math.sin(x * freq * 2.3 + f[1]) + 0.15 * Math.sin(x * freq * 5.1 + f[2]))]); return pts; }
  function down(top) { return top.concat([[SW + 14, SH + 30], [-14, SH + 30]]); }
  var TEX = {};
  function grainTex(seed, dark, light, n) { var c = document.createElement("canvas"); c.width = c.height = 256; var g = c.getContext("2d"), r = R(seed); for (var i = 0; i < (n || 2400); i++) { g.fillStyle = r() < 0.5 ? "rgba(" + dark + "," + (r() * 0.1) + ")" : "rgba(" + light + "," + (r() * 0.12) + ")"; g.fillRect(r() * 256, r() * 256, 1 + r(), 1 + r()); } return c; }
  function fiberTex(seed) { var c = document.createElement("canvas"); c.width = c.height = 320; var g = c.getContext("2d"), r = R(seed);
    for (var b = 0; b < 26; b++) { g.fillStyle = "rgba(255,255,255," + (0.05 + r() * 0.08) + ")"; g.filter = "blur(8px)"; g.beginPath(); g.ellipse(r() * 320, r() * 320, 20 + r() * 40, 10 + r() * 30, r() * 3, 0, 7); g.fill(); g.filter = "none"; }
    g.lineCap = "round"; for (var i = 0; i < 230; i++) { var x = r() * 320, y = r() * 320, a = r() * 6.28, l = 6 + r() * 26; g.strokeStyle = r() < 0.7 ? "rgba(255,255,255," + (0.18 + r() * 0.3) + ")" : "rgba(90,70,50," + (0.06 + r() * 0.08) + ")"; g.lineWidth = 0.4 + r() * 0.9; g.beginPath(); g.moveTo(x, y); g.quadraticCurveTo(x + Math.cos(a + 0.6) * l * 0.5, y + Math.sin(a + 0.6) * l * 0.5, x + Math.cos(a) * l, y + Math.sin(a) * l); g.stroke(); }
    return c; }
  // 굵기가 변하는 선 (붓 · 진한 테두리): 점마다 굵기
  function varLine(ctx, pts, col, wf, closed) { var P = closed ? pts.concat([pts[0]]) : pts; ctx.save(); ctx.fillStyle = col; ctx.strokeStyle = col; ctx.lineCap = "round"; for (var i = 0; i < P.length - 1; i++) { var t = i / (P.length - 1); ctx.lineWidth = wf(t); ctx.beginPath(); ctx.moveTo(P[i][0], P[i][1]); ctx.lineTo(P[i + 1][0], P[i + 1][1]); ctx.stroke(); } ctx.restore(); }


  function texFill(ctx, tex, alpha, op, scale) { ctx.save(); ctx.globalCompositeOperation = op || "source-atop"; ctx.globalAlpha = alpha; var p = ctx.createPattern(tex, "repeat"); if (p.setTransform) p.setTransform(new DOMMatrix().scale(scale || 0.5)); ctx.fillStyle = p; ctx.fillRect(-1200, -1200, 2800, 2800); ctx.restore(); }
