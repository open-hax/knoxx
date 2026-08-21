---
uuid: services-website-as-gated-service
title: Services — website as a first-class gated DigitalOcean service
status: ready
priority: P1
points: 5
labels:
  - tasks
  - deployment
  - website
  - has-parent
---

# Services — website as a first-class gated DigitalOcean service

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

`services#19` has been open since 2026-06-04 and would deploy a site that cannot
serve, from a runner that cannot build it, into the lane that is being retired.
Replace it with a service declared against `docs/deployment-model.md`. The
hostname choice and the intent survive; the transport, the ingress config and the
deploy script do not.

## Dependencies

`services-website-content-root`.

## The findings to fix, from the model doc §6

1. **Docroot cannot serve the site.** The PR mounts `public/` as nginx's root,
   but the build emits `dist/cljs/app.js` and `dist/app.css`, and `index.html` is
   at the repo root. `public/` holds only `graphics/` and `music/`. The site's
   `:dev-http` merges three roots — `["." "dist" "public"]` — which one docroot
   cannot reproduce. The build must emit **one** directory.
2. **The runner lacks the toolchain.** `deploy-website.sh` calls `pnpm install`
   and `pnpm exec shadow-cljs release app`; `deploy-promethean.yml` adds no
   `setup-node`, `pnpm/action-setup`, `setup-java` or `setup-clojure` step.
   Knoxx's own `deploy-production.yml` sets up all four. As an image, the
   toolchain is a builder stage and the runner needs none of it.
3. **`rsync -az --delete` ships the checkout.** Excludes omit `orgs/`; the
   website's `.gitmodules` is 62 KB of submodules.
4. **Two authorities for compose** — one committed, one written by heredoc on
   the host.
5. **No `verify.sh`.**
6. **Wrong lane, therefore wrong ingress.** It writes nginx server blocks into
   `promethean/nginx/promethean.conf`, which is not what serves production. On
   DigitalOcean the ingress is Caddy.

## Work

- Add a `Dockerfile` to `open-hax/website`: a builder stage carrying node, pnpm,
  java and clojure that runs the release build, and a serving stage that is
  `nginx:alpine` plus `COPY --from=build` of the single output directory. Split
  the build-output change out as its own website-side commit if it is not
  trivial — the site must emit one directory before this can copy one.
- Add `website` to `build-images.yml`'s service list and to the deploy chain.
  Its image is independent of proxx and knoxx, so it does not belong inside the
  proxx → knoxx → caddy ordering; it needs its own step, not a new link in that
  chain.
- Add `digitalocean/services/website/` with `compose.yaml`, `env.template`,
  `service.yaml` and `verify.sh`; add `website` to the host's `roles`.
- Mount the content root read-only. Replacing the website image must never touch
  it.
- Ingress: add a `CADDY_WEBSITE_HOST` placeholder and a site block importing
  `common`, reverse-proxying the website container. Note in the Caddyfile that
  this is the fourth hostname on HTTP-01, which is the count
  `caddy/compose.yaml`'s header weighed against a wildcard.
- **DNS cutover.** `open-hax.promethean.rest` must resolve to 157.245.125.134
  before Caddy can issue its first certificate, and records stay DNS-only rather
  than proxied so ACME reaches the origin. Sequence it explicitly: record first,
  deploy second, certificate third.
- `verify.sh`, per the model's gate contract: fetch `/` and assert the app shell
  (`id="root"`, as the knoxx gate already does for its frontend); fetch the asset
  the shell references and assert 200; assert TLS on the public hostname; assert
  the content manifest parses when present, and that its **absence is a PASS**.
  Bound every probe; enumerate acceptable statuses explicitly.
- Close `services#19` referencing this work, so nobody merges it later after
  reading only its diff.

## Definition of Done

- The website deploys as a GHCR image through the DigitalOcean lane and
  `verify.sh` is required.
- A deploy that would serve an empty docroot fails the gate.
- Nothing is built on the runner or on the host.
- The content root is mounted read-only and survives an image replacement.
- The hostname resolves to the DigitalOcean host, Caddy serves it, and its
  certificate issued.
- One committed compose file; no host-written config.
- `services#19` is closed with a pointer here.
