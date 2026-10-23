import { createElement } from 'react';
import { collectModules, modules } from './registry';
import type { ModuleDefinition } from './types';

const def = (overrides: Partial<ModuleDefinition>): { default: ModuleDefinition } => ({
  default: { id: 'x', path: 'x', navLabel: 'X', element: createElement('div'), ...overrides },
});

describe('module registry', () => {
  it('auto-registers modules found under src/modules/*/index.ts', () => {
    expect(modules.map((m) => m.id)).toContain('health');
  });

  it('sorts by order then label', () => {
    const sorted = collectModules({
      './b/index.ts': def({ id: 'b', path: 'b', navLabel: 'Beta' }),
      './a/index.ts': def({ id: 'a', path: 'a', navLabel: 'Alpha' }),
      './z/index.ts': def({ id: 'z', path: 'z', navLabel: 'Zulu', order: 1 }),
    });
    expect(sorted.map((m) => m.id)).toEqual(['z', 'a', 'b']);
  });

  it('rejects duplicate route paths', () => {
    expect(() =>
      collectModules({ './a/index.ts': def({ id: 'a' }), './b/index.ts': def({ id: 'b' }) }),
    ).toThrow(/Duplicate module route path/);
  });

  it('rejects duplicate module ids', () => {
    expect(() =>
      collectModules({ './a/index.ts': def({ path: 'a' }), './b/index.ts': def({ path: 'b' }) }),
    ).toThrow(/Duplicate module id/);
  });
});
