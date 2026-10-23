import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { inventoryApi, type InventoryItemRequest, type Option } from './api';
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

export function InventoryForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(() => ({ ...emptyValues(ITEM_FIELDS), quantity: '0', manufacturerId: '', storeId: '' }));
  const [options, setOptions] = useState<Record<'manufacturerId' | 'storeId', Option[]>>({ manufacturerId: [], storeId: [] });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(Boolean(id));
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
        (item) =>
          active &&
          setValues({
            ...toFormValues(item, ITEM_FIELDS),
            manufacturerId: item.manufacturerId ? String(item.manufacturerId) : '',
            storeId: item.storeId ? String(item.storeId) : '',
          }),
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
    const clientErrors = validate(values, ITEM_FIELDS);
    setErrors(clientErrors);
    setError(null);
    if (Object.keys(clientErrors).length) return;
    setSaving(true);
    try {
      const body = toRequest(values);
      if (id) await inventoryApi.update(id, body);
      else await inventoryApi.create(body);
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
            <SpecFields specs={ITEM_FIELDS} values={values} errors={errors} onChange={set} />
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
    </section>
  );
}
