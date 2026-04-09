import type { AdminPermissionDefinition, AdminToolDefinition } from '../../lib/types';
import { Badge, SectionCard } from './common';

export function CatalogSection({
  permissionGroups,
  tools,
}: {
  permissionGroups: Array<[string, AdminPermissionDefinition[]]>;
  tools: AdminToolDefinition[];
}) {
  return (
    <SectionCard
      title="Permission and tool catalog"
      description="Use this as the live reference when composing custom roles or membership overrides."
    >
      <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
          <div className="mb-3 text-sm font-semibold text-slate-100">Permission atoms</div>
          <div className="max-h-[28rem] space-y-4 overflow-auto pr-1">
            {permissionGroups.map(([kind, items]) => (
              <div key={`catalog-${kind}`}>
                <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">{kind}</div>
                <div className="space-y-2">
                  {items.map((permission) => (
                    <div key={`catalog-${permission.id}`} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                      <div className="text-sm font-medium text-slate-100">{permission.code}</div>
                      <div className="mt-1 text-xs text-slate-500">{permission.description}</div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
          <div className="mb-3 text-sm font-semibold text-slate-100">Tool definitions</div>
          <div className="space-y-3">
            {tools.map((tool) => (
              <div key={`tool-${tool.id}`} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-medium text-slate-100">{tool.id}</div>
                    <div className="mt-1 text-xs text-slate-500">{tool.description}</div>
                  </div>
                  <Badge tone={tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warn' : 'success'}>
                    {tool.riskLevel}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </SectionCard>
  );
}
