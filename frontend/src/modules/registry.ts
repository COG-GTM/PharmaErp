import type { ModuleDefinition } from './types';

type ModuleFile = { default: ModuleDefinition };

export function collectModules(files: Record<string, ModuleFile>): ModuleDefinition[] {
  const modules = Object.entries(files).map(([file, mod]) => {
    if (!mod?.default?.path || !mod.default.id) {
      throw new Error(`${file} must default-export a ModuleDefinition`);
    }
    return mod.default;
  });
  const seen = new Set<string>();
  for (const m of modules) {
    if (seen.has(m.path)) throw new Error(`Duplicate module route path "${m.path}"`);
    seen.add(m.path);
  }
  return modules.sort((a, b) => (a.order ?? 100) - (b.order ?? 100) || a.navLabel.localeCompare(b.navLabel));
}

export const modules: ModuleDefinition[] = collectModules(
  import.meta.glob<ModuleFile>('./*/index.ts', { eager: true }),
);
