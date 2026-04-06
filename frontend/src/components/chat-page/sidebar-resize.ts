import type { MutableRefObject, MouseEvent as ReactMouseEvent } from 'react';

type CreateSidebarResizeHandlersParams = {
  sidebarSplitContainerRef: MutableRefObject<HTMLDivElement | null>;
  sidebarWidthPx: number;
  setSidebarPaneSplitPct: (value: number) => void;
  setSidebarWidthPx: (value: number) => void;
};

export function createSidebarResizeHandlers({
  sidebarSplitContainerRef,
  sidebarWidthPx,
  setSidebarPaneSplitPct,
  setSidebarWidthPx,
}: CreateSidebarResizeHandlersParams) {
  const startSidebarPaneResize = (event: ReactMouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    const container = sidebarSplitContainerRef.current;
    if (!container) return;

    const rect = container.getBoundingClientRect();
    document.body.style.cursor = 'row-resize';
    document.body.style.userSelect = 'none';

    const onMove = (moveEvent: MouseEvent) => {
      const deltaY = moveEvent.clientY - rect.top;
      const nextPct = Math.min(75, Math.max(25, (deltaY / rect.height) * 100));
      setSidebarPaneSplitPct(nextPct);
    };

    const onUp = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  };

  const startSidebarWidthResize = (event: ReactMouseEvent<HTMLDivElement>) => {
    event.preventDefault();

    const startX = event.clientX;
    const startWidth = sidebarWidthPx;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';

    const onMove = (moveEvent: MouseEvent) => {
      const deltaX = moveEvent.clientX - startX;
      const maxWidth = Math.min(640, Math.floor(window.innerWidth * 0.55));
      const nextWidth = Math.min(maxWidth, Math.max(260, startWidth + deltaX));
      setSidebarWidthPx(nextWidth);
    };

    const onUp = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  };

  return { startSidebarPaneResize, startSidebarWidthResize };
}
