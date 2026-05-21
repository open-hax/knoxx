import type React from "react";
import { WorkbenchBottomPanel } from "./WorkbenchShell";

export type BottomPanelProps = {
  children: React.ReactNode;
  label: string;
  storageKey: string;
  defaultHeight?: number;
  minHeight?: number;
  maxHeight?: number;
  className?: string;
  header?: React.ReactNode;
};

export function BottomPanel(props: BottomPanelProps) {
  return <WorkbenchBottomPanel {...props} />;
}
