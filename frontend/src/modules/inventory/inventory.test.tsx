import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import inventory from './index';
import type { InventoryItem } from './api';
import { applyFilter, daysUntil, stockBadges } from './stock';
import { mockApi, page } from './testing';

function isoInDays(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const base: InventoryItem = {
  id: 1,
  name: 'Paracetamol 500mg',
  description: null,
  batchNo: 'B-100',
  expiryDate: isoInDays(400),
  quantity: 120,
  unitPrice: 2.5,
  gstRate: 12,
  hsnCode: '3004',
  barcode: '8901234567890',
  manufacturerId: 5,
  manufacturerName: 'Sun Labs',
  storeId: 2,
  storeName: 'Central Store',
};
const expiring: InventoryItem = { ...base, id: 2, name: 'Amoxicillin 250mg', expiryDate: isoInDays(10) };
const low: InventoryItem = { ...base, id: 3, name: 'Insulin pen', quantity: 4 };
const expired: InventoryItem = { ...base, id: 4, name: 'Cough syrup', expiryDate: isoInDays(-3), quantity: 0 };

function renderAt(route: string) {
  setToken('t');
  return renderWithProviders(
    <Routes>
      <Route path="/inventory/*" element={inventory.element} />
    </Routes>,
    { route },
  );
}

describe('stock rules', () => {
  it('computes days until expiry', () => {
    expect(daysUntil('2026-01-11', new Date(2026, 0, 1, 23, 59))).toBe(10);
    expect(daysUntil('2025-12-31', new Date(2026, 0, 1))).toBe(-1);
    expect(daysUntil('garbage')).toBeNull();
  });

  it('derives badges from expiry date and quantity', () => {
    expect(stockBadges(base)).toEqual([]);
    expect(stockBadges(expiring)).toEqual([{ label: 'Expires in 10 days', tone: 'warning' }]);
    expect(stockBadges(low)).toEqual([{ label: 'Low stock', tone: 'warning' }]);
    expect(stockBadges(expired)).toEqual([
      { label: 'Expired', tone: 'danger' },
      { label: 'Out of stock', tone: 'danger' },
    ]);
  });

  it('prefers server flags when present', () => {
    expect(stockBadges({ ...base, lowStock: true, expiringSoon: true }).map((b) => b.label)).toEqual([
      'Expires in 400 days',
      'Low stock',
    ]);
    expect(stockBadges({ ...low, lowStock: false })).toEqual([]);
  });

  it('filters locally for expiring-soon and low-stock', () => {
    const all = [base, expiring, low, expired];
    expect(applyFilter(all, 'expiring').map((i) => i.id)).toEqual([2, 4]);
    expect(applyFilter(all, 'low-stock').map((i) => i.id)).toEqual([3, 4]);
    expect(applyFilter(all, 'all')).toHaveLength(4);
  });
});

describe('inventory module', () => {
  it('registers a module definition', () => {
    expect(inventory).toMatchObject({ id: 'inventory', path: 'inventory', navLabel: 'Inventory' });
  });

  it('lists items with expiry and stock badges', async () => {
    mockApi(({ url }) => {
      if (url.startsWith('/api/stores')) return { body: page([{ id: 2, name: 'Central Store' }]) };
      return { body: page([base, expiring, { ...low, manufacturerName: undefined, manufacturerId: 6 }, expired]) };
    });
    renderAt('/inventory');

    const row = await screen.findByTestId('item-2');
    expect(within(row).getByText('Expires in 10 days')).toBeInTheDocument();
    expect(within(screen.getByTestId('item-3')).getByText('Low stock')).toBeInTheDocument();
    expect(within(screen.getByTestId('item-4')).getByText('Expired')).toBeInTheDocument();
    expect(within(screen.getByTestId('item-4')).getByText('Out of stock')).toBeInTheDocument();
    expect(within(screen.getByTestId('item-1')).queryByText(/Expires|stock/)).not.toBeInTheDocument();
    expect(within(screen.getByTestId('item-3')).getByText('#6')).toBeInTheDocument();
  });

  it('switches to the expiring-soon and low-stock filters', async () => {
    const { calls } = mockApi(() => ({ body: page([base, expiring, low, expired]) }));
    renderAt('/inventory');
    await screen.findByTestId('item-1');

    await userEvent.click(screen.getByRole('button', { name: /Expiring soon/ }));
    await waitFor(() => expect(calls.at(-1)?.url).toContain('expiringWithinDays=30'));
    expect(screen.getByRole('button', { name: /Expiring soon/ })).toHaveAttribute('aria-pressed', 'true');
    await waitFor(() => expect(screen.queryByTestId('item-1')).not.toBeInTheDocument());
    expect(screen.getByTestId('item-2')).toBeInTheDocument();
    expect(screen.queryByTestId('item-3')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Low stock/ }));
    await waitFor(() => expect(calls.at(-1)?.url).toContain('lowStockThreshold=10'));
    await waitFor(() => expect(screen.queryByTestId('item-2')).not.toBeInTheDocument());
    expect(screen.getByTestId('item-3')).toBeInTheDocument();
  });

  it('honours a deep-linked filter', async () => {
    const { calls } = mockApi(() => ({ body: page([low]) }));
    renderAt('/inventory?filter=low-stock');
    expect(await screen.findByTestId('item-3')).toBeInTheDocument();
    expect(calls.find((c) => c.url.startsWith('/api/inventory-items'))?.url).toContain('lowStockThreshold=10');
  });

  it('filters each page locally and says so when results span several pages', async () => {
    mockApi(({ url }) =>
      url.startsWith('/api/inventory-items') ? { body: { ...page([base, low]), totalPages: 2, totalElements: 21 } } : { body: page([]) },
    );
    renderAt('/inventory?filter=low-stock');
    expect(await screen.findByTestId('item-3')).toBeInTheDocument();
    expect(screen.queryByTestId('item-1')).not.toBeInTheDocument();
    expect(screen.getByTestId('partial-filter')).toBeInTheDocument();
  });

  it('creates an item with manufacturer and store', async () => {
    const { calls } = mockApi(({ method, url }) => {
      if (url.startsWith('/api/manufacturers')) return { body: page([{ id: 5, name: 'Sun Labs' }]) };
      if (url.startsWith('/api/stores')) return { body: page([{ id: 2, name: 'Central Store' }]) };
      if (method === 'POST') return { status: 201, body: base };
      return { body: page([base]) };
    });
    renderAt('/inventory/new');

    await userEvent.type(screen.getByLabelText('Item name'), 'Paracetamol 500mg');
    await userEvent.type(screen.getByLabelText('Batch no.'), 'B-100');
    await userEvent.type(screen.getByLabelText('Expiry date'), '2027-06-30');
    await userEvent.clear(screen.getByLabelText('Quantity'));
    await userEvent.type(screen.getByLabelText('Quantity'), '120');
    await userEvent.type(screen.getByLabelText('Unit price (₹)'), '2.50');
    await userEvent.type(screen.getByLabelText('GST rate (%)'), '12');
    await userEvent.selectOptions(screen.getByLabelText('Manufacturer'), await screen.findByRole('option', { name: 'Sun Labs' }));
    await userEvent.selectOptions(screen.getByLabelText('Store'), await screen.findByRole('option', { name: 'Central Store' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save item' }));

    await screen.findByRole('heading', { name: 'Inventory' });
    expect(calls.find((c) => c.method === 'POST')).toMatchObject({
      url: '/api/inventory-items',
      body: {
        name: 'Paracetamol 500mg',
        batchNo: 'B-100',
        expiryDate: '2027-06-30',
        quantity: 120,
        unitPrice: 2.5,
        gstRate: 12,
        manufacturerId: 5,
        storeId: 2,
        barcode: null,
      },
    });
  });

  it('rejects negative quantity and out-of-range GST', async () => {
    const { calls } = mockApi(({ url }) => (url.includes('?') ? { body: page([]) } : undefined));
    renderAt('/inventory/new');
    await userEvent.type(screen.getByLabelText('Item name'), 'X');
    await userEvent.clear(screen.getByLabelText('Quantity'));
    await userEvent.type(screen.getByLabelText('Quantity'), '-1');
    await userEvent.type(screen.getByLabelText('GST rate (%)'), '150');
    await userEvent.click(screen.getByRole('button', { name: 'Save item' }));

    expect(screen.getByText('Quantity must be a whole number ≥ 0')).toBeInTheDocument();
    expect(screen.getByText('GST rate must be between 0 and 100')).toBeInTheDocument();
    expect(calls.filter((c) => c.method !== 'GET')).toHaveLength(0);
  });

  it('edits an existing item', async () => {
    const { calls } = mockApi(({ method, url }) => {
      if (method === 'PUT') return { body: low };
      if (url === '/api/inventory-items/3') return { body: low };
      return { body: page([]) };
    });
    renderAt('/inventory/3/edit');

    const qty = await screen.findByDisplayValue('4');
    await userEvent.clear(qty);
    await userEvent.type(qty, '50');
    await userEvent.click(screen.getByRole('button', { name: 'Save item' }));

    await screen.findByRole('heading', { name: 'Inventory' });
    expect(calls.find((c) => c.method === 'PUT')).toMatchObject({ url: '/api/inventory-items/3', body: { quantity: 50, storeId: 2 } });
  });
});
