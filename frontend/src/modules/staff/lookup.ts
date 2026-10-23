import { api, type PageResponse } from '../../api/client';

const LOOKUP_PAGE_SIZE = 200;

/** Fetches every page of a lookup endpoint so pickers are not capped at one page. */
export async function fetchAll<T>(path: string, sort: string, query: Record<string, string | number> = {}): Promise<T[]> {
  const out: T[] = [];
  for (let page = 0; ; page++) {
    const res = await api.get<PageResponse<T>>(path, { query: { ...query, page, size: LOOKUP_PAGE_SIZE, sort } });
    out.push(...res.content);
    if (res.content.length === 0 || page + 1 >= res.totalPages) return out;
  }
}
