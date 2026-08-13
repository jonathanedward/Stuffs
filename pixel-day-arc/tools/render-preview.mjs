/**
 * Render design/day-arc-1009.svg to the preview drawable at exactly 450x450.
 *
 * Keeps preview.png honest: it is generated from the same SVG that carries the
 * geometry, so it can't drift from the design the way a hand-exported image
 * would.
 *
 *   node tools/render-preview.mjs
 *
 * Needs Playwright. If it's installed globally rather than in this project,
 * point NODE_MODULES at it:
 *   NODE_MODULES=/usr/lib/node_modules node tools/render-preview.mjs
 *
 * Loading the .svg directly in a browser is not equivalent — the document gets
 * body margin and scrollbars, which crops the bottom of the dial and silently
 * loses the curved complications. Hence the wrapper and the fixed viewport.
 */
import { createRequire } from 'module';
import { readFileSync } from 'fs';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '..');

const require = createRequire(
  process.env.NODE_MODULES ? `${process.env.NODE_MODULES}/` : import.meta.url
);

let chromium;
try {
  ({ chromium } = require('playwright'));
} catch {
  console.error('Playwright not found. Install it, or set NODE_MODULES to a');
  console.error('directory containing it, e.g. /usr/lib/node_modules');
  process.exit(1);
}

// The interactive face is the watch face's own preview drawable; the ambient
// renders live in design/ as documentation of the always-on state.
const jobs = [
  ['design/day-arc-1009.svg',         'app/src/main/res/drawable/preview.png'],
  ['design/day-arc-configured.svg',   'design/configured.png'],
  ['design/day-arc-ambient-1009.svg', 'design/ambient-1009.png'],
  ['design/day-arc-ambient-2215.svg', 'design/ambient-2215.png'],
];

const browser = await chromium.launch();
const page = await browser.newPage({
  viewport: { width: 450, height: 450 },
  deviceScaleFactor: 1,
});

for (const [svg, out] of jobs) {
  const html = `<!doctype html><meta charset="utf-8">
<style>
  html,body{margin:0;padding:0;background:transparent;overflow:hidden}
  svg{display:block;width:450px;height:450px}
</style>
${readFileSync(resolve(root, svg), 'utf8')}`;

  await page.setContent(html, { waitUntil: 'load' });
  await page.screenshot({ path: resolve(root, out), omitBackground: true });
  console.log(`wrote ${out}`);
}

await browser.close();
