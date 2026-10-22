import { useEffect } from 'react';
import { Stack, SplashScreen } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useFonts } from 'expo-font';
// Per-weight imports so only the 4 weights the design uses are bundled (each is ~6MB).
import { NotoSansKR_400Regular } from '@expo-google-fonts/noto-sans-kr/400Regular';
import { NotoSansKR_500Medium } from '@expo-google-fonts/noto-sans-kr/500Medium';
import { NotoSansKR_700Bold } from '@expo-google-fonts/noto-sans-kr/700Bold';
import { NotoSansKR_900Black } from '@expo-google-fonts/noto-sans-kr/900Black';
import { AuthProvider, useAuth } from '../auth/AuthContext';
import { colors } from '../theme';

SplashScreen.preventAutoHideAsync();

function RootStack() {
  const { ready } = useAuth();
  const [fontsLoaded] = useFonts({ NotoSansKR_400Regular, NotoSansKR_500Medium, NotoSansKR_700Bold, NotoSansKR_900Black });

  useEffect(() => {
    if (ready && fontsLoaded) SplashScreen.hideAsync();
  }, [ready, fontsLoaded]);

  if (!ready || !fontsLoaded) return null;

  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}>
      <Stack.Screen name="(tabs)" />
      <Stack.Screen name="search" options={{ animation: 'fade' }} />
      <Stack.Screen name="route" />
      <Stack.Screen name="report/new" />
      <Stack.Screen name="report/[id]/index" />
      <Stack.Screen name="report/[id]/edit" />
      <Stack.Screen name="login" options={{ presentation: 'modal' }} />
      <Stack.Screen name="signup" />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <StatusBar style="dark" />
        <RootStack />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
