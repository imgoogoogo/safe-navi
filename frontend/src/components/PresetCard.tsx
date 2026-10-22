import { StyleSheet, View } from 'react-native';
import type { RouteCriteria } from '../api/types';
import { colors } from '../theme';
import { CctvIcon, LampIcon, PersonPinIcon, ShieldCheckIcon } from './icons';
import { Text } from './Text';
import { Touchable } from './ui';

export const PRESETS: { id: RouteCriteria; name: string; short: string; desc: string }[] = [
  { id: 'cctv', name: 'CCTV 위주', short: 'CCTV 위주', desc: 'CCTV가 설치된 구간을 우선으로 경로를 생성합니다.' },
  { id: 'light', name: '가로등 위주', short: '가로등 위주', desc: '야간 조도가 높은 구간을 우선으로 경로를 생성합니다.' },
  { id: 'police', name: '경찰서 위주', short: '경찰서 위주', desc: '지구대, 파출소 등 치안시설 근접 구간을 우선합니다.' },
  { id: 'safe', name: '여성안심귀가길 위주', short: '안심귀가길', desc: '지정된 안심귀가 전용 구간을 최대한 포함합니다.' },
];

const ICON: Record<RouteCriteria, { bg: string; fg: string; Icon: typeof CctvIcon }> = {
  cctv: { bg: '#E4EDFD', fg: '#2563EB', Icon: CctvIcon },
  light: { bg: '#FDF1D9', fg: '#C07C09', Icon: LampIcon },
  police: { bg: '#E2F1E8', fg: '#15803D', Icon: ShieldCheckIcon },
  safe: { bg: '#F2E8FA', fg: '#7C3AED', Icon: PersonPinIcon },
};

type Props = { id: RouteCriteria; title: string; meta: string; selected: boolean; onPress: () => void };

export function PresetCard({ id, title, meta, selected, onPress }: Props) {
  const icon = ICON[id];
  return (
    <Touchable
      onPress={onPress}
      accessibilityRole="radio"
      accessibilityState={{ selected }}
      style={[
        styles.card,
        selected
          ? { backgroundColor: colors.primarySoft, borderColor: colors.primary }
          : { backgroundColor: colors.surface, borderColor: colors.borderSoft },
      ]}
    >
      <View style={[styles.icon, { backgroundColor: icon.bg }]}>
        <icon.Icon color={icon.fg} />
      </View>
      <View style={{ gap: 2 }}>
        <Text size={14} weight={900} color={selected ? colors.primary : colors.text} numberOfLines={1}>
          {title}
        </Text>
        <Text size={11} color={selected ? colors.primaryMeta : colors.muted} numberOfLines={1}>
          {meta}
        </Text>
      </View>
    </Touchable>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderWidth: 2,
    borderRadius: 20,
    paddingVertical: 14,
    paddingHorizontal: 16,
  },
  icon: { width: 36, height: 36, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
});
