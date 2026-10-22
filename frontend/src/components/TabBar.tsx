import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { BottomTabBarProps } from 'expo-router/js-tabs';
import { colors, shadows } from '../theme';
import { BellIcon, HomeIcon, MapIcon, UserIcon } from './icons';
import { Text } from './Text';
import { Touchable } from './ui';

const TABS: Record<string, { label: string; Icon: typeof HomeIcon }> = {
  index: { label: '홈', Icon: HomeIcon },
  map: { label: '지도', Icon: MapIcon },
  alerts: { label: '알림', Icon: BellIcon },
  my: { label: 'My', Icon: UserIcon },
};

/** 홈 / 지도 / 알림 / My — active tab is blue + bold + thicker stroke. */
export function TabBar({ state, emitter, navigateToTab }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.bar, { paddingBottom: 4 + insets.bottom }]}>
      {state.routes.map((route, i) => {
        const tab = TABS[route.name];
        if (!tab) return null;
        const active = state.index === i;
        const color = active ? colors.primary : colors.muted;
        return (
          <Touchable
            key={route.key}
            style={styles.item}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            onPress={() => {
              const e = emitter.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
              if (!active && !e.defaultPrevented) navigateToTab(route.key);
            }}
          >
            <tab.Icon color={color} strokeWidth={active ? 2 : 1.8} />
            <Text size={11} weight={active ? 700 : 500} color={color}>
              {tab.label}
            </Text>
          </Touchable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    boxShadow: shadows.tabBar,
    paddingTop: 10,
    paddingHorizontal: 6,
  },
  item: { flex: 1, alignItems: 'center', gap: 3 },
});
