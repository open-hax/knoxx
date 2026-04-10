import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { createSidebarResizeHandlers } from "./sidebar-resize";

describe("createSidebarResizeHandlers", () => {
  let containerRef: { current: HTMLDivElement | null };
  let setSidebarPaneSplitPct: ReturnType<typeof vi.fn>;
  let setSidebarWidthPx: ReturnType<typeof vi.fn>;
  let mockContainer: HTMLDivElement;

  beforeEach(() => {
    mockContainer = {
      getBoundingClientRect: () => ({ top: 100, left: 0, width: 400, height: 600, right: 400, bottom: 700, x: 0, y: 100, toJSON: () => {} }),
    } as HTMLDivElement;

    containerRef = { current: mockContainer };
    setSidebarPaneSplitPct = vi.fn();
    setSidebarWidthPx = vi.fn();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe("startSidebarPaneResize", () => {
    it("does nothing if container ref is null", () => {
      containerRef.current = null;
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientY: 150 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarPaneResize(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(setSidebarPaneSplitPct).not.toHaveBeenCalled();
    });

    it("sets cursor to row-resize", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientY: 150 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarPaneResize(event);

      expect(document.body.style.cursor).toBe("row-resize");
      expect(document.body.style.userSelect).toBe("none");
    });

    it("calculates percentage on mousemove", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn() } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarPaneResize(event);

      // Simulate mousemove - clientY 250 = 150px below container top (100)
      // deltaY = 250 - 100 = 150, pct = 150/600 * 100 = 25%
      const moveEvent = new MouseEvent("mousemove", { clientY: 250 });
      window.dispatchEvent(moveEvent);

      expect(setSidebarPaneSplitPct).toHaveBeenCalledWith(25);
    });

    it("clamps percentage to 25-75 range", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn() } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarPaneResize(event);

      // Below 25% - clientY 100 = 0px delta, pct = 0% -> clamped to 25%
      const moveEvent1 = new MouseEvent("mousemove", { clientY: 100 });
      window.dispatchEvent(moveEvent1);
      expect(setSidebarPaneSplitPct).toHaveBeenLastCalledWith(25);

      // Above 75% - clientY 700 = 600px delta, pct = 100% -> clamped to 75%
      const moveEvent2 = new MouseEvent("mousemove", { clientY: 700 });
      window.dispatchEvent(moveEvent2);
      expect(setSidebarPaneSplitPct).toHaveBeenLastCalledWith(75);
    });

    it("cleans up on mouseup", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn() } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarPaneResize(event);

      const upEvent = new MouseEvent("mouseup");
      window.dispatchEvent(upEvent);

      expect(document.body.style.cursor).toBe("");
      expect(document.body.style.userSelect).toBe("");
    });
  });

  describe("startSidebarWidthResize", () => {
    it("sets cursor to col-resize", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientX: 400 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarWidthResize(event);

      expect(document.body.style.cursor).toBe("col-resize");
      expect(document.body.style.userSelect).toBe("none");
    });

    it("calculates width delta on mousemove", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientX: 400 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarWidthResize(event);

      // deltaX = 450 - 400 = 50, nextWidth = 300 + 50 = 350
      const moveEvent = new MouseEvent("mousemove", { clientX: 450 });
      window.dispatchEvent(moveEvent);

      expect(setSidebarWidthPx).toHaveBeenCalledWith(350);
    });

    it("clamps width to 260-min", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientX: 400 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarWidthResize(event);

      // deltaX = 300 - 400 = -100, nextWidth = 300 - 100 = 200 -> clamped to 260
      const moveEvent = new MouseEvent("mousemove", { clientX: 300 });
      window.dispatchEvent(moveEvent);

      expect(setSidebarWidthPx).toHaveBeenCalledWith(260);
    });

    it("clamps width to max (55% of innerWidth or 640)", () => {
      // Mock window.innerWidth
      Object.defineProperty(window, "innerWidth", { value: 1000, writable: true });

      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientX: 400 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarWidthResize(event);

      // 55% of 1000 = 550, which is less than 640, so max is 550
      // deltaX = 800 - 400 = 400, nextWidth = 300 + 400 = 700 -> clamped to 550
      const moveEvent = new MouseEvent("mousemove", { clientX: 800 });
      window.dispatchEvent(moveEvent);

      expect(setSidebarWidthPx).toHaveBeenCalledWith(550);
    });

    it("cleans up on mouseup", () => {
      const handlers = createSidebarResizeHandlers({
        sidebarSplitContainerRef: containerRef,
        sidebarWidthPx: 300,
        setSidebarPaneSplitPct,
        setSidebarWidthPx,
      });

      const event = { preventDefault: vi.fn(), clientX: 400 } as unknown as React.MouseEvent<HTMLDivElement>;
      handlers.startSidebarWidthResize(event);

      const upEvent = new MouseEvent("mouseup");
      window.dispatchEvent(upEvent);

      expect(document.body.style.cursor).toBe("");
      expect(document.body.style.userSelect).toBe("");
    });
  });
});
