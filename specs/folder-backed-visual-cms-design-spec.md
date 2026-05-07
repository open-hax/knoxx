# Folder-Backed Visual CMS Design Spec

Status: draft  
Owner: Knoxx / OpenPlanner CMS / Broadcast Studio  
Created: 2026-05-07  
Related:
- `specs/broadcast-studio-playlist-publication-and-block-cms.md`
- `specs/knowledge-ops-cms-data-model.md`
- `specs/knowledge-ops-gardens.md`
- `contracts/agents/page_defaults/cms_default.edn`
- `contracts/actors/cms_chat.edn`

---

## Purpose

Define the architecture, file structure, data model, and UI/UX for a folder-backed visual CMS where `view-contract.edn` is the human-readable source of truth, the page is a folder on disk, and the editing UI is a visual page builder over that contract.

This spec bridges the rendering foundation we already have with a complete authoring experience for non-coders.

---

## Core Principles

1. **The page is a folder.** A draft or published page lives as a directory on the filesystem, versioned alongside code.
2. **view-contract.edn is the source of truth.** It declares layout, blocks, metadata, publishing rules, and garden bindings. It is human-readable, versionable, and agent-editable.
3. **The UI is a visual editor over the contract.** Non-coders never touch EDN directly. Power users and agents can inspect and mutate the contract through a source panel.
4. **Markdown is import material, not the data model.** Markdown files can be imported into a page and converted into editable blocks, but the durable model is the structured block array in `view-contract.edn`.
5. **Only approved CMS blocks are draggable.** Not every React component becomes an editor block. A curated block registry controls what can be placed where.
6. **One shared renderer.** The public page renderer, the preview renderer, and the layout canvas editor all use the same React component registry. No duplicate preview/published renderers.
7. **OpenPlanner remains the canonical publication boundary.** Gardens, visibility, and publication state are governed by the existing OpenPlanner document model.

---

## Folder Structure

### Draft pages

```
cms/
  drafts/
    <page-slug>/
      view-contract.edn       # source of truth
      content.md              # optional: imported markdown body
      assets/
        cover.png
        hero-audio.mp3
      blocks/                 # optional: per-block markdown files
        intro.md
        track-notes.md
```

### Published / reviewed pages (inside a garden)

```
gardens/
  <garden-id>/
    drafts/
      <page-slug>/
        view-contract.edn
        content.md
        assets/
    published/
      <page-slug>/
        view-contract.edn
        content.md
        assets/
```

### Rationale

- Folders map 1:1 to the mental model of a "document" or "page."
- Assets are colocated with the page that uses them.
- `view-contract.edn` is inspectable in any editor, diffable in git, and readable by agents.
- Separating `drafts/` and `published/` makes publication a filesystem move/rename, which is easy to reason about and audit.

---

## view-contract.edn Schema

```clojure
{:view/id "error-coded-radio"
 :view/title "Error Coded Radio"
 :view/kind :playlist-page          ;; :article-page, :playlist-page, :landing-page, :collection-page
 :view/schema-version 1
 :view/status :draft                 ;; :draft, :review, :public, :archived

 ;; Provenance: where did this page come from?
 :source
 {:kind :markdown-import             ;; :manual, :markdown-import, :broadcast-studio, :ai-drafted
  :path "docs/notes/error-coded-radio.md"
  :imported-at "2026-05-07T20:00:00Z"}

 ;; Garden binding: which garden owns this page?
 :garden
 {:id "sonic"
  :visibility :review}              ;; :draft, :review, :public, :archived

 ;; Layout declaration: which template and which zones exist?
 :layout
 {:template :studio-playlist-page
  :zones
  [{:id :hero
    :label "Hero"
    :accepts [:hero :rich-text :image]}
   {:id :main
    :label "Main content"
    :accepts [:rich-text :playlist :track :callout :cta]}
   {:id :aside
    :label "Sidebar"
    :accepts [:chat :labels :related-links]}]}

 ;; Blocks: ordered list of content modules placed into zones
 :blocks
 [{:id "hero"
   :type :hero
   :zone :hero
   :props
   {:title "Error Coded Radio"
    :subtitle "A playlist from Broadcast Studio"
    :image-path "assets/cover.png"
    :audio-path "assets/hero-audio.mp3"}}

  {:id "intro"
   :type :rich-text
   :zone :main
   :source {:kind :file :path "content.md"}}

  {:id "playlist"
   :type :studio-playlist
   :zone :main
   :props
   {:source_audio_paths ["audio/a.mp3" "audio/b.mp3"]
    :show_labels true
    :show_descriptions true
    :show_duration true}}

  {:id "track-1"
   :type :track
   :zone :main
   :props
   {:audio-path "audio/a.mp3"
    :title "Track A"
    :commentary "Heard description or agent-written notes."}
   :hidden false}

  {:id "cta-1"
   :type :cta
   :zone :aside
   :props
   {:label "Listen on SoundCloud"
    :href "https://soundcloud.com/..."
    :tone :primary}}]

 ;; Editor policy: what actions are allowed in the visual editor?
 :editor
 {:locked false
  :allowed-actions [:drag :drop :duplicate :hide :delete :edit-props]
  :default-panel :layout}

 ;; Publishing metadata
 :publishing
 {:last-published-at nil
  :defer-index false            ;; don't show in garden index until explicitly set
  :skip-translation true
  :canonical-url nil
  :seo
  {:title nil
   :description nil
   :og-image nil}}

 ;; Page settings
 :settings
 {:language "en"
  :allow-comments true
  :chat-actor-id nil}}           ;; which cms_chat.edn actor answers comments?
```

### Why EDN

- **Homoiconic:** Clojure/ClojureScript tooling reads it natively.
- **Keyword types:** `:hero`, `:rich-text`, `:draft` are self-documenting.
- **Maps over JSON:** EDN maps preserve key order visually and support richer literals.
- **Git-friendly:** Line-oriented, commentable, diffable.
- **Agent-editable:** LLMs and agent tools already speak EDN in this codebase.

---

## Block Registry

Only blocks declared in the registry can be placed in the visual editor.

```clojure
;; contracts/cms-block-registry.edn (example)
{:blocks
 {:hero
  {:label "Hero"
   :category :layout
   :icon :image
   :props-schema
   {:title {:type :string :required true}
    :subtitle {:type :string}
    :image-path {:type :asset}
    :audio-path {:type :asset}}}

  :rich-text
  {:label "Rich Text"
   :category :content
   :icon :text
   :props-schema
   {:markdown {:type :markdown :required true}}}

  :studio-playlist
  {:label "Studio Playlist"
   :category :media
   :icon :music
   :props-schema
   {:source_audio_paths {:type :audio-list :required true}
    :show_labels {:type :boolean :default true}
    :show_descriptions {:type :boolean :default true}
    :show_duration {:type :boolean :default true}}}

  :track
  {:label "Track Card"
   :category :media
   :icon :play-circle
   :props-schema
   {:audio-path {:type :audio :required true}
    :title {:type :string :required true}
    :commentary {:type :markdown}}}

  :callout
  {:label "Callout"
   :category :content
   :icon :alert-circle
   :props-schema
   {:tone {:type :enum :values [:note :tip :warning :promo] :default :note}
    :title {:type :string}
    :markdown {:type :markdown :required true}}}

  :cta
  {:label "Call to Action"
   :category :content
   :icon :mouse-pointer
   :props-schema
   {:label {:type :string :required true}
    :href {:type :url :required true}
    :tone {:type :enum :values [:primary :secondary] :default :primary}}}

  :chat
  {:label "Chat Panel"
   :category :interactive
   :icon :message-square
   :props-schema
   {:actor-id {:type :string}
    :placeholder {:type :string :default "Ask about this page..."}}}

  :labels
  {:label "Label Cloud"
   :category :interactive
   :icon :tag
   :props-schema
   {:source {:type :enum :values [:page-audio :garden-audio] :default :page-audio}}}

  :image
  {:label "Image"
   :category :media
   :icon :image
   :props-schema
   {:src {:type :asset :required true}
    :alt {:type :string}
    :caption {:type :string}}}

  :divider
  {:label "Divider"
   :category :layout
   :icon :minus
   :props-schema {}}}}
```

### Registry rules

- A block's `:category` controls which palette section it appears in.
- `:props-schema` drives the right-inspector form fields.
- New blocks are added by:
  1. Creating a React component in the shared component registry.
  2. Declaring it in `contracts/cms-block-registry.edn`.
  3. Adding it to one or more template zone `:accepts` lists.

---

## Templates and Zones

Templates declare the page skeleton. Zones are drop targets with a whitelist of accepted block types.

```clojure
;; contracts/cms-templates.edn (example)
{:templates
 {:studio-playlist-page
  {:label "Studio Playlist Page"
   :zones
   [{:id :hero
     :label "Hero"
     :accepts [:hero :image :rich-text]
     :max-blocks 1}
    {:id :main
     :label "Main Content"
     :accepts [:rich-text :studio-playlist :track :callout :cta :image]
     :max-blocks nil}
    {:id :aside
     :label "Sidebar"
     :accepts [:chat :labels :related-links :cta]
     :max-blocks nil}]}

  :article-page
  {:label "Article"
   :zones
   [{:id :header
     :label "Header"
     :accepts [:hero :image]
     :max-blocks 1}
    {:id :body
     :label "Body"
     :accepts [:rich-text :heading :callout :image :cta :divider]
     :max-blocks nil}]}

  :landing-page
  {:label "Landing Page"
   :zones
   [{:id :hero
     :label "Hero"
     :accepts [:hero]
     :max-blocks 1}
    {:id :sections
     :label "Sections"
     :accepts [:rich-text :callout :cta :image :divider :studio-playlist]
     :max-blocks nil}]}}}
```

---

## UI Layout

### Default layout (configurable per user)

```
+-----------------------------------------------------------+
|  Toolbar: page title, status, save, preview, publish      |
+------+------------------------------------------+---------+
|      |                                          |         |
| Left |     Upper: Visual Layout Canvas          | Right   |
|      |     (rendered React components,          |         |
|      |      drag/drop zones)                    |         |
|      |                                          |         |
|Add   |------------------------------------------|Inspector|
|Block |                                          |         |
|      |     Lower: Source / Files Panel          |         |
|Media |     Tabs: content.md | view-contract.edn |         |
|      |              assets/ | validation        |         |
|      |                                          |         |
+------+------------------------------------------+---------+
```

The user asked for a horizontal split. The default proposed here is:

- **Upper half:** visual layout canvas (what the page looks like)
- **Lower half:** source/files panel (tabs for `content.md`, `view-contract.edn`, `assets/`, validation)
- **Left sidebar:** palette (add block, templates, media, existing markdown sections, Broadcast Studio playlists)
- **Right sidebar:** inspector (page settings, selected block properties, layout zone, visibility, publishing, SEO)

### Panels

| Panel | Contents |
|-------|----------|
| **Left palette** | Block categories, templates, media library, Broadcast Studio playlists/tracks, markdown sections importer |
| **Upper canvas** | Rendered page using the same React component registry as the public renderer. Zones are outlined on hover. Blocks are draggable between zones. |
| **Lower source** | Tabs: `content.md` editor, `view-contract.edn` viewer/formatter, `assets/` file tree, validation warnings, diff from published |
| **Right inspector** | Page metadata, selected block props form, zone assignment, visibility toggle, publish actions, SEO fields |

### Context menus (right-click on block)

- Move to zone
- Insert before / after
- Wrap in section
- Convert markdown section to block
- Duplicate
- Hide / Show
- Delete
- Attach audio (for media blocks)
- Add label block
- Publish / Request review

---

## Document / File Browser

A file browser view (accessible from the CMS dashboard) shows:

```
+------------------------------------------+
|  cms/drafts/          [+ New Draft]      |
+------------------------------------------+
|  > error-coded-radio/                    |
|    > view-contract.edn                   |
|    > content.md                          |
|    > assets/                             |
|  > brain-damage-transmissions/           |
|    > view-contract.edn                   |
|    > content.md                          |
|                                          |
|  gardens/sonic/drafts/                   |
|    > error-coded-radio/                  |
|                                          |
|  gardens/sonic/published/                |
|    > error-coded-radio/                  |
+------------------------------------------+
```

### Actions on files

| Action | Trigger | Result |
|--------|---------|--------|
| Create CMS draft from file | Right-click any `.md` file | Opens wizard: choose garden, choose template, creates draft folder with imported markdown as `content.md` and a `rich-text` block referencing it |
| Open draft | Click folder | Opens visual editor |
| Duplicate draft | Right-click folder | Copies folder, appends `-copy`, updates `:view/id` |
| Move to garden | Right-click folder | Moves `cms/drafts/` to `gardens/<id>/drafts/` |
| Publish | Toolbar button or right-click | Sets `:view/status :public`, moves to `gardens/<id>/published/`, triggers OpenPlanner sync |
| Delete | Right-click | Moves to trash or sets `:view/status :archived` |

---

## Markdown Import Flow

```
User right-clicks docs/notes/error-coded-radio.md
  -> "Create CMS draft from file"
  -> Wizard:
       1. Choose garden (or leave unassigned)
       2. Choose template (article-page, studio-playlist-page, etc.)
       3. Preview parsed markdown sections
       4. Confirm
  -> System creates:
       cms/drafts/error-coded-radio/
         view-contract.edn
         content.md          (copy of original)
         assets/             (empty, user can add)
  -> view-contract.edn contains:
       :source {:kind :markdown-import :path "docs/notes/error-coded-radio.md" ...}
       :blocks [{:id "intro" :type :rich-text :zone :main
                 :source {:kind :file :path "content.md"}}]
  -> Opens in visual editor
```

### Markdown section splitter

When importing markdown with clear section boundaries (`# Heading`, `## Subheading`), the importer can optionally split into separate blocks:

```
# Error Coded Radio          -> hero block (title)
## Introduction              -> heading block
Intro paragraph...           -> rich-text block
## Track Notes               -> heading block
Notes...                     -> rich-text block
```

This is optional; the default is a single `rich-text` block referencing the whole `content.md`.

---

## Publish Flow

```
Draft in cms/drafts/<slug>/
  -> User clicks "Request Review"
  -> System:
       1. Sets :view/status :review
       2. Validates contract (required props, broken asset paths, etc.)
       3. If validation passes:
            - copies folder to gardens/<garden-id>/drafts/<slug>/
            - creates/updates OpenPlanner document
       4. If user clicks "Publish":
            - sets :view/status :public
            - moves folder to gardens/<garden-id>/published/<slug>/
            - updates OpenPlanner meta.visibility = 'public'
            - freezes asset references (copies or symlinks)
```

### Validation rules

| Rule | Severity | Auto-fix? |
|------|----------|-----------|
| `:view/id` matches folder slug | Error | No |
| `:view/title` is non-empty | Error | No |
| All `:blocks` have `:id`, `:type`, `:zone` | Error | No |
| All `:blocks` `:type` exists in block registry | Error | No |
| All `:blocks` `:zone` exists in template zones | Error | No |
| All `:props` match block `:props-schema` | Warning | Partial (type coercion) |
| All `:asset` paths resolve in `assets/` or workspace | Warning | No |
| All `:audio` paths resolve in workspace `audio/` | Warning | No |
| Duplicate block IDs | Error | Yes (auto-renumber) |
| Orphaned markdown files in `blocks/` | Info | No |

---

## Agent Integration

Agents read and write `view-contract.edn` directly. The visual editor is the human-friendly wrapper.

### Agent tools (proposed)

```edn
;; Create a draft from markdown
{:tool/id "cms.draft.create_from_markdown"
 :params {:source-path "docs/notes/error-coded-radio.md"
          :garden-id "sonic"
          :template :studio-playlist-page}
 :returns {:draft-path "cms/drafts/error-coded-radio"
           :view-contract {...}}}

;; Mutate blocks
{:tool/id "cms.blocks.add"
 :params {:draft-path "cms/drafts/error-coded-radio"
          :block {:id "cta-2" :type :cta :zone :aside
                  :props {:label "Subscribe" :href "/subscribe" :tone :primary}}}}

{:tool/id "cms.blocks.update"
 :params {:draft-path "cms/drafts/error-coded-radio"
          :block-id "hero"
          :props {:subtitle "Updated subtitle"}}}

{:tool/id "cms.blocks.reorder"
 :params {:draft-path "cms/drafts/error-coded-radio"
          :zone :main
          :order ["intro" "playlist" "track-1"]}}

{:tool/id "cms.blocks.delete"
 :params {:draft-path "cms/drafts/error-coded-radio"
          :block-id "cta-1"}}

;; Publishing
{:tool/id "cms.publish.request_review"
 :params {:draft-path "cms/drafts/error-coded-radio"
          :garden-id "sonic"}}

{:tool/id "cms.publish.publish"
 :params {:draft-path "gardens/sonic/drafts/error-coded-radio"}}
```

### Agent policy

- Agents may draft, propose, and mutate blocks.
- Agents may request review.
- **Agents may NOT publish without explicit human confirmation.**
- Mutating a published page moves it back to `review` status.

---

## Technology Choices

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Visual editor framework | **Puck** (first candidate) | Open-source React visual page builder. Uses your own components. Drag/drop. Own your data. Maps well to block+zones model. |
| Alternative | Craft.js | More powerful, lower-level. Consider if Puck's opinions are too restrictive. |
| Contract format | EDN (Clojure) | Native to Knoxx backend. Git-friendly. LLM/agent-friendly. |
| Markdown parsing | Unified / Remark | Industry standard. Split into sections for block conversion. |
| Asset management | Colocated folders + symlink labels | Reuse existing `audio-labels.json` and symlink system. |
| Backend store | OpenPlanner events/docs | CMS is a view/mutation layer, not a separate DB. |

### Why Puck over Craft.js for v1

- Puck is closer to the desired UX out of the box: component registry, drag/drop canvas, sidebar fields.
- Craft.js requires building every editing affordance from primitives.
- Puck's data model (JSON with component types + props) maps cleanly to `view-contract.edn` `:blocks`.
- We can always eject to Craft.js later if we need deeper customization.

---

## Migration Path from Existing CMS

### Current state

- CMS documents are stored in OpenPlanner as `kind="docs"` with `content` (markdown) and `metadata`.
- Some documents already have `metadata.blocks` from the Broadcast Studio playlist publication spec.

### Migration steps

1. **Phase 0 — Folder bridge (spec & validation)**
   - Define `view-contract.edn` schema.
   - Define block registry and templates.
   - Build validator CLI tool.
   - Existing markdown docs remain untouched.

2. **Phase 1 — Dual-mode editor**
   - CMS editor gets "Markdown" and "Blocks" tabs (from existing spec).
   - For docs without `view-contract.edn`, default to Markdown tab.
   - For docs with `view-contract.edn`, default to Blocks tab.

3. **Phase 2 — Folder-backed drafts**
   - New docs created via "Create draft from file" generate a folder + `view-contract.edn`.
   - Existing docs can be "converted to folder draft" on demand.
   - Keep legacy OpenPlanner doc store as fallback.

4. **Phase 3 — Visual editor (Puck integration)**
   - Integrate Puck canvas as the Blocks tab.
   - Map Puck's internal data to/from `view-contract.edn` `:blocks`.
   - Upper/lower split panel: Puck canvas above, source panel below.

5. **Phase 4 — Garden publication via folders**
   - Publish action moves folder to `gardens/<id>/published/`.
   - OpenPlanner sync updates `meta.visibility`.
   - Public renderer reads `view-contract.edn` from published folders.

---

## Files to Create

| File | Purpose |
|------|---------|
| `contracts/cms-block-registry.edn` | Canonical list of draggable CMS blocks and their prop schemas |
| `contracts/cms-templates.edn` | Page templates and zone definitions |
| `contracts/schemas/view-contract.edn` | Malli/TypeBox schema for validating `view-contract.edn` |
| `specs/folder-backed-visual-cms-design-spec.md` | This document |
| `src/cms/validator.cljs` | CLI tool to validate a `view-contract.edn` file |
| `src/cms/importer.cljs` | Markdown -> folder draft converter |
| `src/cms/editor/VisualEditor.tsx` | Puck-based visual editor component |
| `src/cms/editor/SourcePanel.tsx` | Lower panel: content.md / view-contract.edn / assets / validation |
| `src/cms/editor/BlockInspector.tsx` | Right panel: block props form |
| `src/cms/editor/PageBrowser.tsx` | File browser for drafts and published pages |
| `src/cms/renderer/BlockRenderer.tsx` | Shared React block renderer (public + preview + canvas) |
| `src/cms/renderer/ZoneRenderer.tsx` | Renders a template zone and its blocks |
| `src/cms/blocks/*.tsx` | Individual block React components (Hero, RichText, Playlist, Track, etc.) |

---

## Open Questions

1. Should Puck be wrapped inside our own React app, or should we build a custom drag/drop canvas using `@dnd-kit`?
2. Should `view-contract.edn` support comments? (EDN readers generally don't, but we could use `:comment` keys.)
3. Should asset paths in `:props` be relative to the page folder or workspace root?
4. How do we handle large audio files in `assets/`? Symlink, copy, or reference-only?
5. Should the CMS editor be a standalone app or integrated into the existing frontend?
6. How does collaborative editing work? (File locks? Operational transforms?)
7. Should we support real-time preview sync (save -> instant re-render) or explicit save?
8. What is the fallback if Puck's data model diverges from our block schema?

---

## Recommended Decisions

| Question | Decision |
|----------|----------|
| Visual editor framework | **Puck** for v1. Evaluate Craft.js if we hit Puck limits. |
| Asset paths | Relative to page folder (`assets/cover.png`), with workspace-root fallback (`audio/a.mp3`). |
| Audio in assets | Reference-only for workspace audio. Copy only if explicitly uploaded through CMS. |
| Editor app | Integrated into existing frontend as a new route `/cms/editor/:draftPath`. |
| Save behavior | Auto-save to file every 3s debounce, with explicit "Publish" for visibility changes. |
| Collaborative editing | Out of scope for v1. Use file-lock warnings if two sessions open the same draft. |
| Fallback renderer | If `view-contract.edn` is missing or invalid, fall back to rendering `content.md` as markdown. |
| EDN comments | Use `:comment` metadata keys sparingly; don't rely on reader comments. |

---

## Status

- Spec drafted: 2026-05-07
- Pending: operator review and Phase 0 kickoff
