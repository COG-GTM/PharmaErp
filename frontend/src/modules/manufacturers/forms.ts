import { ApiError } from '../../api/client';

/** Field -> messages, same shape as the backend's problem+json `errors` map. */
export type FieldErrors = Record<string, string[]>;

export type FormValues = Record<string, string>;

export interface FieldSpec {
  name: string;
  label: string;
  type?: 'text' | 'email' | 'tel' | 'number' | 'date' | 'password' | 'textarea' | 'select';
  /** Choices for `type: 'select'`. */
  options?: { value: string; label: string }[];
  required?: boolean;
  maxLength?: number;
  pattern?: RegExp;
  patternMessage?: string;
  /** Upper-case the value as it is typed (codes such as GSTIN). */
  uppercase?: boolean;
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 404) return 'Record not found.';
    return err.problem.detail ?? err.problem.title ?? `Request failed (HTTP ${err.status})`;
  }
  return err instanceof Error ? err.message : 'Unexpected error';
}

export function fieldErrorsOf(err: unknown): FieldErrors {
  return err instanceof ApiError ? (err.problem.errors ?? {}) : {};
}

export function blankToNull(value: string | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed === '' ? null : trimmed;
}

export function numberOrNull(value: string | undefined): number | null {
  const trimmed = (value ?? '').trim();
  if (trimmed === '') return null;
  const n = Number(trimmed);
  return Number.isFinite(n) ? n : null;
}

export function toFormValues(record: object, specs: FieldSpec[]): FormValues {
  const source = record as Record<string, unknown>;
  return Object.fromEntries(
    specs.map((s) => {
      const v = source[s.name];
      return [s.name, v === null || v === undefined ? '' : String(v)];
    }),
  );
}

export function emptyValues(specs: FieldSpec[]): FormValues {
  return Object.fromEntries(specs.map((s) => [s.name, '']));
}

/** Client-side checks mirroring the backend's Bean Validation; the server stays authoritative. */
export function validate(values: FormValues, specs: FieldSpec[]): FieldErrors {
  const errors: FieldErrors = {};
  for (const s of specs) {
    const v = (values[s.name] ?? '').trim();
    const msgs: string[] = [];
    if (s.required && v === '') msgs.push(`${s.label} is required`);
    if (v !== '') {
      if (s.maxLength && v.length > s.maxLength) msgs.push(`${s.label} must be at most ${s.maxLength} characters`);
      if (s.type === 'email' && !EMAIL.test(v)) msgs.push(`${s.label} must be a valid email address`);
      if (s.type === 'number' && !Number.isFinite(Number(v))) msgs.push(`${s.label} must be a number`);
      if (s.pattern && !s.pattern.test(v)) msgs.push(s.patternMessage ?? `${s.label} is invalid`);
    }
    if (msgs.length) errors[s.name] = msgs;
  }
  return errors;
}
