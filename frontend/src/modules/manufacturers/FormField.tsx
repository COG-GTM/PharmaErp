import type { ReactNode } from 'react';
import type { FieldErrors, FieldSpec, FormValues } from './forms';

export function FormField({
  name,
  label,
  errors,
  children,
}: {
  name: string;
  label: string;
  errors?: FieldErrors;
  children: ReactNode;
}) {
  const messages = errors?.[name] ?? [];
  return (
    <div className={messages.length ? 'field field-invalid' : 'field'}>
      <label htmlFor={`field-${name}`}>{label}</label>
      {children}
      {messages.map((m) => (
        <span key={m} className="field-error" id={`field-${name}-error`}>
          {m}
        </span>
      ))}
    </div>
  );
}

/** Renders a list of plain inputs driven by {@link FieldSpec}s. */
export function SpecFields({
  specs,
  values,
  errors,
  onChange,
}: {
  specs: FieldSpec[];
  values: FormValues;
  errors: FieldErrors;
  onChange: (name: string, value: string) => void;
}) {
  return (
    <>
      {specs.map((s) => {
        const common = {
          id: `field-${s.name}`,
          name: s.name,
          value: values[s.name] ?? '',
          required: s.required,
          maxLength: s.maxLength,
          'aria-invalid': errors[s.name] ? true : undefined,
          'aria-describedby': errors[s.name] ? `field-${s.name}-error` : undefined,
        };
        return (
          <FormField key={s.name} name={s.name} label={s.label} errors={errors}>
            {s.type === 'textarea' ? (
              <textarea {...common} onChange={(e) => onChange(s.name, e.target.value)} />
            ) : s.type === 'select' ? (
              <select {...common} onChange={(e) => onChange(s.name, e.target.value)}>
                {(s.options ?? []).map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            ) : (
              <input
                {...common}
                type={s.type ?? 'text'}
                step={s.type === 'number' ? 'any' : undefined}
                onChange={(e) => onChange(s.name, s.uppercase ? e.target.value.toUpperCase() : e.target.value)}
              />
            )}
          </FormField>
        );
      })}
    </>
  );
}
