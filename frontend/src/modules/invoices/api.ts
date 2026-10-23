import { api, type PageResponse } from '../../api/client';

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

const ALL = { page: 0, size: 200 };

export const invoicesApi = {
  list: ({ page = 0, size = 20 }: { page?: number; size?: number } = {}) =>
    api.get<PageResponse<Invoice>>(INVOICES_PATH, { query: { page, size, sort: 'invoiceDate,desc' } }),
  get: (id: number | string) => api.get<Invoice>(`${INVOICES_PATH}/${id}`),
  create: (body: CreateInvoiceRequest) => api.post<Invoice>(INVOICES_PATH, body),
  payments: (invoiceId: number | string) =>
    api.get<PageResponse<Payment>>(PAYMENTS_PATH, { query: { invoiceId, ...ALL } }).then((p) => p.content),
  clients: () =>
    api
      .get<PageResponse<Option>>(CLIENTS_PATH, { query: { ...ALL, sort: 'name' } })
      .then((p) => p.content.map((c): Option => ({ id: c.id, name: c.name }))),
  stores: () =>
    api
      .get<PageResponse<Option>>(STORES_PATH, { query: { ...ALL, sort: 'name' } })
      .then((p) => p.content.map((s): Option => ({ id: s.id, name: s.name }))),
  items: () =>
    api
      .get<PageResponse<{ id: number; name: string; batchNo: string | null; unitPrice: number | null; gstRate: number | null; quantity: number }>>(
        INVENTORY_PATH,
        { query: { ...ALL, sort: 'name' } },
      )
      .then((p) =>
        p.content.map(
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
