import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { invoicesApi, type CreateInvoiceRequest, type ItemOption, type Option } from './api';
import { FormField } from './FormField';
import { GstBreakdown } from './GstBreakdown';
import { blankToNull, errorMessage, fieldErrorsOf, numberOrNull, type FieldErrors } from './forms';
import { formatInr, lineAmounts, summarize, type LineInput } from './gst';

interface DraftLine {
  key: number;
  itemId: string;
  description: string;
  quantity: string;
  unitPrice: string;
  gstRate: string;
}

const GST_SLABS = ['0', '5', '12', '18', '28'];

let nextKey = 1;
const blankLine = (): DraftLine => ({ key: nextKey++, itemId: '', description: '', quantity: '1', unitPrice: '', gstRate: '12' });

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function toInput(line: DraftLine): LineInput {
  return {
    quantity: Math.max(0, Math.trunc(numberOrNull(line.quantity) ?? 0)),
    unitPrice: Math.max(0, numberOrNull(line.unitPrice) ?? 0),
    gstRate: Math.max(0, numberOrNull(line.gstRate) ?? 0),
  };
}

function validateLines(lines: DraftLine[]): FieldErrors {
  const errors: FieldErrors = {};
  if (lines.length === 0) errors.lines = ['Add at least one line'];
  lines.forEach((l, i) => {
    const msgs: string[] = [];
    if (!l.description.trim()) msgs.push('description is required');
    if (!/^[1-9]\d*$/.test(l.quantity.trim())) msgs.push('quantity must be a whole number ≥ 1');
    if (!/^\d+(\.\d{1,2})?$/.test(l.unitPrice.trim())) msgs.push('unit price must be an amount with up to 2 decimals');
    const rate = Number(l.gstRate);
    if (l.gstRate.trim() === '' || !Number.isFinite(rate) || rate < 0 || rate > 100) msgs.push('GST rate must be between 0 and 100');
    if (msgs.length) errors[`lines[${i}]`] = msgs.map((m) => `Line ${i + 1}: ${m}`);
  });
  return errors;
}

export function InvoiceCreate() {
  const navigate = useNavigate();
  const [clients, setClients] = useState<Option[]>([]);
  const [stores, setStores] = useState<Option[]>([]);
  const [items, setItems] = useState<ItemOption[]>([]);
  const [clientId, setClientId] = useState('');
  const [storeId, setStoreId] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(todayIso);
  const [description, setDescription] = useState('');
  const [lines, setLines] = useState<DraftLine[]>(() => [blankLine()]);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    // Lookups are best-effort; free-text lines still work without inventory.
    invoicesApi.clients().then((c) => active && setClients(c)).catch(() => undefined);
    invoicesApi.stores().then((s) => active && setStores(s)).catch(() => undefined);
    invoicesApi.items().then((i) => active && setItems(i)).catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  const summary = useMemo(() => summarize(lines.map(toInput)), [lines]);

  const updateLine = (key: number, patch: Partial<DraftLine>) =>
    setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));

  const pickItem = (key: number, itemId: string) => {
    const item = items.find((i) => String(i.id) === itemId);
    updateLine(key, {
      itemId,
      ...(item && {
        description: item.itemName,
        unitPrice: item.unitPrice !== null ? String(item.unitPrice) : '',
        gstRate: item.gstRate !== null ? String(item.gstRate) : '0',
      }),
    });
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next = validateLines(lines);
    if (!invoiceDate) next.invoiceDate = ['Invoice date is required'];
    setErrors(next);
    setError(null);
    if (Object.keys(next).length) return;
    const body: CreateInvoiceRequest = {
      clientId: numberOrNull(clientId),
      storeId: numberOrNull(storeId),
      invoiceDate,
      description: blankToNull(description),
      lines: lines.map((l) => ({ itemId: numberOrNull(l.itemId), description: l.description.trim(), ...toInput(l) })),
    };
    setSaving(true);
    try {
      const created = await invoicesApi.create(body);
      navigate(`../${created.id}`);
    } catch (err) {
      setErrors(fieldErrorsOf(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const lineErrors = Object.entries(errors).filter(([k]) => k.startsWith('lines'));

  return (
    <section className="card">
      <h2>New invoice</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate aria-label="New invoice">
        <div className="form-grid">
          <FormField name="clientId" label="Client" errors={errors}>
            <select id="field-clientId" value={clientId} onChange={(e) => setClientId(e.target.value)}>
              <option value="">Walk-in customer</option>
              {clients.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField name="storeId" label="Store" errors={errors}>
            <select id="field-storeId" value={storeId} onChange={(e) => setStoreId(e.target.value)}>
              <option value="">— None —</option>
              {stores.map((s) => (
                <option key={s.id} value={String(s.id)}>
                  {s.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField name="invoiceDate" label="Invoice date" errors={errors}>
            <input id="field-invoiceDate" type="date" value={invoiceDate} onChange={(e) => setInvoiceDate(e.target.value)} />
          </FormField>
          <FormField name="description" label="Description" errors={errors}>
            <input id="field-description" maxLength={1000} value={description} onChange={(e) => setDescription(e.target.value)} />
          </FormField>
        </div>

        <h3>Lines</h3>
        <table className="line-table">
          <thead>
            <tr>
              <th>Inventory item</th>
              <th>Description</th>
              <th className="num">Qty</th>
              <th className="num">Unit price (₹)</th>
              <th className="num">GST %</th>
              <th className="num">Taxable</th>
              <th className="num">GST</th>
              <th className="num">Line total</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {lines.map((l, i) => {
              const amounts = lineAmounts(toInput(l));
              const n = i + 1;
              return (
                <tr key={l.key} data-testid={`line-${n}`}>
                  <td>
                    <select aria-label={`Line ${n} item`} value={l.itemId} onChange={(e) => pickItem(l.key, e.target.value)}>
                      <option value="">— Free text —</option>
                      {items.map((it) => (
                        <option key={it.id} value={String(it.id)}>
                          {it.name}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input aria-label={`Line ${n} description`} value={l.description} onChange={(e) => updateLine(l.key, { description: e.target.value })} />
                  </td>
                  <td>
                    <input aria-label={`Line ${n} quantity`} type="number" min={1} step={1} value={l.quantity} onChange={(e) => updateLine(l.key, { quantity: e.target.value })} />
                  </td>
                  <td>
                    <input aria-label={`Line ${n} unit price`} type="number" min={0} step="0.01" value={l.unitPrice} onChange={(e) => updateLine(l.key, { unitPrice: e.target.value })} />
                  </td>
                  <td>
                    <input
                      aria-label={`Line ${n} GST rate`}
                      list="gst-slabs"
                      type="number"
                      min={0}
                      max={100}
                      step="0.01"
                      value={l.gstRate}
                      onChange={(e) => updateLine(l.key, { gstRate: e.target.value })}
                    />
                  </td>
                  <td className="num">{formatInr(amounts.taxable)}</td>
                  <td className="num">{formatInr(amounts.gst)}</td>
                  <td className="num" data-testid={`line-${n}-total`}>
                    {formatInr(amounts.total)}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="button-secondary"
                      aria-label={`Remove line ${n}`}
                      disabled={lines.length === 1}
                      onClick={() => setLines((ls) => ls.filter((x) => x.key !== l.key))}
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        <datalist id="gst-slabs">
          {GST_SLABS.map((r) => (
            <option key={r} value={r} />
          ))}
        </datalist>
        {lineErrors.map(([k, msgs]) =>
          msgs.map((m) => (
            <p key={`${k}-${m}`} className="field-error">
              {m}
            </p>
          )),
        )}
        <div className="toolbar">
          <button type="button" className="button-secondary" onClick={() => setLines((ls) => [...ls, blankLine()])}>
            Add line
          </button>
        </div>

        <GstBreakdown summary={summary} title="Live GST breakdown" />

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? 'Creating…' : 'Create invoice'}
          </button>
          <Link to=".." className="button-link button-secondary">
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}
