---
uuid: services-website-as-gated-service
title: Services — website as a first-class gated service
status: ready
priority: P1
points: 5
labels:
  - tasks
  - deployment
  - website
  - has-parent
---

# Services — website as a first-class gated service

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

`services#19` has been open since 2026-06-04 and would deploy a site that cannot
serve, from a runner that cannot build it. Replace its shape with a service
declared against `docs/deployment-model.md`, keeping the parts of that branch
that are right — the hostnames, the nginx server blocks, the `services.yaml`
entries.

## Dependencies

`services-website-content-root`. Blocks `website-published-content-source` only
in that the website needs somewhere to be deployed to verify against.

## The findings to fix, from the model doc §6

1. **Docroot cannot serve the site.** The PR mounts `public/` as nginx's root,
   but the build emits `dist/cljs/app.js` and `dist/app.css`, and `index.html` is
   at the repo root. `public/` holds only `graphics/` and `music/`. The site's
   `:dev-http` merges three roots — `["." "dist" "public"]` — which one docroot
   cannot reproduce. The website build must emit **one** directory.
2. **The runner lacks the toolchain.** `deploy-website.sh` calls `pnpm install`
   and `pnpm exec shadow-cljs release app`; `deploy-promethean.yml` adds no
   `setup-node`, `pnpm/action-setup`, `setup-java` or `setup-clojure` step.
   Knoxx's own `deploy-production.yml` sets up all four.
3. **`rsync -az --delete` ships the checkout.** Excludes omit `orgs/`; the
   website's `.gitmodules` is 62 KB of submodules. Ship the build output.
4. **Two authorities for compose.** The PR commits
   `promethean/website/{docker-compose.yml,nginx.conf}` and writes both again by
   heredoc on the host.
5. **No `verify.sh`.**
6. **Wrong lane** — no gate, no host contract, dispatch-based authorization.

## Work

- Add `digitalocean/services/website/` with `compose.yaml`, `env.template`,
  `service.yaml`, and `verify.sh`. Add `website` to the host's `roles`.
- Ship only `build.output`. Rebuild the site's build so a single directory
  contains the shell, the compiled app, the stylesheet and the static assets.
  This may require a change in `open-hax/website`; if so, split it out and name
  it here.
- Set up the toolchain in the workflow: node, pnpm, java, clojure — or build an
  image and ship that instead. Do not call `pnpm` on a runner that has none.
- One authority for compose: the committed file, mounted. No heredocs writing
  config on the host.
- Mount the content root read-only. A website deploy must never delete it.
- `verify.sh`, per the model's gate contract: fetch `/`, assert the app shell
  (`id="root"`, as the knoxx gate already does for its frontend); fetch the asset
  the shell references and assert 200; assert TLS on the public hostname; assert
  the content manifest parses when present and that its absence is a PASS, not a
  failure. Bound every probe; enumerate acceptable statuses explicitly.
- Carry over from `services#19`: the hostnames, the promethean nginx server
  blocks, and the `services.yaml` entries.
- Close `services#19` referencing this work, so the branch is not merged later by
  someone reading only its diff.

## Definition of Done

- The website deploys through the gated lane and `verify.sh` is required.
- A deploy that would serve an empty docroot fails the gate.
- The build runs on a runner declared to have its toolchain.
- Only the build output is shipped.
- The content root is mounted read-only and survives redeploy.
- One committed compose file, no host-written config.
- `services#19` is closed with a pointer here.
