import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { PageResponse } from '../../api/client';
import { clientsApi, type Client } from './api';
import { errorMessage } from './forms';

export function ClientsList() {
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Client> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setError(null);
    clientsApi
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
        <h2>Clients</h2>
        <Link to="new" className="button-link">
          New client
        </Link>
      </header>
      <form role="search" onSubmit={onSearch} className="toolbar">
        <input aria-label="Search clients" placeholder="Search by name" value={query} onChange={(e) => setQuery(e.target.value)} />
        <button type="submit">Search</button>
      </form>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!data && !error && <p>Loading clients…</p>}
      {data && data.content.length === 0 && <p>No clients found.</p>}
      {data && data.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Phone</th>
              <th>Email</th>
              <th>City</th>
              <th>GSTIN</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {data.content.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>{c.phone}</td>
                <td>{c.email}</td>
                <td>{c.city}</td>
                <td>{c.gstin}</td>
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
