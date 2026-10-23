/** Pharma ERP mark: a capsule over a medical cross, drawn inline so no asset pipeline is needed. */
export function Logo({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" focusable="false" className="logo">
      <rect width="32" height="32" rx="8" fill="var(--color-teal, #0e9f9a)" />
      <path d="M13 7h6v6h6v6h-6v6h-6v-6H7v-6h6z" fill="#fff" opacity="0.95" />
      <circle cx="16" cy="16" r="2.4" fill="var(--color-navy, #0f2a44)" />
    </svg>
  );
}
