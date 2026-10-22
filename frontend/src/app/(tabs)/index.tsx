// 01 · 홈
import { FlatList, ScrollView, StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api } from '../../api';
import { useQuery } from '../../api/useQuery';
import { PromoBanner, ReportCard, SafetyScoreCard, SearchPill } from '../../components/home';
import { Text } from '../../components/Text';
import { Touchable } from '../../components/ui';
import { useCurrentLocation } from '../../hooks/useCurrentLocation';
import { colors } from '../../theme';

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const { coords } = useCurrentLocation();
  const key = `${coords.latitude.toFixed(3)},${coords.longitude.toFixed(3)}`;
  const safety = useQuery(`safety:${key}`, () => api.safety(coords));
  const latest = useQuery('reports:latest', () => api.latestReports(5));

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.bg }}
      contentContainerStyle={{ flexGrow: 1, paddingTop: insets.top }}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.top}>
        <SearchPill onPress={() => router.push('/search')} />
        <SafetyScoreCard data={safety.data} />
      </View>

      <PromoBanner />

      <View style={styles.sectionHead}>
        <Text size={18} weight={900}>
          새로 올라온 제보
        </Text>
        <Touchable onPress={() => router.navigate('/map')} hitSlop={8}>
          <Text size={14} weight={700} color={colors.primary}>
            지도로 보기 ›
          </Text>
        </Touchable>
      </View>

      <FlatList
        horizontal
        data={latest.data ?? []}
        keyExtractor={(r) => r.id}
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.list}
        style={{ flexGrow: 0, overflow: 'visible' }}
        renderItem={({ item }) => <ReportCard report={item} onPress={() => router.push(`/report/${item.id}`)} />}
        ListEmptyComponent={
          <Text size={13} color={colors.placeholder} style={{ paddingVertical: 40 }}>
            {latest.loading ? '불러오는 중…' : '아직 올라온 제보가 없어요'}
          </Text>
        }
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  top: { gap: 16, paddingTop: 14, paddingHorizontal: 24, paddingBottom: 4 },
  sectionHead: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    paddingTop: 52,
    paddingHorizontal: 24,
    paddingBottom: 12,
  },
  list: { gap: 14, paddingHorizontal: 24, paddingBottom: 16 },
});
