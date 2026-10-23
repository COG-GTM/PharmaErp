import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { PageResponse } from '../../api/client';
import { storesApi, type ManagerOption, type Store } from './api';
import { errorMessage } from './forms';

export function StoresList() {
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Store> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [managers, setManagers] = useState<ManagerOption[]>([]);

  useEffect(() => {
    let active = true;
    // Manager names are a best-effort lookup; the list still renders staff ids without it.
    storesApi
      .managers()
      .then((m) => active && setManagers(m))
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  const managerName = (id: number | null | undefined) => (id == null ? '—' : (managers.find((m) => m.id === id)?.fullName ?? `Staff #${id}`));

  useEffect(() => {
    let active = true;
    setError(null);
    storesApi
      .list({ q: search, page })
      .then((res) => active && setData(res))
      .catch((err: unknown) => {
        if (!active) return;
        setData(null);
        setError(errorMessage(err));
      });
    return () => {
      active = false;
    };
  }, [search, page]);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearch(query.trim());
  };

  return (
    <section className="card">
      <header className="module-header">
        <h2>Stores</h2>
        <Link to="new" className="button-link">
          New store
        </Link>
      </header>
      <form role="search" onSubmit={onSearch} className="toolbar">
        <input aria-label="Search stores" placeholder="Search by code or name" value={query} onChange={(e) => setQuery(e.target.value)} />
        <button type="submit">Search</button>
      </form>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!data && !error && <p>Loading stores…</p>}
      {data && data.content.length === 0 && <p>No stores found.</p>}
      {data && data.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Address</th>
              <th>GSTIN</th>
              <th>Manager</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {data.content.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.name}</td>
                <td>{c.address}</td>
                <td>{c.gstin}</td>
                <td>{managerName(c.managerStaffId)}</td>
                <td>
                  <Link to={`${c.id}/edit`} aria-label={`Edit ${c.name}`}>
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
