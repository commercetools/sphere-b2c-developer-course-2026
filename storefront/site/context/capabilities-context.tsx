'use client';

import { createContext, useContext, useMemo } from 'react';
import useSWR from 'swr';
import { bffGet } from '@/lib/bff/client';
import { CAPABILITY_META, CapabilityMeta } from '@/lib/capabilities';

interface CapabilityStatus {
  loading: boolean;
  unlocked: boolean;
  meta: CapabilityMeta;
}

interface CapabilitiesContextValue {
  loading: boolean;
  unlocked: Set<string>;
  isUnlocked: (capability: string) => boolean;
  status: (capability: string) => CapabilityStatus;
}

const CapabilitiesContext = createContext<CapabilitiesContextValue | null>(null);

// The BFF returns { unlocked: [<capability of every completed task>] }. Per-capability metadata
// (session / task label / endpoint) lives locally in CAPABILITY_META — the storefront holds no
// credentials and derives everything else from that map.
const fetcher = (path: string) => bffGet<{ unlocked: string[] }>(path);

/**
 * Fetches GET /api/training/capabilities and re-polls, so features unlock live as participants
 * implement tasks. Provided at the layout level; consumed by every <FeatureGate>.
 */
export function CapabilitiesProvider({ children }: { children: React.ReactNode }) {
  const { data, isLoading } = useSWR('training/capabilities', fetcher, {
    refreshInterval: 5000,
    revalidateOnFocus: true,
  });

  const value = useMemo<CapabilitiesContextValue>(() => {
    const unlocked = new Set<string>(data?.data?.unlocked ?? []);
    const metaFor = (capability: string): CapabilityMeta =>
      CAPABILITY_META[capability] ?? {
        capability,
        session: '—',
        sessionName: '',
        taskLabel: capability,
        bffEndpoint: '',
      };
    return {
      loading: isLoading,
      unlocked,
      isUnlocked: (capability: string) => unlocked.has(capability),
      status: (capability: string) => ({
        loading: isLoading,
        unlocked: unlocked.has(capability),
        meta: metaFor(capability),
      }),
    };
  }, [data, isLoading]);

  return <CapabilitiesContext.Provider value={value}>{children}</CapabilitiesContext.Provider>;
}

export function useCapabilities(): CapabilitiesContextValue {
  const ctx = useContext(CapabilitiesContext);
  if (!ctx) throw new Error('useCapabilities must be used within CapabilitiesProvider');
  return ctx;
}

export function useCapability(capability: string): CapabilityStatus {
  return useCapabilities().status(capability);
}
