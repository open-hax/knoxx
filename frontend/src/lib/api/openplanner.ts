import { request } from "./core";

/**
 * Translation pipeline configuration, resolved from Knoxx resources.
 *
 * `model` is a catalog model id spelled exactly as contracts/models/*.edn
 * spells it. `updated_at` is deliberately gone: it was an operational fact
 * from the legacy endpoint, and desired-state configuration does not carry
 * runtime timestamps.
 */
export type TranslationPipelineConfig = {
  model: string;
  "source-locale": string;
  "default-review": "required" | "none";
};

export async function getTranslationPipelineConfig(): Promise<TranslationPipelineConfig> {
  return await request<TranslationPipelineConfig>("/api/translations/config");
}

export async function updateTranslationPipelineConfig(model: string): Promise<TranslationPipelineConfig> {
  return await request<TranslationPipelineConfig>("/api/translations/config", {
    method: "PATCH",
    body: JSON.stringify({ model }),
  });
}
