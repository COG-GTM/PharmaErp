import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { clientsApi, type ClientRequest } from './api';
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

const CLIENT_FIELDS: FieldSpec[] = [
  { name: 'name', label: 'Name', required: true, maxLength: 255 },
  { name: 'phone', label: 'Phone', type: 'tel', maxLength: 20, pattern: /^\+?[0-9(][0-9 ()\-.]{5,19}$/, patternMessage: 'Phone must be a phone number (digits, spaces, +, -, (, ), .)' },
  { name: 'email', label: 'Email', type: 'email', maxLength: 255 },
  { name: 'feedbackEmail', label: 'Feedback email', type: 'email', maxLength: 255 },
  { name: 'address', label: 'Address', type: 'textarea', maxLength: 500 },
  { name: 'city', label: 'City', maxLength: 100 },
  { name: 'district', label: 'District', maxLength: 100 },
  { name: 'state', label: 'State', maxLength: 100 },
  { name: 'gstin', label: 'GSTIN', maxLength: 15, pattern: GSTIN, patternMessage: 'GSTIN must be a valid 15-character GST number', uppercase: true },
];

function toRequest(values: FormValues): ClientRequest {
  return {
    name: (values.name ?? '').trim(),
    address: blankToNull(values.address),
    phone: blankToNull(values.phone),
    email: blankToNull(values.email),
    city: blankToNull(values.city),
    district: blankToNull(values.district),
    state: blankToNull(values.state),
    gstin: blankToNull(values.gstin),
    feedbackEmail: blankToNull(values.feedbackEmail),
  };
}

export function ClientForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(() => emptyValues(CLIENT_FIELDS));
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(Boolean(id));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    let active = true;
    clientsApi
      .get(id)
      .then((c) => active && setValues(toFormValues(c, CLIENT_FIELDS)))
      .catch((err: unknown) => active && setError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const clientErrors = validate(values, CLIENT_FIELDS);
    setErrors(clientErrors);
    setError(null);
    if (Object.keys(clientErrors).length) return;
    setSaving(true);
    try {
      const body = toRequest(values);
      if (id) await clientsApi.update(id, body);
      else await clientsApi.create(body);
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
      <h2>{id ? 'Edit client' : 'New client'}</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading client…</p>
      ) : (
        <form onSubmit={onSubmit} noValidate aria-label={id ? 'Edit client' : 'New client'}>
          <div className="form-grid">
            <SpecFields
              specs={CLIENT_FIELDS}
              values={values}
              errors={errors}
              onChange={(name, value) => setValues((v) => ({ ...v, [name]: value }))}
            />
          </div>
          <div className="form-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save client'}
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
