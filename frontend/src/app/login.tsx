// 06 · 로그인
import { useRef, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, StyleSheet, View, type TextInputInstance } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { Text } from '../components/Text';
import { PillInput, PrimaryButton, Touchable } from '../components/ui';
import { colors } from '../theme';

const APP_NAME = 'SafeWalk';

const close = () => (router.canGoBack() ? router.back() : router.replace('/'));

export default function LoginScreen() {
  const insets = useSafeAreaInsets();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const pwRef = useRef<TextInputInstance>(null);

  const submit = async () => {
    if (!email.trim() || !password) return Alert.alert('이메일과 비밀번호를 입력해주세요');
    setLoading(true);
    try {
      await login(email.trim(), password);
      close();
    } catch (e) {
      Alert.alert('로그인 실패', (e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.root, { paddingTop: insets.top }]}
    >
      <View style={styles.center}>
        <View style={{ alignItems: 'center', gap: 6 }}>
          <Text size={34} weight={900} color={colors.primary} style={{ letterSpacing: -0.34 }}>
            {APP_NAME}
          </Text>
          <Text size={14} color={colors.text3}>
            안전한 밤길, 함께 걸어요
          </Text>
        </View>

        <View style={{ gap: 12 }}>
          <PillInput
            placeholder="이메일"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            returnKeyType="next"
            onSubmitEditing={() => pwRef.current?.focus()}
          />
          <PillInput
            ref={pwRef}
            placeholder="비밀번호"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="current-password"
            returnKeyType="go"
            onSubmitEditing={submit}
          />
          <PrimaryButton label="로그인" py={16} fontSize={16} loading={loading} onPress={submit} style={{ marginTop: 6 }} />
        </View>

        <View style={styles.links}>
          <Touchable onPress={() => router.push('/signup')}>
            <Text size={14} weight={700} color={colors.primary}>
              회원가입
            </Text>
          </Touchable>
          <Text size={14} color={colors.separator}>
            |
          </Text>
          <Touchable onPress={() => Alert.alert('비밀번호 찾기', '준비 중인 기능이에요.')}>
            <Text size={14} color={colors.text3}>
              비밀번호 찾기
            </Text>
          </Touchable>
        </View>
      </View>

      <Touchable onPress={close} style={[styles.guest, { paddingBottom: 30 + insets.bottom }]}>
        <Text size={15} weight={700} color={colors.primary}>
          로그인 없이 둘러보기
        </Text>
        <Text size={12} color={colors.placeholder}>
          경로 탐색 · 지도 · 안전점수는 로그인 없이 이용 가능해요
        </Text>
      </Touchable>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: 28 },
  center: { flex: 1, justifyContent: 'center', gap: 28 },
  links: { flexDirection: 'row', justifyContent: 'center', gap: 18 },
  guest: { alignItems: 'center', gap: 6 },
});
