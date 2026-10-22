import { useEffect, useState } from 'react';
import * as Location from 'expo-location';
import { USE_MOCK } from '../api/client';
import { MOCK_ORIGIN } from '../api/mock';
import type { LatLng } from '../api/types';

/**
 * Current position, falling back to the design's 서초동 sample point
 * when permission is denied or the lookup fails.
 */
export function useCurrentLocation() {
  const [coords, setCoords] = useState<LatLng>(MOCK_ORIGIN);
  const [detected, setDetected] = useState(false);

  useEffect(() => {
    // Mock data lives in 서초동, so stay there in mock mode.
    if (USE_MOCK) return;
    let alive = true;
    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== 'granted') return;
        const pos =
          (await Location.getLastKnownPositionAsync()) ??
          (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
        if (alive && pos) {
          setCoords({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
          setDetected(true);
        }
      } catch {}
    })();
    return () => {
      alive = false;
    };
  }, []);

  return { coords, detected };
}
