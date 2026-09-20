const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {spawnSync} = require('node:child_process');
const root = path.resolve(__dirname, '..');
const backend = path.join(root, 'backend');
const cacheDir = process.env.FORESIGHT_MODEL_CACHE;
if (!cacheDir || !path.isAbsolute(cacheDir)) throw new Error('Set FORESIGHT_MODEL_CACHE to the absolute warmed model cache; this verifier never downloads models');
const build = spawnSync('clojure', ['-M:cljs', path.join(__dirname, 'compile-local-model-verification.clj')], {cwd:backend, stdio:'inherit', timeout:180000});
if (build.error || build.status !== 0) throw build.error || new Error('Local model verification compilation failed');
process.chdir(backend);
const library = require(path.join(backend, 'target/local-model-recovery-live/library.cjs'));
const clj = value => cljs.core.js__GT_clj(value, cljs.core.keyword('keywordize-keys'), true);
const get = (value, key) => cljs.core.get(value, cljs.core.keyword(key));
(async () => {
  const { startGenerationServer } = await import(pathToFileURL(path.join(root, '../devtools/generation-server.mjs')));
  const { startEmbeddingServer } = await import(pathToFileURL(path.join(root, '../devtools/embedding-server.mjs')));
  const service = await startGenerationServer({cacheDir, model: 'onnx-community/Qwen2.5-0.5B-Instruct'});
  let embedding;
  const directory = await fs.mkdtemp(path.join(require('node:os').tmpdir(), 'knoxx-local-model-recovery-'));
  const cleanup = () => require('node:fs').rmSync(directory, {recursive:true,force:true});
  const interrupted = () => process.exit(130);
  const terminated = () => process.exit(143);
  process.once('exit', cleanup);
  process.once('SIGINT', interrupted);
  process.once('SIGTERM', terminated);
  try {
    embedding = await startEmbeddingServer({cacheDir});
    const vectors = cljs.core.clj__GT_js(await library.embed(clj({
      'embed-provider-base-url': embedding.baseUrl,
      'embed-provider-model': embedding.model,
      'embed-provider-dimensions': 384,
    }), clj(['Hello, world.', 'Hello, world.', 'A different sentence.'])));
    assert.equal(vectors.dimensions,384);
    assert.equal(vectors.vectors.length,3);
    assert(vectors.vectors.every(vector=>vector.length===384 && vector.every(Number.isFinite)));
    assert.deepEqual(vectors.vectors[0],vectors.vectors[1]);
    assert.notDeepEqual(vectors.vectors[0],vectors.vectors[2]);
    const cfg = clj({'contracts-dir': 'test/fixtures/translation-local-model-contracts',
      'translation-agent-structured-output-timeout-ms':180000,
      'wiki-model-provider':'openai-compatible', 'wiki-model':service.model,
      'wiki-model-base-url':service.baseUrl});
    const writing = cljs.core.clj__GT_js(await library.generate(cfg,clj({
      instruction:'Improve clarity in one sentence.',content:'A wiki lets people edit pages together.',lessons:[]})));
    assert(writing.content.trim());
    const state=await library.admitted(clj({model:service.model}));
    const deps=library.baseDeps(directory+'/content',state);
    const turn=get(state,'turn');
    const result=await library.complete(cfg,deps,get(state,'record'),turn);
    const receipt=cljs.core.clj__GT_js(get(result,'translation/receipt'));
    assert(receipt && Object.keys(receipt).length>0);
    const candidates=cljs.core.clj__GT_js(await library.candidates(get(state,'splits'),get(turn,'translation-turn/id')));
    assert.equal(candidates.length,2);
    assert(candidates.every(item=>typeof item.text==='string'&&item.text.trim()));
    const replay=await library.complete(cfg,deps,get(state,'record'),turn);
    assert.deepEqual(cljs.core.clj__GT_js(get(replay,'translation/receipt')),receipt);
    console.log(JSON.stringify({model:service.model,provider:'transformers-js',offline:true,
      writing,translations:candidates.map(item=>item.text),receipt,exactReplay:true,
      embeddings:{model:vectors.model,dimensions:vectors.dimensions,count:vectors.vectors.length,
        identicalInputEqual:true,differentInputDifferent:true},
      note:'Actual offline outputs through reconstructed production adapters and canonical translation sink. Reference stores/emitter are fixtures. Human review remains required; structural acceptance is not translation-quality approval.'},null,2));
  } finally {
    if(embedding)await embedding.close();
    await service.close();
    await fs.rm(directory,{recursive:true,force:true});
    process.removeListener('exit',cleanup);
    process.removeListener('SIGINT',interrupted);
    process.removeListener('SIGTERM',terminated);
  }
})().catch(error=>{console.error(error);process.exitCode=1;});
