import { api, type PageResponse } from '../../api/client';
import { fetchAll } from './lookup';

/** REST contract of the inventory backend (T07): `/api/inventory`, plus `/expiring` and `/low-stock` views. */
export const INVENTORY_PATH = '/api/inventory';
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
  storeId: number | null;
  /** Server-computed: days until expiry (negative once expired), null without an expiry date. */
  daysToExpiry?: number | null;
  /** Per-item low-stock level; the backend flags LOW at or below it. */
  reorderLevel?: number;
  /** Stored per-batch level; null when the batch follows the configured default. */
  reorderLevelOverride?: number | null;
  stockValue?: number | null;
  status?: string | null;
  stockStatus?: StockStatus;
  employeeId?: number | null;
  clientId?: number | null;
  assignDate?: string | null;
  deliveryDate?: string | null;
  totalAmount?: number | null;
  advanceAmount?: number | null;
}

export type StockStatus = 'IN_STOCK' | 'LOW' | 'OUT_OF_STOCK' | 'EXPIRED';

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

/** Attributes the edit form does not show; they are carried over from the loaded item on update. */
export const PRESERVED_ON_UPDATE = [
  'reorderLevel',
  'status',
  'employeeId',
  'clientId',
  'assignDate',
  'deliveryDate',
  'totalAmount',
  'advanceAmount',
] as const;

/**
 * Body of `PUT /api/inventory/{id}`: a full replacement without `quantity`, which only changes through
 * `POST /api/inventory/{id}/adjust` so every movement is tracked.
 */
export type InventoryItemUpdateRequest = Omit<InventoryItemRequest, 'quantity'> &
  Pick<InventoryItem, (typeof PRESERVED_ON_UPDATE)[number]>;

/** Movement reasons accepted by the adjust endpoint (sign rules are enforced by the backend). */
export const ADJUSTMENT_REASONS = [
  'RECEIPT',
  'RETURN',
  'TRANSFER_IN',
  'SALE',
  'TRANSFER_OUT',
  'DAMAGE',
  'EXPIRY_WRITE_OFF',
  'CORRECTION',
] as const;
export type AdjustmentReason = (typeof ADJUSTMENT_REASONS)[number];

export interface StockAdjustmentRequest {
  quantityChange: number;
  reason: AdjustmentReason;
  note: string | null;
}

export interface StockAdjustmentResponse {
  item: InventoryItem;
}

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
  list: ({ q, filter = 'all', page = 0, size = 20 }: InventoryQuery = {}) => {
    if (filter === 'expiring') {
      return api.get<PageResponse<InventoryItem>>(`${INVENTORY_PATH}/expiring`, {
        query: { days: EXPIRY_WINDOW_DAYS, q: q || undefined, page, size, sort: 'expiryDate' },
      });
    }
    if (filter === 'low-stock') {
      // No threshold: the backend compares each item against its own reorder level.
      return api.get<PageResponse<InventoryItem>>(`${INVENTORY_PATH}/low-stock`, {
        query: { q: q || undefined, page, size, sort: 'quantity' },
      });
    }
    return api.get<PageResponse<InventoryItem>>(INVENTORY_PATH, { query: { q: q || undefined, page, size, sort: 'name' } });
  },
  get: (id: number | string) => api.get<InventoryItem>(`${INVENTORY_PATH}/${id}`),
  create: (body: InventoryItemRequest) => api.post<InventoryItem>(INVENTORY_PATH, body),
  update: (id: number | string, body: InventoryItemUpdateRequest) =>
    api.put<InventoryItem>(`${INVENTORY_PATH}/${id}`, body),
  adjust: (id: number | string, body: StockAdjustmentRequest) =>
    api.post<StockAdjustmentResponse>(`${INVENTORY_PATH}/${id}/adjust`, body),
  manufacturers: () =>
    fetchAll<Option>(MANUFACTURERS_PATH, 'name').then((all) => all.map((m): Option => ({ id: m.id, name: m.name }))),
  stores: () =>
    fetchAll<Option>(STORES_PATH, 'name').then((all) => all.map((s): Option => ({ id: s.id, name: s.name }))),
};
