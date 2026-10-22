export type LatLng = { latitude: number; longitude: number };

export type ReportType = 'DARK' | 'BLOCKED' | 'OTHER';
export type ReportStatus = 'RECEIVED' | 'REVIEWING' | 'RESOLVED';
export type RouteCriteria = 'cctv' | 'light' | 'police' | 'safe';

export const REPORT_TYPE_LABEL: Record<ReportType, string> = {
  DARK: '조명부족',
  BLOCKED: '시야가림',
  OTHER: '기타',
};
export const REPORT_TYPES: ReportType[] = ['DARK', 'BLOCKED', 'OTHER'];

export const REPORT_STATUS_LABEL: Record<ReportStatus, string> = {
  RECEIVED: '접수됨',
  REVIEWING: '검토중',
  RESOLVED: '반영됨',
};

export type User = { id: string; name: string; email: string };

export type AuthTokens = { accessToken: string; refreshToken: string };
export type AuthResponse = AuthTokens & { user: User };

export type Report = {
  id: string;
  type: ReportType;
  status: ReportStatus;
  photoUrl: string | null;
  location: LatLng;
  /** Short place name shown as card title, e.g. "서초대로 46길 3" */
  place: string;
  /** Full road address, e.g. "서울 서초구 서초대로46길 3" */
  address: string;
  memo: string;
  authorName: string;
  createdAt: string; // ISO
  isOwner: boolean;
};

export type ReportInput = {
  type: ReportType;
  location: LatLng;
  address: string;
  memo?: string;
  photoId?: string | null;
};

export type SafetySummary = {
  areaName: string; // "서초동 일대"
  comment: string; // "밤에도 비교적 안전한 편이에요"
  score: number; // 0–100
  cctvCount: number;
  streetlightCount: number;
  recentReportCount: number;
};

export type Place = {
  id: string;
  name: string;
  address: string;
  location: LatLng;
  distanceMeters: number;
};

export type Favorite = { id: string; label: string; place: Place };

export type RouteResult = {
  criteria: RouteCriteria;
  polyline: LatLng[];
  durationMinutes: number;
  distanceMeters: number;
  /** Criteria-specific highlight, e.g. "CCTV 34대" */
  highlight: string;
};

export type BBox = { minLat: number; minLng: number; maxLat: number; maxLng: number };
