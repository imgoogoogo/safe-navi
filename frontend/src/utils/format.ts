/** "방금 전", "15분 전", "1시간 전", "어제", "7월 28일" */
export function relativeTime(iso: string, now = Date.now()) {
  const diff = Math.max(0, now - new Date(iso).getTime()) / 60_000;
  if (diff < 1) return '방금 전';
  if (diff < 60) return `${Math.floor(diff)}분 전`;
  if (diff < 60 * 24) return `${Math.floor(diff / 60)}시간 전`;
  if (diff < 60 * 48) return '어제';
  return monthDay(iso);
}

export function monthDay(iso: string) {
  const d = new Date(iso);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

/** "2026. 8. 1. 21:42" */
export function fullDateTime(iso: string) {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}. ${d.getMonth() + 1}. ${d.getDate()}. ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function km(meters: number) {
  return meters < 1000 ? `${Math.round(meters)}m` : `${(meters / 1000).toFixed(1)}km`;
}
