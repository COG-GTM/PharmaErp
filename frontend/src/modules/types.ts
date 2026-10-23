import type { ReactNode } from 'react';

/**
 * Contract every feature module exports as the default export of
 * `src/modules/<module>/index.ts`. The shell discovers modules via import.meta.glob.
 */
export interface ModuleDefinition {
  /** Unique id, conventionally the folder name. */
  id: string;
  /** Route path without leading slash (e.g. "inventory"); nested routes live under "<path>/*". */
  path: string;
  /** Label shown in the navigation. */
  navLabel: string;
  /** Root element rendered for the route. */
  element: ReactNode;
  /** Sort key for navigation (ascending, default 100). */
  order?: number;
  /** Defaults to true; set false for public pages (sign-in, health). */
  requiresAuth?: boolean;
  /** Register the route but keep it out of the navigation. */
  hideFromNav?: boolean;
}
