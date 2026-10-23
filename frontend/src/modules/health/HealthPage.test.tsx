import { screen } from '@testing-library/react';
import { HealthPage } from './HealthPage';
import { mockFetchJson, renderWithProviders } from '../../test/utils';

describe('HealthPage', () => {
  it('shows an error when the backend is down', async () => {
    mockFetchJson({ status: 'DOWN' }, 503);

    renderWithProviders(<HealthPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('HTTP 503');
  });
});
