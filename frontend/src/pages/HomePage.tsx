import { forwardRef, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import { clearToken, type UserInfo } from "../lib/auth";
import GlobalHeader from "../components/GlobalHeader";
import MonthlyActivityHeatmap from "../components/MonthlyActivityHeatmap";
import ProblemListPanel from "../components/ProblemListPanel";
import { CASE_STATUS_META, getStatusMeta, type UserCaseStatus } from "../types/case";
import type { RefObject } from "react";
import { computeSmartTooltipPosition, type TooltipPlacement } from "../lib/tooltip";

type DashboardSummary = {
    correctCount: number;
    wrongCount: number;
    reattemptCorrectCount: number;
    xp: number;
    level: number;
    streak: number;
    correctThreshold: number;
};

type AttemptedStatus = Extract<UserCaseStatus, "CORRECT" | "WRONG" | "REATTEMPT_CORRECT">;

type DashboardCaseItem = {
    caseId: number;
    title: string;
    status: AttemptedStatus;
    lastAttemptAt: string | null;
    lastScore: number | null;
};

const STATUS_META = CASE_STATUS_META;
const PROGRESS_INFO_MESSAGE =
    "A case is marked correct when final score ≥ 70.\nClick a status to view only that list or click anywhere else in progress to reset.";

export default function HomePage() {
    const navigate = useNavigate();
    const [user, setUser] = useState<UserInfo | null>(null);
    const [summary, setSummary] = useState<DashboardSummary | null>(null);
    const [activeStatus, setActiveStatus] = useState<AttemptedStatus | null>(null);
    const [cases, setCases] = useState<DashboardCaseItem[]>([]);
    const [attemptedCases, setAttemptedCases] = useState<DashboardCaseItem[]>([]);
    const [statusCases, setStatusCases] = useState<Record<AttemptedStatus, DashboardCaseItem[]>>({
        CORRECT: [],
        WRONG: [],
        REATTEMPT_CORRECT: [],
    });
    const [loadingCases, setLoadingCases] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [animationProgress, setAnimationProgress] = useState(0);
    const animationFrameRef = useRef<number | null>(null);
    const hasAnimatedRef = useRef(false);
    const progressAreaRef = useRef<HTMLDivElement | null>(null);
    const statusButtonsRef = useRef<HTMLDivElement | null>(null);
    const caseListRef = useRef<HTMLDivElement | null>(null);
    const infoTooltipRef = useRef<HTMLDivElement | null>(null);
    const infoIconRef = useRef<HTMLButtonElement | null>(null);
    const donutRef = useRef<SVGSVGElement | null>(null);
    const [infoTooltip, setInfoTooltip] = useState<{
        anchorRect: DOMRect;
        position?: { left: number; top: number };
        placement?: TooltipPlacement;
    } | null>(null);

    const isAdmin = user?.role === "ADMIN";

    const handleAuthError = (e: unknown) => {
        const apiError = e as ApiError;
        if (apiError.status === 401 || apiError.status === 403) {
            clearToken();
            navigate("/login", { replace: true });
            return;
        }
        setError(apiError?.message || "Failed to load dashboard");
    };

    useEffect(() => {
        api.get<UserInfo>("/auth/me")
            .then(setUser)
            .catch((e: unknown) => handleAuthError(e));

        api.get<DashboardSummary>("/dashboard/summary")
            .then(setSummary)
            .catch((e: unknown) => handleAuthError(e));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const statusCounts: Record<AttemptedStatus, number> = useMemo(
        () => ({
            CORRECT: summary?.correctCount ?? 0,
            WRONG: summary?.wrongCount ?? 0,
            REATTEMPT_CORRECT: summary?.reattemptCorrectCount ?? 0,
        }),
        [summary]
    );

    const attemptedTotal = useMemo(
        () => statusCounts.CORRECT + statusCounts.WRONG + statusCounts.REATTEMPT_CORRECT,
        [statusCounts]
    );

    const availableStatuses = useMemo(() => {
        const statuses: AttemptedStatus[] = ["CORRECT", "WRONG", "REATTEMPT_CORRECT"];
        return statuses.filter((status) => statusCounts[status] > 0);
    }, [statusCounts]);

    const fetchAttemptedCases = async () => {
        setLoadingCases(true);
        setError(null);
        try {
            const statuses: AttemptedStatus[] = ["CORRECT", "WRONG", "REATTEMPT_CORRECT"];
            const responses = await Promise.all(
                statuses.map(async (status) => {
                    const items = await api.get<DashboardCaseItem[]>(`/dashboard/cases?status=${status}`);
                    return { status, items };
                })
            );

            const byStatus: Record<AttemptedStatus, DashboardCaseItem[]> = {
                CORRECT: [],
                WRONG: [],
                REATTEMPT_CORRECT: [],
            };

            responses.forEach(({ status, items }) => {
                byStatus[status] = items;
            });

            const priority: AttemptedStatus[] = ["REATTEMPT_CORRECT", "CORRECT", "WRONG"];
            const deduped = new Map<number, DashboardCaseItem>();

            priority.forEach((status) => {
                byStatus[status].forEach((item) => {
                    if (!deduped.has(item.caseId)) {
                        deduped.set(item.caseId, { ...item, status });
                    }
                });
            });

            const sortedAttempted = Array.from(deduped.values()).sort((a, b) => {
                const aTime = a.lastAttemptAt ? new Date(a.lastAttemptAt).getTime() : 0;
                const bTime = b.lastAttemptAt ? new Date(b.lastAttemptAt).getTime() : 0;
                if (aTime === bTime) return b.caseId - a.caseId;
                return bTime - aTime;
            });

            setStatusCases(byStatus);
            setAttemptedCases(sortedAttempted);
            setCases(activeStatus ? byStatus[activeStatus] ?? [] : sortedAttempted);
        } catch (e) {
            handleAuthError(e);
        } finally {
            setLoadingCases(false);
        }
    };

    useEffect(() => {
        void fetchAttemptedCases();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleStatusToggle = (status: AttemptedStatus) => {
        setActiveStatus((prev) => (prev === status ? null : status));
    };

    useEffect(() => {
        if (activeStatus && statusCounts[activeStatus] === 0) {
            setActiveStatus(null);
            setCases(attemptedCases);
            return;
        }
        setCases(activeStatus ? statusCases[activeStatus] ?? [] : attemptedCases);
    }, [activeStatus, attemptedCases, statusCases, statusCounts]);

    // Animate donut once (same as you had)
    useEffect(() => {
        if (attemptedTotal <= 0) {
            setAnimationProgress(1);
            return;
        }
        if (hasAnimatedRef.current) {
            setAnimationProgress(1);
            return;
        }
        hasAnimatedRef.current = true;

        const duration = 900;
        const start = performance.now();
        const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3);

        const step = (now: number) => {
            const elapsed = now - start;
            const t = Math.min(1, elapsed / duration);
            setAnimationProgress(easeOutCubic(t));
            if (t < 1) {
                animationFrameRef.current = requestAnimationFrame(step);
            }
        };

        animationFrameRef.current = requestAnimationFrame(step);

        return () => {
            if (animationFrameRef.current) cancelAnimationFrame(animationFrameRef.current);
        };
    }, [attemptedTotal]);

    /**
     * ✅ 안정형 도넛 세그먼트 계산 (circle strokeDasharray 방식)
     * - 각 조각은 circle 하나로 표현
     * - dasharray = "segmentLength gapLength"
     * - dashoffset = 누적 길이(회전 포함)
     */
    const donutStrokeSegments = useMemo(() => {
        if (attemptedTotal <= 0 || availableStatuses.length === 0) return [];

        const radius = 72;
        const circumference = 2 * Math.PI * radius;

        // 시작점을 12시로 올리기: circle은 기본 3시에서 시작하니 -90도 회전
        // (SVG에서 회전으로 처리할 거라 여기서는 offset 계산만 0부터)
        let acc = 0;

        return availableStatuses.map((status) => {
            const value = statusCounts[status];
            const rawRatio = value / attemptedTotal;

            // 애니메이션 반영
            const ratio = rawRatio * animationProgress;

            // "정확히 100%" 안정성: ratio가 1일 때도 정상적으로 꽉 차도록 계산
            // segmentLen = circumference * ratio
            const segmentLen = circumference * ratio;

            // gapLen은 나머지
            // ratio가 1이면 gapLen = 0
            const gapLen = Math.max(0, circumference - segmentLen);

            const seg = {
                status,
                value,
                meta: STATUS_META[status],
                dasharray: `${segmentLen} ${gapLen}`,
                // 누적 오프셋: 이전 조각들 길이만큼 뒤로 밀기
                // circle의 strokeDashoffset은 "앞으로 당김" 느낌이라 (-acc) 사용
                dashoffset: -acc,
            };

            // 누적은 "애니메이션 적용된 길이" 기준으로 쌓아야 자연스러움
            acc += segmentLen;

            return seg;
        });
    }, [attemptedTotal, availableStatuses, statusCounts, animationProgress]);

    const totalSolved = attemptedTotal;

    useEffect(() => {
        const handleDocumentClick = (event: MouseEvent) => {
            const target = event.target as Node;
            const inProgress = progressAreaRef.current?.contains(target) ?? false;
            const inButtons = statusButtonsRef.current?.contains(target) ?? false;
            const inCases = caseListRef.current?.contains(target) ?? false;
            const inDonut = donutRef.current?.contains(target as Node) ?? false;
            if (inProgress && !inButtons && !inCases && !inDonut) {
                setActiveStatus(null);
            }
        };

        document.addEventListener("click", handleDocumentClick);
        return () => {
            document.removeEventListener("click", handleDocumentClick);
        };
    }, []);

    useLayoutEffect(() => {
        if (!infoTooltip || !infoTooltipRef.current) return;
        const containerRect =
            progressAreaRef.current?.getBoundingClientRect() ?? new DOMRect(0, 0, window.innerWidth, window.innerHeight);
        const tooltipRect = infoTooltipRef.current.getBoundingClientRect();
        const { left, top, placement } = computeSmartTooltipPosition({
            anchorRect: infoTooltip.anchorRect,
            tooltipRect,
            containerRect,
            offset: 8,
            preferredOrder: ["tr", "br", "tl", "bl"],
        });

        if (
            infoTooltip.position?.left !== left ||
            infoTooltip.position?.top !== top ||
            infoTooltip.placement !== placement
        ) {
            setInfoTooltip((prev) => (prev ? { ...prev, position: { left, top }, placement } : prev));
        }
    }, [infoTooltip]);

    const showInfoTooltip = () => {
        if (!infoIconRef.current) return;
        setInfoTooltip({ anchorRect: infoIconRef.current.getBoundingClientRect() });
    };

    const hideInfoTooltip = () => setInfoTooltip(null);

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <GlobalHeader
                subtitle="Student Dashboard"
                isAdmin={isAdmin}
                user={user}
                onUserChange={setUser}
            />

            <main className="flex flex-col gap-6 px-6 py-6">
                <div className="h-px w-full bg-slate-800/70" />
                <section
                    ref={progressAreaRef}
                    className="relative rounded-xl border border-slate-800 bg-slate-950/60 p-5 shadow-lg shadow-black/10"
                >
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center gap-2">
                            <h2 className="text-lg font-semibold">Progress</h2>
                            <div className="relative">
                                <button
                                    ref={infoIconRef}
                                    className="flex h-6 w-6 items-center justify-center rounded-full border border-slate-700 bg-slate-900 text-[11px] font-bold text-slate-200 transition hover:border-teal-400 hover:text-teal-200 focus:outline-none focus:ring-2 focus:ring-teal-400/50"
                                    aria-label="Progress info"
                                    onMouseEnter={showInfoTooltip}
                                    onMouseLeave={hideInfoTooltip}
                                    onFocus={showInfoTooltip}
                                    onBlur={hideInfoTooltip}
                                    type="button"
                                >
                                    i
                                </button>
                            </div>
                        </div>
                        {/*<div className="flex flex-wrap items-center gap-2">*/}
                        {/*    <StatPill label="Level" value={summary?.level ?? 1} accent="text-teal-200" />*/}
                        {/*    <StatPill label="XP" value={summary?.xp ?? 0} accent="text-amber-200" />*/}
                        {/*    <StatPill label="Current streak" value={summary?.streak ?? 0} accent="text-blue-200" />*/}
                        {/*</div>*/}
                    </div>

                    {infoTooltip && (
                        <div
                            ref={infoTooltipRef}
                            className="pointer-events-none absolute z-30 w-80 rounded-md border border-slate-800 bg-slate-900 px-3 py-2 text-xs text-slate-100 shadow-xl"
                            style={{
                                left: infoTooltip.position?.left ?? 0,
                                top: infoTooltip.position?.top ?? 0,
                            }}
                        >
                            <p className="whitespace-pre-line leading-relaxed">{PROGRESS_INFO_MESSAGE}</p>
                        </div>
                    )}

                    <div className="mt-5 grid gap-4 xl:grid-cols-2">
                        <div className="space-y-4">
                            <div className="rounded-lg bg-slate-950/30 p-4">
                                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                                    <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-6">
                                        <svg
                                            ref={donutRef}
                                            width="200"
                                            height="200"
                                            viewBox="0 0 200 200"
                                            className="shrink-0"
                                        >
                                            <circle
                                                cx="100"
                                                cy="100"
                                                r="72"
                                                stroke="rgba(148, 163, 184, 0.18)"
                                                strokeWidth="16"
                                                fill="none"
                                                strokeDasharray={donutStrokeSegments.length === 0 ? "6 8" : undefined}
                                            />

                                            <g transform="rotate(-90 100 100)">
                                                {donutStrokeSegments.map((seg) => (
                                                    <circle
                                                        key={seg.status}
                                                        cx="100"
                                                        cy="100"
                                                        r="72"
                                                        fill="none"
                                                        stroke={seg.meta.color}
                                                        strokeWidth="16"
                                                        strokeLinecap="round"
                                                        strokeDasharray={seg.dasharray}
                                                        strokeDashoffset={seg.dashoffset}
                                                        className={`cursor-pointer transition-opacity ${
                                                            activeStatus === seg.status ? "opacity-100" : "opacity-60"
                                                        }`}
                                                        role="button"
                                                        tabIndex={0}
                                                        onClick={() => handleStatusToggle(seg.status)}
                                                        onKeyDown={(e) => {
                                                            if (e.key === "Enter" || e.key === " ") {
                                                                e.preventDefault();
                                                                handleStatusToggle(seg.status);
                                                            }
                                                        }}
                                                    />
                                                ))}
                                            </g>

                                            <circle cx="100" cy="100" r="45" fill="#0f172a" />

                                            {donutStrokeSegments.length > 0 ? (
                                                <>
                                                    <text
                                                        x="100"
                                                        y="95"
                                                        textAnchor="middle"
                                                        className="fill-slate-200 text-xl font-bold"
                                                    >
                                                        {summary ? summary.correctCount + summary.reattemptCorrectCount : 0}
                                                    </text>
                                                    <text x="100" y="115" textAnchor="middle" className="fill-slate-400 text-xs">
                                                        mastered
                                                    </text>
                                                </>
                                            ) : (
                                                <text
                                                    x="100"
                                                    y="105"
                                                    textAnchor="middle"
                                                    className="fill-slate-400 text-xs uppercase tracking-wide"
                                                >
                                                    No attempts yet
                                                </text>
                                            )}
                                        </svg>

                                        <div className="space-y-3 text-sm text-slate-400">
                                            <div className="w-44 rounded-lg bg-slate-950/50 px-3 py-2 text-center">
                                                <p className="text-[11px] uppercase tracking-wide text-slate-400">Total attempts</p>
                                                <span className="text-lg font-semibold text-slate-100">{totalSolved}</span>
                                            </div>
                                            <StatusButtons
                                                ref={statusButtonsRef}
                                                activeStatus={activeStatus}
                                                onSelect={handleStatusToggle}
                                                statusCounts={statusCounts}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <MonthlyActivityHeatmap days={365} title="Streak" />
                        </div>

                        <CaseListPanel
                            panelRef={caseListRef}
                            cases={cases}
                            error={error}
                            loading={loadingCases}
                            activeStatus={activeStatus}
                            onRefresh={() => fetchAttemptedCases()}
                            onSelectCase={(caseId) => navigate(`/quiz/${caseId}`)}
                        />
                    </div>
                </section>

                <div>
                    <ProblemListPanel className="max-h-[820px]" />
                </div>
            </main>
        </div>
    );
}

const StatusButtons = forwardRef<HTMLDivElement, {
    activeStatus: AttemptedStatus | null;
    onSelect: (status: AttemptedStatus) => void;
    statusCounts: Record<AttemptedStatus, number>;
}>(function StatusButtonsInner({ activeStatus, onSelect, statusCounts }, ref) {
    const statuses: AttemptedStatus[] = ["CORRECT", "WRONG", "REATTEMPT_CORRECT"];

    return (
        <div ref={ref} className="flex flex-col gap-2">
            {statuses.map((status) => {
                const meta = STATUS_META[status];
                const active = activeStatus === status;
                const disabled = statusCounts[status] === 0;
                return (
                    <button
                        key={status}
                        className={`w-40 rounded-md border px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-400 ${
                            active
                                ? "border-teal-400 bg-teal-500/20 text-teal-100 shadow-[0_0_0_1px_rgba(45,212,191,0.25)]"
                                : "border-slate-800 bg-slate-900/70 text-slate-200 hover:border-slate-700"
                        } ${disabled ? "cursor-not-allowed opacity-60" : ""}`}
                        onClick={() => !disabled && onSelect(status)}
                        type="button"
                        disabled={disabled}
                    >
                        <div className="flex items-center justify-between">
                            <span className="flex items-center gap-2">
                                <span className="h-2 w-2 rounded-full" style={{ backgroundColor: meta.color }} />
                                {meta.label}
                            </span>
                            <span className="rounded bg-slate-800 px-1.5 py-0.5 text-[10px] text-slate-100">{statusCounts[status]}</span>
                        </div>
                    </button>
                );
            })}
        </div>
    );
});

function CaseListPanel({
    cases,
    loading,
    error,
    activeStatus,
    onRefresh,
    onSelectCase,
    panelRef,
}: {
    cases: DashboardCaseItem[];
    loading: boolean;
    error: string | null;
    activeStatus: AttemptedStatus | null;
    onRefresh: () => void;
    onSelectCase: (caseId: number) => void;
    panelRef?: RefObject<HTMLDivElement | null>;
}) {
    const headerLabel = activeStatus ? STATUS_META[activeStatus].label : "Attempted";

    return (
        <div
            ref={panelRef}
            className="flex h-full min-h-[420px] flex-col rounded-lg border border-slate-800 bg-slate-950/60 p-4"
        >
            <div className="flex items-center justify-between gap-3">
                <div>
                    <h3 className="text-base font-semibold">Cases: {headerLabel}</h3>
                    <p className="text-xs text-slate-400">
                        Showing {activeStatus ? headerLabel.toLowerCase() : "all attempted"} cases. Click outside buttons to reset.
                    </p>
                </div>
                <button
                    className="text-xs text-teal-300 hover:text-teal-200"
                    onClick={onRefresh}
                    disabled={loading}
                    type="button"
                >
                    Refresh
                </button>
            </div>

            <div className="mt-3 flex-1 space-y-2 overflow-hidden">
                {loading && (
                    <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-3 py-2 text-sm text-slate-400">
                        Loading cases...
                    </div>
                )}
                {error && (
                    <div className="rounded-lg border border-red-500/50 bg-red-500/10 px-3 py-2 text-xs text-red-200">{error}</div>
                )}
                {!loading && !error && cases.length === 0 && (
                    <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-3 py-2 text-sm text-slate-400">
                        No cases in this status yet.
                    </div>
                )}

                <div className="scrollbar-hide space-y-2 overflow-y-auto pr-1" style={{ maxHeight: "450px" }}>
                    {cases.map((item) => {
                        const meta = getStatusMeta(item.status);
                        return (
                            <button
                                key={item.caseId}
                                className="flex w-full flex-col gap-2 rounded-lg border border-slate-800 bg-slate-950/50 p-3 text-left transition hover:border-teal-400 hover:bg-slate-900/60"
                                onClick={() => onSelectCase(item.caseId)}
                                type="button"
                            >
                                <div className="flex items-center justify-between">
                                    <p className="font-semibold">
                                        Case #{item.caseId} - {item.title}
                                    </p>
                                    <span
                                        className={`rounded px-2 py-0.5 text-[11px] uppercase tracking-wide ${meta.bg} ${meta.textClass}`}
                                        style={{ color: meta.color }}
                                    >
                                        {meta.label}
                                    </span>
                                </div>
                                <p className="text-xs text-slate-400">
                                    Last score: {item.lastScore?.toFixed(1) ?? "N/A"} ·{" "}
                                    {item.lastAttemptAt ? new Date(item.lastAttemptAt).toLocaleString() : "No attempts"}
                                </p>
                            </button>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}
