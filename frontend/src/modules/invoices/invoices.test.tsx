import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';
import invoices from './index';
import type { Invoice } from './api';
import { formatInr, lineAmounts, summarize } from './gst';
import { fetchAll } from './lookup';
import { mockApi, page } from './testing';

const invoice: Invoice = {
  id: 7,
  invoiceNumber: 'INV-2026-0007',
  clientId: 3,
  clientName: 'City Hospital',
  storeId: 2,
  storeName: 'Central Store',
  status: 'PARTIALLY_PAID',
  invoiceDate: '2026-10-20',
  invoiceDescription: 'Monthly supply',
  subtotal: 450,
  gstAmount: 39.99,
  totalAmount: 489.99,
  paidAmount: 200,
  lines: [
    { id: 1, itemId: 1, description: 'Paracetamol 500mg', quantity: 100, unitPrice: 2.5, gstRate: 12, gstAmount: 29.99, lineTotal: 279.99 },
    { id: 2, itemId: null, description: 'Bandage roll', quantity: 10, unitPrice: 20, gstRate: 5, gstAmount: 10, lineTotal: 210 },
  ],
};

function renderAt(route: string) {
  setToken('t');
  return renderWithProviders(
    <Routes>
      <Route path="/invoices/*" element={invoices.element} />
    </Routes>,
    { route },
  );
}

/** Intl uses a narrow no-break space in some locales; compare on digits only. */
const digits = (s: string | null | undefined) => (s ?? '').replace(/[^\d.]/g, '');

describe('GST arithmetic', () => {
  it('computes line amounts with half-up rounding to paise', () => {
    expect(lineAmounts({ quantity: 3, unitPrice: 33.33, gstRate: 18 })).toEqual({ taxable: 99.99, gst: 18, total: 117.99 });
    expect(lineAmounts({ quantity: 1, unitPrice: 0.1, gstRate: 5 })).toEqual({ taxable: 0.1, gst: 0.01, total: 0.11 });
    expect(lineAmounts({ quantity: 7, unitPrice: 0.1, gstRate: 2.5 })).toEqual({ taxable: 0.7, gst: 0.02, total: 0.72 });
  });

  it('groups by rate and splits CGST/SGST without losing a paisa', () => {
    const s = summarize([
      { quantity: 100, unitPrice: 2.5, gstRate: 12 },
      { quantity: 10, unitPrice: 20, gstRate: 5 },
      { quantity: 1, unitPrice: 0.1, gstRate: 5 },
    ]);
    expect(s.subtotal).toBe(450.1);
    expect(s.gst).toBe(40.01);
    expect(s.total).toBe(490.11);
    expect(s.rates.map((r) => r.rate)).toEqual([5, 12]);
    expect(s.rates[0]).toEqual({ rate: 5, taxable: 200.1, cgst: 5.01, sgst: 5, gst: 10.01 });
    expect(Math.round((s.cgst + s.sgst) * 100)).toBe(4001);
  });
});

describe('invoices module', () => {
  it('registers a module definition', () => {
    expect(invoices).toMatchObject({ id: 'invoices', path: 'invoices', navLabel: 'Invoices' });
  });

  it('lists invoices with totals', async () => {
    const { calls } = mockApi(() => ({ body: page([invoice]) }));
    renderAt('/invoices');
    expect(await screen.findByText('INV-2026-0007')).toBeInTheDocument();
    expect(screen.getByText('City Hospital')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View e-receipt INV-2026-0007' })).toHaveAttribute('href', '/invoices/7');
    expect(calls[0]?.url).toMatch(/^\/api\/invoices\?page=0&size=20/);
  });

  it('shows a live GST breakdown while lines are edited, then creates the invoice', async () => {
    const { calls } = mockApi(({ method, url }) => {
      if (url.startsWith('/api/clients')) return { body: page([{ id: 3, name: 'City Hospital' }]) };
      if (url.startsWith('/api/stores')) return { body: page([]) };
      if (url.startsWith('/api/inventory-items'))
        return { body: page([{ id: 1, name: 'Paracetamol 500mg', batchNo: 'B-100', unitPrice: 2.5, gstRate: 12, quantity: 500 }]) };
      if (method === 'POST') return { status: 201, body: invoice };
      if (url.startsWith('/api/payments')) return { body: page([]) };
      if (url === '/api/invoices/7') return { body: invoice };
      return undefined;
    });
    renderAt('/invoices/new');

    await userEvent.selectOptions(screen.getByLabelText('Client'), await screen.findByRole('option', { name: 'City Hospital' }));
    await userEvent.selectOptions(screen.getByLabelText('Line 1 item'), await screen.findByRole('option', { name: 'Paracetamol 500mg (B-100)' }));
    expect(screen.getByLabelText('Line 1 description')).toHaveValue('Paracetamol 500mg');
    const qty1 = screen.getByLabelText('Line 1 quantity');
    await userEvent.clear(qty1);
    await userEvent.type(qty1, '100');
    expect(digits(screen.getByTestId('line-1-total').textContent)).toBe('280.00');
    expect(digits(screen.getByTestId('summary-gst').textContent)).toBe('30.00');

    await userEvent.click(screen.getByRole('button', { name: 'Add line' }));
    await userEvent.type(screen.getByLabelText('Line 2 description'), 'Bandage roll');
    await userEvent.clear(screen.getByLabelText('Line 2 quantity'));
    await userEvent.type(screen.getByLabelText('Line 2 quantity'), '10');
    await userEvent.type(screen.getByLabelText('Line 2 unit price'), '20');
    const rate2 = screen.getByLabelText('Line 2 GST rate');
    await userEvent.clear(rate2);
    await userEvent.type(rate2, '5');

    expect(digits(screen.getByTestId('summary-subtotal').textContent)).toBe('450.00');
    expect(digits(screen.getByTestId('summary-cgst').textContent)).toBe('20.00');
    expect(digits(screen.getByTestId('summary-sgst').textContent)).toBe('20.00');
    expect(digits(screen.getByTestId('summary-total').textContent)).toBe('490.00');
    const breakdown = screen.getByRole('region', { name: 'Live GST breakdown' });
    expect(within(breakdown).getByText('5%')).toBeInTheDocument();
    expect(within(breakdown).getByText('12%')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Create invoice' }));
    await screen.findByRole('heading', { name: /E-receipt · INV-2026-0007/ });
    expect(calls.find((c) => c.method === 'POST')).toMatchObject({
      url: '/api/invoices',
      body: {
        clientId: 3,
        storeId: null,
        invoiceDescription: null,
        lines: [
          { itemId: 1, description: 'Paracetamol 500mg', quantity: 100, unitPrice: 2.5, gstRate: 12 },
          { itemId: null, description: 'Bandage roll', quantity: 10, unitPrice: 20, gstRate: 5 },
        ],
      },
    });
  });

  it('validates lines before posting', async () => {
    const { calls } = mockApi(({ url }) => (url.includes('?') ? { body: page([]) } : undefined));
    renderAt('/invoices/new');
    await userEvent.clear(screen.getByLabelText('Line 1 quantity'));
    await userEvent.type(screen.getByLabelText('Line 1 quantity'), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Create invoice' }));
    expect(screen.getByText('Line 1: description is required')).toBeInTheDocument();
    expect(screen.getByText('Line 1: quantity must be a whole number ≥ 1')).toBeInTheDocument();
    expect(screen.getByText('Line 1: unit price must be an amount with up to 2 decimals')).toBeInTheDocument();
    expect(calls.filter((c) => c.method === 'POST')).toHaveLength(0);
  });

  it('surfaces server problem details on create', async () => {
    mockApi(({ method, url }) => {
      if (method === 'POST') return { status: 409, body: { status: 409, title: 'Conflict', detail: 'Insufficient stock for item 1' } };
      return url.includes('?') ? { body: page([]) } : undefined;
    });
    renderAt('/invoices/new');
    await userEvent.type(screen.getByLabelText('Line 1 description'), 'Paracetamol');
    await userEvent.type(screen.getByLabelText('Line 1 unit price'), '2.5');
    await userEvent.click(screen.getByRole('button', { name: 'Create invoice' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Insufficient stock for item 1');
  });

  it('renders the e-receipt with GST split, payments and balance', async () => {
    const { calls } = mockApi(({ url }) => {
      if (url === '/api/invoices/7') return { body: invoice };
      if (url.startsWith('/api/payments'))
        return { body: page([{ id: 9, receiptNumber: 'RCPT-0009', paymentAmount: 200, paymentType: 'UPI', paidAt: '2026-10-21T10:00:00Z' }]) };
      return undefined;
    });
    renderAt('/invoices/7');

    expect(await screen.findByRole('heading', { name: 'E-receipt · INV-2026-0007' })).toBeInTheDocument();
    expect(screen.getByText('City Hospital')).toBeInTheDocument();
    expect(await screen.findByText('RCPT-0009')).toBeInTheDocument();
    // Breakdown uses the persisted line GST (29.99 + 10.00), not a recalculation (30 + 10).
    expect(digits(screen.getByTestId('summary-gst').textContent)).toBe('39.99');
    expect(digits(screen.getByTestId('summary-cgst').textContent)).toBe('20.00');
    expect(digits(screen.getByTestId('summary-sgst').textContent)).toBe('19.99');
    expect(digits(screen.getByTestId('summary-total').textContent)).toBe('489.99');
    expect(digits(screen.getByTestId('receipt-balance').textContent)).toBe('289.99');
    expect(screen.getByTestId('receipt-balance').textContent).toContain(formatInr(289.99));
    await waitFor(() => expect(calls.some((c) => c.url.startsWith('/api/payments?invoiceId=7'))).toBe(true));
  });

  it('keeps saved invoice totals and flags line/header differences', async () => {
    mockApi(({ url }) =>
      url === '/api/invoices/7' ? { body: { ...invoice, gstAmount: 37.5, totalAmount: 487.5 } } : { body: page([]) },
    );
    renderAt('/invoices/7');
    expect(await screen.findByTestId('receipt-adjustment')).toHaveTextContent(formatInr(489.99));
    expect(digits(screen.getByTestId('summary-gst').textContent)).toBe('37.50');
    expect(digits(screen.getByTestId('summary-total').textContent)).toBe('487.50');
    expect(digits(screen.getByTestId('receipt-balance').textContent)).toBe('287.50');
  });

  it('keeps loaded lookup pages when a later page fails', async () => {
    mockApi(({ url }) =>
      url.includes('page=0')
        ? { body: { content: [{ id: 1 }], page: 0, size: 200, totalElements: 2, totalPages: 2 } }
        : { status: 503, body: { status: 503 } },
    );
    expect((await fetchAll<{ id: number }>('/api/clients', 'name')).map((c) => c.id)).toEqual([1]);
  });

  it('loads every page of a lookup endpoint', async () => {
    const { calls } = mockApi(({ url }) => {
      const p = Number(new URL(url, 'http://x').searchParams.get('page'));
      return { body: { content: [{ id: p + 1, name: `Client ${p + 1}` }], page: p, size: 200, totalElements: 3, totalPages: 3 } };
    });
    const all = await fetchAll<{ id: number }>('/api/clients', 'name');
    expect(all.map((c) => c.id)).toEqual([1, 2, 3]);
    expect(calls).toHaveLength(3);
  });

  it('still renders the e-receipt when payments are unavailable', async () => {
    mockApi(({ url }) => (url === '/api/invoices/7' ? { body: invoice } : { status: 503, body: { status: 503, title: 'Unavailable' } }));
    renderAt('/invoices/7');
    expect(await screen.findByRole('heading', { name: /E-receipt/ })).toBeInTheDocument();
    expect(screen.queryByText('Payments')).not.toBeInTheDocument();
    expect(await screen.findByTestId('payments-unavailable')).toBeInTheDocument();
  });

  it('does not show a truncated payment history', async () => {
    mockApi(({ url }) => {
      if (url === '/api/invoices/7') return { body: invoice };
      if (url.includes('page=0'))
        return { body: { content: [{ id: 9, receiptNumber: 'RCPT-0009', paymentAmount: 200, paymentType: 'UPI', paidAt: null }], page: 0, size: 200, totalElements: 201, totalPages: 2 } };
      return { status: 503, body: { status: 503 } };
    });
    renderAt('/invoices/7');
    expect(await screen.findByTestId('payments-unavailable')).toBeInTheDocument();
    expect(screen.queryByText('RCPT-0009')).not.toBeInTheDocument();
  });

  it('shows not-found errors on the e-receipt', async () => {
    mockApi(() => ({ status: 404, body: { status: 404, title: 'Not Found', detail: 'Invoice 99 not found' } }));
    renderAt('/invoices/99');
    expect(await screen.findByRole('alert')).toHaveTextContent('Record not found.');
  });
});
