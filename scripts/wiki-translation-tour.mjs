import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { observeBrowserResponse } from './browser-response-observer.mjs';

// Real browser commands and persisted evidence only. The supervisor owns the
// model, account, resource fixtures, static artifact server and Clio reader.
const localeNames = { en: 'English', es: 'Español', fr: 'Français', de: 'Deutsch', ja: '日本語', zh: '中文', ko: '한국어', pt: 'Português', ru: 'Русский', it: 'Italiano' };
const defaultCorrections = {
  es: {
    first: 'Las personas deciden la aceptación final. Los agentes contribuyen con las mismas funciones autorizadas.',
    accepted: 'Las personas toman las decisiones finales de aceptación; los agentes contribuyen mediante las mismas funciones autorizadas.',
  },
};
const endpoint = (document, suffix) => `/api/publications/documents/${encodeURIComponent(document)}/${suffix}`;
const absolute = (page, url) => new URL(url, page.url()).href;
const sha256 = bytes => createHash('sha256').update(bytes).digest('hex');

async function jsonGet(page, url) {
  const response = await page.request.get(absolute(page, url));
  assert(response.ok(), `GET ${url}: ${response.status()} ${await response.text()}`);
  return response.json();
}

async function command(page, method, pathname, click, timeout = 180_000) {
  const observed = observeBrowserResponse(page, response => response.request().method() === method
    && new URL(response.url()).pathname === pathname, async response => {
    assert(response.ok(), `${method} ${pathname}: ${response.status()} ${await response.text()}`);
    return response.json();
  }, timeout);
  try { await click(); return await observed.value(); }
  finally { observed.cancel(); }
}

async function until(read, predicate, description, timeout = 180_000) {
  const deadline = Date.now() + timeout;
  let latest;
  while (Date.now() < deadline) {
    latest = await read();
    if (predicate(latest)) return latest;
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  assert.fail(`Timed out waiting for ${description}; latest=${JSON.stringify(latest)}`);
}

async function inventory(page, document) {
  const payload = await jsonGet(page, '/api/publications/translations/reviews');
  assert(Array.isArray(payload.reviews), 'Translation inventory must contain real work rows');
  return payload.reviews.map(row => ({ ...row, ...(row.candidate || {}) }))
    .filter(row => row.document === document);
}

async function work(page, document, publication) {
  const rows = await inventory(page, document);
  const selected = rows.find(row => row.publication === publication);
  assert(selected, `Publication ${publication} is missing from translation inventory`);
  return selected;
}

function candidate(row) {
  const review = row?.split_review;
  return review?.candidate_set_id && review?.manifest_id && review.splits?.length ? review : null;
}

async function openTranslation(page, row) {
  await page.goto(absolute(page, '/translations'));
  await page.getByRole('heading', { name: 'Translation Review', exact: true }).waitFor();
  const card = page.getByRole('button').filter({ hasText: row.title })
    .filter({ hasText: localeNames[row.locale] || row.locale }).filter({ hasText: row.garden });
  await card.waitFor();
  assert.equal(await card.count(), 1, 'Select the exact document, locale and garden work row');
  await card.click();
}

async function dispatch(page, document, row, sourceRevision, priorCandidate, timeout) {
  await openTranslation(page, row);
  const action = row.allowed_actions?.includes('retry') ? 'Retry' : 'Dispatch';
  const receipt = await command(page, 'POST', '/api/publications/translations/dispatch',
    () => page.getByRole('button', { name: action, exact: true }).click(), timeout);
  const generated = await until(() => work(page, document, row.publication), value => {
    const selected = candidate(value);
    return value.revision === sourceRevision && value.reviewable === true
      && selected && selected.candidate_set_id !== priorCandidate;
  }, `a real generated candidate for ${row.publication}`, timeout);
  assert(candidate(generated).splits.every(split => typeof split.candidate_text === 'string'),
    'All candidate text must come from the persisted model result');
  await openTranslation(page, generated);
  return { row: generated, dispatch: receipt };
}

async function openSplit(page, split) {
  const annotation = page.locator('button[aria-pressed]')
    .filter({ hasText: `seg ${split.split_index}` }).filter({ hasText: split.source_text });
  assert.equal(await annotation.count(), 1, 'The exact persisted source split must be visible');
  await annotation.click();
  await page.getByLabel('Corrected translation', { exact: true }).waitFor();
}

async function splitVerdict(page, text, notes, label) {
  await page.getByLabel('Corrected translation', { exact: true }).fill(text);
  await page.getByLabel('Editor notes', { exact: true }).fill(notes);
  return command(page, 'POST', '/api/publications/translations/reviews',
    () => page.getByRole('button', { name: label, exact: true }).click());
}

async function correctAndAccept(page, document, generated, correction, shot, prefix) {
  const splits = candidate(generated).splits;
  const selected = splits.find(split => /Humans make the final/.test(split.source_text))
    || splits.find(split => !split.source_text.trim().startsWith('#')) || splits[0];
  await openSplit(page, selected);
  await shot(`${prefix}-generated-candidate`, 'The configured model produced these persisted source and target splits.');
  await splitVerdict(page, correction.first, 'First human revision: make acceptance authority explicit.', 'Submit review');
  const first = await until(() => work(page, document, generated.publication), row => {
    const split = candidate(row)?.splits.find(value => value.split_id === selected.split_id);
    return split?.review_status === 'in-review' && split.corrected_text === correction.first;
  }, 'the first immutable correction review');
  const firstReview = candidate(first).splits.find(split => split.split_id === selected.split_id).review_id;
  await openTranslation(page, first);
  await openSplit(page, candidate(first).splits.find(split => split.split_id === selected.split_id));
  await splitVerdict(page, correction.accepted, 'Revised and accepted: distinguish contribution from final acceptance.', 'Approve split');
  const revised = await until(() => work(page, document, generated.publication), row => {
    const split = candidate(row)?.splits.find(value => value.split_id === selected.split_id);
    return split?.review_status === 'approved' && split.corrected_text === correction.accepted;
  }, 'the accepted replacement correction');
  const revisedSplit = candidate(revised).splits.find(split => split.split_id === selected.split_id);
  assert(firstReview && revisedSplit.review_id && firstReview !== revisedSplit.review_id);
  assert(revisedSplit.labels.some(label => label.review_id === firstReview && label.corrected_text === correction.first),
    'The earlier correction must remain in immutable review history');
  await openTranslation(page, revised);
  await openSplit(page, revisedSplit);
  await shot(`${prefix}-correction-history`, 'Correction A remains in history; the accepted correction B is the effective translation.');
  // A fresh page leaves the bulk form empty; a selected split's correction must
  // never be copied to every other split by the tour.
  await openTranslation(page, revised);
  await command(page, 'POST', '/api/publications/translations/reviews/bulk',
    () => page.getByRole('button', { name: 'Approve All', exact: true }).click());
  let accepted = await until(() => work(page, document, generated.publication), row =>
    candidate(row)?.splits.every(split => split.review_status === 'approved'), 'approval of every split');
  const acceptedSplit = candidate(accepted).splits.find(split => split.split_id === selected.split_id);
  assert.equal(acceptedSplit.corrected_text, correction.accepted, 'Bulk acceptance must preserve the exact accepted correction');
  await command(page, 'POST', '/api/publications/translations/approvals',
    () => page.getByRole('button', { name: 'Approve whole output', exact: true }).click());
  accepted = await until(() => work(page, document, generated.publication), row => row.approved === true,
    'the revision-bound whole-output approval');
  return {
    publication: generated.publication, garden: generated.garden, locale: generated.locale,
    sourceRevision: generated.revision, candidateSetId: candidate(generated).candidate_set_id,
    manifestId: candidate(generated).manifest_id, splitId: selected.split_id, sourceText: selected.source_text,
    firstReviewId: firstReview, acceptedReviewId: acceptedSplit.review_id,
    firstCorrection: correction.first, acceptedCorrection: correction.accepted,
    generatedCandidateText: selected.candidate_text, acceptedRow: accepted,
  };
}

async function openWiki(page, title) {
  await page.goto(absolute(page, '/cms'));
  await page.getByRole('navigation', { name: 'Wiki pages', exact: true })
    .getByRole('button', { name: title, exact: true }).click();
}

async function publishAndRead(page, document, snapshot, artifactBaseUrl, shot, prefix) {
  await openWiki(page, snapshot.title);
  const targets = (await jsonGet(page, endpoint(document, 'publications'))).publications;
  assert(targets.length, 'The document needs real publication placements');
  const receipts = [];
  for (const target of targets) {
    const exact = page.locator(`[data-publication-id=${JSON.stringify(target.id)}]`);
    assert.equal(await exact.getAttribute('data-publication-id'), target.id);
    const result = await command(page, 'POST', endpoint(document, 'publish'),
      () => exact.getByRole('button', { name: 'Publish', exact: true }).click());
    assert(result.receipt, 'Publish must return the real reconciler receipt');
    const current = await until(() => jsonGet(page, endpoint(document, 'publications')), value => {
      const observed = value.publications.find(item => item.id === target.id);
      return observed?.desired === 'published' && observed.observed && observed.blockers.length === 0;
    }, `manifest-confirmed publication of ${target.id}`);
    receipts.push({ publication: current.publications.find(item => item.id === target.id), receipt: result.receipt });
  }
  await shot(`${prefix}-publish-decisions`, 'Explicit publication commands now agree with independently observed, manifest-backed artifacts.');
  const artifacts = [];
  for (const { publication } of receipts) {
    const url = new URL(publication.path, artifactBaseUrl);
    assert.equal(url.origin, new URL(artifactBaseUrl).origin, 'Artifacts must come from the supervised manifest-only server');
    const response = await page.request.get(url.href);
    assert.equal(response.status(), 200, `The admitted artifact must actually be served: ${url.href}`);
    const bytes = await response.body();
    assert(bytes.length > 0, 'The served artifact must contain actual bytes');
    await page.goto(url.href);
    await shot(`${prefix}-${publication.locale}-served-artifact`, 'This is the actual published HTML served from the admitted artifact manifest.');
    artifacts.push({ publication: publication.id, url: url.href, sha256: sha256(bytes), bytes: bytes.length });
  }
  return { receipts, artifacts };
}

export async function verifySourceUnaccepted(page, { document, snapshot, shot }) {
  assert.equal(snapshot.accepted, false, 'The negative gate must run on unaccepted source');
  const before = await jsonGet(page, endpoint(document, 'publications'));
  assert(before.publications.length);
  const target = before.publications[0];
  const response = await page.request.post(absolute(page, endpoint(document, 'publish')), {
    data: { publication: target.id, expected_revision: snapshot.revision },
    headers: { Origin: new URL(page.url()).origin },
  });
  const body = await response.json();
  assert.equal(response.status(), 409, 'Publishing unaccepted source must be rejected');
  assert(/source.*review|source.*accept/i.test(JSON.stringify(body)), 'The refusal must identify source acceptance');
  const after = await jsonGet(page, endpoint(document, 'publications'));
  assert.deepEqual(after, before, 'A rejected source publication must not change desired state or materialization');
  await shot('10-source-publication-refused', 'The shared publication command rejects this unaccepted source revision. Desired and observed publication state remain unchanged.');
  return { document, sourceRevision: snapshot.revision, publication: target.id, status: response.status(), response: body };
}

async function nextSourceRevision(page, document, snapshot) {
  await openWiki(page, snapshot.title);
  await page.getByRole('tab', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Source content', { exact: true }).fill(`${snapshot.content}\n\nNew content joins the same review cycle, using accepted translation lessons from previous work.`);
  const saved = await command(page, 'PATCH', endpoint(document, 'source'),
    () => page.getByRole('button', { name: 'Save revision', exact: true }).click());
  assert.notEqual(saved.review.revision, snapshot.revision);
  assert.equal(saved.review.accepted, false, 'A changed source must require new human acceptance');
  await page.getByRole('tab', { name: 'Discussion & history', exact: true }).click();
  await page.getByLabel('Review notes', { exact: true }).fill('Review this new content before its next translation cycle.');
  await command(page, 'POST', endpoint(document, 'review'),
    () => page.getByRole('button', { name: 'Submit for content review', exact: true }).click());
  await page.getByLabel('Review notes', { exact: true }).fill('Accept the new source revision with the existing human decision boundary intact.');
  const accepted = await command(page, 'POST', endpoint(document, 'review'),
    () => page.getByRole('button', { name: 'Accept source content', exact: true }).click());
  assert.equal(accepted.review.accepted, true);
  return accepted.review;
}

export async function translationTour(page, config) {
  const { document, snapshot, shot, artifactBaseUrl, verifyMemory, timeoutMs = 180_000 } = config;
  assert.equal(typeof shot, 'function');
  assert(artifactBaseUrl, 'The full tour requires a manifest-only artifact server');
  assert.equal(typeof verifyMemory, 'function', 'The full tour requires canonical Clio memory verification');
  assert.equal(snapshot.accepted, true);
  const corrections = { ...defaultCorrections, ...(config.correctionsByLocale || {}) };
  const rows = await inventory(page, document);
  assert(rows.length, 'The accepted document needs real target-language work');
  const origin = new URL(page.url()).origin;
  const firstCycle = [];
  for (const row of rows) {
    assert(corrections[row.locale], `Supply human-reviewed corrections for locale ${row.locale}`);
    const generated = await dispatch(page, document, row, snapshot.revision, null, timeoutMs);
    const record = await correctAndAccept(page, document, generated.row, corrections[row.locale], shot, `11-r1-${row.locale}`);
    firstCycle.push({ ...record, dispatch: generated.dispatch });
  }
  const firstPublication = await publishAndRead(page, document, snapshot, artifactBaseUrl, shot, '15-r1');
  await page.goto(new URL('/cms', origin).href);
  const secondSource = await nextSourceRevision(page, document, snapshot);
  await shot('20-new-source-accepted', 'New source bytes required a fresh review and acceptance before a second translation turn.');
  const secondCycle = [];
  for (const prior of firstCycle) {
    const row = await work(page, document, prior.publication);
    const generated = await dispatch(page, document, row, secondSource.revision, prior.candidateSetId, timeoutMs);
    const memoryInput = {
      document, publication: prior.publication, garden: prior.garden, locale: prior.locale,
      firstCandidateSetId: prior.candidateSetId, secondCandidateSetId: candidate(generated.row).candidate_set_id,
      firstSourceRevision: prior.sourceRevision, secondSourceRevision: secondSource.revision,
      acceptedReviewId: prior.acceptedReviewId, acceptedSplitId: prior.splitId,
      sourceText: prior.sourceText, acceptedCorrection: prior.acceptedCorrection,
    };
    const memory = await verifyMemory(memoryInput);
    assert.equal(memory?.verified, true, 'Canonical memory verification must pass, without a warning fallback');
    assert.equal(typeof memory.firstTurnId, 'string');
    assert.equal(typeof memory.secondTurnId, 'string');
    assert(memory.firstTurnId && memory.secondTurnId && memory.firstTurnId !== memory.secondTurnId,
      'Clio must resolve distinct immutable first and second translation turns');
    const record = await correctAndAccept(page, document, generated.row, corrections[row.locale], shot, `21-r2-${row.locale}`);
    secondCycle.push({ ...record, dispatch: generated.dispatch, memoryInput, memory });
  }
  const secondPublication = await publishAndRead(page, document, secondSource, artifactBaseUrl, shot, '25-r2');
  await page.goto(new URL('/cms', origin).href);
  await openWiki(page, snapshot.title);
  return { document, publicationIds: firstCycle.map(row => row.publication), firstCycle, secondCycle,
    firstPublication, secondPublication, firstSourceRevision: snapshot.revision,
    secondSourceRevision: secondSource.revision, memoryVerified: true, completed: true };
}
