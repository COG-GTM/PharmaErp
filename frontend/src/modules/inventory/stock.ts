import { EXPIRY_WINDOW_DAYS, LOW_STOCK_THRESHOLD, type InventoryFilter, type InventoryItem } from './api';

const DAY_MS = 86_400_000;

function startOfDay(d: Date): number {
  return Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
}

/** Whole days from `today` until an ISO yyyy-MM-dd date (negative when already past). */
export function daysUntil(isoDate: string, today: Date = new Date()): number | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(isoDate);
  if (!m) return null;
  const target = Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return Math.round((target - startOfDay(today)) / DAY_MS);
}

export type BadgeTone = 'danger' | 'warning' | 'muted';

export interface StockBadge {
  label: string;
  tone: BadgeTone;
}

export function isExpiringSoon(item: InventoryItem, today?: Date): boolean {
  if (!item.expiryDate) return false;
  const days = daysUntil(item.expiryDate, today);
  if (days === null || days < 0) return false;
  return item.expiringSoon ?? days <= EXPIRY_WINDOW_DAYS;
}

export function isExpired(item: InventoryItem, today?: Date): boolean {
  if (!item.expiryDate) return false;
  const days = daysUntil(item.expiryDate, today);
  return days !== null && days < 0;
}

export function isLowStock(item: InventoryItem): boolean {
  return item.lowStock ?? item.quantity <= LOW_STOCK_THRESHOLD;
}

export function stockBadges(item: InventoryItem, today?: Date): StockBadge[] {
  const badges: StockBadge[] = [];
  if (isExpired(item, today)) badges.push({ label: 'Expired', tone: 'danger' });
  else if (isExpiringSoon(item, today)) {
    const days = daysUntil(item.expiryDate ?? '', today) ?? 0;
    badges.push({ label: days === 0 ? 'Expires today' : `Expires in ${days} day${days === 1 ? '' : 's'}`, tone: 'warning' });
  }
  if (item.quantity <= 0) badges.push({ label: 'Out of stock', tone: 'danger' });
  else if (isLowStock(item)) badges.push({ label: 'Low stock', tone: 'warning' });
  return badges;
}

/**
 * Applies the filter locally as well, so the screen stays correct even if the backend ignores
 * the filter query parameters (idempotent when it does honour them).
 * "Expiring soon" includes already-expired stock, which also needs pulling from shelves (FEFO).
 */
export function applyFilter(items: InventoryItem[], filter: InventoryFilter, today?: Date): InventoryItem[] {
  if (filter === 'expiring') return items.filter((i) => isExpired(i, today) || isExpiringSoon(i, today));
  if (filter === 'low-stock') return items.filter(isLowStock);
  return items;
}
