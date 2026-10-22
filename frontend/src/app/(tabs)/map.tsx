// 04 · 지도
import { useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { Region } from 'react-native-maps';
import { api, REPORT_TYPE_LABEL, REPORT_TYPES, type BBox, type Report, type ReportType } from '../../api';
import { useQuery } from '../../api/useQuery';
import { CloseIcon, MenuIcon, PlusIcon, SearchIcon } from '../../components/icons';
import { deltaForZoom, OsmAttribution, OsmMap, PhotoMarker } from '../../components/map';
import { Photo } from '../../components/Photo';
import { Text } from '../../components/Text';
import { Touchable, TypeBadge } from '../../components/ui';
import { useCurrentLocation } from '../../hooks/useCurrentLocation';
import { colors, radius, shadows } from '../../theme';
import { relativeTime } from '../../utils/format';

const toBBox = (r: Region): BBox => {
  const round = (n: number) => Math.round(n * 1000) / 1000;
  return {
    minLat: round(r.latitude - r.latitudeDelta / 2),
    maxLat: round(r.latitude + r.latitudeDelta / 2),
    minLng: round(r.longitude - r.longitudeDelta / 2),
    maxLng: round(r.longitude + r.longitudeDelta / 2),
  };
};

export default function MapScreen() {
  const insets = useSafeAreaInsets();
  const { coords } = useCurrentLocation();
  const initialRegion = useMemo(
    () => ({ ...coords, latitudeDelta: deltaForZoom(15), longitudeDelta: deltaForZoom(15) }),
    [coords],
  );
  const [bbox, setBBox] = useState<BBox>(() => toBBox(initialRegion));
  const [selected, setSelected] = useState<Report | null>(null);
  const [filterOpen, setFilterOpen] = useState(false);
  const [filter, setFilter] = useState<ReportType | null>(null);

  const reports = useQuery(`reports:bbox:${JSON.stringify(bbox)}`, () => api.reportsInBBox(bbox));
  const area = useQuery(`geo:${coords.latitude.toFixed(3)},${coords.longitude.toFixed(3)}`, () => api.reverseGeocode(coords));
  const visible = (reports.data ?? []).filter((r) => !filter || r.type === filter);

  return (
    <View style={{ flex: 1, backgroundColor: colors.mapBg }}>
      <OsmMap
        key={`${coords.latitude},${coords.longitude}`}
        style={StyleSheet.absoluteFill}
        initialRegion={initialRegion}
        onRegionChangeComplete={(r) => setBBox(toBBox(r))}
        onPress={() => setSelected(null)}
      >
        {visible.map((r) => (
          <PhotoMarker key={r.id} report={r} onPress={() => setSelected(r)} />
        ))}
      </OsmMap>

      {/* 검색 / 필터 바 */}
      <View style={[styles.searchWrap, { top: insets.top + 8 }]}>
        <View style={styles.searchBar}>
          <SearchIcon color="#5A6577" />
          <Text size={15} color={colors.text2} style={{ flex: 1 }} numberOfLines={1}>
            {filter ? `${REPORT_TYPE_LABEL[filter]} 제보만 보는 중` : `${area.data?.areaName.replace(/ 일대$/, '') ?? '내 주변'} 주변 제보 검색`}
          </Text>
          <Touchable onPress={() => setFilterOpen((v) => !v)} hitSlop={10}>
            <MenuIcon />
          </Touchable>
        </View>
        {filterOpen && (
          <View style={styles.filterRow}>
            {[null, ...REPORT_TYPES].map((t) => {
              const on = filter === t;
              return (
                <Touchable
                  key={t ?? 'all'}
                  onPress={() => {
                    setFilter(t);
                    setFilterOpen(false);
                    setSelected(null);
                  }}
                  style={[styles.filterChip, on && { backgroundColor: colors.primary }]}
                >
                  <Text size={13} weight={700} color={on ? '#FFFFFF' : colors.text2}>
                    {t ? REPORT_TYPE_LABEL[t] : '전체'}
                  </Text>
                </Touchable>
              );
            })}
          </View>
        )}
      </View>

      <OsmAttribution />

      {selected ? (
        <View style={styles.detail}>
          <Photo uri={selected.photoUrl} radius={12} style={{ width: 92, height: 118 }} />
          <View style={{ flex: 1, minWidth: 0, gap: 6, paddingTop: 4 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
              <TypeBadge type={selected.type} />
              <Text size={12} color={colors.placeholder}>
                {relativeTime(selected.createdAt)}
              </Text>
            </View>
            <Text size={18} weight={900} numberOfLines={1}>
              {selected.place}
            </Text>
            <Text size={13} color={colors.text3} numberOfLines={1}>
              {selected.memo}
            </Text>
            <Touchable onPress={() => router.push(`/report/${selected.id}`)} hitSlop={6}>
              <Text size={14} weight={700} color={colors.primary}>
                자세히 보기 ›
              </Text>
            </Touchable>
          </View>
          <Touchable onPress={() => setSelected(null)} style={styles.close} hitSlop={8}>
            <CloseIcon size={15} />
          </Touchable>
        </View>
      ) : (
        <View style={styles.fabWrap} pointerEvents="box-none">
          <Touchable onPress={() => router.push('/report/new')} style={styles.fab}>
            <PlusIcon />
            <Text size={14} weight={900} color="#FFFFFF">
              제보 등록
            </Text>
          </Touchable>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  searchWrap: { position: 'absolute', left: 20, right: 20, gap: 10 },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
    paddingVertical: 14,
    paddingHorizontal: 20,
    boxShadow: shadows.floating,
  },
  filterRow: { flexDirection: 'row', gap: 8 },
  filterChip: {
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
    paddingVertical: 8,
    paddingHorizontal: 14,
    boxShadow: shadows.chip,
  },
  detail: {
    position: 'absolute',
    left: 14,
    right: 14,
    bottom: 16,
    flexDirection: 'row',
    gap: 16,
    backgroundColor: colors.surface,
    borderRadius: 22,
    padding: 16,
    boxShadow: shadows.popup,
  },
  close: { width: 26, height: 26, alignItems: 'center', justifyContent: 'center' },
  fabWrap: { position: 'absolute', left: 0, right: 0, bottom: 28, alignItems: 'center' },
  fab: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: colors.primary,
    borderRadius: radius.pill,
    paddingVertical: 11,
    paddingHorizontal: 18,
    boxShadow: shadows.fab,
  },
});
