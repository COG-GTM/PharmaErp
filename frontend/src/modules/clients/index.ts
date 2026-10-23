import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { ClientsModule } from './ClientsModule';

const clients: ModuleDefinition = {
  id: 'clients',
  path: 'clients',
  navLabel: 'Clients',
  element: createElement(ClientsModule),
  order: 40,
};

export default clients;
