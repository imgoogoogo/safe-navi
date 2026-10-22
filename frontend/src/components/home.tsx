import { StyleSheet, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import type { Report, SafetySummary } from '../api/types';
import { colors, radius, shadows } from '../theme';
import { relativeTime } from '../utils/format';
import { SearchIcon, ShieldCheckIcon } from './icons';
import { Photo } from './Photo';
import { Text } from './Text';
import { Touchable, TypeBadge } from './ui';

export function SearchPill({ onPress, label = '어디로 갈까요?' }: { onPress: () => void; label?: string }) {
  return (
    <Touchable onPress={onPress} style={styles.searchPill}>
      <SearchIcon />
      <Text size={15} color={colors.placeholder}>
        {label}
      </Text>
    </Touchable>
  );
}

/** 86px donut: 10px ring (#2563EB on #E3EAF6) starting at 12 o'clock, like the conic-gradient in the design. */
export function ScoreRing({ score, size = 86, thickness = 10 }: { score: number; size?: number; thickness?: number }) {
  const r = (size - thickness) / 2;
  const c = 2 * Math.PI * r;
  const pct = Math.max(0, Math.min(100, score)) / 100;
  return (
    <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
      <Svg width={size} height={size} style={StyleSheet.absoluteFill}>
        <Circle cx={size / 2} cy={size / 2} r={r} stroke={colors.ringTrack} strokeWidth={thickness} fill="none" />
        <Circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          stroke={colors.primary}
          strokeWidth={thickness}
          fill="none"
          strokeDasharray={`${c * pct} ${c}`}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </Svg>
      <Text size={26} weight={900}>
        {Math.round(score)}
      </Text>
    </View>
  );
}

function Stat({ label, value, color }: { label: string; value: number | string; color?: string }) {
  return (
    <View style={styles.stat}>
      <Text size={12} color={colors.muted}>
        {label}
      </Text>
      <Text size={17} weight={900} color={color}>
        {value}
      </Text>
    </View>
  );
}

export function SafetyScoreCard({ data }: { data?: SafetySummary }) {
  return (
    <View style={styles.scoreCard}>
      <View style={styles.scoreTop}>
        <View style={{ gap: 5, flex: 1 }}>
          <Text size={14} color={colors.muted}>
            현재 위치
          </Text>
          <Text size={22} weight={900}>
            {data?.areaName ?? '위치 확인 중…'}
          </Text>
          <Text size={13} color={colors.text3}>
            {data?.comment ?? ' '}
          </Text>
        </View>
        <View style={{ alignItems: 'center', gap: 6 }}>
          <ScoreRing score={data?.score ?? 0} />
          <Text size={13} weight={700} color={colors.primary}>
            안전점수
          </Text>
        </View>
      </View>
      <View style={styles.statsRow}>
        <Stat label="CCTV" value={data?.cctvCount ?? '-'} />
        <View style={styles.vr} />
        <Stat label="가로등" value={data?.streetlightCount ?? '-'} />
        <View style={styles.vr} />
        <Stat label="최근 제보" value={data?.recentReportCount ?? '-'} color={colors.amberText} />
      </View>
    </View>
  );
}

export function PromoBanner() {
  return (
    <View style={styles.banner}>
      <View style={styles.bannerIcon}>
        <ShieldCheckIcon />
      </View>
      <View style={{ flex: 1, gap: 2 }}>
        <Text size={15} weight={900} color="#FFFFFF">
          밤길, 혼자 걷지 마세요
        </Text>
        <Text size={12} color="rgba(255,255,255,0.82)" style={{ lineHeight: 17 }}>
          CCTV·가로등·안심귀가길을 반영한 안전경로를 안내해 드려요
        </Text>
      </View>
    </View>
  );
}

/** 225px horizontal card on 01 홈. */
export function ReportCard({ report, onPress }: { report: Report; onPress: () => void }) {
  return (
    <Touchable onPress={onPress} style={styles.reportCard}>
      <View style={{ height: 145 }}>
        <Photo uri={report.photoUrl} radius={14} style={StyleSheet.absoluteFill} />
        <TypeBadge type={report.type} withIcon style={{ position: 'absolute', left: 10, top: 10 }} />
      </View>
      <View style={{ gap: 2, paddingTop: 12, paddingHorizontal: 6, paddingBottom: 4 }}>
        <Text size={16} weight={900} numberOfLines={1}>
          {report.place}
        </Text>
        <Text size={13} color={colors.placeholder}>
          {relativeTime(report.createdAt)}
        </Text>
      </View>
    </Touchable>
  );
}

const styles = StyleSheet.create({
  searchPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
    paddingVertical: 15,
    paddingHorizontal: 20,
    boxShadow: shadows.card,
  },
  scoreCard: {
    gap: 18,
    backgroundColor: colors.surface,
    borderRadius: radius.card,
    padding: 24,
    boxShadow: shadows.card,
  },
  scoreTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  statsRow: { flexDirection: 'row', gap: 10, borderTopWidth: 1, borderTopColor: colors.divider, paddingTop: 16 },
  stat: { flex: 1, alignItems: 'center', gap: 3 },
  vr: { width: 1, backgroundColor: colors.divider },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    marginTop: 16,
    marginHorizontal: 24,
    borderRadius: 20,
    paddingVertical: 13,
    paddingHorizontal: 18,
    experimental_backgroundImage: 'linear-gradient(120deg, #2563EB, #1D4ED8)',
    backgroundColor: colors.primary,
    boxShadow: shadows.banner,
  },
  bannerIcon: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: 'rgba(255,255,255,0.18)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  reportCard: {
    width: 225,
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 12,
    boxShadow: shadows.card,
  },
});
