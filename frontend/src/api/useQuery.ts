// Tiny cached query hook (stand-in for react-query, which conflicts with this SDK's peer deps).
import { useCallback, useEffect, useRef, useSyncExternalStore } from 'react';

type Entry = { data?: unknown; error?: Error; inFlight?: boolean; stale?: boolean };

const cache = new Map<string, Entry>();
const listeners = new Set<() => void>();
let version = 0;
const emit = () => {
  version++;
  listeners.forEach((l) => l());
};
const subscribe = (l: () => void) => {
  listeners.add(l);
  return () => {
    listeners.delete(l);
  };
};

/** Mark cached results whose key starts with `prefix` stale; mounted screens refetch, keeping old data meanwhile. */
export function invalidate(prefix: string) {
  for (const [key, e] of cache) if (key.startsWith(prefix)) cache.set(key, { ...e, stale: true });
  emit();
}

export function useQuery<T>(key: string | null, fn: () => Promise<T>) {
  const v = useSyncExternalStore(subscribe, () => version);
  const fnRef = useRef(fn);
  fnRef.current = fn;

  const run = useCallback((k: string) => {
    const prev = cache.get(k) ?? {};
    cache.set(k, { ...prev, inFlight: true, stale: false });
    fnRef
      .current()
      .then((data) => cache.set(k, { data }))
      .catch((error: Error) => cache.set(k, { ...prev, error, inFlight: false, stale: false }))
      .finally(emit);
  }, []);

  useEffect(() => {
    if (!key) return;
    const e = cache.get(key);
    if (!e || (e.stale && !e.inFlight)) run(key);
  }, [key, v, run]);

  const entry = key ? cache.get(key) : undefined;
  return {
    data: entry?.data as T | undefined,
    error: entry?.error,
    loading: !!key && entry?.data === undefined && !entry?.error,
    refetch: () => {
      if (key) run(key);
    },
  };
}
