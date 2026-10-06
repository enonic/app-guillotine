# URL handling proposal

Status: discussion notes, 2026-09-11. This describes a proposed direction, not the current API contract or an implementation decision.

## Intent

Separate content and media paths from deployment-specific addresses. Clients should control their frontend routes and media hosts. XP can provide explicitly configured bases when available, without inferring an address from the incoming request.

The existing `xxxUrl.url` field is under discussion. Current documentation intentionally uses `path` and `queryString` instead of recommending that field.

## Proposed fields

Apply a consistent structure to `PageUrl`, `ImageUrl` and `AttachmentUrl` (including values returned by `mediaUrl`):

| Field | Proposed meaning |
| --- | --- |
| `path` | URL-escaped path, without the query string. |
| `queryString` | Escaped query string, including `?` when non-empty. |
| `relativeUrl` | Combined `path + queryString`, independent of deployment. |
| `baseUrl` | Optional applicable configured base; `null` when unavailable. |
| `absoluteUrl` | Complete address when an absolute `baseUrl` exists; otherwise `null`. |

Keep the separate components for clients that need custom routing. The combined field makes the common case easier and helps clients retain query parameters such as attachment download intent.

Illustrative media value:

```json
{
  "path": "/media:attachment/myproject/id:hash/report.pdf",
  "queryString": "?download",
  "relativeUrl": "/media:attachment/myproject/id:hash/report.pdf?download",
  "baseUrl": "https://cdn.example.com/api",
  "absoluteUrl": "https://cdn.example.com/api/media:attachment/myproject/id:hash/report.pdf?download"
}
```

## Bases and client routing

- Media bases may come from explicit media configuration. Images and attachments may have different bases.
- Page bases may come from the selected site's declared Base URL.
- An absent configured base should remain absent. Do not fall back to an inferred request host, native XP address or vhost route.
- Clients can use the configured base or override it with their own API/CDN base or frontend routing.
- A base such as `/api` can produce a usable relative address, but cannot by itself produce `absoluteUrl`. That field requires a scheme and host.

Media paths include the API descriptor, such as `/media:image/...`, but not the deployment's public API prefix. Page paths retain their site/project context; the frontend may prepend a route prefix or apply a different route mapping.

## Rich-text processing

The proposed default for internal links and images in `processedHtml` is the same combined relative representation. Use it in `href`, `src` and each `srcset` candidate, retaining image transformations, fingerprints and query parameters.

Preserve `data-link-ref` and `data-image-ref` so clients can match elements to structured GraphQL data and apply their routing. Keep external URLs and same-page anchors unchanged. Preserve fragments on internal links as well.

Availability of `baseUrl` or `absoluteUrl` should not silently change the HTML output to absolute addresses. Clients resolve internal references before rendering; leading-slash paths otherwise resolve against the document's base origin and may address the wrong deployment.

## Naming and details to settle

`uri` was considered for the aggregated field. Strictly, a value such as `/posts/example?lang=en` is a relative URI reference. `relativeUrl` is the preferred working name because it is clearer to clients and pairs with `absoluteUrl`; `uriReference` is another technically precise option. See [RFC 3986, section 4.2](https://www.rfc-editor.org/rfc/rfc3986.html#section-4.2).

Before implementation, settle:

- Whether to adopt these field names and how to retire the existing `url` field.
- How fragments are represented in structured fields and included in the combined value. The current `path` and `queryString` fields do not define a separate fragment field.
- Which explicit configuration wins for each base, including site context and per-API configuration.
- Whether `baseUrl` permits relative prefixes or only absolute bases.
- Joining rules for trailing slashes, empty page paths and already-escaped values. A leading-slash media path must preserve a base prefix such as `/api` when combined.
