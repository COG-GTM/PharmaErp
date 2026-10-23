import { createElement } from 'react';
import type { ModuleDefinition } from '../types';
import { UsersModule } from './UsersModule';

const users: ModuleDefinition = {
  id: 'users',
  path: 'users',
  navLabel: 'Users',
  element: createElement(UsersModule),
  order: 80,
};

export default users;
