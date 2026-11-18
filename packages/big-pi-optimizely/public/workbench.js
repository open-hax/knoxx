const byId = id => document.getElementById(id);
let token = '', selectedKey = null, currentVersion = null, etag = null, loading = false, versionList = [], dirty = false, connected = false;
const notice = (message, kind = '') => { byId('notice').textContent = message; byId('notice').className = kind; };
function controls() {
  for (const id of ['refresh', 'new', 'seed', 'title', 'body', 'locale', 'save']) byId(id).disabled = !connected || loading;
  for (const id of ['token', 'connect', 'disconnect']) byId(id).disabled = loading;
  for (const id of ['versions', 'new-locale']) byId(id).disabled = !connected || loading || !currentVersion;
  document.querySelectorAll('.content-item').forEach(item => { item.disabled = !connected || loading; });
}
function enabled(value) {
  connected = value; controls();
  byId('disconnect').hidden = !connected;
  byId('connection-status').textContent = connected ? 'Connected locally' : 'Not connected';
}
async function request(path, { method = 'GET', body, headers = {} } = {}) {
  const response = await fetch(path, {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': method === 'PATCH' ? 'application/merge-patch+json' : 'application/json', Prefer: 'return=representation' } : {}), ...headers },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  const text = await response.text();
  let data; try { data = text ? JSON.parse(text) : null; } catch { throw new Error('The local server returned an invalid response.'); }
  if (!response.ok) throw new Error(response.status === 412 ? 'This draft changed since you loaded it. Your text is still here. Select a version again to compare before saving.' : data?.detail || data?.details || data?.title || `Request failed (${response.status}).`);
  return { data, etag: response.headers.get('etag') };
}
async function busy(operation) {
  if (loading) return;
  loading = true; controls();
  try { await operation(); } catch (error) { if (currentVersion) byId('versions').value = currentVersion.version; notice(error.message, 'error'); }
  finally { loading = false; controls(); }
}
function clearEditor() {
  selectedKey = null; currentVersion = null; etag = null; versionList = []; dirty = false;
  byId('title').value = ''; byId('body').value = ''; byId('locale').value = 'en';
  byId('versions').replaceChildren(new Option('New document', '')); byId('versions').disabled = true;
  byId('content-id').textContent = 'NEW DOCUMENT'; byId('editor-heading').textContent = 'Draft editor';
  byId('save').textContent = 'Create draft'; byId('locale-actions').hidden = true;
  byId('revision-note').textContent = 'New content starts as a local draft.';
  document.querySelectorAll('.content-item').forEach(item => item.classList.remove('active'));
  controls();
}
async function refreshList() {
  const { data } = await request('/v1/content');
  const items = data?.items || [];
  byId('content-list').replaceChildren();
  if (!items.length) { const p = document.createElement('p'); p.className = 'empty'; p.textContent = 'No content yet. Create a document or load the example below.'; byId('content-list').append(p); }
  for (const item of items) {
    const button = document.createElement('button'); button.type = 'button'; button.className = `content-item${item.key === selectedKey ? ' active' : ''}`;
    const title = document.createElement('strong'); title.textContent = item.displayName || item.key;
    const meta = document.createElement('span'); meta.textContent = `${item.contentType || 'Content'} · ${item.key}`;
    button.append(title, meta); button.addEventListener('click', () => { if (dirty && !confirm('Discard your unsaved changes?')) return; busy(() => selectContent(item.key)); });
    byId('content-list').append(button);
  }
}
async function selectContent(key, requestedVersion) {
  const { data } = await request(`/v1/content/${encodeURIComponent(key)}/versions`);
  const versions = data.items || [];
  if (!versions.length) throw new Error('This content has no versions.');
  await loadVersion(requestedVersion || versions.at(-1).version, key);
  versionList = versions;
  byId('versions').replaceChildren(...versionList.map(v => new Option(`${v.locale} · ${v.version} · ${v.status}`, v.version)));
  byId('versions').value = currentVersion.version;
  await refreshList();
}
async function loadVersion(version, key = selectedKey) {
  const response = await request(`/v1/content/${encodeURIComponent(key)}/versions/${encodeURIComponent(version)}`);
  const data = response.data;
  if (typeof data?.properties?.Body !== 'string') throw new Error('This editor supports the plain text Body field. Other property shapes remain available through the API.');
  selectedKey = key; currentVersion = data; etag = response.etag; dirty = false;
  byId('title').value = data.displayName; byId('body').value = data.properties.Body;
  if (![...byId('locale').options].some(o => o.value === data.locale)) byId('locale').add(new Option(data.locale, data.locale));
  byId('locale').value = data.locale; byId('versions').value = data.version;
  byId('content-id').textContent = selectedKey; byId('editor-heading').textContent = data.displayName;
  byId('save').textContent = 'Save draft changes'; byId('locale-actions').hidden = false;
  byId('revision-note').textContent = `Loaded ${data.version}. Saving requires its current ETag.`;
  notice('Draft loaded. Choose a different language below to start a translated version.');
}
async function create(body) {
  const { data } = await request('/v1/content', { method: 'POST', body });
  if (!data?.key) throw new Error('The server did not return the created content key. Refresh the list before retrying.');
  await selectContent(data.key);
}
byId('connection').addEventListener('submit', event => { event.preventDefault(); busy(async () => {
  token = byId('token').value.trim(); if (!token) throw new Error('Enter the token printed by the local server.');
  try { await refreshList(); enabled(true); clearEditor(); notice('Connected. Your token stays in this page’s memory.', 'success'); }
  catch (error) { token = ''; enabled(false); throw error; }
}); });
byId('disconnect').addEventListener('click', () => {
  if (dirty && !confirm('Disconnect and discard your unsaved changes?')) return;
  token = ''; byId('token').value = ''; clearEditor(); enabled(false); byId('content-list').replaceChildren(); notice('Disconnected.');
});
byId('refresh').addEventListener('click', () => busy(refreshList));
byId('new').addEventListener('click', () => { if (!dirty || confirm('Discard your unsaved changes?')) clearEditor(); });
byId('versions').addEventListener('change', () => { const version = byId('versions').value; if (dirty && !confirm('Discard your unsaved changes?')) { byId('versions').value = currentVersion.version; return; } busy(() => loadVersion(version)); });
for (const id of ['title', 'body', 'locale']) byId(id).addEventListener('input', () => { dirty = true; });
byId('editor').addEventListener('submit', event => { event.preventDefault(); busy(async () => {
  const draft = { displayName: byId('title').value, locale: byId('locale').value, properties: { ...(currentVersion?.properties || {}), Body: byId('body').value } };
  if (!selectedKey) await create({ contentType: 'Article', initialVersion: draft });
  else {
    if (draft.locale !== currentVersion.locale) throw new Error('Use Create language draft to make a separate language version.');
    if (!etag) throw new Error('No ETag was returned; reload the draft before editing.');
    const { data } = await request(`/v1/content/${encodeURIComponent(selectedKey)}/versions/${encodeURIComponent(currentVersion.version)}`, { method: 'PATCH', body: { displayName: draft.displayName, properties: draft.properties }, headers: { 'If-Match': etag } });
    await selectContent(selectedKey, data?.version || currentVersion.version);
  }
  notice('Draft saved locally.', 'success');
}); });
byId('new-locale').addEventListener('click', () => busy(async () => {
  const locale = byId('locale').value;
  if (locale === currentVersion.locale) throw new Error('Choose the target language in the Language selector first.');
  const { data } = await request(`/v1/content/${encodeURIComponent(selectedKey)}/versions`, { method: 'POST', body: { locale, displayName: byId('title').value, properties: { ...currentVersion.properties, Body: byId('body').value }, status: 'draft' } });
  if (!data?.version) throw new Error('The server did not return a version. Refresh before retrying.');
  await selectContent(selectedKey, data.version); notice('Language draft created. It is ready for translation and review.', 'success');
}));
byId('seed').addEventListener('click', () => { if (dirty && !confirm('Discard your unsaved changes?')) return; busy(async () => {
  await create({ contentType: 'Article', initialVersion: { displayName: 'Thermal sensor · field note', locale: 'en', properties: { Body: 'Synthetic integration fixture — not product guidance.\n\nTest range: −20 °C to 120 °C. Reference value: 0.05 °C.\n\nCalibration, verification, and adjustment identify distinct technical operations. Preserve the terminology and numerical values when creating a language draft.', PartNumber: 'TEST-001', Source: 'Synthetic public fixture' } } });
  notice('Technical example created. All values are synthetic test data.', 'success');
}); });
window.addEventListener('beforeunload', event => { if (dirty) { event.preventDefault(); event.returnValue = ''; } });
