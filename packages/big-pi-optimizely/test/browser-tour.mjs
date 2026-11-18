import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdtemp, mkdir, rm } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { randomBytes } from 'node:crypto';
import { createServer } from 'node:net';
import { chromium } from 'playwright';

const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function freePort() {
  const server = createServer();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

test('browser authoring preserves technical text, versions and unsaved edits', { timeout: 60000 }, async () => {
  const dir = await mkdtemp(join(tmpdir(), 'big-pi-browser-'));
  const port = await freePort(), token = randomBytes(24).toString('hex');
  const base = `http://127.0.0.1:${port}`;
  const api = async (path, options = {}) => fetch(base + path, {
    ...options, headers: { authorization: `Bearer ${token}`, ...options.headers },
  });
  const createContent = body => api('/v1/content', {
    method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body),
  });
  const child = spawn(resolve('node_modules/.bin/nbb'), ['-cp', 'src', '-m', 'big-pi.optimizely.main'], {
    env: { ...process.env, BIG_PI_PORT: String(port), BIG_PI_TOKEN: token, BIG_PI_DATA_DIR: dir }, stdio: ['ignore', 'ignore', 'pipe'],
  });
  let diagnostics = '', browser, page;
  child.stderr.on('data', data => { diagnostics += data; });
  const errors = [];
  try {
    let ready = false;
    for (let i = 0; i < 100; i++) {
      if (child.exitCode !== null) throw new Error(`Server exited: ${diagnostics}`);
      try { ready = (await fetch(`${base}/healthz`)).ok; } catch {}
      if (ready) break;
      await pause(100);
    }
    assert.ok(ready, `Server unavailable: ${diagnostics}`);
    const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || (existsSync('/usr/bin/chromium') ? '/usr/bin/chromium' : undefined);
    browser = await chromium.launch({ executablePath, headless: true });
    page = await browser.newPage({ viewport: { width: 1365, height: 1100 } });
    page.on('pageerror', error => errors.push(error.message));
    page.on('dialog', dialog => dialog.accept());
    await page.goto(base);
    await page.locator('#token').fill(token);
    await page.locator('#connect').click();
    await page.locator('#seed:enabled').waitFor();
    await page.locator('#seed').click();
    await page.getByText('Technical example created. All values are synthetic test data.').waitFor();
    const key = await page.locator('#content-id').textContent();
    const original = await page.locator('#body').inputValue();
    assert.match(original, /−20 °C to 120 °C/);
    assert.match(original, /0\.05 °C/);
    // JSON Merge Patch uses null for deletion; editing Body must leave other nulls intact.
    assert.equal((await createContent({ key: 'null-properties', contentType: 'Article', initialVersion: {
      displayName: 'Null preservation', locale: 'en', properties: { Body: 'Original', Other: null, nested: { value: null, kept: 7 } },
    } })).status, 201);
    await page.locator('#refresh').click();
    await page.getByRole('button', { name: /null-properties/ }).click();
    await page.getByRole('heading', { name: 'Null preservation' }).waitFor();
    await page.locator('#body').fill('Edited plain text');
    await page.locator('#save').click();
    await page.getByText('Draft saved locally.').waitFor();
    const preserved = await (await api('/v1/content/null-properties/versions/1')).json();
    assert.deepEqual(preserved.properties, { Body: 'Edited plain text', Other: null, nested: { value: null, kept: 7 } });
    await page.getByRole('button', { name: new RegExp(key) }).click();
    await page.getByRole('heading', { name: 'Thermal sensor · field note' }).waitFor();
    await page.locator('#body').fill(original + '\n\nTerminology note: retain the distinction.');
    await page.locator('#save').click();
    await page.getByText('Draft saved locally.').waitFor();
    await page.locator('#locale').selectOption('de');
    const translated = 'Synthetische Testdaten. Bereich: −20 °C bis 120 °C. Referenz: 0.05 °C.\nKalibrierung und Justierung sind verschiedene Vorgänge.';
    await page.locator('#body').fill(translated);
    await page.locator('#new-locale').click();
    await page.getByText('Language draft created. It is ready for translation and review.').waitFor();
    assert.equal(await page.locator('#body').inputValue(), translated);
    assert.equal(await page.locator('#locale').inputValue(), 'de');
    const version = await page.locator('#versions').inputValue();
    let current = await api(`/v1/content/${key}/versions/${version}`);
    const currentEtag = current.headers.get('etag');
    const draft = await current.json();
    assert.equal(draft.properties.PartNumber, 'TEST-001');
    assert.equal(draft.status, 'draft');
    assert.equal((await (await api(`/v1/content/${key}/versions/1`)).json()).locale, 'en');

    // A concurrent API edit must not silently overwrite the user's unsaved text.
    assert.equal((await api(`/v1/content/${key}/versions/${version}`, {
      method: 'PATCH', headers: { 'content-type': 'application/merge-patch+json', 'if-match': currentEtag },
      body: JSON.stringify({ displayName: 'Changed in another client' }),
    })).status, 204);
    const unsaved = translated + '\nMy pending revision.';
    await page.locator('#body').fill(unsaved);
    await page.locator('#save').click();
    await page.getByText('This draft changed since you loaded it.', { exact: false }).waitFor();
    assert.equal(await page.locator('#body').inputValue(), unsaved);
    await page.locator('#versions').selectOption('1');
    await page.locator('#save:enabled').waitFor();
    assert.equal(await page.locator('#locale').inputValue(), 'en');
    await page.locator('#versions').selectOption(version);
    await page.getByRole('heading', { name: 'Changed in another client' }).waitFor();
    assert.equal(await page.locator('#body').inputValue(), translated);

    // Collections must remain reachable after the API's default 100-item page.
    for (let i = 0; i < 100; i++) {
      assert.equal((await createContent({ key: `page-${String(i).padStart(3, '0')}`, contentType: 'Article', initialVersion: {
        displayName: `Page fixture ${i}`, locale: 'en', properties: { Body: 'Synthetic pagination fixture' },
      } })).status, 201);
    }
    assert.equal((await createContent({ key: 'zz-last-content', contentType: 'Article', initialVersion: {
      displayName: 'Last page content', locale: 'en', properties: { Body: 'Visible beyond the first page' },
    } })).status, 201);
    for (let i = 0; i < 100; i++) {
      assert.equal((await api(`/v1/content/${key}/versions`, {
        method: 'POST', headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ displayName: `Version ${i + 3}`, locale: 'en', properties: { Body: original } }),
      })).status, 201);
    }
    await page.locator('#refresh').click();
    await page.getByRole('button', { name: /zz-last-content/ }).click();
    await page.getByRole('heading', { name: 'Last page content' }).waitFor();
    await page.getByRole('button', { name: new RegExp(key) }).click();
    await page.getByRole('heading', { name: 'Version 102' }).waitFor();
    assert.equal(await page.locator('#versions option').count(), 102);
    assert.equal(await page.locator('#versions').inputValue(), '102');
    await page.locator('#versions').selectOption(version);
    await page.getByRole('heading', { name: 'Changed in another client' }).waitFor();

    // An unsupported field shape must leave the previous editor selection intact.
    await api('/v1/content', { method: 'POST', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ key: 'unsupported-shape', contentType: 'Article', initialVersion: { locale: 'en', displayName: 'Structured field', properties: { Body: { html: '<p>Nested</p>' } } } }),
    });
    await page.locator('#refresh').click();
    await page.getByRole('button', { name: /unsupported-shape/ }).click();
    await page.getByText('This editor supports the plain text Body field.', { exact: false }).waitFor();
    assert.equal(await page.locator('#content-id').textContent(), key);
    assert.equal(await page.locator('#body').inputValue(), translated);
    await page.locator('#versions').selectOption('1');
    await page.getByRole('heading', { name: 'Thermal sensor · field note' }).waitFor();
    await page.locator('#versions').selectOption(version);
    await page.getByRole('heading', { name: 'Changed in another client' }).waitFor();

    const screenshots = resolve(process.env.BIG_PI_SCREENSHOTS_DIR || 'screenshots');
    await mkdir(screenshots, { recursive: true });
    // Remove the test secret from the form before capturing evidence.
    await page.locator('#token').fill('');
    await page.screenshot({ path: join(screenshots, 'desktop.png'), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'mobile layout must not overflow');
    await page.screenshot({ path: join(screenshots, 'mobile.png'), fullPage: true });
    await page.locator('#disconnect').click();
    assert.equal(await page.locator('#save').isDisabled(), true);
    assert.equal(await page.locator('#body').inputValue(), '');
    assert.deepEqual(errors, []);
  } catch (error) {
    if (page) {
      console.error('Workbench status:', await page.locator('#notice').textContent({ timeout: 2000 }).catch(() => 'unavailable'));
      console.error('Page errors:', errors);
      const screenshots = resolve(process.env.BIG_PI_SCREENSHOTS_DIR || 'screenshots');
      await mkdir(screenshots, { recursive: true });
      await page.screenshot({ path: join(screenshots, 'failed.png'), fullPage: true }).catch(() => {});
    }
    throw error;
  } finally {
    await browser?.close();
    if (child.exitCode === null) { child.kill('SIGTERM'); await new Promise(resolve => child.once('exit', resolve)); }
    await rm(dir, { recursive: true, force: true });
  }
});
