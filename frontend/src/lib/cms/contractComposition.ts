/**
 * Contract composition resolver for the folder-backed visual CMS.
 *
 * Handles resolving contract references by path or ID,
 * merging blocks from referenced contracts into the current view.
 */

import type { ViewContract, ViewBlock, ViewBlockRef } from "./viewContract";

export interface ResolvedContract {
  contract: ViewContract;
  blocks: ViewBlock[];
}

/**
 * Resolve a contract reference to its blocks.
 * Returns null if reference is invalid or cannot be resolved.
 */
export async function resolveContractRef(
  ref: ViewBlockRef,
  basePath?: string
): Promise<ResolvedContract | null> {
  let contractPath: string | null = null;

  if (ref.ref_type === "path" && ref.ref_path) {
    contractPath = ref.ref_path;
  } else if (ref.ref_type === "id" && ref.ref_id) {
    // Search for contract by view/id
    // Try common locations
    const searchPaths = [
      `cms/drafts/${ref.ref_id}/view-contract.edn`,
      `cms/drafts/${ref.ref_id}`,
      `gardens/*/drafts/${ref.ref_id}/view-contract.edn`,
      `gardens/*/published/${ref.ref_id}/view-contract.edn`,
    ];

    for (const searchPath of searchPaths) {
      try {
        const resp = await fetch(`/api/ingestion/file?path=${encodeURIComponent(searchPath)}`);
        if (resp.ok) {
          contractPath = searchPath;
          break;
        }
      } catch {
        // Continue searching
      }
    }
  }

  if (!contractPath) return null;

  try {
    const resp = await fetch(`/api/ingestion/file?path=${encodeURIComponent(contractPath)}`);
    if (!resp.ok) return null;

    const data = (await resp.json()) as { content?: string };
    const contractText = data.content ?? "";

    // Parse EDN - this is a simplified parser
    // In practice, the backend should provide a JSON endpoint
    const contract = parseEdnContract(contractText);
    if (!contract) return null;

    // If include_layout is true, merge layout zones
    let blocks = contract.blocks;

    // Apply zone mapping if provided
    if (ref.zone_mapping) {
      blocks = blocks.map((block) => ({
        ...block,
        zone: ref.zone_mapping![block.zone] ?? block.zone,
      }));
    }

    return { contract, blocks };
  } catch {
    return null;
  }
}

/**
 * Simple EDN parser for view contracts.
 * This is a fallback - the backend should ideally serve JSON.
 */
function parseEdnContract(ednText: string): ViewContract | null {
  try {
    // Extract key fields using regex patterns
    const extractString = (key: string): string | undefined => {
      const match = ednText.match(new RegExp(`${key}\\s+"([^"]+)"`));
      return match?.[1];
    };

    const extractKeyword = (key: string): string | undefined => {
      const match = ednText.match(new RegExp(`${key}\\s+:(\\w+)`));
      return match?.[1];
    };

    const viewId = extractString(":view/id") ?? "unknown";
    const viewTitle = extractString(":view/title") ?? "Untitled";
    const viewKind = (extractKeyword(":view/kind") ?? "article-page") as ViewContract["view_kind"];

    // Extract blocks array - very simplified
    const blocksMatch = ednText.match(/:blocks\s*\[([\s\S]*?)\](?=\s*:\w|$)/);
    const blocksText = blocksMatch?.[1] ?? "";

    // Parse individual blocks
    const blocks: ViewBlock[] = [];
    const blockMatches = blocksText.matchAll(/\{([^}]+)\}/g);

    for (const blockMatch of blockMatches) {
      const blockText = blockMatch[1];
      const id = extractStringFromBlock(blockText, ":id");
      const type = extractKeywordFromBlock(blockText, ":type");
      const zone = extractKeywordFromBlock(blockText, ":zone");

      if (id && type) {
        blocks.push({
          id,
          type,
          zone: zone ?? "main",
          props: {},
        });
      }
    }

    return {
      view_id: viewId,
      view_title: viewTitle,
      view_kind: viewKind,
      view_schema_version: 1,
      view_status: "draft",
      layout: {
        template: viewKind,
        zones: [],
      },
      blocks,
      editor: {
        locked: false,
        allowed_actions: ["drag", "drop", "duplicate", "hide", "delete", "edit-props"],
        default_panel: "layout",
      },
      publishing: {
        defer_index: false,
        skip_translation: true,
      },
      settings: {
        language: "en",
        allow_comments: true,
      },
    };
  } catch {
    return null;
  }
}

function extractStringFromBlock(text: string, key: string): string | undefined {
  const match = text.match(new RegExp(`${key}\\s+"([^"]+)"`));
  return match?.[1];
}

function extractKeywordFromBlock(text: string, key: string): string | undefined {
  const match = text.match(new RegExp(`${key}\\s+:(\\w+)`));
  return match?.[1];
}

/**
 * Compose a contract with all its resolved references.
 * Returns the flattened list of blocks with references resolved.
 */
export async function composeContract(
  contract: ViewContract,
  maxDepth = 3,
  currentDepth = 0
): Promise<ViewBlock[]> {
  if (currentDepth >= maxDepth) {
    return contract.blocks;
  }

  const composed: ViewBlock[] = [];

  for (const block of contract.blocks) {
    if (block.type === "contract-ref" && block.ref) {
      const resolved = await resolveContractRef(block.ref);
      if (resolved) {
        // Recursively compose referenced contract
        const nestedBlocks = await composeContract(
          resolved.contract,
          maxDepth,
          currentDepth + 1
        );
        composed.push(...nestedBlocks);
      } else {
        // Keep the ref block as placeholder if resolution fails
        composed.push(block);
      }
    } else {
      composed.push(block);
    }
  }

  return composed;
}
