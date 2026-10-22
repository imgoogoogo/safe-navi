// 09 · 제보 수정
import { useState } from 'react';
import { Alert, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api } from '../../../api';
import { confirmDelete } from '../../../api/actions';
import { invalidate, useQuery } from '../../../api/useQuery';
import { useAuth } from '../../../auth/AuthContext';
import { ReportForm, type ReportFormValues } from '../../../components/ReportForm';
import { Text } from '../../../components/Text';
import { ScreenHeader, Touchable } from '../../../components/ui';
import { colors } from '../../../theme';

export default function EditReportScreen() {
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { user } = useAuth();
  const { data: r } = useQuery(`reports:detail:${id}`, () => api.report(id));
  const [submitting, setSubmitting] = useState(false);

  const submit = async (v: ReportFormValues) => {
    if (!r) return;
    setSubmitting(true);
    try {
      // Only send what changed (PATCH).
      let photoId: string | null | undefined;
      let photoUrl: string | null | undefined;
      if (v.photoUri !== r.photoUrl) {
        if (v.photoUri) {
          const up = await api.uploadPhoto(v.photoUri);
          photoId = up.photoId;
          photoUrl = up.url;
        } else {
          photoId = null;
          photoUrl = null;
        }
      }
      const moved = v.location.latitude !== r.location.latitude || v.location.longitude !== r.location.longitude;
      await api.updateReport(
        r.id,
        {
          type: v.type !== r.type ? v.type : undefined,
          memo: v.memo !== r.memo ? v.memo : undefined,
          location: moved ? v.location : undefined,
          address: moved ? v.address : undefined,
          photoId,
        },
        photoUrl,
      );
      invalidate('reports');
      router.back();
    } catch (e) {
      Alert.alert('수정 실패', (e as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}>
      {r && (
        <ReportForm
          mode="edit"
          header={
            <ScreenHeader
              title="제보 수정"
              right={
                <Touchable onPress={() => confirmDelete(r.id, () => router.dismissTo('/'))} hitSlop={8}>
                  <Text size={14} weight={700} color={colors.danger}>
                    삭제
                  </Text>
                </Touchable>
              }
            />
          }
          initial={{ photoUri: r.photoUrl, type: r.type, location: r.location, address: r.address, memo: r.memo }}
          loggedIn={!!user}
          submitting={submitting}
          onSubmit={submit}
          onCancel={() => router.back()}
        />
      )}
    </View>
  );
}
