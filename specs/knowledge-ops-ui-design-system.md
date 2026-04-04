# Knowledge Ops — UI Design System

> *Monokai. Spacemacs chords. Vim modality. Every action has a button, every button has a chord. Mouse users never feel lost, keyboard users never reach for the mouse.*

---

## Component Library

All UI implementations use `@open-hax/uxx` (or `@open-hax/uxx-reagent` for ClojureScript).

### Package structure

```
orgs/open-hax/uxx/
├── tokens/          ← @open-hax/uxx/tokens (colors, spacing, typography, keybindings)
│   └── src/
│       ├── colors.ts        ← Monokai palette + semantic aliases
│       ├── keybindings.ts   ← Spacemacs chord map + modal modes
│       ├── spacing.ts       ← Spacing scale
│       ├── typography.ts    ← Font stacks
│       ├── motion.ts        ← Transitions
│       └── shadows.ts       ← Elevation + z-index
├── react/           ← @open-hax/uxx (React component library)
│   └── src/
│       ├── primitives/      ← Button, Badge, Card, Input, Modal, Spinner, Tooltip, Progress, ModeIndicator
│       └── composites/      ← ChordOverlay, (future: DataTable, CommandPalette, ChatPanel)
├── reagent/         ← @open-hax/uxx-reagent (ClojureScript component library)
├── contracts/       ← EDN component contracts (button.edn, card.edn, etc.)
```

### External consumers reference

| Consumer | Import |
|----------|--------|
| Ragussy frontend | `@open-hax/uxx` |
| futuresight-kms frontend | `@open-hax/uxx` |
| OpenPlanner frontend | `@open-hax/uxx` |
| ClojureScript UIs | `@open-hax/uxx-reagent` |

No project implements its own Button, Card, Input, or Modal. All come from `@open-hax/uxx`.

---

## Theme: Monokai

Already defined in `orgs/open-hax/uxx/tokens/src/colors.ts`.

```
Background:  #272822 (default)  #3e3d32 (surface)  #1e1f1c (darker)
Foreground:  #f8f8f2 (default)  #75715e (muted)
Accent:      #a6e22e (green/cyan)  #fd971f (orange)  #f92672 (red)  #ae81ff (magenta)  #66d9ef (blue)
Semantic:    error=#f92672  warning=#fd971f  success=#a6e22e  info=#66d9ef
```

All background panels use the Monokai dark palette. All text uses Monokai foreground tones. Accent colors are used for interactive elements, status indicators, and mode indicators.

---

## Keyboard System

### Design principle

> Every action has a button. Every button has a chord.
> Mouse users never feel lost. Keyboard users never reach for the mouse.

### Modes

| Mode | Color | Meaning | Entry |
|------|-------|---------|-------|
| **Normal** | `#a6e22e` (green) | Navigation, selection, chord entry | Default. `Esc` from any other mode |
| **Insert** | `#fd971f` (orange) | Text input | `i` from normal mode |
| **Command** | `#66d9ef` (blue) | Spacemacs leader chords | `Space` from normal mode |
| **Visual** | `#ae81ff` (magenta) | Selection | `v` from normal mode |

### Chord model

The leader key is `Space`.

```
Space → enters command mode → shows chord overlay

Space f f → find file
Space f s → save file
Space c c → open chat
Space c l → label this
Space t a → approve
Space t r → reject
Space l s → select lake/layer
Space l q → query current lake
Space w / → split right
Space w h → focus left pane
```

### Full chord map

Defined in `orgs/open-hax/uxx/tokens/src/keybindings.ts` as `defaultChords`.

Groups:
- `Space f` — file operations
- `Space b` — buffer operations
- `Space s` — search
- `Space g` — git
- `Space p` — project
- `Space w` — window management
- `Space l` — lake/layer
- `Space c` — chat
- `Space t` — tags/labels
- `Space q` — quit

### Chord overlay behavior

When `Space` is pressed:
1. Overlay appears at bottom-left (Spacemacs-style)
2. Shows available next keys with descriptions
3. Pressing a key narrows the list
4. If a complete chord matches, the action fires and overlay closes
5. If no match, overlay closes
6. Auto-closes after 3 seconds of inactivity

### Accessibility

- All chord-bound actions also have visible buttons in the UI
- Screen reader users interact through buttons, not chords
- Keyboard-only users get full functionality through chords
- Chord overlay is purely visual — it doesn't block screen readers

---

## Component Usage Rules

### Buttons

Every interactive action must use `@open-hax/uxx` `Button` with:
- `variant` — primary, secondary, ghost, danger
- `size` — sm, md, lg
- Optional `iconStart`/`iconEnd`
- Optional `chord` prop for displaying the keyboard shortcut hint

```tsx
import { Button } from '@open-hax/uxx';

<Button variant="primary" chord="SPC t a" onClick={handleApprove}>
  Approve
</Button>
```

### Cards

All panel containers use `@open-hax/uxx` `Card`:
- `variant` — default, outlined, elevated
- `padding` — none, sm, md, lg
- `header`/`footer` slots for titles and actions

### Inputs

All text inputs use `@open-hax/uxx` `Input`:
- `variant` — default, error, success
- `size` — sm, md, lg
- Automatic focus-visible styling (Monokai cyan border)

### Mode Indicator

Always visible in the status bar. Shows current mode with color:
```tsx
import { ModeIndicator } from '@open-hax/uxx';

<ModeIndicator mode="normal" />
```

---

## Status Bar

Every page has a status bar at the bottom:

```
[NORMAL] [devel-docs] [Proxx glm-4.7] [14 docs] [3 pending labels]
```

Components:
- `ModeIndicator` — current modal mode
- Collection name — active lake/layer
- Model — active inference provider and model
- Counts — relevant context (doc count, label count, etc.)

---

## Existing Code References

| File | What it provides |
|------|-----------------|
| `orgs/open-hax/uxx/tokens/src/colors.ts` | Monokai palette + semantic aliases |
| `orgs/open-hax/uxx/tokens/src/keybindings.ts` | Chord map, modal modes, mode colors |
| `orgs/open-hax/uxx/tokens/src/spacing.ts` | Spacing scale |
| `orgs/open-hax/uxx/tokens/src/typography.ts` | Font stacks |
| `orgs/open-hax/uxx/react/src/primitives/Button.tsx` | Button with variant/size/loading |
| `orgs/open-hax/uxx/react/src/primitives/ModeIndicator.tsx` | Modal mode indicator |
| `orgs/open-hax/uxx/react/src/composites/ChordOverlay.tsx` | Spacemacs chord overlay |
| `orgs/open-hax/uxx/contracts/button.edn` | Button component contract |
| `orgs/open-hax/uxx/contracts/card.edn` | Card component contract |
| `orgs/open-hax/uxx/contracts/input.edn` | Input component contract |

---

## Specs Updated

All UI specs now reference `@open-hax/uxx`:

| Spec | Component references |
|------|---------------------|
| `knowledge-ops-workbench-ui.md` | `@open-hax/uxx` primitives + `ChordOverlay` + `ModeIndicator` |
| `knowledge-ops-chat-ui-library.md` | `@open-hax/uxx` as foundation, composites built on top |
| `knowledge-ops-chat-widget-layers.md` | Widget uses `@open-hax/uxx` Button, Input, Card |
| `knowledge-ops-cms-data-model.md` | CMS UI uses `@open-hax/uxx` Button, Card, Badge, Input |
| `knowledge-ops-shibboleth-lite-labeling.md` | Label form uses `@open-hax/uxx` Input, Select, Button |
| `knowledge-ops-kms-query.md` | Query page uses `@open-hax/uxx` Input, Button, Card, Badge |
| `knowledge-ops-exposure-monitor.md` | Leads page uses `@open-hax/uxx` Button, Badge, Spinner |

---

## Status

Specified 2026-04-02. Design system definition.
