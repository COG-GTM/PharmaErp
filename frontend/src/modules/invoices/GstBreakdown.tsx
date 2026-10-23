import { formatInr, type GstSummary } from './gst';

export function GstBreakdown({ summary, title = 'GST breakdown' }: { summary: GstSummary; title?: string }) {
  return (
    <section aria-label={title} className="gst-breakdown">
      <h3>{title}</h3>
      <table>
        <thead>
          <tr>
            <th className="num">GST rate</th>
            <th className="num">Taxable value</th>
            <th className="num">CGST</th>
            <th className="num">SGST</th>
            <th className="num">Total GST</th>
          </tr>
        </thead>
        <tbody>
          {summary.rates.map((r) => (
            <tr key={r.rate}>
              <td className="num">{r.rate}%</td>
              <td className="num">{formatInr(r.taxable)}</td>
              <td className="num">{formatInr(r.cgst)}</td>
              <td className="num">{formatInr(r.sgst)}</td>
              <td className="num">{formatInr(r.gst)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <dl className="totals">
        <dt>Subtotal</dt>
        <dd data-testid="summary-subtotal">{formatInr(summary.subtotal)}</dd>
        <dt>CGST</dt>
        <dd data-testid="summary-cgst">{formatInr(summary.cgst)}</dd>
        <dt>SGST</dt>
        <dd data-testid="summary-sgst">{formatInr(summary.sgst)}</dd>
        <dt>Total GST</dt>
        <dd data-testid="summary-gst">{formatInr(summary.gst)}</dd>
        <dt>Grand total</dt>
        <dd data-testid="summary-total">
          <strong>{formatInr(summary.total)}</strong>
        </dd>
      </dl>
    </section>
  );
}
