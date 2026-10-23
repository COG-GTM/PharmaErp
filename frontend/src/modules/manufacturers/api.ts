import { api, type PageResponse } from '../../api/client';

/** REST contract of the manufacturers backend (T06). Adjust here if the backend differs. */
export const MANUFACTURERS_PATH = '/api/manufacturers';

export interface Manufacturer {
  id: number;
  name: string;
  address: string | null;
  phone: string | null;
  email: string | null;
  city: string | null;
  district: string | null;
  state: string | null;
  gstin: string | null;
}

export type ManufacturerRequest = Omit<Manufacturer, 'id'>;

export interface ManufacturerQuery {
  q?: string;
  page?: number;
  size?: number;
}

export const manufacturersApi = {
  list: ({ q, page = 0, size = 20 }: ManufacturerQuery = {}) =>
    api.get<PageResponse<Manufacturer>>(MANUFACTURERS_PATH, { query: { q: q || undefined, page, size, sort: 'name' } }),
  get: (id: number | string) => api.get<Manufacturer>(`${MANUFACTURERS_PATH}/${id}`),
  create: (body: ManufacturerRequest) => api.post<Manufacturer>(MANUFACTURERS_PATH, body),
  update: (id: number | string, body: ManufacturerRequest) => api.put<Manufacturer>(`${MANUFACTURERS_PATH}/${id}`, body),
};
