import { api, type PageResponse } from '../../api/client';
import { fetchAll } from './lookup';

/** REST contract of the inventory backend (T07). Adjust here if the backend differs. */
export const INVENTORY_PATH = '/api/inventory-items';
/** Lookups (T06 / T04) for the item form. */
export const MANUFACTURERS_PATH = '/api/manufacturers';
export const STORES_PATH = '/api/stores';

export interface InventoryItem {
  id: number;
  name: string;
  description: string | null;
  batchNo: string | null;
  /** ISO date (yyyy-MM-dd). */
  expiryDate: string | null;
  quantity: number;
  unitPrice: number | null;
  /** Percent, e.g. 12 for 12 %. */
  gstRate: number | null;
  hsnCode: string | null;
  barcode: string | null;
  manufacturerId: number | null;
  manufacturerName?: string | null;
  storeId: number | null;
  storeName?: string | null;
  statusId?: number | null;
  statusName?: string | null;
  /** Server-computed flags; the UI falls back to its own rules when absent. */
  expiringSoon?: boolean;
  lowStock?: boolean;
}

export type InventoryItemRequest = Pick<
  InventoryItem,
  | 'name'
  | 'description'
  | 'batchNo'
  | 'expiryDate'
  | 'quantity'
  | 'unitPrice'
  | 'gstRate'
  | 'hsnCode'
  | 'barcode'
  | 'manufacturerId'
  | 'storeId'
>;

export type InventoryFilter = 'all' | 'expiring' | 'low-stock';

export interface InventoryQuery {
  q?: string;
  filter?: InventoryFilter;
  page?: number;
  size?: number;
}

export interface Option {
  id: number;
  name: string;
}

/** Window used for the "expiring soon" filter and badge. */
export const EXPIRY_WINDOW_DAYS = 30;
/** Quantity at or below which an item counts as low stock. */
export const LOW_STOCK_THRESHOLD = 10;

export const inventoryApi = {
  list: ({ q, filter = 'all', page = 0, size = 20 }: InventoryQuery = {}) =>
    api.get<PageResponse<InventoryItem>>(INVENTORY_PATH, {
      query: {
        q: q || undefined,
        expiringWithinDays: filter === 'expiring' ? EXPIRY_WINDOW_DAYS : undefined,
        lowStockThreshold: filter === 'low-stock' ? LOW_STOCK_THRESHOLD : undefined,
        page,
        size,
        sort: filter === 'expiring' ? 'expiryDate' : filter === 'low-stock' ? 'quantity' : 'name',
      },
    }),
  get: (id: number | string) => api.get<InventoryItem>(`${INVENTORY_PATH}/${id}`),
  create: (body: InventoryItemRequest) => api.post<InventoryItem>(INVENTORY_PATH, body),
  update: (id: number | string, body: InventoryItemRequest) => api.put<InventoryItem>(`${INVENTORY_PATH}/${id}`, body),
  manufacturers: () =>
    fetchAll<Option>(MANUFACTURERS_PATH, 'name').then((all) => all.map((m): Option => ({ id: m.id, name: m.name }))),
  stores: () =>
    fetchAll<Option>(STORES_PATH, 'name').then((all) => all.map((s): Option => ({ id: s.id, name: s.name }))),
};
