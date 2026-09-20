// Enforce the README's source-file budget; generated outputs are not source inputs.
const sourceThresholds = Object.freeze({warn:350, error:500});
export default {
  includePaths:['backend/src/cljs','frontend/src','shared/src','ingestion/src'],
  ignoreDirectories:['node_modules','dist','target'],
  thresholdsByExtension:Object.fromEntries(
    ['.clj','.cljs','.cljc','.js','.mjs','.cjs','.ts','.tsx'].map(extension=>[extension,sourceThresholds])),
};
