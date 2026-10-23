import { api, type PageResponse } from '../../api/client';
import { fetchAll } from './lookup';

/** REST contract of the finance backend (T08). Adjust here if the backend differs. */
export const INVOICES_PATH = '/api/invoices';
/** Payments (T09) shown on the e-receipt. */
export const PAYMENTS_PATH = '/api/payments';
/** Lookups for the create screen (T05 / T04 / T07). */
export const CLIENTS_PATH = '/api/clients';
export const STORES_PATH = '/api/stores';
export const INVENTORY_PATH = '/api/inventory-items';

export interface InvoiceLine {
  id?: number;
  itemId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  /** Percent. */
  gstRate: number;
  gstAmount: number;
  lineTotal: number;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  clientId: number | null;
  clientName?: string | null;
  storeId: number | null;
  storeName?: string | null;
  status: string;
  invoiceDate: string;
  invoiceDescription: string | null;
  subtotal: number;
  gstAmount: number;
  totalAmount: number;
  paidAmount: number;
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
  invoiceDescription: string | null;
  lines: InvoiceLineRequest[];
}

export interface Payment {
  id: number;
  receiptNumber: string | null;
  paymentAmount: number;
  paymentType: string | null;
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
    api.get<PageResponse<Invoice>>(INVOICES_PATH, { query: { page, size, sort: 'invoiceDate,desc' } }),
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
            unitPrice: i.unitPrice,
            gstRate: i.gstRate,
            quantity: i.quantity,
          }),
        ),
      ),
};
