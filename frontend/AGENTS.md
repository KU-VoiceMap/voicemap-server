# FRONTEND KNOWLEDGE BASE

**Generated:** 2026-03-21
**Commit:** 5de723f
**Branch:** feature/document

## OVERVIEW

Voice-based conversation app with document generation and keyword graph visualization. React 19 + TypeScript + Vite SPA — no router, single `App.tsx` orchestrates all views via state.

## STRUCTURE

```
frontend/
├── src/
│   ├── App.tsx              # God component — ALL app state, view switching, data loading
│   ├── main.tsx             # StrictMode entry
│   ├── types.ts             # Shared domain interfaces (Chat, Document, Graph types)
│   ├── api/
│   │   ├── client.ts        # fetch wrapper with auto token rotation (401 → refresh → retry)
│   │   └── websocket.ts     # VoiceWebSocket class — SESSION_INIT → SESSION_READY handshake
│   ├── auth/
│   │   ├── AuthView.tsx     # Google Sign-In button (GIS library, script injection)
│   │   ├── useAuth.ts       # Login/register/logout flow, auto-callback handling
│   │   └── tokenStorage.ts  # localStorage tokens, JWT decode, memberNumber extraction
│   ├── chat/
│   │   ├── useVoiceSession.ts  # AudioWorklet mic capture → WS → playback queue (complex)
│   │   ├── ChatSidebar.tsx     # Chat list in sidebar
│   │   ├── TranscriptPanel.tsx # Live transcript display
│   │   └── RecorderFooter.tsx  # Record/stop + document creation buttons
│   ├── document/
│   │   ├── DocumentList.tsx       # Card grid with keyword badges
│   │   └── DocumentDetailView.tsx # Unified detail card
│   ├── graph/
│   │   ├── GraphView.tsx      # d3-force layout → ReactFlow rendering (complex)
│   │   ├── GraphContext.ts    # Hover/click interaction context
│   │   ├── DocumentNode.tsx   # Custom ReactFlow node for documents
│   │   └── KeywordNode.tsx    # Custom ReactFlow node for keywords
│   ├── components/
│   │   ├── Toast.tsx          # Toast container + auto-dismiss element
│   │   └── toastUtils.ts     # createToast factory (separated for react-refresh)
│   ├── styles/
│   │   └── global.css         # ALL styles — single file, CSS custom properties
│   └── utils/
│       └── formatDate.ts
├── eslint.config.js           # ESLint flat config + sonarjs + react-hooks + react-refresh
├── vite.config.ts             # Proxy: /auth, /v1, /chats, /documents → :8080, /ws → ws://:8080
└── tsconfig.app.json          # strict, noUnusedLocals, noUnusedParameters, verbatimModuleSyntax
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add new view/tab | `App.tsx` | Add to `MainView` type in `types.ts`, handle in render switch |
| Add API endpoint | `api/client.ts` | Use `fetchJson<T>()` — auto-attaches auth, handles 401 |
| Add WS message type | `api/websocket.ts` | Add to `WsMessageHandler` interface + switch case |
| Modify auth flow | `auth/useAuth.ts` | Google OAuth → `/auth/login` → fallback `/v1/register` |
| Change audio behavior | `chat/useVoiceSession.ts` | AudioWorklet mic → PCM16 base64 → WS; playback via AudioContext queue |
| Add graph node type | `graph/` | New component + register in `GraphView.tsx` nodeTypes |
| Change styling | `styles/global.css` | Single file, use existing CSS custom properties |

## CONVENTIONS

- **No router** — view switching via `useState<MainView>` in App.tsx
- **No state library** — all state in App.tsx, passed as props
- **No CSS modules** — single `global.css` with BEM-like class names, state modifiers: `is-active`, `is-loading`, `is-leaving`
- **No Prettier / EditorConfig** — formatting not enforced beyond ESLint
- **No barrel exports** — direct file imports everywhere
- **Shared types** — domain interfaces in `types.ts`, component-local interfaces inline
- **API pattern** — `fetchJson<T>(path, { method, body })` returns `{ status, json }`
- **Auth tokens** — localStorage with `voicemap.*` prefix keys
- **Google OAuth** — GIS script injection, credential → sessionStorage → redirect `/oauth/callback` → exchange
- **Korean UI** — all user-facing strings are Korean

## ANTI-PATTERNS (THIS PROJECT)

- **Never suppress TypeScript errors** — strict mode enforced, `noUnusedLocals` + `noUnusedParameters`
- **Never mix component + non-component exports** — `react-refresh/only-export-components` enforced via ESLint
- **Never empty catch blocks** — `no-empty` enforced, use intentional comment if ignoring
- **Never nested ternaries** — `sonarjs/no-nested-conditional` enforced
- **Never access refs during render** — `react-hooks/refs` enforced
- **Never reference `useCallback` before declaration** — `react-hooks/immutability` enforced; use ref pattern for recursive callbacks

## AUDIO PIPELINE (CRITICAL PATH)

```
Mic → AudioWorklet (downsample 48k→16k) → PCM16 → base64 → WS AUDIO_INPUT
WS AUDIO_OUTPUT → base64 → PCM → resample → AudioBufferSource → scheduled playback queue
```

- `useVoiceSession.ts` manages the full pipeline
- AudioWorklet processor defined as inline JS string → `Blob` → `URL.createObjectURL` (not a separate file)
- Two separate AudioContexts: `micContextRef` (capture) and `playbackContextRef` (playback)
- Playback uses `nextStartTimeRef` for gapless scheduling
- Recursive `playAudioQueue` via `playAudioQueueRef` (ref pattern to avoid hooks violation)
- Interruption: flush queue + stop all sources immediately

## GRAPH RENDERING

- `d3-force` computes layout (200 tick simulation), outputs to ReactFlow nodes
- `zIndexMode="manual"` — edges always below nodes (EDGE_Z=0, NODE_Z=10)
- Hover: highlight connected edges + dim others via adjacency map
- Node sizes differ: DOCUMENT radius 90, KEYWORD radius 45 (collision force)

## COMMANDS

```bash
npm run dev      # Vite dev server on :3000, proxies API to :8080
npm run build    # tsc -b && vite build
npm run lint     # eslint . (includes sonarjs rules)
```

## NON-OBVIOUS PATTERNS

- **Streaming transcript**: `liveMessageRef` tracks in-progress role; same-role transcripts are appended, different-role creates new message entry
- **New chat detection**: `knownChatIdsRef` snapshots chat IDs before session start; on `TURN_COMPLETED`, diff finds the auto-created chat
- **Parallel data loading**: `Promise.allSettled([loadChatList(), loadDocuments()])` on auth — partial failure is OK
- **Tab-switch guard**: recording state blocks navigation away from chats tab

## NOTES

- `App.tsx` is ~500 lines — monolithic by design, not refactored into context yet
- Backend is Spring Boot (Oracle DB in prod, H2 in dev) at same origin via Vite proxy
- Google Client ID is hardcoded in `AuthView.tsx`
- `verbatimModuleSyntax` in tsconfig — must use `import type` for type-only imports
