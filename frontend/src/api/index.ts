// One function per backend call. Each screen uses only these.
// Paths follow the API flow agreed in chats/chat2.md — adjust to match the real server.
import { request, USE_MOCK } from './client';
import { mock } from './mock';
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

export * from './types';

const ll = (p: LatLng) => ({ lat: p.latitude, lng: p.longitude });

export const api = {
  // ── Auth (06 · 07 · 10) ──────────────────────────────
  login: (email: string, password: string) =>
    USE_MOCK ? mock.login(email, password) : request<AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }),
  signup: (name: string, email: string, password: string) =>
    USE_MOCK ? mock.signup(name, email, password) : request<AuthResponse>('/auth/signup', { method: 'POST', body: { name, email, password } }),
  logout: (refreshToken: string) =>
    USE_MOCK ? mock.logout() : request<void>('/auth/logout', { method: 'POST', body: { refreshToken } }),
  me: () => (USE_MOCK ? mock.me() : request<User>('/me')),

  // ── Home (01) ────────────────────────────────────────
  safety: (p: LatLng) =>
    USE_MOCK ? mock.safety(p) : request<SafetySummary>('/safety/summary', { query: { ...ll(p), radius: 500 } }),
  latestReports: (limit = 5) =>
    USE_MOCK ? mock.latestReports(limit) : request<Report[]>('/reports', { query: { sort: 'latest', limit } }),
  reverseGeocode: (p: LatLng) =>
    USE_MOCK
      ? mock.reverseGeocode(p)
      : request<{ areaName: string; address: string }>('/geo/reverse', { query: ll(p) }),

  // ── Search (02) ──────────────────────────────────────
  searchPlaces: (q: string, near: LatLng) =>
    USE_MOCK ? mock.searchPlaces(q) : request<Place[]>('/places/search', { query: { q, ...ll(near) } }),
  favorites: () => (USE_MOCK ? mock.favorites() : request<Favorite[]>('/me/favorites')),

  // ── Route (03) ───────────────────────────────────────
  route: (from: LatLng, to: LatLng, criteria: RouteCriteria) =>
    USE_MOCK
      ? mock.route(from, to, criteria)
      : request<RouteResult>('/routes', {
          query: { fromLat: from.latitude, fromLng: from.longitude, toLat: to.latitude, toLng: to.longitude, criteria },
        }),

  // ── Map (04) ─────────────────────────────────────────
  reportsInBBox: (b: BBox) => (USE_MOCK ? mock.reportsInBBox(b) : request<Report[]>('/reports', { query: b })),

  // ── Report CRUD (05 · 08 · 09) ───────────────────────
  report: (id: string) => (USE_MOCK ? mock.report(id) : request<Report>(`/reports/${id}`)),
  uploadPhoto: async (uri: string) => {
    if (USE_MOCK) return mock.uploadPhoto(uri);
    const form = new FormData();
    form.append('file', { uri, name: 'photo.jpg', type: 'image/jpeg' } as unknown as Blob);
    return request<{ photoId: string; url: string }>('/uploads', { method: 'POST', body: form });
  },
  createReport: (input: ReportInput, photoUrl?: string | null) =>
    USE_MOCK ? mock.createReport({ ...input, photoUrl }) : request<Report>('/reports', { method: 'POST', body: input }),
  updateReport: (id: string, patch: Partial<ReportInput>, photoUrl?: string | null) =>
    USE_MOCK
      ? mock.updateReport(id, { ...patch, photoUrl })
      : request<Report>(`/reports/${id}`, { method: 'PATCH', body: patch }),
  deleteReport: (id: string) =>
    USE_MOCK ? mock.deleteReport(id) : request<void>(`/reports/${id}`, { method: 'DELETE' }),
  myReports: () => (USE_MOCK ? mock.myReports() : request<Report[]>('/me/reports')),
};
