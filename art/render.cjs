// Rasterize every SVG listed in jobs.json with headless Chromium.
const fs = require('fs'), path = require('path');
let pw;
try { pw = require('playwright'); } catch { pw = require('/opt/node-tools/node_modules/playwright'); }
(async () => {
  const dir = process.argv[2];
  const jobs = JSON.parse(fs.readFileSync(path.join(dir, 'jobs.json')));
  const browser = await pw.chromium.launch();
  const page = await browser.newPage();
  for (const j of jobs) {
    const svg = fs.readFileSync(path.join(dir, j.name + '.svg'), 'utf8')
      .replace(/<svg([^>]*?)\swidth="[^"]*"\s+height="[^"]*"/, '<svg$1');
    await page.setViewportSize({ width: j.w, height: j.h });
    await page.setContent(`<html><body style="margin:0;background:transparent">
      <div style="width:${j.w}px;height:${j.h}px">${svg.replace('<svg', '<svg width="100%" height="100%"')}</div></body></html>`);
    await page.screenshot({ path: path.join(dir, j.name + '.png'), omitBackground: j.transparent });
  }
  await browser.close();
})();
