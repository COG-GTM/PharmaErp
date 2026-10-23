import { api, type PageResponse } from '../../api/client';
import { fetchAll } from './lookup';

/** REST contract of the finance backend (T08). Adjust here if the backend differs. */
export const INVOICES_PATH = '/api/invoices';
/** Payments (T09) shown on the e-receipt. */
export const PAYMENTS_PATH = '/api/payments';
/** Lookups for the create screen (T05 / T04 / T07). */
export const CLIENTS_PATH = '/api/clients';
export const STORES_PATH = '/api/stores';
export const INVENTORY_PATH = '/api/inventory';

export interface InvoiceLine {
  id?: number;
  itemId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  taxableAmount: number;
  /** Percent. */
  gstRate: number;
  cgstRate: number;
  cgstAmount: number;
  sgstRate: number;
  sgstAmount: number;
  gstAmount: number;
  lineTotal: number;
}

/** Row of `GET /api/invoices` (backend `InvoiceSummaryResponse`). */
export interface InvoiceSummary {
  id: number;
  invoiceNumber: string;
  clientId: number | null;
  storeId: number | null;
  status: string;
  invoiceDate: string;
  subtotal: number;
  gstAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
}

/** `GET /api/invoices/{id}` (backend `InvoiceResponse`). */
export interface Invoice extends InvoiceSummary {
  prescriptionId: number | null;
  description: string | null;
  cgstAmount: number;
  sgstAmount: number;
  createdAt?: string;
  lines: InvoiceLine[];
}

export interface InvoiceLineRequest {
  itemId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  gstRate: number;
}

export interface CreateInvoiceRequest {
  clientId: number | null;
  storeId: number | null;
  invoiceDate: string;
  description: string | null;
  lines: InvoiceLineRequest[];
}

/** Backend `PaymentResponse`. */
export interface Payment {
  id: number;
  invoiceId: number;
  clientId: number | null;
  paymentMethodId: number;
  paymentType: string | null;
  amount: number;
  reference: string | null;
  receiptNumber: string | null;
  paidAt: string;
}

export interface Option {
  id: number;
  name: string;
}

export interface ItemOption extends Option {
  itemName: string;
  unitPrice: number | null;
  gstRate: number | null;
  quantity: number;
}

export const invoicesApi = {
  list: ({ page = 0, size = 20 }: { page?: number; size?: number } = {}) =>
    api.get<PageResponse<InvoiceSummary>>(INVOICES_PATH, { query: { page, size, sort: 'invoiceDate,desc' } }),
  get: (id: number | string) => api.get<Invoice>(`${INVOICES_PATH}/${id}`),
  create: (body: CreateInvoiceRequest) => api.post<Invoice>(INVOICES_PATH, body),
  payments: (invoiceId: number | string) =>
    fetchAll<Payment>(PAYMENTS_PATH, 'paidAt', { invoiceId }, { allowPartial: false }),
  clients: () =>
    fetchAll<Option>(CLIENTS_PATH, 'name').then((all) => all.map((c): Option => ({ id: c.id, name: c.name }))),
  stores: () =>
    fetchAll<Option>(STORES_PATH, 'name').then((all) => all.map((s): Option => ({ id: s.id, name: s.name }))),
  items: () =>
    fetchAll<{ id: number; name: string; batchNo: string | null; unitPrice: number | null; gstRate: number | null; quantity: number }>(
      INVENTORY_PATH,
      'name',
    ).then((all) =>
        all.map(
          (i): ItemOption => ({
            id: i.id,
            name: i.batchNo ? `${i.name} (${i.batchNo})` : i.name,
            itemName: i.name,
            unitPrice: i.unitPrice ?? null,
            gstRate: i.gstRate ?? null,
            quantity: i.quantity,
          }),
        ),
      ),
};
