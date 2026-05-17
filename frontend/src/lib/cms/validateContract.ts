/**
 * View Contract validator.
 *
 * Validates a ViewContract against the expected schema and returns
 * a list of human-readable error messages.
 */

import type { ViewContract, ViewBlock } from "./viewContract";

export function validateContract(contract: ViewContract): string[] {
  const errors: string[] = [];

  // Required fields
  if (!contract.view_id || contract.view_id.trim().length === 0) {
    errors.push("view_id is required");
  }

  if (!contract.view_title || contract.view_title.trim().length === 0) {
    errors.push("view_title is required");
  }

  if (!contract.view_kind) {
    errors.push("view_kind is required");
  }

  if (typeof contract.view_schema_version !== "number") {
    errors.push("view_schema_version must be a number");
  }

  if (!contract.view_status) {
    errors.push("view_status is required");
  }

  // Layout validation
  if (!contract.layout || !contract.layout.template) {
    errors.push("layout.template is required");
  }

  if (!contract.layout.zones || contract.layout.zones.length === 0) {
    errors.push("layout.zones must contain at least one zone");
  }

  // Zone validation
  const zoneIds = new Set<string>();
  for (const zone of contract.layout.zones) {
    if (!zone.id) {
      errors.push("All zones must have an id");
    } else if (zoneIds.has(zone.id)) {
      errors.push(`Duplicate zone id: ${zone.id}`);
    } else {
      zoneIds.add(zone.id);
    }

    if (!zone.label) {
      errors.push(`Zone ${zone.id ?? "(unknown)"} must have a label`);
    }

    if (!zone.accepts || zone.accepts.length === 0) {
      errors.push(`Zone ${zone.id ?? "(unknown)"} must specify accepted block types`);
    }
  }

  // Block validation
  const blockIds = new Set<string>();
  for (const block of contract.blocks) {
    if (!block.id) {
      errors.push("All blocks must have an id");
    } else if (blockIds.has(block.id)) {
      errors.push(`Duplicate block id: ${block.id}`);
    } else {
      blockIds.add(block.id);
    }

    if (!block.type) {
      errors.push(`Block ${block.id ?? "(unknown)"} must have a type`);
    }

    if (!block.zone) {
      errors.push(`Block ${block.id ?? "(unknown)"} must specify a zone`);
    } else if (!zoneIds.has(block.zone)) {
      errors.push(`Block ${block.id} references unknown zone: ${block.zone}`);
    }

    // Check if block type is accepted in its zone
    if (block.zone) {
      const zone = contract.layout.zones.find((z) => z.id === block.zone);
      if (zone && zone.accepts && !zone.accepts.includes(block.type)) {
        errors.push(`Block type '${block.type}' is not accepted in zone '${block.zone}'`);
      }
    }
  }

  // Editor policy validation
  if (!contract.editor) {
    errors.push("editor policy is required");
  }

  // Publishing validation
  if (!contract.publishing) {
    errors.push("publishing metadata is required");
  }

  // Settings validation
  if (!contract.settings) {
    errors.push("settings are required");
  }

  return errors;
}

export function isContractValid(contract: ViewContract): boolean {
  return validateContract(contract).length === 0;
}
