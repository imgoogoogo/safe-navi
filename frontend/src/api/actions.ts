import { Alert } from 'react-native';
import { api } from '.';
import { invalidate } from './useQuery';

/** Confirm, delete, refresh lists, then call `onDone`. Used on 08 and 09. */
export function confirmDelete(reportId: string, onDone: () => void) {
  Alert.alert('제보를 삭제할까요?', '삭제한 제보는 되돌릴 수 없어요.', [
    { text: '취소', style: 'cancel' },
    {
      text: '삭제',
      style: 'destructive',
      onPress: async () => {
        try {
          await api.deleteReport(reportId);
          invalidate('reports');
          invalidate('safety');
          onDone();
        } catch (e) {
          Alert.alert('삭제 실패', (e as Error).message);
        }
      },
    },
  ]);
}
