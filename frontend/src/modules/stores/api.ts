import { api, type PageResponse } from '../../api/client';
import { fetchAll } from './lookup';

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
    fetchAll<ManagerOption>(STAFF_PATH, 'firstName').then((all) => all.map((s): ManagerOption => ({ id: s.id, fullName: s.fullName }))),
};
