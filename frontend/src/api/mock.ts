// In-memory mock backend using the sample data from the design.
// Active when `expo.extra.apiBaseUrl` is empty.
import type {
  AuthResponse,
  BBox,
  Favorite,
  LatLng,
  Place,
  Report,
  ReportInput,
  RouteCriteria,
  RouteResult,
  SafetySummary,
  User,
} from './types';

const wait = (ms = 250) => new Promise((r) => setTimeout(r, ms));
const ago = (min: number) => new Date(Date.now() - min * 60_000).toISOString();

export const MOCK_ORIGIN: LatLng = { latitude: 37.4893, longitude: 127.0172 };

const demoUser: User = { id: 'u1', name: '김서영', email: 'seoyoung@example.com' };

let reports: Report[] = [
  {
    id: 'r1', type: 'DARK', status: 'REVIEWING', photoUrl: null,
    location: { latitude: 37.4935, longitude: 127.0148 },
    place: '서초대로 46길 3', address: '서울 서초구 서초대로46길 3',
    memo: '골목길 가로등이 고장나서 밤에 앞이 거의 보이지 않습니다. 통행량이 많은 길이라 빠른 조치가 필요해 보여요.',
    authorName: '김서영', createdAt: ago(0), isOwner: true,
  },
  {
    id: 'r2', type: 'BLOCKED', status: 'RECEIVED', photoUrl: null,
    location: { latitude: 37.4902, longitude: 127.0205 },
    place: '반포대로 22길', address: '서울 서초구 반포대로22길',
    memo: '담쟁이 수풀이 우거져 앞이 잘 보이지 않아요.',
    authorName: '이도윤', createdAt: ago(15), isOwner: false,
  },
  {
    id: 'r3', type: 'DARK', status: 'RECEIVED', photoUrl: null,
    location: { latitude: 37.4874, longitude: 127.0166 },
    place: '방배중앙로 12길', address: '서울 서초구 방배중앙로12길',
    memo: '가로등 2개가 연달아 꺼져 있습니다.',
    authorName: '박지호', createdAt: ago(60), isOwner: false,
  },
  {
    id: 'r4', type: 'OTHER', status: 'REVIEWING', photoUrl: null,
    location: { latitude: 37.4921, longitude: 127.0241 },
    place: '서초역 3번 출구', address: '서울 서초구 서초대로 지하 233',
    memo: '공사장 가림막으로 인도가 좁아졌어요.',
    authorName: '최하은', createdAt: ago(180), isOwner: false,
  },
  {
    id: 'r5', type: 'BLOCKED', status: 'RECEIVED', photoUrl: null,
    location: { latitude: 37.4895, longitude: 127.0118 },
    place: '우면산 진입로', address: '서울 서초구 우면산 진입로',
    memo: '가지치기가 안 되어 시야가 가려요.',
    authorName: '정민준', createdAt: ago(60 * 26), isOwner: false,
  },
];

const myHistory: Report[] = [
  { ...reports[0], id: 'h1', type: 'DARK', status: 'RESOLVED', place: '서초대로 74길', createdAt: '2026-07-28T12:00:00+09:00' },
  { ...reports[0], id: 'h2', type: 'BLOCKED', status: 'REVIEWING', place: '반포천 산책로', createdAt: '2026-07-25T12:00:00+09:00' },
  { ...reports[0], id: 'h3', type: 'OTHER', status: 'RECEIVED', place: '서초역 3번 출구', createdAt: '2026-07-21T12:00:00+09:00' },
];

const places: Place[] = [
  { id: 'p1', name: '강남역 2호선', address: '서울 강남구 역삼동 858', location: { latitude: 37.4979, longitude: 127.0276 }, distanceMeters: 1700 },
  { id: 'p2', name: '강남역 11번 출구', address: '서울 강남구 강남대로 396', location: { latitude: 37.4971, longitude: 127.0272 }, distanceMeters: 1600 },
  { id: 'p3', name: '강남역 신분당선', address: '서울 강남구 강남대로 지하 396', location: { latitude: 37.4966, longitude: 127.0283 }, distanceMeters: 1700 },
  { id: 'p4', name: '서초역 2호선', address: '서울 서초구 서초대로 지하 233', location: { latitude: 37.4918, longitude: 127.0076 }, distanceMeters: 900 },
  { id: 'p5', name: '교대역 3호선', address: '서울 서초구 서초대로 지하 294', location: { latitude: 37.4934, longitude: 127.0141 }, distanceMeters: 500 },
];

const favorites: Favorite[] = [
  { id: 'f1', label: '집', place: { id: 'home', name: '집', address: '서울 서초구 서초대로74길 29', location: MOCK_ORIGIN, distanceMeters: 0 } },
  { id: 'f2', label: '학교', place: { ...places[0], id: 'school', name: '학교' } },
];

let loggedIn = false;
const assertAuth = () => {
  if (!loggedIn) throw new Error('로그인이 필요해요');
};

export const mock = {
  async login(email: string, _password: string): Promise<AuthResponse> {
    await wait();
    loggedIn = true;
    return { accessToken: 'mock-access', refreshToken: 'mock-refresh', user: { ...demoUser, email: email || demoUser.email } };
  },
  async signup(name: string, email: string, _password: string): Promise<AuthResponse> {
    await wait();
    loggedIn = true;
    return { accessToken: 'mock-access', refreshToken: 'mock-refresh', user: { id: 'u2', name, email } };
  },
  async restore(): Promise<User> {
    loggedIn = true;
    return demoUser;
  },
  async logout() {
    loggedIn = false;
  },

  async reverseGeocode(_p: LatLng) {
    await wait(100);
    return { areaName: '서초동 일대', address: '서울 서초구 서초대로74길 29' };
  },
  async safety(_p: LatLng): Promise<SafetySummary> {
    await wait();
    return { areaName: '서초동 일대', comment: '밤에도 비교적 안전한 편이에요', score: 82, cctvCount: 42, streetlightCount: 186, recentReportCount: reports.length };
  },
  async latestReports(limit: number) {
    await wait();
    return [...reports].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, limit);
  },
  async reportsInBBox(_b: BBox) {
    await wait();
    return reports;
  },
  async report(id: string) {
    await wait();
    const r = reports.find((x) => x.id === id) ?? myHistory.find((x) => x.id === id);
    if (!r) throw new Error('제보를 찾을 수 없어요');
    return r;
  },
  async uploadPhoto(uri: string) {
    await wait(400);
    return { photoId: `photo-${Date.now()}`, url: uri };
  },
  async createReport(input: ReportInput & { photoUrl?: string | null }) {
    assertAuth();
    await wait();
    const r: Report = {
      id: `r${Date.now()}`, type: input.type, status: 'RECEIVED', photoUrl: input.photoUrl ?? null,
      location: input.location, place: input.address.replace(/^서울 \S+ /, ''), address: input.address,
      memo: input.memo ?? '', authorName: demoUser.name, createdAt: new Date().toISOString(), isOwner: true,
    };
    reports = [r, ...reports];
    return r;
  },
  async updateReport(id: string, patch: Partial<ReportInput> & { photoUrl?: string | null }) {
    assertAuth();
    await wait();
    reports = reports.map((r) =>
      r.id === id
        ? { ...r, ...patch, photoUrl: patch.photoUrl !== undefined ? patch.photoUrl : r.photoUrl, memo: patch.memo ?? r.memo, location: patch.location ?? r.location, address: patch.address ?? r.address }
        : r,
    );
    return reports.find((r) => r.id === id)!;
  },
  async deleteReport(id: string) {
    assertAuth();
    await wait();
    reports = reports.filter((r) => r.id !== id);
  },
  async me() {
    assertAuth();
    return demoUser;
  },
  async myReports() {
    assertAuth();
    await wait();
    return [...reports.filter((r) => r.isOwner && !myHistory.some((h) => h.id === r.id)), ...myHistory];
  },
  async favorites() {
    assertAuth();
    await wait(100);
    return favorites;
  },
  async searchPlaces(q: string) {
    await wait(150);
    const s = q.trim();
    if (!s) return [];
    return places.filter((p) => p.name.includes(s) || p.address.includes(s));
  },
  async route(from: LatLng, to: LatLng, criteria: RouteCriteria): Promise<RouteResult> {
    await wait(300);
    const meta: Record<RouteCriteria, [number, string]> = {
      cctv: [22, 'CCTV 34대'],
      light: [20, '밝은 길'],
      police: [25, '치안시설 4'],
      safe: [27, '안심구간 2'],
    };
    // Staircase path like the design, with a per-criteria bend so routes look different.
    const bend = { cctv: 0.35, light: 0.5, police: 0.65, safe: 0.25 }[criteria];
    const lerp = (a: number, b: number, t: number) => a + (b - a) * t;
    const lat = (t: number) => lerp(from.latitude, to.latitude, t);
    const lng = (t: number) => lerp(from.longitude, to.longitude, t);
    const polyline: LatLng[] = [
      from,
      { latitude: lat(bend * 0.6), longitude: lng(0) },
      { latitude: lat(bend * 0.6), longitude: lng(0.4) },
      { latitude: lat(bend + 0.2), longitude: lng(0.4) },
      { latitude: lat(bend + 0.2), longitude: lng(0.8) },
      { latitude: lat(1), longitude: lng(0.8) },
      to,
    ];
    const [durationMinutes, highlight] = meta[criteria];
    return { criteria, polyline, durationMinutes, distanceMeters: 1700, highlight };
  },
};
