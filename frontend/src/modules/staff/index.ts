import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { StaffModule } from './StaffModule';

const staff: ModuleDefinition = {
  id: 'staff',
  path: 'staff',
  navLabel: 'Staff',
  element: createElement(StaffModule),
  order: 70,
};

export default staff;
