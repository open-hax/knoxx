/**
 * Puck-based Visual Editor for the folder-backed CMS.
 *
 * Maps our view-contract.edn block model to Puck's component registry.
 */

import { Puck, type Config, type Data } from "@measured/puck";
import { useCallback, useState } from "react";
import type { ViewContract, ViewBlock, ViewZone } from "../../lib/cms/viewContract";
import type { ViewBlockRef } from "../../lib/cms/viewContract";
import { PublicationBlocksRenderer } from "@open-hax/garden-publication-components";

// Convert view-contract blocks to Puck data model
function blocksToPuckData(contract: ViewContract): Data {
  const zones: Record<string, { type: string; props: Record<string, unknown> }[]> = {};

  for (const zone of contract.layout.zones) {
    zones[zone.id] = [];
  }

  for (const block of contract.blocks) {
    if (block.hidden) continue;
    const zoneBlocks = zones[block.zone] ?? [];
    zoneBlocks.push({
      type: block.type,
      props: {
        ...block.props,
        _blockId: block.id,
        _blockSource: block.source,
        _blockRef: block.ref,
      },
    });
    zones[block.zone] = zoneBlocks;
  }

  return {
    content: [],
    zones,
    root: {
      title: contract.view_title,
      // Puck root props
    },
  };
}

// Convert Puck data back to view-contract blocks
function puckDataToBlocks(data: Data, contract: ViewContract): ViewBlock[] {
  const blockMap = new Map<string, ViewBlock>();

  for (const zone of contract.layout.zones) {
    const zoneItems = (data.zones?.[zone.id] ?? []) as Array<{
      type: string;
      props: Record<string, unknown>;
    }>;

    for (const item of zoneItems) {
      const blockId = (item.props._blockId as string) ?? crypto.randomUUID();
      const { _blockId, _blockSource, _blockRef, ...props } = item.props;

      blockMap.set(blockId, {
        id: blockId,
        type: item.type,
        zone: zone.id,
        props,
        source: _blockSource as ViewBlock["source"],
        ref: _blockRef as ViewBlock["ref"],
      });
    }
  }

  // Preserve hidden blocks
  for (const block of contract.blocks) {
    if (block.hidden && !blockMap.has(block.id)) {
      blockMap.set(block.id, block);
    }
  }

  return Array.from(blockMap.values());
}

// Build Puck config from template zones
function buildPuckConfig(zones: ViewZone[]): Config {
  const config: Config = {
    components: {
      hero: {
        fields: {
          title: { type: "text" },
          subtitle: { type: "text" },
          image_path: { type: "text" },
          audio_path: { type: "text" },
        },
        defaultProps: {
          title: "Hero Title",
          subtitle: "",
        },
        render: ({ title, subtitle, image_path, audio_path }) => (
          <div style={{ padding: "2rem", textAlign: "center", background: "linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)", borderRadius: "8px" }}>
            {image_path && <img src={image_path} alt={title} style={{ maxWidth: "100%", borderRadius: "8px", marginBottom: "1rem" }} />}
            <h1 style={{ color: "#fff", margin: "0 0 0.5rem" }}>{title}</h1>
            {subtitle && <p style={{ color: "#a0a0a0", margin: 0 }}>{subtitle}</p>}
            {audio_path && <audio controls src={audio_path} style={{ marginTop: "1rem", width: "100%", maxWidth: "400px" }} />}
          </div>
        ),
      },
      "rich-text": {
        fields: {
          markdown: { type: "textarea" },
        },
        defaultProps: {
          markdown: "Start writing...",
        },
        render: ({ markdown }) => (
          <div style={{ lineHeight: 1.7, color: "#d0d0d0" }}>
            <div dangerouslySetInnerHTML={{ __html: markdown.replace(/\n/g, "<br/>") }} />
          </div>
        ),
      },
      heading: {
        fields: {
          text: { type: "text" },
          level: { type: "number", min: 2, max: 4 },
        },
        defaultProps: {
          text: "Heading",
          level: 2,
        },
        render: ({ text, level }) => {
          const Tag = `h${level}` as keyof JSX.IntrinsicElements;
          return <Tag style={{ color: "#e0e0e0", margin: "1.5rem 0 0.75rem" }}>{text}</Tag>;
        },
      },
      playlist: {
        fields: {
          title: { type: "text" },
          description: { type: "text" },
          layout: {
            type: "select",
            options: [
              { label: "Compact", value: "compact" },
              { label: "Cards", value: "cards" },
              { label: "Broadcast", value: "broadcast" },
            ],
          },
          show_labels: { type: "radio", options: [
            { label: "Yes", value: true },
            { label: "No", value: false },
          ]},
          show_descriptions: { type: "radio", options: [
            { label: "Yes", value: true },
            { label: "No", value: false },
          ]},
          show_duration: { type: "radio", options: [
            { label: "Yes", value: true },
            { label: "No", value: false },
          ]},
        },
        defaultProps: {
          title: "Playlist",
          description: "",
          layout: "cards",
          show_labels: true,
          show_descriptions: true,
          show_duration: true,
        },
        render: ({ title, description }) => (
          <div style={{ margin: "1.5rem 0" }}>
            {title && <h3 style={{ fontWeight: 600, margin: "0 0 0.5rem" }}>{title}</h3>}
            {description && <p style={{ color: "#a0a0a0" }}>{description}</p>}
            <div style={{ padding: "1rem", background: "rgba(255,255,255,0.05)", borderRadius: "8px", color: "#a0a0a0" }}>
              [Playlist tracks would render here]
            </div>
          </div>
        ),
      },
      track: {
        fields: {
          audio_path: { type: "text" },
          title: { type: "text" },
          commentary: { type: "textarea" },
          show_player: { type: "radio", options: [
            { label: "Yes", value: true },
            { label: "No", value: false },
          ]},
        },
        defaultProps: {
          title: "Track",
          show_player: true,
        },
        render: ({ title }) => (
          <div style={{ padding: "1rem", background: "rgba(255,255,255,0.05)", borderRadius: "8px", display: "flex", alignItems: "center", gap: "1rem" }}>
            <button style={{ width: "48px", height: "48px", borderRadius: "50%", border: "none", background: "#3b82f6", color: "#fff", cursor: "pointer" }}>▶</button>
            <div style={{ fontWeight: 600 }}>{title}</div>
          </div>
        ),
      },
      callout: {
        fields: {
          tone: {
            type: "select",
            options: [
              { label: "Note", value: "note" },
              { label: "Tip", value: "tip" },
              { label: "Warning", value: "warning" },
              { label: "Promo", value: "promo" },
            ],
          },
          title: { type: "text" },
          markdown: { type: "textarea" },
        },
        defaultProps: {
          tone: "note",
          title: "",
          markdown: "Callout content...",
        },
        render: ({ tone, title, markdown }) => {
          const tones: Record<string, string> = {
            note: "#3b82f6",
            tip: "#22c55e",
            warning: "#f59e0b",
            promo: "#ec4899",
          };
          return (
            <div style={{ borderLeft: `4px solid ${tones[tone] ?? tones.note}`, background: `${tones[tone] ?? tones.note}20`, padding: "1rem", borderRadius: "0 8px 8px 0" }}>
              {title && <div style={{ fontWeight: 600, marginBottom: "0.5rem" }}>{title}</div>}
              <div>{markdown}</div>
            </div>
          );
        },
      },
      cta: {
        fields: {
          label: { type: "text" },
          href: { type: "text" },
          tone: {
            type: "select",
            options: [
              { label: "Primary", value: "primary" },
              { label: "Secondary", value: "secondary" },
            ],
          },
        },
        defaultProps: {
          label: "Click here",
          href: "#",
          tone: "primary",
        },
        render: ({ label, href, tone }) => (
          <div style={{ textAlign: "center", margin: "1.5rem 0" }}>
            <a
              href={href}
              style={{
                display: "inline-block",
                padding: "0.75rem 1.5rem",
                borderRadius: "8px",
                background: tone === "primary" ? "#3b82f6" : "transparent",
                color: tone === "primary" ? "#fff" : "#3b82f6",
                border: tone === "primary" ? "none" : "2px solid #3b82f6",
                textDecoration: "none",
                fontWeight: 600,
              }}
            >
              {label}
            </a>
          </div>
        ),
      },
      image: {
        fields: {
          src: { type: "text" },
          alt: { type: "text" },
          caption: { type: "text" },
        },
        defaultProps: {
          src: "",
          alt: "",
        },
        render: ({ src, alt, caption }) => (
          <figure style={{ margin: "1.5rem 0" }}>
            {src && <img src={src} alt={alt} style={{ maxWidth: "100%", borderRadius: "8px" }} />}
            {caption && <figcaption style={{ textAlign: "center", color: "#a0a0a0", fontSize: "0.875rem", marginTop: "0.5rem" }}>{caption}</figcaption>}
          </figure>
        ),
      },
      divider: {
        fields: {},
        render: () => <hr style={{ border: "none", borderTop: "1px solid rgba(255,255,255,0.1)", margin: "2rem 0" }} />,
      },
      "contract-ref": {
        fields: {
          ref_type: {
            type: "select",
            options: [
              { label: "Path", value: "path" },
              { label: "ID", value: "id" },
            ],
          },
          ref_path: { type: "text" },
          ref_id: { type: "text" },
          include_layout: { type: "radio", options: [
            { label: "Yes", value: true },
            { label: "No", value: false },
          ]},
        },
        defaultProps: {
          ref_type: "path",
          ref_path: "",
          ref_id: "",
          include_layout: false,
        },
        render: ({ ref_type, ref_path, ref_id }) => (
          <div style={{ padding: "1rem", background: "rgba(59, 130, 246, 0.1)", border: "2px dashed #3b82f6", borderRadius: "8px", color: "#93bbfd" }}>
            <div style={{ fontWeight: 600, marginBottom: "0.5rem" }}>Contract Reference</div>
            {ref_type === "path" ? (
              <div style={{ fontSize: "0.875rem" }}>Path: {ref_path || "(none)"}</div>
            ) : (
              <div style={{ fontSize: "0.875rem" }}>ID: {ref_id || "(none)"}</div>
            )}
            <div style={{ fontSize: "0.75rem", marginTop: "0.5rem", opacity: 0.7 }}>
              Referenced contract blocks will be composed into this zone at render time.
            </div>
          </div>
        ),
      },
    },
    root: {
      fields: {
        title: { type: "text" },
      },
    },
  };

  return config;
}

interface VisualEditorProps {
  contract: ViewContract;
  onChange?: (contract: ViewContract) => void;
}

export function VisualEditor({ contract, onChange }: VisualEditorProps) {
  const [data, setData] = useState<Data>(() => blocksToPuckData(contract));

  const handleChange = useCallback(
    (newData: Data) => {
      setData(newData);
      if (onChange) {
        const newBlocks = puckDataToBlocks(newData, contract);
        onChange({
          ...contract,
          blocks: newBlocks,
        });
      }
    },
    [contract, onChange]
  );

  const config = buildPuckConfig(contract.layout.zones);

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <Puck
        config={config}
        data={data}
        onChange={handleChange}
        overrides={{
          header: () => <></>, // We use our own header
        }}
      />
    </div>
  );
}
