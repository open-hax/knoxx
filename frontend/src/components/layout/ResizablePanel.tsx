import type React from "react";
import { WorkbenchPanel } from "./WorkbenchShell";

type ResizablePanelProps = {
  children: React.ReactNode;
  edge: "left" | "right";
  label: string;
  storageKey: string;
  defaultWidth?: number;
  minWidth?: number;
  maxWidth?: number;
  className?: string;
  header?: React.ReactNode;
};

export function ResizablePanel(props: ResizablePanelProps) {
  return <WorkbenchPanel minWidth={240} maxWidth={520} {...props} />;
}
