import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { DashboardPage } from './DashboardPage';

const dashboard: ModuleDefinition = {
  id: 'dashboard',
  path: 'dashboard',
  navLabel: 'Dashboard',
  element: createElement(DashboardPage),
  order: 0,
};

export default dashboard;
