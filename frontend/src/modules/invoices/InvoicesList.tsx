import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { PageResponse } from '../../api/client';
import { invoicesApi, type Invoice } from './api';
import { errorMessage } from './forms';
import { formatInr } from './gst';

export function InvoicesList() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Invoice> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setError(null);
    invoicesApi
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
        <h2>Invoices</h2>
        <Link to="new" className="button-link">
          New invoice
        </Link>
      </header>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!data && !error && <p>Loading invoices…</p>}
      {data && data.content.length === 0 && <p>No invoices yet.</p>}
      {data && data.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Invoice</th>
              <th>Date</th>
              <th>Client</th>
              <th>Status</th>
              <th className="num">Subtotal</th>
              <th className="num">GST</th>
              <th className="num">Total</th>
              <th className="num">Paid</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {data.content.map((inv) => (
              <tr key={inv.id}>
                <td>{inv.invoiceNumber}</td>
                <td>{inv.invoiceDate}</td>
                <td>{inv.clientName ?? (inv.clientId ? `#${inv.clientId}` : 'Walk-in')}</td>
                <td>
                  <span className="badge badge-muted">{inv.status}</span>
                </td>
                <td className="num">{formatInr(inv.subtotal)}</td>
                <td className="num">{formatInr(inv.gstAmount)}</td>
                <td className="num">{formatInr(inv.totalAmount)}</td>
                <td className="num">{formatInr(inv.paidAmount)}</td>
                <td>
                  <Link to={String(inv.id)} aria-label={`View e-receipt ${inv.invoiceNumber}`}>
                    E-receipt
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
