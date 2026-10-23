import { api, ApiError } from '../../api/client';

export interface KpiDefinition {
  id: string;
  label: string;
  /** Candidate endpoints, tried in order; the first one that is not 404 wins. */
  endpoints: string[];
  description: string;
}

export const KPIS: KpiDefinition[] = [
  { id: 'inventory', label: 'Inventory items', endpoints: ['/api/inventory'], description: 'Stock lines on hand' },
  { id: 'invoices', label: 'Invoices', endpoints: ['/api/invoices'], description: 'Sales invoices raised' },
  { id: 'clients', label: 'Clients', endpoints: ['/api/clients'], description: 'Registered customers' },
  { id: 'manufacturers', label: 'Manufacturers', endpoints: ['/api/manufacturers'], description: 'Supplying manufacturers' },
  { id: 'stores', label: 'Stores', endpoints: ['/api/stores'], description: 'Outlets and warehouses' },
  { id: 'staff', label: 'Staff', endpoints: ['/api/staff'], description: 'Employees on record' },
  { id: 'payments', label: 'Payments', endpoints: ['/api/payments'], description: 'Payments received' },
  { id: 'users', label: 'User accounts', endpoints: ['/api/users'], description: 'Accounts able to sign in' },
];

export type KpiResult =
  | { kind: 'ok'; count: number; endpoint: string }
  | { kind: 'unavailable' }
  | { kind: 'forbidden' }
  | { kind: 'error'; message: string };

/** Count from a PageResponse (`totalElements`) or a plain array. A paged wrapper without a total is not countable. */
export function countFrom(data: unknown): number | null {
  if (Array.isArray(data)) return data.length;
  if (data && typeof data === 'object') {
    const record = data as Record<string, unknown>;
    if (typeof record.totalElements === 'number') return record.totalElements;
  }
  return null;
}

export async function loadKpi(kpi: KpiDefinition): Promise<KpiResult> {
  for (const endpoint of kpi.endpoints) {
    try {
      const data = await api.get<unknown>(endpoint, { query: { page: 0, size: 1 } });
      const count = countFrom(data);
      return count === null ? { kind: 'error', message: 'Unexpected response' } : { kind: 'ok', count, endpoint };
    } catch (err) {
      if (err instanceof ApiError && (err.status === 404 || err.status === 405 || err.status === 501)) continue;
      if (err instanceof ApiError && err.status === 403) return { kind: 'forbidden' };
      if (err instanceof ApiError) return { kind: 'error', message: `HTTP ${err.status}` };
      return { kind: 'error', message: 'Backend unreachable' };
    }
  }
  return { kind: 'unavailable' };
}
