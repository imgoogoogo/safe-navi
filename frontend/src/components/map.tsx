import { forwardRef, useState, type ReactNode } from 'react';
import { Platform, StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import MapView, { Marker, UrlTile, type MapViewProps } from 'react-native-maps';
import type { LatLng, Report } from '../api/types';
import { colors, radius, shadows } from '../theme';
import { Photo } from './Photo';
import { Text } from './Text';

// OSM's public tile server is fine for development only (see tile usage policy).
// Swap for a commercial provider (MapTiler, Stadia, …) before release.
export const OSM_TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';

export const deltaForZoom = (z: number) => 360 / 2 ** z;

/** MapView rendering only OpenStreetMap raster tiles. */
export const OsmMap = forwardRef<MapView, MapViewProps & { children?: ReactNode }>(function OsmMap(
  { children, ...props },
  ref,
) {
  return (
    <MapView
      ref={ref}
      // Hide the native basemap so only OSM tiles show.
      mapType={Platform.OS === 'android' ? 'none' : 'standard'}
      showsPointsOfInterests={false}
      showsCompass={false}
      toolbarEnabled={false}
      rotateEnabled={false}
      pitchEnabled={false}
      {...props}
      style={[{ backgroundColor: colors.mapBg }, props.style]}
    >
      <UrlTile urlTemplate={OSM_TILE_URL} maximumZ={19} shouldReplaceMapContent tileSize={256} />
      {children}
    </MapView>
  );
});

export function OsmAttribution({ style }: { style?: StyleProp<ViewStyle> }) {
  return (
    <View pointerEvents="none" style={[styles.attribution, style]}>
      <Text size={9} color="#5A6577">
        © OpenStreetMap
      </Text>
    </View>
  );
}

/** 60px rounded photo thumbnail with an amber dot when the report isn't resolved yet. */
export function PhotoMarker({ report, onPress }: { report: Report; onPress: () => void }) {
  const [tracks, setTracks] = useState(true);
  return (
    <Marker
      coordinate={report.location}
      onPress={onPress}
      anchor={{ x: 0.5, y: 0.5 }}
      tracksViewChanges={tracks}
    >
      {/* Outer padding leaves room for the dot (-4px) and the shadow. */}
      <View style={styles.markerWrap} onLayout={() => setTimeout(() => setTracks(false), 500)}>
        <View style={styles.markerFrame}>
          <Photo uri={report.photoUrl} radius={12} style={{ width: 52, height: 52 }} />
        </View>
        {report.status !== 'RESOLVED' && <View style={styles.markerDot} />}
      </View>
    </Marker>
  );
}

/** White circle with blue center — current location / route start. */
export function OriginMarker({ coordinate }: { coordinate: LatLng }) {
  return (
    <Marker coordinate={coordinate} anchor={{ x: 0.5, y: 0.5 }} tracksViewChanges={false}>
      <View style={styles.originWrap}>
        <View style={styles.origin}>
          <View style={styles.originDot} />
        </View>
      </View>
    </Marker>
  );
}

/** Dark label pill + dot — route destination. Anchored at the dot. */
export function DestinationMarker({ coordinate, label }: { coordinate: LatLng; label: string }) {
  return (
    <Marker coordinate={coordinate} anchor={{ x: 0.5, y: 0.86 }} tracksViewChanges={false}>
      <View style={{ alignItems: 'center', padding: 4 }}>
        <View style={styles.destLabel}>
          <Text size={12} weight={900} color="#FFFFFF" numberOfLines={1}>
            {label}
          </Text>
        </View>
        <View style={styles.destDot} />
      </View>
    </Marker>
  );
}

/** Blue dot with white border — single location pin (08 미니맵). */
export function PinDot() {
  return <View style={styles.pinDot} />;
}

const styles = StyleSheet.create({
  attribution: {
    position: 'absolute',
    left: 10,
    bottom: 8,
    backgroundColor: 'rgba(255,255,255,0.7)',
    borderRadius: 4,
    paddingHorizontal: 5,
    paddingVertical: 1,
  },
  markerWrap: { padding: 8 },
  markerFrame: {
    width: 60,
    height: 60,
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: 4,
    boxShadow: shadows.marker,
  },
  markerDot: {
    position: 'absolute',
    right: 4,
    top: 4,
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: colors.amber,
    borderWidth: 2.5,
    borderColor: '#FFFFFF',
  },
  originWrap: { padding: 8 },
  origin: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#FFFFFF',
    boxShadow: shadows.dot,
    alignItems: 'center',
    justifyContent: 'center',
  },
  originDot: { width: 12, height: 12, borderRadius: 6, backgroundColor: colors.primary },
  destLabel: {
    backgroundColor: colors.text,
    borderRadius: radius.pill,
    paddingVertical: 5,
    paddingHorizontal: 12,
  },
  destDot: {
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: colors.text,
    borderWidth: 3,
    borderColor: '#FFFFFF',
    marginTop: 4,
  },
  pinDot: {
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: colors.primary,
    borderWidth: 3,
    borderColor: '#FFFFFF',
    boxShadow: '0 2px 8px rgba(27,42,84,0.35)',
  },
});
