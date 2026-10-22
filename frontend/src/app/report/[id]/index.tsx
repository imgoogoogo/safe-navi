// 08 · 제보 상세
import { ScrollView, StyleSheet, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api } from '../../../api';
import { confirmDelete } from '../../../api/actions';
import { useQuery } from '../../../api/useQuery';
import { useAuth } from '../../../auth/AuthContext';
import { BackIcon } from '../../../components/icons';
import { deltaForZoom, OsmMap, PinDot } from '../../../components/map';
import { Photo } from '../../../components/Photo';
import { Text } from '../../../components/Text';
import { OutlineButton, SectionLabel, StatusBadge, Touchable, TypeBadge } from '../../../components/ui';
import { colors, shadows } from '../../../theme';
import { fullDateTime, relativeTime } from '../../../utils/format';

export default function ReportDetailScreen() {
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { user } = useAuth();
  const { data: r, error } = useQuery(`reports:detail:${id}`, () => api.report(id));
  const canEdit = !!user && !!r?.isOwner;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <View style={{ height: 290 }}>
        <Photo uri={r?.photoUrl} style={StyleSheet.absoluteFill} placeholder={r?.photoUrl ? undefined : '제보 현장 사진'} />
        <Touchable onPress={() => router.back()} style={[styles.back, { top: insets.top + 8 }]}>
          <BackIcon size={20} />
        </Touchable>
      </View>

      <ScrollView style={styles.body} contentContainerStyle={styles.bodyContent} showsVerticalScrollIndicator={false}>
        {!r ? (
          <Text size={14} color={colors.placeholder}>
            {error ? error.message : '불러오는 중…'}
          </Text>
        ) : (
          <>
            <View style={{ gap: 10 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                <TypeBadge type={r.type} style={{ paddingVertical: 4, paddingHorizontal: 12 }} />
                <StatusBadge status={r.status} />
                <Text size={12} color={colors.placeholder}>
                  {relativeTime(r.createdAt)}
                </Text>
              </View>
              <Text size={22} weight={900}>
                {r.place}
              </Text>
              {!!r.memo && (
                <Text size={14} color={colors.text3} style={{ lineHeight: 22 }}>
                  {r.memo}
                </Text>
              )}
            </View>

            <View style={{ gap: 10 }}>
              <SectionLabel>위치</SectionLabel>
              <View style={styles.miniMap}>
                <OsmMap
                  scrollEnabled={false}
                  zoomEnabled={false}
                  pointerEvents="none"
                  style={StyleSheet.absoluteFill}
                  initialRegion={{ ...r.location, latitudeDelta: deltaForZoom(16), longitudeDelta: deltaForZoom(16) }}
                />
                <View pointerEvents="none" style={styles.center}>
                  <PinDot />
                </View>
              </View>
              <Text size={13} color={colors.text3}>
                {r.address}
              </Text>
            </View>

            <View style={styles.infoCard}>
              <View style={[styles.infoRow, styles.hr]}>
                <Text size={13} color={colors.muted}>
                  제보자
                </Text>
                <Text size={14} weight={700}>
                  {r.authorName}
                </Text>
              </View>
              <View style={styles.infoRow}>
                <Text size={13} color={colors.muted}>
                  등록 시각
                </Text>
                <Text size={14} weight={700}>
                  {fullDateTime(r.createdAt)}
                </Text>
              </View>
            </View>
          </>
        )}
      </ScrollView>

      {r && canEdit && (
        <View style={[styles.footer, { paddingBottom: 16 + insets.bottom }]}>
          <OutlineButton label="수정" onPress={() => router.push(`/report/${r.id}/edit`)} />
          <OutlineButton label="삭제" tone="danger" onPress={() => confirmDelete(r.id, () => router.back())} />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  back: {
    position: 'absolute',
    left: 20,
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.92)',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: shadows.roundButton,
  },
  body: {
    flex: 1,
    backgroundColor: colors.bg,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    marginTop: -28,
  },
  bodyContent: { gap: 20, padding: 24 },
  miniMap: { height: 130, borderRadius: 20, overflow: 'hidden', backgroundColor: colors.mapBg },
  center: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center' },
  infoCard: { backgroundColor: colors.surface, borderRadius: 20, boxShadow: shadows.cardSoft },
  infoRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 15, paddingHorizontal: 18 },
  hr: { borderBottomWidth: 1, borderBottomColor: colors.divider },
  footer: { flexDirection: 'row', gap: 10, paddingTop: 14, paddingHorizontal: 24, backgroundColor: colors.bg },
});
