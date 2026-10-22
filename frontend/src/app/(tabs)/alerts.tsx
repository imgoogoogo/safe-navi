// 알림 — not designed yet; placeholder that matches the app's look.
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { BellIcon } from '../../components/icons';
import { Text } from '../../components/Text';
import { colors } from '../../theme';

export default function AlertsScreen() {
  const insets = useSafeAreaInsets();
  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}>
      <Text size={26} weight={900} style={{ paddingTop: 20, paddingHorizontal: 24, paddingBottom: 10 }}>
        알림
      </Text>
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 10 }}>
        <BellIcon size={36} color={colors.faint} />
        <Text size={14} color={colors.placeholder}>
          새로운 알림이 없어요
        </Text>
      </View>
    </View>
  );
}
