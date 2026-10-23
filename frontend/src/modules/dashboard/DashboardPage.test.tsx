import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DashboardPage } from './DashboardPage';
import { countFrom, type KpiDefinition } from './kpis';
import { inOneHour, makeJwt } from '../../auth/testing';
import { setToken } from '../../auth/tokenStorage';
import { renderWithProviders } from '../../test/utils';

type Reply = { status: number; body?: unknown };

function routeFetch(routes: Record<string, Reply>) {
  const fn = vi.fn((input: RequestInfo | URL) => {
    const path = String(input).split('?')[0] ?? '';
    const reply = routes[path] ?? { status: 404, body: { status: 404, detail: 'Not Found' } };
    return Promise.resolve(
      new Response(JSON.stringify(reply.body ?? {}), {
        status: reply.status,
        headers: { 'Content-Type': reply.status >= 400 ? 'application/problem+json' : 'application/json' },
      }),
    );
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}

const page = (totalElements: number) => ({ status: 200, body: { content: [], page: 0, size: 1, totalElements, totalPages: 1 } });

describe('DashboardPage', () => {
  beforeEach(() => setToken(makeJwt({ username: 'tarang', scope: 'admin', exp: inOneHour() })));

  it('shows KPI counts and tolerates missing or forbidden endpoints', async () => {
    const fetchMock = routeFetch({
      '/api/inventory': page(1280),
      '/api/invoices': page(42),
      '/api/clients': { status: 200, body: [{ id: 1 }, { id: 2 }, { id: 3 }] },
      '/api/staff': { status: 403, body: { status: 403, detail: 'Forbidden' } },
      '/api/payments': { status: 500, body: { status: 500, detail: 'boom' } },
      '/actuator/health': { status: 200, body: { status: 'UP' } },
    });

    renderWithProviders(<DashboardPage />);

    expect(screen.getByText(/Welcome back, tarang/)).toBeInTheDocument();
    expect(await screen.findByText('1,280')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId('kpi-invoices')).toHaveTextContent('42'));
    expect(screen.getByTestId('kpi-clients')).toHaveTextContent('3');
    expect(screen.getByTestId('kpi-manufacturers')).toHaveTextContent('Not available yet');
    expect(screen.getByTestId('kpi-staff')).toHaveTextContent('No access');
    expect(screen.getByTestId('kpi-payments')).toHaveTextContent('HTTP 500');
    await waitFor(() => expect(screen.getByTestId('backend-status')).toHaveTextContent('Backend online'));

    const urls = fetchMock.mock.calls.map(([u]) => String(u));
    expect(urls).toContain('/api/inventory?page=0&size=1');
    expect(await screen.findByText(/Last updated/)).toBeInTheDocument();
  });

  it('degrades gracefully when the backend is unreachable and can refresh', async () => {
    const kpis: KpiDefinition[] = [{ id: 'clients', label: 'Clients', endpoints: ['/api/clients'], description: 'x' }];
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    renderWithProviders(<DashboardPage kpis={kpis} />);

    await waitFor(() => expect(screen.getByTestId('kpi-clients')).toHaveTextContent('Backend unreachable'));
    await waitFor(() => expect(screen.getByTestId('backend-status')).toHaveTextContent('Backend offline'));

    routeFetch({ '/api/clients': page(7), '/actuator/health': { status: 200, body: { status: 'UP' } } });
    await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => expect(screen.getByTestId('kpi-clients')).toHaveTextContent('7'));
  });
});

describe('countFrom', () => {
  it.each([
    [{ totalElements: 5, content: [] }, 5],
    [{ content: [1] }, null],
    [[1, 2, 3], 3],
    [{ foo: 1 }, null],
    ['x', null],
  ])('%j -> %s', (input, expected) => {
    expect(countFrom(input)).toBe(expected);
  });
});
