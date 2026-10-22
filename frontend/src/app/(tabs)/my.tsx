// 10 · My
import { Fragment } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api, REPORT_TYPE_LABEL } from '../../api';
import { useQuery } from '../../api/useQuery';
import { useAuth } from '../../auth/AuthContext';
import { Text } from '../../components/Text';
import { OutlineButton, PrimaryButton, StatusBadge, Touchable } from '../../components/ui';
import { colors, radius, shadows } from '../../theme';
import { monthDay } from '../../utils/format';

export default function MyScreen() {
  const insets = useSafeAreaInsets();
  const { user, logout } = useAuth();
  const mine = useQuery(user ? `reports:mine:${user.id}` : null, () => api.myReports());

  if (!user)
    return (
      <View style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}>
        <Text size={26} weight={900} style={styles.title}>
          My
        </Text>
        <View style={{ flex: 1, justifyContent: 'center', gap: 14, paddingHorizontal: 24 }}>
          <Text size={14} color={colors.text3} style={{ textAlign: 'center' }}>
            로그인하고 내 제보 이력을 확인하세요
          </Text>
          <PrimaryButton label="로그인" py={16} fontSize={16} onPress={() => router.push('/login')} />
        </View>
      </View>
    );

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}>
      <Text size={26} weight={900} style={styles.title}>
        My
      </Text>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View style={styles.profile}>
          <View style={styles.avatar}>
            <Text size={20} weight={900} color={colors.primary}>
              {user.name[1] ?? user.name[0]}
            </Text>
          </View>
          <View style={{ flex: 1, gap: 2 }}>
            <Text size={18} weight={900}>
              {user.name}
            </Text>
            <Text size={13} color={colors.placeholder}>
              {user.email}
            </Text>
          </View>
        </View>

        <View style={{ gap: 10 }}>
          <Text size={16} weight={900}>
            내 제보 이력
          </Text>
          <View style={styles.list}>
            {(mine.data ?? []).map((r, i) => (
              <Fragment key={r.id}>
                {i > 0 && <View style={styles.hr} />}
                <Touchable onPress={() => router.push(`/report/${r.id}`)} style={styles.row}>
                  <View style={{ flex: 1, gap: 2 }}>
                    <Text size={15} weight={700} numberOfLines={1}>
                      {REPORT_TYPE_LABEL[r.type]} · {r.place}
                    </Text>
                    <Text size={12} color={colors.placeholder}>
                      {monthDay(r.createdAt)}
                    </Text>
                  </View>
                  <StatusBadge status={r.status} />
                </Touchable>
              </Fragment>
            ))}
            {!mine.data?.length && (
              <Text size={13} color={colors.placeholder} style={{ padding: 20, textAlign: 'center' }}>
                {mine.loading ? '불러오는 중…' : '아직 등록한 제보가 없어요'}
              </Text>
            )}
          </View>
        </View>

        <OutlineButton label="로그아웃" tone="muted" onPress={async () => {
            router.navigate('/');
            await logout();
          }} style={{ flex: 0 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  title: { paddingTop: 20, paddingHorizontal: 24, paddingBottom: 10 },
  scroll: { gap: 20, paddingTop: 4, paddingHorizontal: 24, paddingBottom: 14 },
  profile: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    backgroundColor: colors.surface,
    borderRadius: radius.card,
    padding: 20,
    boxShadow: shadows.card,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primarySoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  list: { backgroundColor: colors.surface, borderRadius: radius.card, boxShadow: shadows.cardSoft },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 16, paddingHorizontal: 20 },
  hr: { height: 1, backgroundColor: colors.divider },
});
