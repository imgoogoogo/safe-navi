// 03 · 경로 탐색 (지도 + 기준 선택)
import { useEffect, useRef, useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import MapView, { Polyline } from 'react-native-maps';
import { api, type LatLng, type RouteCriteria, type RouteResult } from '../api';
import { useQuery } from '../api/useQuery';
import { BackIcon } from '../components/icons';
import { deltaForZoom, DestinationMarker, OriginMarker, OsmAttribution, OsmMap } from '../components/map';
import { PRESETS, PresetCard } from '../components/PresetCard';
import { Text } from '../components/Text';
import { PrimaryButton, Touchable } from '../components/ui';
import { useCurrentLocation } from '../hooks/useCurrentLocation';
import { colors, radius, shadows } from '../theme';
import { km } from '../utils/format';

const meta = (r?: RouteResult) => (r ? `${r.durationMinutes}분 · ${r.highlight}` : '경로 계산 중…');

/** One cached request per criteria, so switching cards is instant after the first load. */
function useRoutes(from: LatLng, to: LatLng) {
  const k = `${from.latitude},${from.longitude}>${to.latitude},${to.longitude}`;
  const cctv = useQuery(`route:${k}:cctv`, () => api.route(from, to, 'cctv'));
  const light = useQuery(`route:${k}:light`, () => api.route(from, to, 'light'));
  const police = useQuery(`route:${k}:police`, () => api.route(from, to, 'police'));
  const safe = useQuery(`route:${k}:safe`, () => api.route(from, to, 'safe'));
  return { cctv: cctv.data, light: light.data, police: police.data, safe: safe.data } as Record<RouteCriteria, RouteResult | undefined>;
}

export default function RouteScreen() {
  const insets = useSafeAreaInsets();
  const params = useLocalSearchParams<{ name: string; lat: string; lng: string }>();
  const dest: LatLng = { latitude: Number(params.lat), longitude: Number(params.lng) };
  const { coords: origin } = useCurrentLocation();
  const [criteria, setCriteria] = useState<RouteCriteria>('cctv');
  const routes = useRoutes(origin, dest);
  const route = routes[criteria];
  const mapRef = useRef<MapView>(null);
  const [sheetH, setSheetH] = useState(260);

  // Frame the whole route above the bottom sheet.
  useEffect(() => {
    if (!route) return;
    mapRef.current?.fitToCoordinates(route.polyline, {
      edgePadding: { top: insets.top + 110, right: 70, bottom: sheetH + 40, left: 70 },
      animated: true,
    });
  }, [route, sheetH, insets.top]);

  const mid = { latitude: (origin.latitude + dest.latitude) / 2, longitude: (origin.longitude + dest.longitude) / 2 };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <View style={{ flex: 1, backgroundColor: colors.mapBg }}>
        <OsmMap
          ref={mapRef}
          style={StyleSheet.absoluteFill}
          initialRegion={{ ...mid, latitudeDelta: deltaForZoom(14), longitudeDelta: deltaForZoom(14) }}
        >
          {route && (
            <>
              <Polyline coordinates={route.polyline} strokeColor="#FFFFFF" strokeWidth={15} lineCap="round" lineJoin="round" />
              <Polyline coordinates={route.polyline} strokeColor={colors.primary} strokeWidth={8} lineCap="round" lineJoin="round" zIndex={1} />
            </>
          )}
          <OriginMarker coordinate={origin} />
          <DestinationMarker coordinate={dest} label={params.name} />
        </OsmMap>
        {/* Soft wash over the tiles, as in the design */}
        <View pointerEvents="none" style={[StyleSheet.absoluteFill, { backgroundColor: 'rgba(244,246,250,0.35)' }]} />

        <View style={[styles.topBar, { top: insets.top + 8 }]}>
          <Touchable onPress={() => router.back()} hitSlop={10}>
            <BackIcon size={22} />
          </Touchable>
          <View style={{ flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8, minWidth: 0 }}>
            <Text size={14} color={colors.text3}>
              내 현재 위치
            </Text>
            <Text size={13} color={colors.faint}>
              →
            </Text>
            <Text size={15} weight={900} numberOfLines={1} style={{ flexShrink: 1 }}>
              {params.name}
            </Text>
          </View>
        </View>
        <OsmAttribution style={{ bottom: 8 + 28 }} />
      </View>

      <View style={[styles.sheet, { paddingBottom: 16 + insets.bottom }]} onLayout={(e) => setSheetH(e.nativeEvent.layout.height)}>
        <View style={styles.grabber} />
        <View style={styles.sheetHead}>
          <Text size={16} weight={900}>
            어떤 길로 안내할까요?
          </Text>
          <Text size={13} color={colors.muted}>
            {route ? `${meta(route)} · ${km(route.distanceMeters)}` : ''}
          </Text>
        </View>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.presets}
          accessibilityRole="radiogroup"
        >
          {PRESETS.map((p) => (
            <PresetCard
              key={p.id}
              id={p.id}
              title={p.short}
              meta={meta(routes[p.id])}
              selected={criteria === p.id}
              onPress={() => setCriteria(p.id)}
            />
          ))}
        </ScrollView>
        <View style={{ paddingHorizontal: 24 }}>
          <PrimaryButton
            label="이 기준으로 안내 시작"
            disabled={!route}
            style={{ boxShadow: shadows.primaryButtonSoft }}
            onPress={() =>
              Alert.alert('안내를 시작할게요', `${PRESETS.find((p) => p.id === criteria)!.name} 경로로 ${params.name}까지 안내합니다.`)
            }
          />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  topBar: {
    position: 'absolute',
    left: 20,
    right: 20,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
    paddingVertical: 13,
    paddingHorizontal: 18,
    boxShadow: shadows.floating,
  },
  sheet: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
    marginTop: -28,
    paddingTop: 12,
    boxShadow: shadows.sheet,
  },
  grabber: { width: 44, height: 5, borderRadius: 3, backgroundColor: colors.grabber, alignSelf: 'center', marginBottom: 12 },
  sheetHead: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    paddingHorizontal: 24,
    paddingBottom: 12,
  },
  presets: { gap: 12, paddingTop: 2, paddingHorizontal: 24, paddingBottom: 14 },
});
