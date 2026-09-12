/** Observe one real browser response without leaving rejecting promises after a failed click. */
export function observeBrowserResponse(page, matches, read = response => response, timeout = 30_000) {
  let resolve, settled = false;
  const outcome = new Promise(done => { resolve = done; });
  const finish = value => {
    if (settled) return;
    settled = true;
    clearTimeout(timer);
    page.off('response', response);
    page.off('close', closed);
    page.off('crash', crashed);
    resolve(value);
  };
  const response = candidate => {
    try {
      if (!matches(candidate)) return;
      page.off('response', response);
      Promise.resolve().then(() => read(candidate)).then(value => finish({value}), error => finish({error}));
    } catch (error) { finish({error}); }
  };
  const closed = () => finish({error: new Error('Browser page closed while awaiting a response')});
  const crashed = () => finish({error: new Error('Browser page crashed while awaiting a response')});
  const timer = setTimeout(() => finish({error: new Error('Timed out awaiting browser response')}), timeout);
  timer.unref?.();
  page.on('response', response); page.on('close', closed); page.on('crash', crashed);
  return {
    async value() { const result = await outcome; if (result.error) throw result.error; return result.value; },
    cancel() { finish({error: new Error('Browser response observation cancelled')}); },
  };
}
