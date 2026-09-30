---
original_name: "2026.04.27.17.01.39.md"
title: "Admin Contracts Role Empty Response"
summary: "Curl capture showing the admin contracts role endpoint returning an empty contracts array."
category: "contracts"
created: "2026-04-27"
---

> **Note (2026-09-30):** Historical bug capture. The `knoxx_session` cookie value in the original capture was redacted during the docs audit. The route is still `GET /api/admin/contracts` (`backend/src/cljs/knoxx/backend/infra/routes/resources.cljs:1265`); it now lists via the resource loader (`handle-list-contracts`, same file:516) and `contracts/roles/` holds ~50 role files, so this empty response is not known to reproduce. Not re-verified against a live instance.

```
curl 'https://knoxx.promethean.rest/api/admin/contracts?kind=role' \
  -H 'User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:149.0) Gecko/20100101 Firefox/149.0' \
  -H 'Accept: */*' \
  -H 'Accept-Language: en-US,en;q=0.9' \
  -H 'Accept-Encoding: gzip, deflate, br, zstd' \
  -H 'Referer: https://knoxx.promethean.rest/contracts' \
  -H 'Sec-GPC: 1' \
  -H 'Connection: keep-alive' \
  -H 'Cookie: knoxx_session=<redacted>' \
  -H 'Sec-Fetch-Dest: empty' \
  -H 'Sec-Fetch-Mode: cors' \
  -H 'Sec-Fetch-Site: same-origin' \
  -H 'Priority: u=4' \
  -H 'TE: trailers'
```
---

## Response
```
{
	"contracts": []
}
```
