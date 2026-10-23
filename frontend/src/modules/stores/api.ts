import { api, type PageResponse } from '../../api/client';

/** REST contract of the stores backend (T04, StoreRequest/StoreResponse). Adjust here if the backend differs. */
export const STORES_PATH = '/api/stores';
/** Staff list (T03) used to pick a store manager. */
export const STAFF_PATH = '/api/staff';

export interface Store {
  id: number;
  code: string;
  name: string;
  address: string | null;
  gstin: string | null;
  managerStaffId: number | null;
}

export type StoreRequest = Omit<Store, 'id'>;

export interface ManagerOption {
  id: number;
  fullName: string;
}

export const storesApi = {
  list: ({ q, page = 0, size = 20 }: { q?: string; page?: number; size?: number } = {}) =>
    api.get<PageResponse<Store>>(STORES_PATH, { query: { q: q || undefined, page, size, sort: 'name' } }),
  get: (id: number | string) => api.get<Store>(`${STORES_PATH}/${id}`),
  create: (body: StoreRequest) => api.post<Store>(STORES_PATH, body),
  update: (id: number | string, body: StoreRequest) => api.put<Store>(`${STORES_PATH}/${id}`, body),
  managers: () =>
    api
      .get<PageResponse<ManagerOption>>(STAFF_PATH, { query: { page: 0, size: 200, sort: 'firstName' } })
      .then((p) => p.content.map((s): ManagerOption => ({ id: s.id, fullName: s.fullName }))),
};
