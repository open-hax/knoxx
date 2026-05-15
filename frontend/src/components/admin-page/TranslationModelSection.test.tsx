import { fireEvent, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { TranslationModelSection } from "./TranslationModelSection";

const mockListProxxModels = vi.fn();
const mockGetTranslationPipelineConfig = vi.fn();
const mockUpdateTranslationPipelineConfig = vi.fn();

vi.mock("../../lib/api/runtime", () => ({
  listProxxModels: (...args: unknown[]) => mockListProxxModels(...args),
}));

vi.mock("../../lib/api/openplanner", () => ({
  getTranslationPipelineConfig: (...args: unknown[]) => mockGetTranslationPipelineConfig(...args),
  updateTranslationPipelineConfig: (...args: unknown[]) => mockUpdateTranslationPipelineConfig(...args),
}));

vi.mock("./common", () => ({
  SectionCard: ({ title, description, children }: { title: string; description: string; children: ReactNode }) => (
    <section>
      <h2>{title}</h2>
      <p>{description}</p>
      {children}
    </section>
  ),
}));

describe("TranslationModelSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetTranslationPipelineConfig.mockResolvedValue({ model: "glm-5", updated_at: "2026-05-15T00:00:00.000Z" });
    mockListProxxModels.mockResolvedValue([
      { id: "glm-5", name: "GLM 5" },
      { id: "gpt-5.4", name: "GPT 5.4" },
    ]);
    mockUpdateTranslationPipelineConfig.mockResolvedValue({ model: "gpt-5.4", updated_at: "2026-05-15T00:01:00.000Z" });
  });

  it("loads Proxx model options and saves the selected translation model", async () => {
    render(<TranslationModelSection canManage={true} />);

    const input = await screen.findByDisplayValue("glm-5") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "gpt-5.4" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(await screen.findByText("Translation model updated to gpt-5.4.")).toBeInTheDocument();
    expect(mockGetTranslationPipelineConfig).toHaveBeenCalledTimes(1);
    expect(mockListProxxModels).toHaveBeenCalledTimes(1);
    expect(mockUpdateTranslationPipelineConfig).toHaveBeenCalledWith("gpt-5.4");
    expect(screen.getByText("Current:")).toHaveTextContent("Current: gpt-5.4");
  });
});
