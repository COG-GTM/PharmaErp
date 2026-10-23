import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { invoicesApi, type Invoice, type Payment } from './api';
import { GstBreakdown } from './GstBreakdown';
import { errorMessage } from './forms';
import { formatInr, summarizeAmounts } from './gst';

/** Printable e-receipt for an invoice (use case "E-receipt"). */
export function InvoiceReceipt() {
  const { id = '' } = useParams();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    invoicesApi
      .get(id)
      .then((inv) => active && setInvoice(inv))
      .catch((err: unknown) => active && setError(errorMessage(err)));
    // Payments are optional on the receipt (the payments module may not be deployed).
    invoicesApi
      .payments(id)
      .then((p) => active && setPayments(p))
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [id]);

  if (error) {
    return (
      <section className="card">
        <p className="error" role="alert">
          {error}
        </p>
        <Link to="..">Back to invoices</Link>
      </section>
    );
  }
  if (!invoice) return <p>Loading e-receipt…</p>;

  const summary = summarizeAmounts(
    invoice.lines.map((l) => ({ gstRate: l.gstRate, taxable: l.lineTotal - l.gstAmount, gst: l.gstAmount })),
  );
  const balance = Math.round((invoice.totalAmount - invoice.paidAmount) * 100) / 100;

  return (
    <section className="card receipt" aria-label="E-receipt">
      <header className="module-header">
        <h2>E-receipt · {invoice.invoiceNumber}</h2>
        <div className="toolbar no-print">
          <button type="button" onClick={() => window.print()}>
            Print
          </button>
          <Link to=".." className="button-link button-secondary">
            Back to invoices
          </Link>
        </div>
      </header>
      <div className="receipt-meta">
        <div>
          <strong>Date</strong>
          <div>{invoice.invoiceDate}</div>
        </div>
        <div>
          <strong>Billed to</strong>
          <div>{invoice.clientName ?? (invoice.clientId ? `Client #${invoice.clientId}` : 'Walk-in customer')}</div>
        </div>
        <div>
          <strong>Store</strong>
          <div>{invoice.storeName ?? (invoice.storeId ? `Store #${invoice.storeId}` : '—')}</div>
        </div>
        <div>
          <strong>Status</strong>
          <div>
            <span className="badge badge-muted">{invoice.status}</span>
          </div>
        </div>
      </div>
      {invoice.invoiceDescription && <p>{invoice.invoiceDescription}</p>}
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Description</th>
            <th className="num">Qty</th>
            <th className="num">Unit price</th>
            <th className="num">GST %</th>
            <th className="num">GST</th>
            <th className="num">Amount</th>
          </tr>
        </thead>
        <tbody>
          {invoice.lines.map((l, i) => (
            <tr key={l.id ?? i}>
              <td>{i + 1}</td>
              <td>{l.description}</td>
              <td className="num">{l.quantity}</td>
              <td className="num">{formatInr(l.unitPrice)}</td>
              <td className="num">{l.gstRate}%</td>
              <td className="num">{formatInr(l.gstAmount)}</td>
              <td className="num">{formatInr(l.lineTotal)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <GstBreakdown summary={summary} />
      <dl className="totals">
        <dt>Paid</dt>
        <dd data-testid="receipt-paid">{formatInr(invoice.paidAmount)}</dd>
        <dt>Balance due</dt>
        <dd data-testid="receipt-balance">
          <strong>{formatInr(balance)}</strong>
        </dd>
      </dl>
      {payments.length > 0 && (
        <>
          <h3>Payments</h3>
          <table>
            <thead>
              <tr>
                <th>Receipt no.</th>
                <th>Paid at</th>
                <th>Method</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id}>
                  <td>{p.receiptNumber}</td>
                  <td>{p.paidAt}</td>
                  <td>{p.paymentType}</td>
                  <td className="num">{formatInr(p.paymentAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}
