import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import type { PageResponse } from '../../api/client';
import { EXPIRY_WINDOW_DAYS, LOW_STOCK_THRESHOLD, inventoryApi, type InventoryFilter, type InventoryItem, type Option } from './api';
import { errorMessage } from './forms';
import { applyFilter, stockBadges } from './stock';

const FILTERS: { value: InventoryFilter; label: string }[] = [
  { value: 'all', label: 'All items' },
  { value: 'expiring', label: `Expiring soon (≤ ${EXPIRY_WINDOW_DAYS} days)` },
  { value: 'low-stock', label: `Low stock (≤ ${LOW_STOCK_THRESHOLD})` },
];

function parseFilter(raw: string | null): InventoryFilter {
  return raw === 'expiring' || raw === 'low-stock' ? raw : 'all';
}

export function InventoryList() {
  const [params, setParams] = useSearchParams();
  const filter = parseFilter(params.get('filter'));
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<InventoryItem> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [manufacturers, setManufacturers] = useState<Option[]>([]);
  const [stores, setStores] = useState<Option[]>([]);

  useEffect(() => {
    let active = true;
    // Name lookups are best-effort; the table falls back to ids.
    inventoryApi.manufacturers().then((m) => active && setManufacturers(m)).catch(() => undefined);
    inventoryApi.stores().then((s) => active && setStores(s)).catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  const nameOf = (options: Option[], id: number | null, fallback?: string | null) =>
    id === null ? '' : (fallback ?? options.find((o) => o.id === id)?.name ?? `#${id}`);

  useEffect(() => {
    let active = true;
    setError(null);
    inventoryApi
      .list({ q: search, filter, page })
      .then((res) => active && setData(res))
      .catch((err: unknown) => {
        if (!active) return;
        setData(null);
        setError(errorMessage(err));
      });
    return () => {
      active = false;
    };
  }, [search, filter, page]);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearch(query.trim());
  };

  const selectFilter = (next: InventoryFilter) => {
    setPage(0);
    setParams(next === 'all' ? {} : { filter: next }, { replace: true });
  };

  // Client-side filtering is a safety net for a backend that ignores the filter params.
  const rows = data ? applyFilter(data.content, filter) : [];
  const partiallyFiltered = !!data && data.totalPages > 1 && rows.length < data.content.length;

  return (
    <section className="card">
      <header className="module-header">
        <h2>Inventory</h2>
        <Link to="new" className="button-link">
          New item
        </Link>
      </header>
      <div className="toolbar" role="group" aria-label="Inventory filters">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            className={f.value === filter ? undefined : 'button-secondary'}
            aria-pressed={f.value === filter}
            onClick={() => selectFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>
      <form role="search" onSubmit={onSearch} className="toolbar">
        <input
          aria-label="Search inventory"
          placeholder="Search by name, batch or barcode"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!data && !error && <p>Loading inventory…</p>}
      {partiallyFiltered && (
        <p className="muted" data-testid="partial-filter">
          Some items on this page don&apos;t match the filter and are hidden; other pages may still have matches.
        </p>
      )}
      {data && rows.length === 0 && <p>No items match on this page.</p>}
      {rows.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Item</th>
              <th>Batch</th>
              <th>Expiry</th>
              <th className="num">Qty</th>
              <th className="num">Unit price</th>
              <th className="num">GST %</th>
              <th>Manufacturer</th>
              <th>Store</th>
              <th>Alerts</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => (
              <tr key={item.id} data-testid={`item-${item.id}`}>
                <td>
                  {item.name}
                  {item.barcode && <div className="hint">{item.barcode}</div>}
                </td>
                <td>{item.batchNo}</td>
                <td>{item.expiryDate}</td>
                <td className="num">{item.quantity}</td>
                <td className="num">{item.unitPrice?.toFixed(2)}</td>
                <td className="num">{item.gstRate}</td>
                <td>{nameOf(manufacturers, item.manufacturerId)}</td>
                <td>{nameOf(stores, item.storeId)}</td>
                <td>
                  {stockBadges(item).map((b) => (
                    <span key={b.label} className={`badge badge-${b.tone}`}>
                      {b.label}
                    </span>
                  ))}
                </td>
                <td>
                  <Link to={`${item.id}/edit`} aria-label={`Edit ${item.name}`}>
                    Edit
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {data && data.totalPages > 1 && (
        <nav className="pager" aria-label="Pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span>
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
            Next
          </button>
        </nav>
      )}
    </section>
  );
}
