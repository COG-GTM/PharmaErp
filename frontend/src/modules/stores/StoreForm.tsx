import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { storesApi, type ManagerOption, type StoreRequest } from './api';
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

const GSTIN = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/i;

const STORE_FIELDS: FieldSpec[] = [
  {
    name: 'code',
    label: 'Store code',
    required: true,
    maxLength: 20,
    pattern: /^[A-Z0-9][A-Z0-9-]{1,19}$/i,
    patternMessage: "Store code must be 2-20 letters, digits or '-', starting with a letter or digit",
  },
  { name: 'name', label: 'Store name', required: true, maxLength: 255 },
  { name: 'gstin', label: 'GSTIN', maxLength: 15, pattern: GSTIN, patternMessage: 'GSTIN must be 15 characters, e.g. 27AABCP1234F1Z5' },
  { name: 'address', label: 'Address', type: 'textarea', maxLength: 500 },
];

function toRequest(values: FormValues): StoreRequest {
  return {
    code: (values.code ?? '').trim().toUpperCase(),
    name: (values.name ?? '').trim(),
    address: blankToNull(values.address),
    gstin: blankToNull(values.gstin)?.toUpperCase() ?? null,
    managerStaffId: numberOrNull(values.managerStaffId),
  };
}

export function StoreForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(() => ({ ...emptyValues(STORE_FIELDS), managerStaffId: '' }));
  const [managers, setManagers] = useState<ManagerOption[]>([]);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(Boolean(id));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    // The manager picker is optional: a failing staff endpoint must not block store editing.
    storesApi
      .managers()
      .then((m) => active && setManagers(m))
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!id) return;
    let active = true;
    storesApi
      .get(id)
      .then(
        (s) =>
          active &&
          setValues({ ...toFormValues(s, STORE_FIELDS), managerStaffId: s.managerStaffId ? String(s.managerStaffId) : '' }),
      )
      .catch((err: unknown) => active && setError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const clientErrors = validate(values, STORE_FIELDS);
    setErrors(clientErrors);
    setError(null);
    if (Object.keys(clientErrors).length) return;
    setSaving(true);
    try {
      const body = toRequest(values);
      if (id) await storesApi.update(id, body);
      else await storesApi.create(body);
      navigate('..');
    } catch (err) {
      setErrors(fieldErrorsOf(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const selectedManager = values.managerStaffId ?? '';
  const managerKnown = selectedManager === '' || managers.some((m) => String(m.id) === selectedManager);

  return (
    <section className="card">
      <h2>{id ? 'Edit store' : 'New store'}</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading store…</p>
      ) : (
        <form onSubmit={onSubmit} noValidate aria-label={id ? 'Edit store' : 'New store'}>
          <div className="form-grid">
            <SpecFields
              specs={STORE_FIELDS}
              values={values}
              errors={errors}
              onChange={(name, value) => setValues((v) => ({ ...v, [name]: value }))}
            />
            <FormField name="managerStaffId" label="Manager" errors={errors}>
              <select
                id="field-managerStaffId"
                value={selectedManager}
                onChange={(e) => setValues((v) => ({ ...v, managerStaffId: e.target.value }))}
              >
                <option value="">— None —</option>
                {!managerKnown && <option value={selectedManager}>Staff #{selectedManager}</option>}
                {managers.map((m) => (
                  <option key={m.id} value={String(m.id)}>
                    {m.fullName}
                  </option>
                ))}
              </select>
            </FormField>
          </div>
          <div className="form-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save store'}
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
