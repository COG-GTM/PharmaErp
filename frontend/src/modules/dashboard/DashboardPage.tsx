import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api/client';
import { useAuth } from '../../auth';
import { KPIS, loadKpi, type KpiDefinition, type KpiResult } from './kpis';

type KpiState = { kind: 'loading' } | KpiResult;
type HealthState = 'checking' | 'up' | 'down';

const numberFormat = new Intl.NumberFormat();

function KpiCard({ kpi, state }: { kpi: KpiDefinition; state: KpiState }) {
  return (
    <article className={`kpi-card kpi-card--${state.kind}`} aria-labelledby={`kpi-${kpi.id}`} aria-busy={state.kind === 'loading'}>
      <h3 id={`kpi-${kpi.id}`} className="kpi-card__label">
        {kpi.label}
      </h3>
      <p className="kpi-card__value" data-testid={`kpi-${kpi.id}`}>
        {state.kind === 'loading' && <span className="skeleton" aria-label="Loading" />}
        {state.kind === 'ok' && numberFormat.format(state.count)}
        {state.kind === 'unavailable' && <span className="kpi-card__status">Not available yet</span>}
        {state.kind === 'forbidden' && <span className="kpi-card__status">No access</span>}
        {state.kind === 'error' && <span className="kpi-card__status kpi-card__status--error">{state.message}</span>}
      </p>
      <p className="kpi-card__hint">{kpi.description}</p>
    </article>
  );
}

export function DashboardPage({ kpis = KPIS }: { kpis?: KpiDefinition[] }) {
  const { user } = useAuth();
  const [states, setStates] = useState<Record<string, KpiState>>({});
  const [health, setHealth] = useState<HealthState>('checking');
  const [refreshedAt, setRefreshedAt] = useState<Date | null>(null);
  const [generation, setGeneration] = useState(0);

  useEffect(() => {
    let active = true;
    setStates(Object.fromEntries(kpis.map((k) => [k.id, { kind: 'loading' } as KpiState])));
    setHealth('checking');
    const loads = kpis.map((kpi) =>
      loadKpi(kpi).then((result) => {
        if (active) setStates((prev) => ({ ...prev, [kpi.id]: result }));
      }),
    );
    const healthCheck = api
      .get<{ status?: string }>('/actuator/health')
      .then((h) => active && setHealth(h?.status === 'UP' ? 'up' : 'down'))
      .catch(() => active && setHealth('down'));
    void Promise.all([...loads, healthCheck]).then(() => active && setRefreshedAt(new Date()));
    return () => {
      active = false;
    };
  }, [kpis, generation]);

  const refresh = useCallback(() => setGeneration((g) => g + 1), []);

  return (
    <section className="dashboard" aria-labelledby="dashboard-title">
      <header className="page-header">
        <div>
          <h2 id="dashboard-title">Dashboard</h2>
          <p className="page-header__subtitle">
            Welcome back{user?.username ? `, ${user.username}` : ''}. Here is today&apos;s snapshot of the estate.
          </p>
        </div>
        <div className="page-header__actions">
          <span className={`status-pill status-pill--${health}`} data-testid="backend-status">
            {health === 'checking' ? 'Checking backend…' : health === 'up' ? 'Backend online' : 'Backend offline'}
          </span>
          <button type="button" className="btn btn--secondary" onClick={refresh}>
            Refresh
          </button>
        </div>
      </header>
      <div className="kpi-grid">
        {kpis.map((kpi) => (
          <KpiCard key={kpi.id} kpi={kpi} state={states[kpi.id] ?? { kind: 'loading' }} />
        ))}
      </div>
      {refreshedAt && (
        <p className="dashboard__footnote">
          Last updated {refreshedAt.toLocaleTimeString()}. Modules that are not deployed yet show “Not available yet”.
        </p>
      )}
    </section>
  );
}
