import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { STAFF_TYPES, staffApi, type LoginType, type StaffRequest, type StaffType, type StoreOption } from './api';
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

const STAFF_FIELDS: FieldSpec[] = [
  { name: 'firstName', label: 'First name', required: true, maxLength: 100 },
  { name: 'middleName', label: 'Middle name', maxLength: 100 },
  { name: 'lastName', label: 'Last name', maxLength: 100 },
  {
    name: 'username',
    label: 'System username',
    maxLength: 12,
    pattern: /^(?!\d+$)\w{8,12}$/,
    patternMessage: 'Username must be 8-12 letters, digits or underscores and not only digits',
  },
  { name: 'email', label: 'Email', type: 'email', maxLength: 255 },
  { name: 'mobileNumber', label: 'Mobile number', type: 'tel', maxLength: 16, pattern: /^\+?[0-9]{10,15}$/, patternMessage: 'Mobile number must be 10-15 digits, optionally prefixed with +' },
  { name: 'dateOfBirth', label: 'Date of birth', type: 'date' },
  { name: 'joinDate', label: 'Join date', type: 'date' },
  { name: 'salary', label: 'Salary (₹)', type: 'number', pattern: /^\d+(\.\d{1,2})?$/, patternMessage: 'Salary must be a positive amount with up to 2 decimals' },
  { name: 'experience', label: 'Experience', maxLength: 255 },
  { name: 'lastCompany', label: 'Last company', maxLength: 255 },
  { name: 'homeTown', label: 'Home town', maxLength: 255 },
  { name: 'homeTownAddress', label: 'Home town address', type: 'textarea', maxLength: 500 },
  { name: 'jobAddress', label: 'Job address', type: 'textarea', maxLength: 500 },
  { name: 'leavingAddress', label: 'Current address', type: 'textarea', maxLength: 500 },
  { name: 'resumePath', label: 'Resume path', maxLength: 500 },
  { name: 'feedback', label: 'Feedback', type: 'textarea', maxLength: 1000 },
  { name: 'suggestion', label: 'Suggestion', type: 'textarea', maxLength: 1000 },
];

const SELECTS = ['staffType', 'loginTypeId', 'storeId'] as const;

function emptyStaff(): FormValues {
  return { ...emptyValues(STAFF_FIELDS), staffType: 'INTERNAL', loginTypeId: '', storeId: '', active: 'true' };
}

function toRequest(v: FormValues): StaffRequest {
  return {
    firstName: (v.firstName ?? '').trim(),
    middleName: blankToNull(v.middleName),
    lastName: blankToNull(v.lastName),
    username: blankToNull(v.username),
    email: blankToNull(v.email),
    mobileNumber: blankToNull(v.mobileNumber),
    dateOfBirth: blankToNull(v.dateOfBirth),
    joinDate: blankToNull(v.joinDate),
    salary: numberOrNull(v.salary),
    experience: blankToNull(v.experience),
    lastCompany: blankToNull(v.lastCompany),
    homeTown: blankToNull(v.homeTown),
    homeTownAddress: blankToNull(v.homeTownAddress),
    jobAddress: blankToNull(v.jobAddress),
    leavingAddress: blankToNull(v.leavingAddress),
    resumePath: blankToNull(v.resumePath),
    feedback: blankToNull(v.feedback),
    suggestion: blankToNull(v.suggestion),
    staffType: (v.staffType || 'INTERNAL') as StaffType,
    loginTypeId: numberOrNull(v.loginTypeId),
    storeId: numberOrNull(v.storeId),
    active: v.active !== 'false',
  };
}

export function StaffForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(emptyStaff);
  const [loginTypes, setLoginTypes] = useState<LoginType[]>([]);
  const [stores, setStores] = useState<StoreOption[]>([]);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(Boolean(id));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    // Lookups are optional: the form still works (with "None") if either endpoint is unavailable.
    staffApi
      .loginTypes()
      .then((l) => active && setLoginTypes(l))
      .catch(() => undefined);
    staffApi
      .stores()
      .then((s) => active && setStores(s))
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!id) return;
    let active = true;
    staffApi
      .get(id)
      .then(
        (s) =>
          active &&
          setValues({
            ...toFormValues(s, STAFF_FIELDS),
            staffType: s.staffType ?? 'INTERNAL',
            loginTypeId: s.loginType ? String(s.loginType.id) : '',
            storeId: s.storeId ? String(s.storeId) : '',
            active: String(s.active ?? true),
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
    const clientErrors = validate(values, STAFF_FIELDS);
    setErrors(clientErrors);
    setError(null);
    if (Object.keys(clientErrors).length) return;
    setSaving(true);
    try {
      const body = toRequest(values);
      if (id) await staffApi.update(id, body);
      else await staffApi.create(body);
      navigate('..');
    } catch (err) {
      setErrors(fieldErrorsOf(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const options: Record<(typeof SELECTS)[number], { value: string; label: string }[]> = {
    staffType: STAFF_TYPES.map((t) => ({ value: t, label: t.charAt(0) + t.slice(1).toLowerCase() })),
    loginTypeId: loginTypes.map((l) => ({ value: String(l.id), label: l.name })),
    storeId: stores.map((s) => ({ value: String(s.id), label: s.name })),
  };
  const labels = { staffType: 'Staff type', loginTypeId: 'Login type', storeId: 'Store' };

  return (
    <section className="card">
      <h2>{id ? 'Edit staff member' : 'New staff member'}</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading staff member…</p>
      ) : (
        <form onSubmit={onSubmit} noValidate aria-label={id ? 'Edit staff member' : 'New staff member'}>
          <div className="form-grid">
            {SELECTS.map((name) => {
              const current = values[name] ?? '';
              const known = current === '' || options[name].some((o) => o.value === current);
              return (
                <FormField key={name} name={name} label={labels[name]} errors={errors}>
                  <select id={`field-${name}`} value={current} onChange={(e) => set(name, e.target.value)}>
                    {name !== 'staffType' && <option value="">— None —</option>}
                    {!known && <option value={current}>#{current}</option>}
                    {options[name].map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </FormField>
              );
            })}
            <SpecFields specs={STAFF_FIELDS} values={values} errors={errors} onChange={set} />
            <FormField name="active" label="Active" errors={errors}>
              <input
                id="field-active"
                type="checkbox"
                checked={values.active !== 'false'}
                onChange={(e) => set('active', String(e.target.checked))}
              />
            </FormField>
          </div>
          <div className="form-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save staff member'}
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
