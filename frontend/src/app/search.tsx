// 02 · 목적지 검색
import { Fragment, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api, type Place } from '../api';
import { useQuery } from '../api/useQuery';
import { useAuth } from '../auth/AuthContext';
import { PinIcon, SearchIcon } from '../components/icons';
import { Text } from '../components/Text';
import { Touchable } from '../components/ui';
import { useCurrentLocation } from '../hooks/useCurrentLocation';
import { colors, fonts, radius, shadows } from '../theme';
import { km } from '../utils/format';

function useDebounced<T>(value: T, ms = 300) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

const goRoute = (p: Place) =>
  router.push({
    pathname: '/route',
    params: { name: p.name, lat: String(p.location.latitude), lng: String(p.location.longitude) },
  });

export default function SearchScreen() {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { coords } = useCurrentLocation();
  const [q, setQ] = useState('');
  const query = useDebounced(q.trim());
  const results = useQuery(query ? `places:${query}` : null, () => api.searchPlaces(query, coords));
  const favs = useQuery(user ? `favorites:${user.id}` : null, () => api.favorites());

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}>
      <View style={styles.header}>
        <View style={styles.inputPill}>
          <SearchIcon color={colors.primary} />
          <TextInput
            autoFocus
            value={q}
            onChangeText={setQ}
            placeholder="어디로 갈까요?"
            placeholderTextColor={colors.placeholder}
            selectionColor={colors.primary}
            returnKeyType="search"
            onSubmitEditing={() => results.data?.[0] && goRoute(results.data[0])}
            style={styles.input}
          />
        </View>
        <Touchable onPress={() => router.back()} hitSlop={8}>
          <Text size={15} color={colors.text3}>
            취소
          </Text>
        </Touchable>
      </View>

      <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingBottom: 24 }}>
        {!!favs.data?.length && (
          <View style={styles.favs}>
            <Text size={13} weight={700} color={colors.muted}>
              즐겨찾기
            </Text>
            <View style={{ flexDirection: 'row', gap: 10 }}>
              {favs.data.map((f) => (
                <Touchable key={f.id} onPress={() => goRoute(f.place)} style={styles.favChip}>
                  <Text size={14} weight={700} color={colors.primary}>
                    {f.label}
                  </Text>
                </Touchable>
              ))}
            </View>
          </View>
        )}

        {!!query && (
          <View style={styles.results}>
            <Text size={13} weight={700} color={colors.muted}>
              검색 결과
            </Text>
            <View style={styles.card}>
              {(results.data ?? []).map((p, i) => (
                <Fragment key={p.id}>
                  {i > 0 && <View style={styles.hr} />}
                  <Touchable onPress={() => goRoute(p)} style={styles.row}>
                    <PinIcon color={i === 0 ? colors.primary : colors.muted} />
                    <View style={{ flex: 1, gap: 2 }}>
                      <Text size={16} weight={700} numberOfLines={1}>
                        {p.name}
                      </Text>
                      <Text size={12} color={colors.placeholder} numberOfLines={1}>
                        {p.address}
                      </Text>
                    </View>
                    <Text size={12} color={colors.placeholder}>
                      {km(p.distanceMeters)}
                    </Text>
                  </Touchable>
                </Fragment>
              ))}
              {!results.data?.length && (
                <Text size={13} color={colors.placeholder} style={{ padding: 20, textAlign: 'center' }}>
                  {results.loading ? '검색 중…' : '검색 결과가 없어요'}
                </Text>
              )}
            </View>
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingTop: 18, paddingHorizontal: 24, paddingBottom: 12 },
  inputPill: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.primary,
    borderRadius: radius.pill,
    paddingVertical: 13,
    paddingHorizontal: 20,
    boxShadow: shadows.card,
  },
  input: { flex: 1, padding: 0, fontSize: 15, fontFamily: fonts[500], color: colors.text },
  favs: { gap: 10, paddingVertical: 8, paddingHorizontal: 24 },
  favChip: {
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
    paddingVertical: 9,
    paddingHorizontal: 18,
    boxShadow: shadows.chip,
  },
  results: { gap: 10, paddingVertical: 14, paddingHorizontal: 24 },
  card: { backgroundColor: colors.surface, borderRadius: radius.card, boxShadow: shadows.cardSoft },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 16, paddingHorizontal: 20 },
  hr: { height: 1, backgroundColor: colors.divider },
});
