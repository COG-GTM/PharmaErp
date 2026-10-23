import { api, type PageResponse } from '../../api/client';

/** REST contract of the clients backend (T05). Adjust here if the backend differs. */
export const CLIENTS_PATH = '/api/clients';

export interface Client {
  id: number;
  name: string;
  address: string | null;
  phone: string | null;
  email: string | null;
  city: string | null;
  district: string | null;
  state: string | null;
  gstin: string | null;
  feedbackEmail: string | null;
}

export type ClientRequest = Omit<Client, 'id'>;

export interface ClientQuery {
  q?: string;
  page?: number;
  size?: number;
}

export const clientsApi = {
  list: ({ q, page = 0, size = 20 }: ClientQuery = {}) =>
    api.get<PageResponse<Client>>(CLIENTS_PATH, { query: { q: q || undefined, page, size, sort: 'name' } }),
  get: (id: number | string) => api.get<Client>(`${CLIENTS_PATH}/${id}`),
  create: (body: ClientRequest) => api.post<Client>(CLIENTS_PATH, body),
  update: (id: number | string, body: ClientRequest) => api.put<Client>(`${CLIENTS_PATH}/${id}`, body),
};
