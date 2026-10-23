import { api, type PageResponse } from '../../api/client';

const LOOKUP_PAGE_SIZE = 200;

/**
 * Fetches every page of a lookup endpoint so pickers are not capped at one page.
 * By default only a failing first page rejects and a later failure keeps the pages already
 * loaded (fine for best-effort pickers); pass `allowPartial: false` when completeness matters.
 */
export async function fetchAll<T>(
  path: string,
  sort: string,
  query: Record<string, string | number> = {},
  { allowPartial = true }: { allowPartial?: boolean } = {},
): Promise<T[]> {
  const out: T[] = [];
  for (let page = 0; ; page++) {
    let res: PageResponse<T>;
    try {
      res = await api.get<PageResponse<T>>(path, { query: { ...query, page, size: LOOKUP_PAGE_SIZE, sort } });
    } catch (err) {
      if (page === 0 || !allowPartial) throw err;
      return out;
    }
    out.push(...res.content);
    if (res.content.length === 0 || page + 1 >= res.totalPages) return out;
  }
}
