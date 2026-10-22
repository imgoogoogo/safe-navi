# SafeWalk (보행 안전경로 탐색 앱)

React Native (Expo SDK 58 + Expo Router + TypeScript) implementation of
`project/보행 안전경로 앱 v2.dc.html`.

## Run

```bash
cd safewalk
npm install
npx expo start          # then press i / a
```

`react-native-maps`, `expo-location`, and `expo-image-picker` include native code. If your Expo Go build
doesn't support them, make a dev build: `npx expo run:ios` / `npx expo run:android`.

**Android maps need a Google Maps API key** even though only OSM tiles are shown
(react-native-maps on Android runs on the Google Maps SDK). Put it in `app.json` →
`plugins` → `react-native-maps` → `androidGoogleMapsApiKey`. iOS uses Apple Maps with the native basemap
replaced by OSM tiles, so it needs no key.

## Backend

`app.json` → `expo.extra.apiBaseUrl`:

- **empty (default)**: in-app mock backend (`src/api/mock.ts`) using the sample data from the design.
  Any email/password logs in as 김서영.
- **set**: real REST calls through `src/api/client.ts` (JWT in SecureStore, auto refresh on 401).

Endpoints follow the API flow in `chats/chat2.md`; all of them are in `src/api/index.ts`, so adjust paths there:

| Screen | Calls |
| --- | --- |
| 01 홈 | `GET /safety/summary`, `GET /reports?sort=latest&limit=5` |
| 02 검색 | `GET /places/search`, `GET /me/favorites` (logged in) |
| 03 경로 | `GET /routes?criteria=cctv\|light\|police\|safe` (one cached call per criteria) |
| 04 지도 | `GET /reports?minLat&minLng&maxLat&maxLng`, `GET /geo/reverse` |
| 05 등록 | `POST /uploads` (multipart), `POST /reports` |
| 08 상세 | `GET /reports/:id`, `DELETE /reports/:id` |
| 09 수정 | `PATCH /reports/:id` (changed fields only) |
| 06 · 07 · 10 | `POST /auth/login`, `POST /auth/signup`, `POST /auth/refresh`, `POST /auth/logout`, `GET /me`, `GET /me/reports` |

## Where things are

```
src/
  app/                      routes (Expo Router)
    (tabs)/index.tsx        01 홈
    search.tsx              02 목적지 검색
    route.tsx               03 경로 탐색 (지도 + 기준 선택)
    (tabs)/map.tsx          04 지도
    report/new.tsx          05 제보 등록
    login.tsx               06 로그인
    signup.tsx              07 회원가입
    report/[id]/index.tsx   08 제보 상세
    report/[id]/edit.tsx    09 제보 수정
    (tabs)/my.tsx           10 My
    (tabs)/alerts.tsx       알림 (not in the design — placeholder)
  components/               SearchPill, SafetyScoreCard, ScoreRing, ReportCard, PresetCard,
                            PhotoMarker, TabBar, ReportForm (05/09 shared), LocationPicker, …
  theme.ts                  colors / shadows / radius / fonts from the design
  api/                      types, client, mock, endpoints, small cached query hook
  auth/AuthContext.tsx      login state + requireAuth()
```

## Notes

- Styling is a direct port of the HTML: CSS `box-shadow` → RN `boxShadow`, the banner gradient →
  `experimental_backgroundImage`, the score ring → `react-native-svg`. Font is Noto Sans KR (400/500/700/900).
- Guests can use 01 · 02 · 03 · 04 · 08. 05 shows "로그인하고 제보하기" for guests; My and edit/delete need login.
- The design's 58px top padding is the iOS status bar; screens use the safe-area inset.
- `tile.openstreetmap.org` is for development only (OSM tile usage policy). Switch `OSM_TILE_URL` in
  `src/components/map.tsx` to a tile provider (MapTiler, Stadia, …) before release.
- Not in the design, so left as placeholders: 알림 tab, turn-by-turn after "이 기준으로 안내 시작" (shows an alert),
  비밀번호 찾기.
