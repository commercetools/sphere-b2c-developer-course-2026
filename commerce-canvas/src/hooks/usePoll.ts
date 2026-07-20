import { useEffect, useRef, useState, useCallback } from 'react';

/**
 * Polls an async function on an interval and returns its latest value plus a manual refresh.
 * Fires immediately, then every `intervalMs`. Ignores results after unmount.
 */
export function usePoll<T>(fn: () => Promise<T>, intervalMs: number): {
  data: T | undefined;
  refresh: () => Promise<void>;
} {
  const [data, setData] = useState<T>();
  const fnRef = useRef(fn);
  fnRef.current = fn;

  const refresh = useCallback(async () => {
    const d = await fnRef.current();
    setData(d);
  }, []);

  useEffect(() => {
    let active = true;
    const tick = async () => {
      const d = await fnRef.current();
      if (active) setData(d);
    };
    void tick();
    const id = window.setInterval(tick, intervalMs);
    return () => {
      active = false;
      window.clearInterval(id);
    };
  }, [intervalMs]);

  return { data, refresh };
}
