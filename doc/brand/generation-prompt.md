# 配套插画生成记录

- 文件：`hero-art.png`
- 方式：内置 `image_gen` 工具，非 CLI/API 回退模式
- 输入：仅文本，无参考图像
- 输出：1536 × 1024 PNG；直接复制原始生成文件
- 主标志、字标、横幅和功能图标为原生 SVG，设计源见 `tools/brand/generate.mjs`
- 分享封面通过 `social-card.html` 组合本地插画与字标，由本地浏览器导出

## 完整提示词

```text
Use case: stylized-concept
Asset type: supporting hero illustration for the xresloader open-source spreadsheet-to-game-data conversion engine, landscape 1536 x 1024.
Primary request: Create a refined, modern editorial 3D illustration of structured spreadsheet data becoming compact reusable data modules. This is supporting artwork for a geometric brand system.
Scene/backdrop: seamless deep ink navy (#10252B) studio backdrop, restrained atmosphere.
Subject: one substantial, thin rounded rectangular spreadsheet slab angled in isometric perspective at left-center, made from matte off-white ceramic with a precise inset 3-by-4 grid. A short, orderly stream of detached mint and turquoise rectangular cells flows toward the right, resolving into three beautifully stacked rounded data tiles, with one small amber accent tile. The cells must feel like structured data, not random debris. The slab and output tiles should share a coherent isometric projection.
Style/medium: premium minimal 3D product sculpture, crisp geometry, soft bevels, tactile ceramic and a few translucent sea-glass elements, sophisticated technology publication art.
Composition/framing: single coherent scene, all objects fully visible, generous negative space on every edge; visual subject occupies central 65 percent, designed to crop safely to wide banner ratios.
Lighting/mood: soft studio light from upper left, gentle contact shadows, subtle mint bounce, quiet and precise.
Color palette: deep ink navy, warm porcelain, muted teal, fresh mint, tiny amber accent.
Text: no text, no letters, no numbers, no logos.
Constraints: no photo frames, no interface screenshot, no watermark, no tiny intricate ornament, no neon cyberpunk, no excessive glow, no purple gradient.
```
