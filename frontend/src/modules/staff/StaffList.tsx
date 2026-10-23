import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { PageResponse } from '../../api/client';
import { fullName, staffApi, type Staff, type StoreOption } from './api';
import { errorMessage } from './forms';

export function StaffList() {
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Staff> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [stores, setStores] = useState<StoreOption[]>([]);

  useEffect(() => {
    let active = true;
    // Store names are a best-effort lookup; the list falls back to store ids.
    staffApi
      .stores()
      .then((s) => active && setStores(s))
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  const storeName = (id: number | null | undefined) => (id == null ? '' : (stores.find((s) => s.id === id)?.name ?? `Store #${id}`));

  useEffect(() => {
    let active = true;
    setError(null);
    staffApi
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
        <h2>Staff</h2>
        <Link to="new" className="button-link">
          New staff member
        </Link>
      </header>
      <form role="search" onSubmit={onSearch} className="toolbar">
        <input aria-label="Search staff" placeholder="Search by name or email" value={query} onChange={(e) => setQuery(e.target.value)} />
        <button type="submit">Search</button>
      </form>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!data && !error && <p>Loading staff…</p>}
      {data && data.content.length === 0 && <p>No staff found.</p>}
      {data && data.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Login type</th>
              <th>Store</th>
              <th>Mobile</th>
              <th>Email</th>
              <th>Status</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {data.content.map((c) => (
              <tr key={c.id}>
                <td>{fullName(c)}</td>
                <td>{c.staffType}</td>
                <td>{c.loginType?.name ?? ''}</td>
                <td>{storeName(c.storeId)}</td>
                <td>{c.mobileNumber}</td>
                <td>{c.email}</td>
                <td>
                  <span className={c.active ? 'badge badge-muted' : 'badge badge-danger'}>{c.active ? 'Active' : 'Inactive'}</span>
                </td>
                <td>
                  <Link to={`${c.id}/edit`} aria-label={`Edit ${fullName(c)}`}>
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
