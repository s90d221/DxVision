export type TooltipPlacement = "br" | "bl" | "tr" | "tl";

type TooltipPositionArgs = {
    anchorRect: DOMRect;
    tooltipRect: DOMRect;
    containerRect?: DOMRect;
    offset?: number;
    preferredOrder?: TooltipPlacement[];
};

export function computeSmartTooltipPosition({
    anchorRect,
    tooltipRect,
    containerRect,
    offset = 10,
    preferredOrder,
}: TooltipPositionArgs): { left: number; top: number; placement: TooltipPlacement } {
    const fallbackPlacement: TooltipPlacement = preferredOrder?.[0] ?? "br";
    const container = containerRect ?? new DOMRect(0, 0, window.innerWidth, window.innerHeight);

    const positions: Record<TooltipPlacement, { left: number; top: number }> = {
        br: {
            left: anchorRect.left - container.left + anchorRect.width + offset,
            top: anchorRect.top - container.top + anchorRect.height + offset,
        },
        bl: {
            left: anchorRect.left - container.left - tooltipRect.width - offset,
            top: anchorRect.top - container.top + anchorRect.height + offset,
        },
        tr: {
            left: anchorRect.left - container.left + anchorRect.width + offset,
            top: anchorRect.top - container.top - tooltipRect.height - offset,
        },
        tl: {
            left: anchorRect.left - container.left - tooltipRect.width - offset,
            top: anchorRect.top - container.top - tooltipRect.height - offset,
        },
    };

    const fits = (pos: { left: number; top: number }) =>
        pos.left >= 0 &&
        pos.left + tooltipRect.width <= container.width &&
        pos.top >= 0 &&
        pos.top + tooltipRect.height <= container.height;

    const priority = (preferredOrder && preferredOrder.length > 0
        ? preferredOrder
        : ([fallbackPlacement, "br", "tr", "bl", "tl"] as TooltipPlacement[])
    ).reduce<TooltipPlacement[]>((acc, place) => {
        if (!acc.includes(place)) acc.push(place);
        return acc;
    }, []);

    let placement: TooltipPlacement = fallbackPlacement;
    let position = positions[fallbackPlacement];

    for (const place of priority) {
        if (fits(positions[place])) {
            placement = place;
            position = positions[place];
            break;
        }
    }

    if (!fits(position)) {
        position = {
            left: Math.min(Math.max(position.left, 0), container.width - tooltipRect.width),
            top: Math.min(Math.max(position.top, 0), container.height - tooltipRect.height),
        };
    }

    return { ...position, placement };
}
