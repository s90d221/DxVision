import { useEffect, useLayoutEffect, useMemo, useRef, useState, type MouseEvent } from "react";
import { ApiError, api } from "../lib/api";

type DashboardActivityDay = { date: string; solvedCount: number };
type DashboardActivityResponse = {
    days: DashboardActivityDay[];
    totalSolved: number;
    streak: number;
};

type GridCell = {
    date: string;
    solvedCount: number;
    isPlaceholder: boolean;
};

const CELL_SIZE = 12;
const CELL_GAP = 4;
const WEEK_COLUMN_WIDTH = CELL_SIZE + CELL_GAP;

const COLOR_SCALE = [
    "bg-slate-900 border border-slate-800/60",
    "bg-emerald-900/70 border border-emerald-800/50",
    "bg-emerald-800/80 border border-emerald-700/60",
    "bg-emerald-700/90 border border-emerald-600/60",
    "bg-emerald-500 border border-emerald-300/80",
];

export default function MonthlyActivityHeatmap({
    days = 365,
    title = "Recent activity",
    className,
    variant = "card",
}: {
    days?: number;
    title?: string;
    className?: string;
    variant?: "card" | "minimal";
}) {
    const [activity, setActivity] = useState<DashboardActivityResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [tooltip, setTooltip] = useState<{
        date: string;
        solvedCount: number;
        pointer: { x: number; y: number };
        position?: { left: number; top: number };
    } | null>(null);
    const gridRef = useRef<HTMLDivElement>(null);
    const tooltipRef = useRef<HTMLDivElement>(null);
    const todayKey = useMemo(() => new Date().toISOString().slice(0, 10), []);

    const maxSolved = useMemo(() => {
        if (!activity?.days?.length) return 0;
        return activity.days.reduce((max, day) => Math.max(max, day.solvedCount), 0);
    }, [activity]);

    const weeks = useMemo(() => buildWeeks(activity?.days ?? []), [activity]);
    const monthLabels = useMemo(() => buildMonthLabels(weeks), [weeks]);

    useEffect(() => {
        void fetchActivity();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [days]);

    const fetchActivity = async () => {
        setLoading(true);
        setError(null);
        setTooltip(null);
        try {
            const response = await api.get<DashboardActivityResponse>(`/dashboard/activity?days=${days}`);
            setActivity(response);
        } catch (e: unknown) {
            const apiError = e as ApiError;
            setError(apiError?.message ?? "Failed to load activity");
            setActivity(null);
        } finally {
            setLoading(false);
        }
    };

    const intensityLevel = (count: number) => {
        if (maxSolved <= 0 || count <= 0) return 0;
        const ratio = count / maxSolved;
        return Math.min(4, Math.ceil(ratio * 4));
    };

    const handleHover = (event: MouseEvent<HTMLDivElement>, cell: GridCell) => {
        if (cell.isPlaceholder || cell.solvedCount < 0) {
            setTooltip(null);
            return;
        }
        setTooltip({
            date: cell.date,
            solvedCount: cell.solvedCount,
            pointer: { x: event.clientX, y: event.clientY },
        });
    };

    const handlePointerMove = (event: MouseEvent<HTMLDivElement>) => {
        setTooltip((prev) => (prev ? { ...prev, pointer: { x: event.clientX, y: event.clientY } } : prev));
    };

    useLayoutEffect(() => {
        if (!tooltip || !tooltipRef.current) return;

        const tooltipRect = tooltipRef.current.getBoundingClientRect();
        const padding = 8;
        const offset = 14;
        let left = tooltip.pointer.x + offset;
        let top = tooltip.pointer.y + offset;

        if (left + tooltipRect.width + padding > window.innerWidth) {
            left = tooltip.pointer.x - tooltipRect.width - offset;
        }
        if (top + tooltipRect.height + padding > window.innerHeight) {
            top = tooltip.pointer.y - tooltipRect.height - offset;
        }

        left = Math.max(padding, Math.min(left, window.innerWidth - tooltipRect.width - padding));
        top = Math.max(padding, Math.min(top, window.innerHeight - tooltipRect.height - padding));

        if (tooltip.position?.left !== left || tooltip.position?.top !== top) {
            setTooltip((prev) => (prev ? { ...prev, position: { left, top } } : prev));
        }
    }, [tooltip]);

    useEffect(() => {
        if (!gridRef.current) return;
        const container = gridRef.current;
        const todayCell = container.querySelector<HTMLElement>(`[data-date="${todayKey}"]`);
        if (todayCell) {
            const targetLeft = todayCell.offsetLeft - container.clientWidth / 2 + todayCell.clientWidth / 2;
            container.scrollTo({ left: Math.max(0, targetLeft), behavior: "smooth" });
        }
    }, [weeks, todayKey]);

    return (
        <section
            className={`flex flex-col rounded-xl ${variant === "minimal" ? "bg-slate-950/20 p-4" : "border border-slate-800 bg-slate-950/60 p-5 shadow-lg shadow-black/10"} ${className ?? ""}`}
        >
            <div className="flex items-start justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold">{title}</h2>
                    <p className="text-xs text-slate-400">
                        Recent {days} days · counted per correct attempt (final score ≥ 70)
                    </p>
                </div>
            </div>

            {loading && (
                <div className="mt-6 space-y-3">
                    <div className="h-40 animate-pulse rounded-lg bg-slate-900/60" />
                    <div className="h-4 w-32 animate-pulse rounded bg-slate-900/60" />
                </div>
            )}

            {!loading && error && (
                <div className="mt-6 rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-200">
                    {error}
                    <button
                        className="ml-3 text-xs font-semibold text-teal-200 underline underline-offset-2 hover:text-teal-100"
                        onClick={() => fetchActivity()}
                        type="button"
                    >
                        Retry
                    </button>
                </div>
            )}

            {!loading && !error && (
                <div className="mt-5 space-y-4">
                    {weeks.length === 0 && (
                        <div className="rounded-lg border border-slate-800 bg-slate-900/60 px-3 py-3 text-sm text-slate-400">
                            No attempts in the last {days} days.
                        </div>
                    )}

                    {weeks.length > 0 && (
                        <div
                            ref={gridRef}
                            className="relative max-w-full overflow-x-auto overflow-y-hidden pb-3 scrollbar-hide"
                            onMouseLeave={() => setTooltip(null)}
                            onMouseMove={handlePointerMove}
                        >
                            <div className="inline-block">
                                {monthLabels.length > 0 && (
                                    <div
                                        className="mb-2 grid select-none items-center text-[10px] font-semibold uppercase tracking-wide text-slate-400"
                                        style={{
                                            gridTemplateColumns: `repeat(${weeks.length}, ${WEEK_COLUMN_WIDTH}px)`,
                                            columnGap: `${CELL_GAP}px`,
                                        }}
                                    >
                                        {weeks.map((_, idx) => {
                                            const label = monthLabels.find((m) => m.index === idx)?.label ?? "";
                                            return (
                                                <div key={`label-${idx}`} className="text-center">
                                                    {label}
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}
                                <div
                                    className="grid justify-items-center"
                                    style={{
                                        gridTemplateColumns: `repeat(${weeks.length}, ${WEEK_COLUMN_WIDTH}px)`,
                                        gap: `${CELL_GAP}px`,
                                    }}
                                >
                                    {weeks.map((week, weekIdx) => (
                                        <div
                                            key={`week-${weekIdx}`}
                                            className="grid justify-items-center"
                                            style={{
                                                gridTemplateRows: `repeat(7, ${CELL_SIZE}px)`,
                                                gap: `${CELL_GAP}px`,
                                            }}
                                        >
                                            {week.map((cell, dayIdx) => {
                                                const level = cell.isPlaceholder ? 0 : intensityLevel(cell.solvedCount);
                                                const colorClass = cell.isPlaceholder ? COLOR_SCALE[0] : COLOR_SCALE[level];
                                                return (
                                                    <div
                                                        key={`${cell.date}-${dayIdx}`}
                                                        className={`rounded-md transition hover:scale-105 hover:ring-2 hover:ring-emerald-400/60 ${colorClass}`}
                                                        style={{ width: CELL_SIZE, height: CELL_SIZE }}
                                                        data-date={cell.isPlaceholder ? undefined : cell.date}
                                                        onMouseEnter={(event) => handleHover(event, cell)}
                                                        onMouseMove={(event) => handleHover(event, cell)}
                                                        title={`${cell.date} · Correct: ${cell.solvedCount}`}
                                                    />
                                                );
                                            })}
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {tooltip && (
                                <div
                                    ref={tooltipRef}
                                    className="pointer-events-none fixed z-30 rounded-lg border border-slate-800 bg-slate-900/95 px-3 py-2 text-xs text-slate-100 shadow-xl"
                                    style={{
                                        left: tooltip.position?.left ?? 0,
                                        top: tooltip.position?.top ?? 0,
                                    }}
                                >
                                    <div className="font-semibold">{tooltip.date}</div>
                                    <div className="text-slate-200">Correct: {tooltip.solvedCount}</div>
                                </div>
                            )}
                        </div>
                    )}

                    <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-400">
                        <div className="flex items-center gap-2">
                            <span>Less</span>
                            <div className="flex items-center gap-1">
                                {COLOR_SCALE.map((className, idx) => (
                                    <div key={className} className={`h-3 w-3 rounded-sm ${className} ${idx === 0 ? "border" : ""}`} />
                                ))}
                            </div>
                            <span>More</span>
                        </div>
                        <div className="ml-auto flex items-center gap-3 text-xs text-slate-300">
                            <span className="font-semibold text-slate-100">Total correct: {activity?.totalSolved ?? 0}</span>
                            <span>|</span>
                            <span>Streak: {activity?.streak ?? 0} days</span>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}

function buildWeeks(days: DashboardActivityDay[]): GridCell[][] {
    if (!days.length) return [];

    const sorted = [...days].sort((a, b) => a.date.localeCompare(b.date));
    const dayMap = new Map(sorted.map((day) => [day.date, day.solvedCount]));
    const start = parseDateKey(sorted[0].date);
    const end = parseDateKey(sorted[sorted.length - 1].date);

    const paddedStart = addDays(start, -start.getUTCDay());
    const paddedEnd = addDays(end, 6 - end.getUTCDay());

    const weeks: GridCell[][] = [];
    let cursor = paddedStart;
    let currentWeek: GridCell[] = [];

    while (cursor.getTime() <= paddedEnd.getTime()) {
        const key = formatDateKey(cursor);
        const isWithinRange = cursor.getTime() >= start.getTime() && cursor.getTime() <= end.getTime();
        currentWeek.push({
            date: key,
            solvedCount: isWithinRange ? dayMap.get(key) ?? 0 : 0,
            isPlaceholder: !isWithinRange,
        });

        if (currentWeek.length === 7) {
            weeks.push(currentWeek);
            currentWeek = [];
        }

        cursor = addDays(cursor, 1);
    }

    if (currentWeek.length > 0) {
        weeks.push(currentWeek);
    }

    return weeks;
}

function parseDateKey(dateStr: string): Date {
    const [year, month, day] = dateStr.split("-").map(Number);
    return new Date(Date.UTC(year, (month ?? 1) - 1, day ?? 1));
}

function formatDateKey(date: Date): string {
    return date.toISOString().slice(0, 10);
}

function addDays(date: Date, days: number): Date {
    const next = new Date(date);
    next.setUTCDate(next.getUTCDate() + days);
    return next;
}

function buildMonthLabels(weeks: GridCell[][]): { label: string; index: number }[] {
    const labels: { label: string; index: number }[] = [];
    weeks.forEach((week, index) => {
        const firstActiveDay = week.find((cell) => !cell.isPlaceholder) ?? week[0];
        const parsed = parseDateKey(firstActiveDay.date);
        const month = parsed.getUTCMonth();
        const lastLabel = labels[labels.length - 1];
        if (!lastLabel || month !== parseDateKey(weeks[lastLabel.index][0].date).getUTCMonth()) {
            labels.push({ label: parsed.toLocaleString("en-US", { month: "short" }), index });
        }
    });
    return labels;
}
