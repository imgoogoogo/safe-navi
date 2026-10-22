import { useState } from 'react';
import { Modal, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { LatLng } from '../api/types';
import { colors, radius, shadows } from '../theme';
import { BackIcon } from './icons';
import { deltaForZoom, OsmAttribution, OsmMap, PinDot } from './map';
import { Text } from './Text';
import { PrimaryButton, Touchable } from './ui';

type Props = {
  visible: boolean;
  initial: LatLng;
  onCancel: () => void;
  onConfirm: (coords: LatLng) => void;
};

/** Full-screen map with a fixed center pin, for "수동 조정" / "변경". */
export function LocationPicker({ visible, initial, onCancel, onConfirm }: Props) {
  const insets = useSafeAreaInsets();
  const [center, setCenter] = useState<LatLng>(initial);
  const d = deltaForZoom(17);

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onCancel} onShow={() => setCenter(initial)}>
      <View style={{ flex: 1, backgroundColor: colors.mapBg }}>
        <OsmMap
          style={StyleSheet.absoluteFill}
          initialRegion={{ ...initial, latitudeDelta: d, longitudeDelta: d }}
          onRegionChangeComplete={(r) => setCenter({ latitude: r.latitude, longitude: r.longitude })}
        />
        <View pointerEvents="none" style={styles.centerPin}>
          <PinDot />
        </View>
        <View style={[styles.topBar, { top: insets.top + 8 }]}>
          <Touchable onPress={onCancel} hitSlop={10}>
            <BackIcon size={22} />
          </Touchable>
          <Text size={15} weight={900}>
            지도를 움직여 위치를 맞춰주세요
          </Text>
        </View>
        <OsmAttribution style={{ bottom: insets.bottom + 96 }} />
        <View style={[styles.bottom, { paddingBottom: insets.bottom + 16 }]}>
          <PrimaryButton label="이 위치로 설정" onPress={() => onConfirm(center)} />
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  centerPin: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center' },
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
  bottom: { position: 'absolute', left: 24, right: 24, bottom: 0 },
});
