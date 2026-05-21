import type React from "react";
import { WorkbenchPanel } from "./WorkbenchShell";

export type SidebarProps = {
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

export function Sidebar(props: SidebarProps) {
  return <WorkbenchPanel minWidth={240} maxWidth={520} {...props} />;
}
