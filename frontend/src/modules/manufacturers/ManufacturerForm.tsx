import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { MANUFACTURER_TYPES, manufacturersApi, type ManufacturerRequest, type ManufacturerType } from './api';
import { SpecFields } from './FormField';
import {
  blankToNull,
  emptyValues,
  errorMessage,
  fieldErrorsOf,
  toFormValues,
  validate,
  type FieldErrors,
  type FieldSpec,
  type FormValues,
} from './forms';

const GSTIN = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;

const MANUFACTURER_FIELDS: FieldSpec[] = [
  { name: 'name', label: 'Name', required: true, maxLength: 255 },
  { name: 'type', label: 'Type', type: 'select', required: true, options: MANUFACTURER_TYPES },
  { name: 'licenceNumber', label: 'Drug licence number', maxLength: 64, pattern: /^[A-Za-z0-9/\-. ]*$/, patternMessage: 'Licence number may contain only letters, digits, spaces and / - .' },
  { name: 'contactPerson', label: 'Contact person', maxLength: 255 },
  { name: 'phone', label: 'Phone', type: 'tel', maxLength: 20, pattern: /^\+?[0-9(][0-9 ()\-.]{5,19}$/, patternMessage: 'Phone must be a phone number (digits, spaces, +, -, (, ), .)' },
  { name: 'email', label: 'Email', type: 'email', maxLength: 255 },
  { name: 'address', label: 'Address', type: 'textarea', maxLength: 500 },
  { name: 'city', label: 'City', maxLength: 100 },
  { name: 'district', label: 'District', maxLength: 100 },
  { name: 'state', label: 'State', maxLength: 100 },
  { name: 'gstin', label: 'GSTIN', maxLength: 15, pattern: GSTIN, patternMessage: 'GSTIN must be a valid 15-character GST number', uppercase: true },
];

function toRequest(values: FormValues): ManufacturerRequest {
  return {
    name: (values.name ?? '').trim(),
    type: (values.type || 'MANUFACTURER') as ManufacturerType,
    licenceNumber: blankToNull(values.licenceNumber),
    contactPerson: blankToNull(values.contactPerson),
    address: blankToNull(values.address),
    phone: blankToNull(values.phone),
    email: blankToNull(values.email),
    city: blankToNull(values.city),
    district: blankToNull(values.district),
    state: blankToNull(values.state),
    gstin: blankToNull(values.gstin),
  };
}

export function ManufacturerForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(() => ({ ...emptyValues(MANUFACTURER_FIELDS), type: 'MANUFACTURER' }));
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(Boolean(id));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    let active = true;
    manufacturersApi
      .get(id)
      .then((c) => active && setValues(toFormValues(c, MANUFACTURER_FIELDS)))
      .catch((err: unknown) => active && setError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const manufacturerErrors = validate(values, MANUFACTURER_FIELDS);
    setErrors(manufacturerErrors);
    setError(null);
    if (Object.keys(manufacturerErrors).length) return;
    setSaving(true);
    try {
      const body = toRequest(values);
      if (id) await manufacturersApi.update(id, body);
      else await manufacturersApi.create(body);
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
      <h2>{id ? 'Edit manufacturer' : 'New manufacturer'}</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading manufacturer…</p>
      ) : (
        <form onSubmit={onSubmit} noValidate aria-label={id ? 'Edit manufacturer' : 'New manufacturer'}>
          <div className="form-grid">
            <SpecFields
              specs={MANUFACTURER_FIELDS}
              values={values}
              errors={errors}
              onChange={(name, value) => setValues((v) => ({ ...v, [name]: value }))}
            />
          </div>
          <div className="form-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save manufacturer'}
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
