import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';
import { after, before, test } from 'node:test';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { JSDOM } from 'jsdom';
import postcss from 'postcss';

const frontend = fileURLToPath(new URL('../', import.meta.url));
let output;
let dom;
let bridge;
let react;
let router;
let createRoot;

before(async () => {
  await fs.mkdir(path.join(frontend, 'dist'), { recursive: true });
  output = await fs.mkdtemp(path.join(frontend, 'dist', 'app-bridge-proof-'));
  const vite = path.join(frontend, 'node_modules', 'vite', 'bin', 'vite.js');
  // Build into an empty directory: an old bridge or stylesheet cannot satisfy this proof.
  for (const [config, directory] of [
    ['vite.app-bridge.config.ts', path.join(output, 'bridge')],
    ['vite.config.ts', output],
  ]) {
    execFileSync(process.execPath, [vite, 'build', '--config', config, '--outDir', directory],
      { cwd: frontend, stdio: 'pipe' });
  }
  dom = new JSDOM('<!doctype html><body></body>', { url: 'http://localhost/' });
  globalThis.window = dom.window;
  globalThis.document = dom.window.document;
  Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true });
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  // jsdom has no layout observer. This loading-state proof must never measure layout.
  globalThis.ResizeObserver = class {
    observe() { throw new Error('Layout observation is outside this bridge loading-state proof'); }
    unobserve() {}
    disconnect() {}
  };
  bridge = await import(pathToFileURL(path.join(output, 'bridge', 'knoxx-app-bridge.es.js')));
  react = await import('react');
  router = await import('react-router-dom');
  ({ createRoot } = await import('react-dom/client'));
});

after(async () => {
  dom?.window.close();
  if (output) await fs.rm(output, { recursive: true, force: true });
});

test('fresh production bridge exports the component used by the visual draft route', () => {
  assert.equal(typeof bridge.VisualCmsEditorPage, 'function');
});

test('the rebuilt component consumes the router draft path and its emitted page style', async () => {
  assert.equal(typeof bridge.VisualCmsEditorPage, 'function');
  const requests = [];
  const previousFetch = globalThis.fetch;
  globalThis.fetch = input => { requests.push(String(input)); return new Promise(() => {}); };
  const container = document.createElement('div');
  document.body.append(container);
  const root = createRoot(container);
  try {
    const element = react.createElement;
    await react.act(async () => {
      root.render(element(router.MemoryRouter,
        { initialEntries: ['/cms/editor/drafts/production-proof'],
          future: { v7_startTransition: true, v7_relativeSplatPath: true } },
        element(router.Routes, null,
          element(router.Route, { path: '/cms/editor/*', element: element(bridge.VisualCmsEditorPage) }))));
    });
    assert.deepEqual(requests, [
      `/api/ingestion/file?path=${encodeURIComponent('drafts/production-proof/view-contract.edn')}`,
    ]);
    assert.match(container.textContent, /Loading draft/);
    const pageClass = container.firstElementChild.className;
    assert.ok(pageClass, 'The real component must reference an emitted CSS module class');
    const css = postcss.parse(await fs.readFile(path.join(output, 'bridge', 'style.css'), 'utf8'));
    const declarations = {};
    css.walkRules(rule => {
      if (rule.selectors.includes(`.${pageClass}`)) {
        rule.walkDecls(declaration => { declarations[declaration.prop] = declaration.value; });
      }
    });
    assert.equal(declarations.display, 'flex');
    assert.equal(declarations.height, '100vh');
  } finally {
    await react.act(async () => root.unmount());
    container.remove();
    globalThis.fetch = previousFetch;
  }
});

test('built HTML loads the emitted editor layout stylesheet', async () => {
  const html = new JSDOM(await fs.readFile(path.join(output, 'index.html'), 'utf8'));
  try {
    assert.ok(html.window.document.querySelector('link[rel="stylesheet"][href="/bridge/style.css"]'),
      'The production entry must load CSS extracted from the application bridge');
    const css = postcss.parse(await fs.readFile(path.join(output, 'bridge', 'style.css'), 'utf8'));
    const selectors = [];
    css.walkRules(rule => selectors.push(...rule.selectors));
    for (const layout of ['splitPanel', 'canvasPanel', 'sourcePanel']) {
      assert.ok(selectors.some(selector => selector.startsWith(`._${layout}_`)),
        `The bridge stylesheet must include the visual editor ${layout} layout`);
    }
  } finally {
    html.window.close();
  }
});
