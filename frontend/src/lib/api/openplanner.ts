import { request } from "./core";

export type TranslationPipelineConfig = {
  model: string;
  updated_at: string | null;
};

type TranslationConfigResponse = {
  ok: boolean;
  config: TranslationPipelineConfig;
};

export async function getTranslationPipelineConfig(): Promise<TranslationPipelineConfig> {
  const data = await request<TranslationConfigResponse>("/api/openplanner/v1/translations/config");
  return data.config;
}

export async function updateTranslationPipelineConfig(model: string): Promise<TranslationPipelineConfig> {
  const data = await request<TranslationConfigResponse>("/api/openplanner/v1/translations/config", {
    method: "PATCH",
    body: JSON.stringify({ model }),
  });
  return data.config;
}
