import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { InvoicesModule } from './InvoicesModule';

const invoices: ModuleDefinition = {
  id: 'invoices',
  path: 'invoices',
  navLabel: 'Invoices',
  element: createElement(InvoicesModule),
  order: 30,
};

export default invoices;
