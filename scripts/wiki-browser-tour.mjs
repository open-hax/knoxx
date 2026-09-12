import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { observeBrowserResponse } from './browser-response-observer.mjs';

// This stage uses a real authenticated browser and shared command callback.
// The supervisor owns service startup, isolated fixture creation and cleanup.
// Recovery note: reconstructed source must receive new browser evidence;
// screenshots from the deleted golden checkout are historical evidence only.

export async function annotatedScreenshot(page, outputDir, name, caption, selectors = []) {
  assert(path.isAbsolute(outputDir), 'Screenshot outputDir must be absolute');
  await fs.mkdir(outputDir, { recursive: true });
  const file = path.join(outputDir, `${name}.png`);
  await page.evaluate(({ caption: note, selectors: targets }) => {
    const nodes = [];
    const banner = document.createElement('div');
    banner.dataset.wikiTourAnnotation = 'true';
    banner.textContent = `Browser walkthrough · ${note}`;
    banner.style.cssText = 'position:fixed;bottom:12px;left:12px;right:12px;padding:14px 20px;z-index:2147483647;background:#07334c;color:#fff;border:1px solid #54d5f1;border-radius:9px;font:13px system-ui;pointer-events:none';
    document.body.append(banner);
    nodes.push(banner);
    targets.forEach((selector, index) => {
      const target = document.querySelector(selector);
      if (!target) return;
      const box = target.getBoundingClientRect();
      const outline = document.createElement('div');
      outline.dataset.wikiTourAnnotation = 'true';
      outline.textContent = String(index + 1);
      outline.style.cssText = `position:fixed;left:${box.left}px;top:${box.top}px;width:${box.width}px;height:${box.height}px;border:2px solid #ffc800;border-radius:7px;z-index:2147483646;color:#ffc800;font:bold 18px system-ui;pointer-events:none`;
      document.body.append(outline);
      nodes.push(outline);
    });
  }, { caption, selectors });
  try { await page.screenshot({ path: file, fullPage: true }); }
  finally {
    await page.evaluate(() => document.querySelectorAll('[data-wiki-tour-annotation]').forEach(node => node.remove()));
  }
  return { name, file, caption };
}

async function readReview(page, document) {
  const response = await page.request.get(`/api/publications/documents/${encodeURIComponent(document)}/review`);
  assert(response.ok(), `Source review read failed: ${response.status()} ${await response.text()}`);
  return response.json();
}

async function command(page, method, suffix, click) {
  const observed = observeBrowserResponse(page, response => {
    const url = new URL(response.url());
    return response.request().method() === method && url.pathname.endsWith(suffix);
  }, async response => {
    assert(response.ok(), `Command failed: ${response.status()} ${await response.text()}`);
    return response.json();
  });
  try { await click(); return await observed.value(); }
  finally { observed.cancel(); }
}

async function review(page, action, notes = '', lessons = '') {
  await page.getByRole('tab', { name: 'Discussion & history', exact: true }).click();
  await page.getByLabel('Review notes', { exact: true }).fill(notes);
  await page.getByLabel('Lessons for future drafts', { exact: true }).fill(lessons);
  const labels = { submit: 'Submit for content review', request_changes: 'Request revision', accept: 'Accept source content', comment: 'Add comment' };
  return command(page, 'POST', '/review', () => page.getByRole('button', { name: labels[action], exact: true }).click());
}

async function save(page, content) {
  await page.getByRole('tab', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Source content', { exact: true }).fill(content);
  return command(page, 'PATCH', '/source', () => page.getByRole('button', { name: 'Save revision', exact: true }).click());
}

export async function tour(page, config) {
  const { baseUrl, outputDir, verifyCheckout, garden, targetLocales = ['es'], agentCommand } = config;
  assert.equal(typeof verifyCheckout, 'function', 'The supervisor must verify the served checkout');
  assert.equal(typeof agentCommand, 'function', 'The supervisor must supply real authorized agent commands');
  assert(garden, 'An isolated active garden is required');
  assert.equal(typeof config.beforeSourceAcceptance, 'function', 'The full tour requires source-publication refusal verification');
  assert.equal(typeof config.translationTour, 'function', 'The full tour requires translation, memory and publication verification');
  await verifyCheckout();
  const screenshots = [];
  const shot = async (name, caption, selectors = []) => {
    const result = await annotatedScreenshot(page, outputDir, name, caption, selectors);
    screenshots.push(result);
    return result;
  };
  const title = config.title || `Human and agent content review ${Date.now()}`;
  await page.goto(new URL('/cms', baseUrl).href);
  await page.getByRole('heading', { name: 'Team wiki', exact: true }).waitFor();
  await page.getByRole('button', { name: 'New page', exact: true }).waitFor();
  await page.waitForFunction(() => [...document.querySelectorAll('button')].some(button => button.textContent.trim() === 'New page' && !button.disabled));
  await shot('01-wiki-inventory', 'Real resource inventory and server-granted authoring controls.', ['nav[aria-label="Wiki pages"]']);
  await page.getByRole('button', { name: 'New page', exact: true }).click();
  await page.getByLabel('Page title', { exact: true }).fill(title);
  await page.getByLabel('Initial source content', { exact: true }).fill('# Shared knowledge\n\nPeople and agents improve each page through review.');
  await page.getByLabel('Source locale', { exact: true }).fill('en');
  await page.getByLabel('Target locales', { exact: true }).fill(targetLocales.join(','));
  await page.getByLabel('Garden', { exact: true }).selectOption(garden);
  const created = await command(page, 'POST', '/api/publications/documents', () => page.getByRole('button', { name: 'Create page', exact: true }).click());
  const document = created.review.document;
  let snapshot = created.review;
  assert.equal(snapshot.accepted, false);
  snapshot = (await save(page, '# Shared knowledge\n\nOur team improves each page through review. Every accepted lesson informs the next draft.')).review;
  await shot('02-first-revision', 'The page has a real resource identity and a saved source revision.', ['section[aria-label="Source editor"]']);
  await page.getByLabel('Assistant instruction', { exact: true }).fill('Rewrite this wiki introduction as two short sentences, at most 60 words. Explain that people and agents share roles, while people make final acceptance decisions.');
  const proposal = await command(page, 'POST', '/assist', () => page.getByRole('button', { name: 'Ask writing assistant', exact: true }).click());
  assert.equal(typeof proposal.content, 'string');
  assert(proposal.content.trim(), 'The configured model must return actual content');
  await shot('03-writing-suggestion', 'The configured model returned a suggestion. It has not replaced the saved source.', ['section[aria-label="Writing assistant"]']);
  await page.getByRole('button', { name: 'Use suggestion in draft', exact: true }).click();
  snapshot = (await command(page, 'PATCH', '/source', () => page.getByRole('button', { name: 'Save revision', exact: true }).click())).review;
  snapshot = (await review(page, 'submit', 'Please review the shared-role explanation.')).review;
  snapshot = (await review(page, 'request_changes', 'Name the decision maker explicitly; avoid implying that writing capability grants acceptance authority.', 'Name who makes final acceptance decisions when describing shared human and agent work.')).review;
  await shot('04-content-review', 'A reviewer requested a revision and recorded a reusable writing lesson.', ['section[aria-label="Source review"]']);
  snapshot = (await save(page, `${snapshot.content}\n\nHumans make the final high-level acceptance decisions. Agents contribute through the same authorized review workflow.`)).review;
  snapshot = (await review(page, 'submit', 'The revision names the human acceptance decision explicitly.')).review;
  snapshot = (await review(page, 'accept', 'The source is ready for translation.')).review;
  assert.equal(snapshot.accepted, true);
  assert(snapshot.lessons.length, 'Accepted writing lessons must be remembered');
  await shot('05-source-accepted', 'Source acceptance binds to this revision and retains the writing lesson.', ['section[aria-label="Source review"]']);
  const agentNote = `Agent discussion update ${Date.now()}`;
  await agentCommand({ kind: 'review', document, action: 'comment', notes: agentNote });
  await page.getByText(agentNote, { exact: true }).waitFor();
  await shot('06-agent-update-live', 'A real agent command updated the already-open discussion through SSE.', ['section[aria-label="Revision discussion"]']);
  await page.getByRole('tab', { name: 'Edit', exact: true }).click();
  const dirtyText = `${snapshot.content}\n\nAn unfinished human draft must survive concurrent changes.`;
  await page.getByLabel('Source content', { exact: true }).fill(dirtyText);
  const dialogPromise = page.waitForEvent('dialog');
  const navigationAttempt = page.getByRole('link', { name: 'Mail', exact: true }).click();
  await (await dialogPromise).dismiss();
  await navigationAttempt;
  assert.equal(new URL(page.url()).pathname, '/cms');
  assert.equal(await page.getByLabel('Source content', { exact: true }).inputValue(), dirtyText);
  await agentCommand({ kind: 'source', document, content: `${snapshot.content}\n\nA new revision from the agent requires fresh acceptance.` });
  await page.getByRole('button', { name: 'Discard draft and load latest', exact: true }).waitFor();
  assert.equal(await page.getByLabel('Source content', { exact: true }).inputValue(), dirtyText);
  assert(await page.getByRole('button', { name: 'Save revision', exact: true }).isDisabled());
  await shot('07-concurrent-draft', 'A concurrent agent revision preserves the unfinished human draft and blocks stale saving.', ['section[aria-label="Source editor"]']);
  await page.getByRole('button', { name: 'Discard draft and load latest', exact: true }).click();
  snapshot = await readReview(page, document);
  assert.equal(snapshot.accepted, false);
  await shot('08-review-invalidated', 'New source bytes invalidate acceptance of the previous revision.');
  const sourceGateResult = await config.beforeSourceAcceptance(page, { document, snapshot, shot });
  snapshot = (await review(page, 'submit', 'Review the new source revision.')).review;
  snapshot = (await review(page, 'accept', 'Accept the latest revision for translation.')).review;
  await page.getByRole('link', { name: 'Open translation reviews →', exact: true }).click();
  const translationResult = await config.translationTour(page, { document, snapshot, shot });
  assert.equal(translationResult.completed, true, 'Both translation cycles must complete');
  assert.equal(translationResult.memoryVerified, true, 'Canonical accepted-correction memory must be verified');
  await page.goto(new URL('/translations', baseUrl).href);
  await page.getByRole('heading', { name: 'Translation Review', exact: true }).waitFor();
  await shot('09-translation-workspace', 'The translation workspace is ready for the next review cycle.');
  const result = { document, title, screenshots, sourceGateResult, translationResult, translationTourCompleted: true };
  await fs.writeFile(path.join(outputDir, 'wiki-tour.json'), `${JSON.stringify(result, null, 2)}\n`);
  return result;
}
