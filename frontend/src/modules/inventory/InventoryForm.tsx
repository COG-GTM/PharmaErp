import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ADJUSTMENT_REASONS,
  PRESERVED_ON_UPDATE,
  inventoryApi,
  type AdjustmentReason,
  type InventoryItem,
  type InventoryItemRequest,
  type InventoryItemUpdateRequest,
  type Option,
} from './api';
import { FormField, SpecFields } from './FormField';
import {
  blankToNull,
  emptyValues,
  errorMessage,
  fieldErrorsOf,
  numberOrNull,
  toFormValues,
  validate,
  type FieldErrors,
  type FieldSpec,
  type FormValues,
} from './forms';

const MONEY = /^\d+(\.\d{1,2})?$/;

const ITEM_FIELDS: FieldSpec[] = [
  { name: 'name', label: 'Item name', required: true, maxLength: 255 },
  { name: 'batchNo', label: 'Batch no.', maxLength: 64 },
  { name: 'expiryDate', label: 'Expiry date', type: 'date' },
  { name: 'quantity', label: 'Quantity', type: 'number', required: true, pattern: /^\d+$/, patternMessage: 'Quantity must be a whole number ≥ 0' },
  { name: 'unitPrice', label: 'Unit price (₹)', type: 'number', pattern: MONEY, patternMessage: 'Unit price must be a positive amount with up to 2 decimals' },
  {
    name: 'gstRate',
    label: 'GST rate (%)',
    type: 'number',
    pattern: /^(100(\.0{1,2})?|\d{1,2}(\.\d{1,2})?)$/,
    patternMessage: 'GST rate must be between 0 and 100',
  },
  { name: 'hsnCode', label: 'HSN code', maxLength: 16 },
  { name: 'barcode', label: 'Barcode', maxLength: 64 },
  { name: 'description', label: 'Description', type: 'textarea', maxLength: 1000 },
];

/** Quantity is set on create only; afterwards stock changes are recorded as adjustments. */
const EDIT_FIELDS = ITEM_FIELDS.filter((f) => f.name !== 'quantity');

const PICKERS = [
  { name: 'manufacturerId', label: 'Manufacturer' },
  { name: 'storeId', label: 'Store' },
] as const;

function toRequest(v: FormValues): InventoryItemRequest {
  return {
    name: (v.name ?? '').trim(),
    description: blankToNull(v.description),
    batchNo: blankToNull(v.batchNo),
    expiryDate: blankToNull(v.expiryDate),
    quantity: numberOrNull(v.quantity) ?? 0,
    unitPrice: numberOrNull(v.unitPrice),
    gstRate: numberOrNull(v.gstRate),
    hsnCode: blankToNull(v.hsnCode),
    barcode: blankToNull(v.barcode),
    manufacturerId: numberOrNull(v.manufacturerId),
    storeId: numberOrNull(v.storeId),
  };
}

function toUpdateRequest(v: FormValues, loaded: InventoryItem): InventoryItemUpdateRequest {
  const edited: Partial<InventoryItemRequest> = toRequest(v);
  delete edited.quantity;
  const preserved = Object.fromEntries(PRESERVED_ON_UPDATE.map((k) => [k, loaded[k] ?? null]));
  return { ...preserved, ...edited } as InventoryItemUpdateRequest;
}

function StockAdjustment({ item, onAdjusted }: { item: InventoryItem; onAdjusted: (item: InventoryItem) => void }) {
  const [change, setChange] = useState('');
  const [reason, setReason] = useState<AdjustmentReason>('RECEIPT');
  const [note, setNote] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!/^-?\d+$/.test(change.trim()) || Number(change) === 0) {
      setErrors({ quantityChange: ['Enter a non-zero whole number (negative to remove stock)'] });
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const res = await inventoryApi.adjust(item.id, { quantityChange: Number(change), reason, note: blankToNull(note) });
      onAdjusted(res.item);
      setChange('');
      setNote('');
    } catch (err) {
      setErrors(fieldErrorsOf(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={onSubmit} noValidate aria-label="Adjust stock">
      <h3>Stock</h3>
      <p>
        Quantity on hand: <strong data-testid="quantity-on-hand">{item.quantity}</strong>
      </p>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <div className="form-grid">
        <FormField name="quantityChange" label="Quantity change" errors={errors}>
          <input
            id="field-quantityChange"
            type="number"
            value={change}
            onChange={(e) => setChange(e.target.value)}
            aria-invalid={errors.quantityChange ? true : undefined}
          />
        </FormField>
        <FormField name="reason" label="Reason" errors={errors}>
          <select id="field-reason" value={reason} onChange={(e) => setReason(e.target.value as AdjustmentReason)}>
            {ADJUSTMENT_REASONS.map((r) => (
              <option key={r} value={r}>
                {r.replace(/_/g, ' ').toLowerCase()}
              </option>
            ))}
          </select>
        </FormField>
        <FormField name="note" label="Note" errors={errors}>
          <input id="field-note" value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} />
        </FormField>
      </div>
      <div className="form-actions">
        <button type="submit" disabled={saving}>
          {saving ? 'Recording…' : 'Record adjustment'}
        </button>
      </div>
    </form>
  );
}

export function InventoryForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(() => ({ ...emptyValues(ITEM_FIELDS), quantity: '0', manufacturerId: '', storeId: '' }));
  const [options, setOptions] = useState<Record<'manufacturerId' | 'storeId', Option[]>>({ manufacturerId: [], storeId: [] });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(Boolean(id));
  const [loaded, setLoaded] = useState<InventoryItem | null>(null);
  const fields = id ? EDIT_FIELDS : ITEM_FIELDS;
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    // Optional lookups: the item can still be saved without them.
    inventoryApi
      .manufacturers()
      .then((m) => active && setOptions((o) => ({ ...o, manufacturerId: m })))
      .catch(() => undefined);
    inventoryApi
      .stores()
      .then((s) => active && setOptions((o) => ({ ...o, storeId: s })))
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!id) return;
    let active = true;
    inventoryApi
      .get(id)
      .then(
        (item) => {
          if (!active) return;
          setLoaded(item);
          setValues({
            ...toFormValues(item, EDIT_FIELDS),
            manufacturerId: item.manufacturerId ? String(item.manufacturerId) : '',
            storeId: item.storeId ? String(item.storeId) : '',
          });
        },
      )
      .catch((err: unknown) => active && setError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id]);

  const set = (name: string, value: string) => setValues((v) => ({ ...v, [name]: value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const clientErrors = validate(values, fields);
    setErrors(clientErrors);
    setError(null);
    if (Object.keys(clientErrors).length) return;
    setSaving(true);
    try {
      if (id && loaded) await inventoryApi.update(id, toUpdateRequest(values, loaded));
      else await inventoryApi.create(toRequest(values));
      navigate('..');
    } catch (err) {
      setErrors(fieldErrorsOf(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card">
      <h2>{id ? 'Edit inventory item' : 'New inventory item'}</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading item…</p>
      ) : (
        <form onSubmit={onSubmit} noValidate aria-label={id ? 'Edit inventory item' : 'New inventory item'}>
          <div className="form-grid">
            <SpecFields specs={fields} values={values} errors={errors} onChange={set} />
            {PICKERS.map(({ name, label }) => {
              const current = values[name] ?? '';
              const known = current === '' || options[name].some((o) => String(o.id) === current);
              return (
                <FormField key={name} name={name} label={label} errors={errors}>
                  <select id={`field-${name}`} value={current} onChange={(e) => set(name, e.target.value)}>
                    <option value="">— None —</option>
                    {!known && <option value={current}>#{current}</option>}
                    {options[name].map((o) => (
                      <option key={o.id} value={String(o.id)}>
                        {o.name}
                      </option>
                    ))}
                  </select>
                </FormField>
              );
            })}
          </div>
          <div className="form-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save item'}
            </button>
            <Link to=".." className="button-link button-secondary">
              Cancel
            </Link>
          </div>
        </form>
      )}
      {loaded && <StockAdjustment item={loaded} onAdjusted={setLoaded} />}
    </section>
  );
}
