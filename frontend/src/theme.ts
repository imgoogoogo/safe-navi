// Design tokens pulled from `보행 안전경로 앱 v2.dc.html`.

export const colors = {
  primary: '#2563EB',
  primaryDark: '#1D4ED8',
  primarySoft: '#EAF0FB', // selected card bg, secondary buttons, "검토중" badge
  primaryMeta: '#4B6DB5',

  bg: '#F4F6FA',
  surface: '#FFFFFF',
  mapBg: '#DDE4EC',

  text: '#1B2333',
  text2: '#3A4356',
  text3: '#6B7280',
  muted: '#8A94A6',
  placeholder: '#9AA1AC',
  faint: '#C4CBD6',

  divider: '#F1F4F9',
  border: '#E7EBF2',
  borderSoft: '#EEF1F5',
  grabber: '#E3E7EE',
  ringTrack: '#E3EAF6',
  separator: '#D6DBE3',

  amber: '#F5A623',
  amberText: '#C07C09',
  amberSoft: '#FCEFD3',

  danger: '#DC2626',
  dangerSoft: '#FDECEC',
  dangerBorder: '#FBD9D9',

  success: '#15803D',
  successSoft: '#E9F7EE',
} as const;

// CSS box-shadow strings (RN supports `boxShadow` on the New Architecture).
export const shadows = {
  card: '0 6px 18px rgba(27,42,84,0.07)',
  cardSoft: '0 6px 18px rgba(27,42,84,0.06)',
  chip: '0 4px 12px rgba(27,42,84,0.06)',
  input: '0 4px 14px rgba(27,42,84,0.06)',
  floating: '0 8px 24px rgba(27,42,84,0.16)',
  sheet: '0 -8px 28px rgba(27,42,84,0.14)',
  popup: '0 12px 36px rgba(27,42,84,0.22)',
  tabBar: '0 -4px 16px rgba(27,42,84,0.05)',
  marker: '0 4px 14px rgba(27,42,84,0.28)',
  primaryButton: '0 8px 20px rgba(37,99,235,0.32)',
  primaryButtonSoft: '0 8px 20px rgba(37,99,235,0.3)',
  primaryChip: '0 6px 14px rgba(37,99,235,0.25)',
  banner: '0 8px 20px rgba(37,99,235,0.26)',
  fab: '0 8px 22px rgba(37,99,235,0.4)',
  dot: '0 3px 10px rgba(27,42,84,0.28)',
  roundButton: '0 4px 12px rgba(27,42,84,0.2)',
} as const;

export const radius = {
  pill: 999,
  card: 24,
  cardSm: 20,
  sheet: 28,
} as const;

export const SCREEN_PADDING = 24;

export const fonts = {
  400: 'NotoSansKR_400Regular',
  500: 'NotoSansKR_500Medium',
  700: 'NotoSansKR_700Bold',
  900: 'NotoSansKR_900Black',
} as const;

export type FontWeight = keyof typeof fonts;
