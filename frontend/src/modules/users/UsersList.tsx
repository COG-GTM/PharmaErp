import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { PageResponse } from '../../api/client';
import { usersApi, type User } from './api';
import { errorMessage } from './forms';

export function UsersList() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<User> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setError(null);
    usersApi
      .list({ page })
      .then((res) => active && setData(res))
      .catch((err: unknown) => active && setError(errorMessage(err)));
    return () => {
      active = false;
    };
  }, [page]);

  return (
    <section className="card">
      <header className="module-header">
        <h2>Users</h2>
        <Link to="new" className="button-link">
          Register user
        </Link>
      </header>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!data && !error && <p>Loading users…</p>}
      {data && data.content.length === 0 && <p>No users registered.</p>}
      {data && data.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Username</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {data.content.map((u) => (
              <tr key={u.username}>
                <td>{u.username}</td>
                <td>
                  <Link to={`${encodeURIComponent(u.username)}/edit`} aria-label={`Change password for ${u.username}`}>
                    Change password
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
