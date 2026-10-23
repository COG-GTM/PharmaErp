import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { HealthPage } from './HealthPage';

const health: ModuleDefinition = {
  id: 'health',
  path: 'health',
  navLabel: 'System health',
  element: createElement(HealthPage),
  order: 1000,
  requiresAuth: false,
};

export default health;
