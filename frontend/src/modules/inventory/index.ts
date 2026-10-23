import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { InventoryModule } from './InventoryModule';

const inventory: ModuleDefinition = {
  id: 'inventory',
  path: 'inventory',
  navLabel: 'Inventory',
  element: createElement(InventoryModule),
  order: 20,
};

export default inventory;
