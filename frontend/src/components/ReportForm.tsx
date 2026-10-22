// Shared form for 05 제보 등록 and 09 제보 수정.
import { useEffect, useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, TextInput, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api } from '../api';
import { REPORT_TYPE_LABEL, REPORT_TYPES, type LatLng, type ReportType } from '../api/types';
import { colors, fonts, radius, shadows } from '../theme';
import { PinIcon } from './icons';
import { LocationPicker } from './LocationPicker';
import { Photo } from './Photo';
import { Text } from './Text';
import { OutlineButton, PrimaryButton, SectionLabel, SoftButton, Touchable } from './ui';

export type ReportFormValues = {
  photoUri: string | null;
  type: ReportType;
  location: LatLng;
  address: string;
  memo: string;
};

type Props = {
  mode: 'create' | 'edit';
  initial: ReportFormValues;
  /** create mode: user not logged in → CTA becomes "로그인하고 제보하기" */
  loggedIn: boolean;
  submitting: boolean;
  onSubmit: (values: ReportFormValues) => void;
  onLogin?: () => void;
  onCancel?: () => void;
  header: React.ReactNode;
};

export function ReportForm({ mode, initial, loggedIn, submitting, onSubmit, onLogin, onCancel, header }: Props) {
  const insets = useSafeAreaInsets();
  const [values, setValues] = useState<ReportFormValues>(initial);
  const [memoFocused, setMemoFocused] = useState(false);
  const [picking, setPicking] = useState(false);
  const set = <K extends keyof ReportFormValues>(k: K, v: ReportFormValues[K]) => setValues((s) => ({ ...s, [k]: v }));

  // Auto-detected location/address can arrive after mount; fill it in unless the user already picked one.
  useEffect(() => {
    setValues((s) => (s.address ? s : { ...s, location: initial.location, address: initial.address }));
  }, [initial.address, initial.location]);

  const pickPhoto = async (source: 'camera' | 'library') => {
    const perm =
      source === 'camera'
        ? await ImagePicker.requestCameraPermissionsAsync()
        : await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) {
      Alert.alert('권한이 필요해요', source === 'camera' ? '카메라 접근을 허용해주세요.' : '사진 접근을 허용해주세요.');
      return;
    }
    const opts: ImagePicker.ImagePickerOptions = { mediaTypes: ['images'], quality: 0.7, allowsEditing: true, aspect: [4, 3] };
    const res = source === 'camera' ? await ImagePicker.launchCameraAsync(opts) : await ImagePicker.launchImageLibraryAsync(opts);
    if (!res.canceled && res.assets[0]) set('photoUri', res.assets[0].uri);
  };

  const onPickLocation = async (coords: LatLng) => {
    setPicking(false);
    set('location', coords);
    try {
      const { address } = await api.reverseGeocode(coords);
      set('address', address);
    } catch {}
  };

  const submit = () => {
    if (mode === 'create' && !loggedIn) return onLogin?.();
    onSubmit(values);
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      {header}
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* 사진 */}
        <View style={styles.section}>
          <SectionLabel>사진</SectionLabel>
          <Touchable onPress={() => pickPhoto('library')}>
            <Photo
              uri={values.photoUri}
              radius={20}
              style={{ height: 160 }}
              placeholder={mode === 'create' ? '현장 사진을 촬영하거나 선택하세요' : '사진을 교체하려면 탭하세요'}
            />
          </Touchable>
          <View style={styles.row}>
            {mode === 'create' ? (
              <>
                <SoftButton label="카메라" onPress={() => pickPhoto('camera')} />
                <SoftButton label="갤러리" onPress={() => pickPhoto('library')} />
              </>
            ) : (
              <>
                <SoftButton label="사진 교체" onPress={() => pickPhoto('library')} />
                <SoftButton label="사진 삭제" danger onPress={() => set('photoUri', null)} />
              </>
            )}
          </View>
        </View>

        {/* 제보 유형 */}
        <View style={styles.section}>
          <SectionLabel>제보 유형</SectionLabel>
          <View style={styles.row} accessibilityRole="radiogroup">
            {REPORT_TYPES.map((t) => {
              const on = values.type === t;
              return (
                <Touchable
                  key={t}
                  onPress={() => set('type', t)}
                  accessibilityRole="radio"
                  accessibilityState={{ selected: on }}
                  style={[styles.chip, on ? styles.chipOn : styles.chipOff]}
                >
                  <Text size={14} weight={on ? 700 : 500} color={on ? '#FFFFFF' : colors.text3} style={{ textAlign: 'center' }}>
                    {REPORT_TYPE_LABEL[t]}
                  </Text>
                </Touchable>
              );
            })}
          </View>
        </View>

        {/* 위치 */}
        <View style={styles.section}>
          <SectionLabel>위치</SectionLabel>
          <View style={styles.locationCard}>
            <PinIcon size={20} />
            <View style={{ flex: 1, gap: 2 }}>
              <Text size={15} weight={700}>
                {values.address || '위치 확인 중…'}
              </Text>
              <Text size={12} color={colors.placeholder}>
                {mode === 'create' ? '현재 위치 자동 감지됨' : '등록 당시 위치'}
              </Text>
            </View>
            <Touchable onPress={() => setPicking(true)} hitSlop={8}>
              <Text size={13} weight={700} color={colors.primary}>
                {mode === 'create' ? '수동 조정' : '변경'}
              </Text>
            </Touchable>
          </View>
        </View>

        {/* 메모 */}
        <View style={styles.section}>
          <SectionLabel>
            메모
            {mode === 'create' && (
              <Text size={14} color={colors.placeholder}>
                {' '}
                (선택)
              </Text>
            )}
          </SectionLabel>
          <TextInput
            value={values.memo}
            onChangeText={(t) => set('memo', t)}
            placeholder="상황을 간단히 적어주세요"
            placeholderTextColor={colors.placeholder}
            multiline
            textAlignVertical="top"
            onFocus={() => setMemoFocused(true)}
            onBlur={() => setMemoFocused(false)}
            style={[styles.memo, { borderColor: memoFocused ? colors.primary : 'transparent' }]}
          />
        </View>
      </ScrollView>

      <View style={[styles.footer, { paddingBottom: 14 + insets.bottom }]}>
        {mode === 'edit' ? (
          <View style={styles.row}>
            <OutlineButton label="취소" tone="muted" py={17} fontSize={16} onPress={onCancel} />
            <PrimaryButton
              label="수정 완료"
              py={17}
              fontSize={16}
              loading={submitting}
              onPress={submit}
              style={{ flex: 1.6, boxShadow: shadows.primaryButtonSoft }}
            />
          </View>
        ) : loggedIn ? (
          <PrimaryButton label="제보 전송" py={18} loading={submitting} onPress={submit} />
        ) : (
          <View style={{ gap: 10 }}>
            <Text size={13} color={colors.text3} style={{ textAlign: 'center' }}>
              제보 등록에는 로그인이 필요해요
            </Text>
            <PrimaryButton label="로그인하고 제보하기" py={18} onPress={submit} style={{ boxShadow: [] }} />
          </View>
        )}
      </View>

      <LocationPicker
        visible={picking}
        initial={values.location}
        onCancel={() => setPicking(false)}
        onConfirm={onPickLocation}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  scroll: { gap: 20, paddingHorizontal: 24, paddingTop: 6, paddingBottom: 6 },
  section: { gap: 10 },
  row: { flexDirection: 'row', gap: 10 },
  chip: { flex: 1, borderRadius: radius.pill, paddingVertical: 12 },
  chipOn: { backgroundColor: colors.primary, boxShadow: shadows.primaryChip },
  chipOff: { backgroundColor: colors.surface, boxShadow: shadows.chip },
  locationCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    backgroundColor: colors.surface,
    borderRadius: 20,
    paddingVertical: 16,
    paddingHorizontal: 18,
    boxShadow: shadows.cardSoft,
  },
  memo: {
    minHeight: 92,
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1.5,
    paddingVertical: 16,
    paddingHorizontal: 18,
    fontSize: 14,
    lineHeight: 22,
    fontFamily: fonts[400],
    color: colors.text,
    boxShadow: shadows.cardSoft,
  },
  footer: { paddingTop: 16, paddingHorizontal: 24 },
});
