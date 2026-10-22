// 05 · 제보 등록
import { useState } from 'react';
import { Alert, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api } from '../../api';
import { invalidate, useQuery } from '../../api/useQuery';
import { useAuth } from '../../auth/AuthContext';
import { ReportForm, type ReportFormValues } from '../../components/ReportForm';
import { ScreenHeader } from '../../components/ui';
import { useCurrentLocation } from '../../hooks/useCurrentLocation';
import { colors } from '../../theme';

export default function NewReportScreen() {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { coords } = useCurrentLocation();
  const geo = useQuery(`geo:${coords.latitude.toFixed(4)},${coords.longitude.toFixed(4)}`, () => api.reverseGeocode(coords));
  const [submitting, setSubmitting] = useState(false);

  const submit = async (v: ReportFormValues) => {
    setSubmitting(true);
    try {
      const upload = v.photoUri ? await api.uploadPhoto(v.photoUri) : null;
      const created = await api.createReport(
        { type: v.type, location: v.location, address: v.address, memo: v.memo || undefined, photoId: upload?.photoId ?? null },
        upload?.url,
      );
      invalidate('reports');
      invalidate('safety');
      router.replace(`/report/${created.id}`);
    } catch (e) {
      Alert.alert('제보 전송 실패', (e as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, paddingTop: insets.top }}>
      <ReportForm
        mode="create"
        header={<ScreenHeader title="제보 등록" />}
        initial={{ photoUri: null, type: 'DARK', location: coords, address: geo.data?.address ?? '', memo: '' }}
        loggedIn={!!user}
        submitting={submitting}
        onSubmit={submit}
        onLogin={() => router.push('/login')}
      />
    </View>
  );
}
