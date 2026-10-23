import { api, type PageResponse } from '../../api/client';

/** REST contract of the staff backend (T03, StaffRequest/StaffResponse). Adjust here if the backend differs. */
export const STAFF_PATH = '/api/staff';
export const LOGIN_TYPES_PATH = '/api/login-types';
/** Stores (T04) used for the store picker. */
export const STORES_PATH = '/api/stores';

export const STAFF_TYPES = ['INTERNAL', 'SUPPLIER', 'DISTRIBUTOR'] as const;
export type StaffType = (typeof STAFF_TYPES)[number];

export interface LoginType {
  id: number;
  name: string;
}

interface StaffFields {
  firstName: string;
  middleName: string | null;
  lastName: string | null;
  dateOfBirth: string | null;
  joinDate: string | null;
  experience: string | null;
  salary: number | null;
  homeTown: string | null;
  homeTownAddress: string | null;
  jobAddress: string | null;
  leavingAddress: string | null;
  mobileNumber: string | null;
  email: string | null;
  lastCompany: string | null;
  resumePath: string | null;
  feedback: string | null;
  suggestion: string | null;
  username: string | null;
  storeId: number | null;
  staffType: StaffType | null;
}

export interface Staff extends StaffFields {
  id: number;
  fullName: string;
  loginType: LoginType | null;
  active: boolean;
}

export interface StaffRequest extends StaffFields {
  loginTypeId: number | null;
  active: boolean;
}

export interface StoreOption {
  id: number;
  name: string;
}

const ALL = { page: 0, size: 200 };

export const staffApi = {
  list: ({ q, page = 0, size = 20 }: { q?: string; page?: number; size?: number } = {}) =>
    api.get<PageResponse<Staff>>(STAFF_PATH, { query: { q: q || undefined, page, size, sort: 'firstName' } }),
  get: (id: number | string) => api.get<Staff>(`${STAFF_PATH}/${id}`),
  create: (body: StaffRequest) => api.post<Staff>(STAFF_PATH, body),
  update: (id: number | string, body: StaffRequest) => api.put<Staff>(`${STAFF_PATH}/${id}`, body),
  loginTypes: () => api.get<PageResponse<LoginType>>(LOGIN_TYPES_PATH, { query: { ...ALL, sort: 'name' } }).then((p) => p.content),
  stores: () =>
    api
      .get<PageResponse<StoreOption>>(STORES_PATH, { query: { ...ALL, sort: 'name' } })
      .then((p) => p.content.map((s): StoreOption => ({ id: s.id, name: s.name }))),
};

export function fullName(s: Pick<Staff, 'firstName' | 'middleName' | 'lastName'> & { fullName?: string }): string {
  return s.fullName || [s.firstName, s.middleName, s.lastName].filter(Boolean).join(' ');
}
