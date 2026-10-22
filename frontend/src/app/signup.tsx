// 07 · 회원가입
import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { Text } from '../components/Text';
import { PillInput, PrimaryButton, ScreenHeader } from '../components/ui';
import { colors } from '../theme';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

export default function SignupScreen() {
  const insets = useSafeAreaInsets();
  const { signup } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' });
  const [loading, setLoading] = useState(false);
  const set = (k: keyof typeof form) => (v: string) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async () => {
    const { name, email, password, confirm } = form;
    if (!name.trim() || !email.trim()) return Alert.alert('이름과 이메일을 입력해주세요');
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) return Alert.alert('이메일 형식을 확인해주세요');
    if (!PASSWORD_RULE.test(password)) return Alert.alert('비밀번호는 영문·숫자 포함 8자 이상이어야 해요');
    if (password !== confirm) return Alert.alert('비밀번호가 일치하지 않아요');
    setLoading(true);
    try {
      await signup(name.trim(), email.trim(), password);
      // Close signup + login.
      router.dismissTo('/');
    } catch (e) {
      Alert.alert('가입 실패', (e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}
    >
      <ScreenHeader title="회원가입" px={28} />
      <ScrollView contentContainerStyle={{ gap: 12, paddingVertical: 14, paddingHorizontal: 28 }} keyboardShouldPersistTaps="handled">
        <PillInput placeholder="이름" value={form.name} onChangeText={set('name')} autoComplete="name" />
        <PillInput
          placeholder="이메일"
          value={form.email}
          onChangeText={set('email')}
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
        />
        <PillInput placeholder="비밀번호" value={form.password} onChangeText={set('password')} secureTextEntry autoComplete="new-password" />
        <PillInput placeholder="비밀번호 확인" value={form.confirm} onChangeText={set('confirm')} secureTextEntry autoComplete="new-password" />
        <Text size={12} color={colors.placeholder} style={{ paddingHorizontal: 8 }}>
          영문·숫자 포함 8자 이상
        </Text>
      </ScrollView>
      <View style={{ gap: 12, paddingTop: 16, paddingHorizontal: 28, paddingBottom: 30 + insets.bottom }}>
        <PrimaryButton label="가입하기" py={17} fontSize={16} loading={loading} onPress={submit} />
        <Text size={12} color={colors.placeholder} style={{ textAlign: 'center' }}>
          가입 시 서비스 이용약관 및 개인정보 처리방침에 동의하게 됩니다
        </Text>
      </View>
    </KeyboardAvoidingView>
  );
}
