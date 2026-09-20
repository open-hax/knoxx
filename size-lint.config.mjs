// Source-file thresholds mirror the repository's existing kondo file-length
// hooks (warning at400, error at800); generated outputs are never source inputs.
const sourceThresholds = Object.freeze({warn:400, error:800});
export default {
  includePaths:['backend/src/cljs','frontend/src','shared/src'],
  ignoreDirectories:['node_modules','dist','target'],
  thresholdsByExtension:Object.fromEntries(
    ['.clj','.cljs','.cljc','.js','.mjs','.cjs','.ts','.tsx'].map(extension=>[extension,sourceThresholds])),
};
