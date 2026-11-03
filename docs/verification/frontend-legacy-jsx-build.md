# Legacy frontend emission uses the application's JSX runtime

An isolated run of the advertised legacy TypeScript build configuration exposed
2,173 compiler errors: 1,953 TS2686 errors referenced a missing React global and
220 TS1361 errors tried to use type-only React imports at runtime. The legacy
configuration explicitly selected the classic `react` JSX transform, while the
application and existing modules use the automatic `react-jsx` transform.

Removing that stale override lets the legacy build inherit the application's
actual JSX transform from `tsconfig.json`. Strict type checking remains enabled;
no error suppression, ambient React global or source import proliferation was
introduced. Emission with the corrected configuration passes for the entire
legacy production source tree:

```sh
pnpm -C frontend exec tsc -p tsconfig.legacy.json --outDir dist-verification/legacy
```

The only command-line override selects an ignored verification output directory
so the running browser tour's production files remain frozen. This proves the
same compiler configuration used by `pnpm -C frontend compile:legacy` without
writing into the served output tree.
