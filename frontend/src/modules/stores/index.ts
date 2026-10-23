import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { StoresModule } from './StoresModule';

const stores: ModuleDefinition = {
  id: 'stores',
  path: 'stores',
  navLabel: 'Stores',
  element: createElement(StoresModule),
  order: 60,
};

export default stores;
