# 에셋 이미지 프롬프트

지금 에셋은 코드로 그린 시안입니다 (`design/art/`). 이 문서는 나중에 AI 그림으로 바꾸고 싶을 때 쓰는 예비 프롬프트입니다. 결과물은 `design/art/` 의 같은 파일 이름으로 바꿔 넣으면 됩니다. 하루(조약돌)는 AI로 만들지 않고 코드로 그립니다 (`design/pebble.md`).

## 쓰는 법
- 도구: ChatGPT(이미지 생성), Gemini, Midjourney 중 편한 것. 모두 같은 프롬프트를 씁니다.
- 그림체 맞추기: 첫 결과 중 마음에 드는 한 장을 고른 뒤, 이후 요청마다 그 그림을 ‘참고 이미지’로 함께 올립니다. Midjourney는 `--sref` 에 그 그림 주소를 넣습니다.
- 소품은 한 번에 하나씩, 흰 배경으로 만든 뒤 배경을 지웁니다(투명 PNG).
- 크기: 소품 1024×1024, 배경 1290×2796(세로) 이상.
- 결과물은 `design/art/` 의 같은 이름 파일을 바꿔 넣습니다 (`design/art/README.md` 의 크기 참고).
- 빛은 모두 왼쪽 위에서 옵니다. 하루도 같은 방향으로 그립니다.

## 스타일 기준
모든 프롬프트 앞에 붙는 문장입니다. 아래 각 프롬프트에는 이미 들어 있습니다.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces
```

피할 것 (프롬프트 끝에 붙이거나, 도구의 ‘negative prompt’ 칸에 넣음):

```
Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 스타일 샘플
처음 한 번, 이것만 넣어 그림체 샘플을 4장 뽑고 가장 마음에 드는 한 장을 이후 참고 이미지로 씁니다.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. A small sample sheet showing a clay flower pot with a sprout, a paper lantern, and a teacup, arranged on a plain warm off-white background (#F3F2EC), evenly spaced.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 정원 — 유리병
배경 없이 병만. 1024×1280. 병 안 바닥에 이끼, 위쪽은 비워 둡니다(소품 자리).

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. A single large glass terrarium jar with a wooden cork lid, front view, clear glass with subtle reflections on the left side, a thin layer of dark soil and soft green moss at the bottom, small pebbles on the moss, the upper two thirds of the jar empty and airy, isolated on a plain warm off-white background (#F3F2EC), centered, 4:5 aspect ratio.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 정원 — 계절 배경 4장
유리병 뒤에 깔리는 창밖 풍경. 같은 구도로 계절만 바꿔 4번 요청합니다. 세로 1290×2796.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. A soft blurred view through a window: rolling hills and distant trees in [early spring with pale new leaves and a few blossoms | lush early summer greens | warm autumn with golden and rust leaves | quiet winter with light snow], morning haze, very low detail, large empty calm space in the center, portrait 9:19.5.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 소품 20종 (하나씩)
[ ] 안을 하나씩 바꿔 20번 요청합니다. 같은 참고 이미지를 계속 올리세요.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. A single small object: [a clay flower pot with a tiny sprout | a paper lantern with a warm glow | a stack of three old books | a steaming ceramic teacup | a tiny wooden chair | a hanging paper star | a red mushroom with white dots | a small snow globe | a crescent moon charm | a folded paper boat | a little brass bell | an acorn | a sprig of lavender in a jar | a candle in a holder | a round pocket watch | a pinecone | a tiny watering can | a woven basket of apples | a smooth seashell | a small hourglass], three-quarter view, soft contact shadow underneath, isolated on a plain warm off-white background (#F3F2EC), centered, square 1:1.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 테마 배경 6종
앱 화면 뒤의 은은한 배경. 글자가 올라가므로 아주 흐리고 단순해야 합니다.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. An extremely soft, abstract background wash for a mobile app, gentle light glow in the upper left corner fading into [warm stone and olive | dawn pink and lilac | sea teal and mist | autumn amber and rust | snow white and pale blue | night indigo with faint stars], subtle paper grain texture, no objects, no horizon line, portrait 9:19.5.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 후원 감사 그림
응원하기 화면 위쪽 그림. 하루(조약돌)는 앱이 따로 그려 넣으니 빈 자리를 둡니다.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. A cozy windowsill at golden hour with a steaming teacup and a paper lantern, an empty smooth spot in the center of the sill left clear, warm light, very calm, landscape 3:2.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```

## 앱스토어 · Play 스토어 홍보 배경
스크린샷 뒤에 깔 배경. 글자는 나중에 따로 얹습니다.

```
Hand-drawn illustration with soft chalk pastel and graphite pencil on textured cream paper, visible paper grain, loose uneven strokes, slightly wobbly pencil outlines, lots of empty paper, calm and quiet mood, muted natural palette of olive green (#5F7236), warm stone beige (#E7E6DB), clay (#B5651D) and soft amber light (#F2B35A), gentle warm light from the upper left, soft contact shadows, simple shapes with few details, cozy and minimal, no text, no letters, no logos, no people, no characters, no faces. A calm, spacious background for app store screenshots: a soft olive and stone gradient with a faint warm glow at the top, a few scattered tiny leaves near the bottom edge, lots of empty space, portrait 9:19.5.

Avoid: text, watermark, signature, faces, eyes, characters, anime style, 3D render, glossy plastic, neon colors, heavy outlines, busy details, dark horror mood
```
