import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { ManufacturersModule } from './ManufacturersModule';

const manufacturers: ModuleDefinition = {
  id: 'manufacturers',
  path: 'manufacturers',
  navLabel: 'Manufacturers',
  element: createElement(ManufacturersModule),
  order: 50,
};

export default manufacturers;
