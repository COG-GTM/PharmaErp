import { useEffect, useState } from 'react';
import { api, ApiError } from '../../api/client';

interface Health {
  status: string;
}

type State = { kind: 'loading' } | { kind: 'ok'; status: string } | { kind: 'error'; message: string };

export function HealthPage() {
  const [state, setState] = useState<State>({ kind: 'loading' });

  useEffect(() => {
    let active = true;
    api
      .get<Health>('/actuator/health')
      .then((health) => active && setState({ kind: 'ok', status: health.status }))
      .catch((err: unknown) => {
        if (!active) return;
        const message = err instanceof ApiError ? `HTTP ${err.status}` : 'Backend unreachable';
        setState({ kind: 'error', message });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="card">
      <h2>System health</h2>
      {state.kind === 'loading' && <p>Checking backend…</p>}
      {state.kind === 'ok' && (
        <p>
          Backend status: <strong data-testid="health-status">{state.status}</strong>
        </p>
      )}
      {state.kind === 'error' && (
        <p className="error" role="alert">
          {state.message}
        </p>
      )}
    </section>
  );
}
