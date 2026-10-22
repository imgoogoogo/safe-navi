import { Tabs } from 'expo-router/js-tabs';
import { TabBar } from '../../components/TabBar';
import { useAuth } from '../../auth/AuthContext';
import { colors } from '../../theme';

export default function TabsLayout() {
  const { requireAuth } = useAuth();
  return (
    <Tabs
      tabBar={(props) => <TabBar {...props} />}
      screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: colors.bg } }}
    >
      <Tabs.Screen name="index" />
      <Tabs.Screen name="map" />
      <Tabs.Screen name="alerts" />
      {/* My needs login; guests are sent to 06 로그인. */}
      <Tabs.Screen name="my" listeners={{ tabPress: (e) => !requireAuth() && e.preventDefault() }} />
    </Tabs>
  );
}
