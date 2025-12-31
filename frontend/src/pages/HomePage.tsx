import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import { clearToken, type UserInfo } from "../lib/auth";
import GlobalHeader from "../components/GlobalHeader";
import MonthlyActivityHeatmap from "../components/MonthlyActivityHeatmap";
import ProblemListPanel from "../components/ProblemListPanel";
import { CASE_STATUS_META, type UserCaseStatus } from "../types/case";

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

export default function HomePage() {
    const navigate = useNavigate();
    const [user, setUser] = useState<UserInfo | null>(null);
    const [summary, setSummary] = useState<DashboardSummary | null>(null);
    const [selectedStatus, setSelectedStatus] = useState<AttemptedStatus>("CORRECT");
    const [cases, setCases] = useState<DashboardCaseItem[]>([]);
    const [loadingCases, setLoadingCases] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [animationProgress, setAnimationProgress] = useState(0);
    const animationFrameRef = useRef<number | null>(null);
    const hasAnimatedRef = useRef(false);

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

    useEffect(() => {
        if (availableStatuses.length > 0 && !availableStatuses.includes(selectedStatus)) {
            setSelectedStatus(availableStatuses[0]);
        }
    }, [availableStatuses, selectedStatus]);

    const fetchCases = async (status: AttemptedStatus) => {
        setLoadingCases(true);
        setError(null);
        try {
            const items = await api.get<DashboardCaseItem[]>(`/dashboard/cases?status=${status}`);
            setCases(items);
        } catch (e) {
            handleAuthError(e);
        } finally {
            setLoadingCases(false);
        }
    };

    useEffect(() => {
        fetchCases(selectedStatus);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedStatus]);

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

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <GlobalHeader
                subtitle="Student Dashboard"
                isAdmin={isAdmin}
            />

            <main className="grid grid-cols-1 gap-6 px-6 py-6 md:grid-cols-2 xl:grid-cols-3">
                <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-5 shadow-lg shadow-black/10">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-xs text-slate-400">Signed in as</p>
                            <p className="text-lg font-semibold">{user?.name ?? "Loading..."}</p>
                            <p className="text-xs text-slate-400">{user?.email}</p>
                        </div>
                        <span className="rounded-full bg-slate-800 px-3 py-1 text-xs font-semibold text-teal-200">
                            {user?.role ?? "STUDENT"}
                        </span>
                    </div>

                    <div className="mt-6 space-y-3">
                        <StatPill label="Level" value={summary?.level ?? 1} accent="text-teal-200" />
                        <StatPill label="XP" value={summary?.xp ?? 0} accent="text-amber-200" />
                        <StatPill label="Current streak" value={summary?.streak ?? 0} accent="text-blue-200" />
                        <p className="text-xs text-slate-500">
                            A case is marked correct when final score ≥ {summary?.correctThreshold ?? 70}.
                        </p>
                    </div>
                </section>

                <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-5 shadow-lg shadow-black/10">
                    <div className="flex flex-col gap-4 lg:flex-row">
                        <div className="flex-1">
                            <div className="flex items-center justify-between">
                                <div>
                                    <h2 className="text-lg font-semibold">Progress</h2>
                                    <p className="text-xs text-slate-400">Click a slice to filter cases</p>
                                </div>
                                <div className="text-sm text-slate-400">
                                    Total attempts: <span className="text-slate-100">{totalSolved}</span>
                                </div>
                            </div>

                            <div className="mt-4 flex items-center gap-6">
                                <svg width="200" height="200" viewBox="0 0 200 200" className="shrink-0">
                                    {/* base ring (transparent) */}
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
                                                strokeLinecap="round" // ✅ rounded ends
                                                strokeDasharray={seg.dasharray}
                                                strokeDashoffset={seg.dashoffset}
                                                className={`cursor-pointer transition-opacity ${
                                                    selectedStatus === seg.status ? "opacity-100" : "opacity-60"
                                                }`}
                                                onClick={() => setSelectedStatus(seg.status)}
                                            />
                                        ))}
                                    </g>

                                    <circle cx="100" cy="100" r="45" fill="#0f172a" />

                                    {donutStrokeSegments.length > 0 ? (
                                        <>
                                            <text x="100" y="95" textAnchor="middle" className="fill-slate-200 text-xl font-bold">
                                                {summary ? summary.correctCount + summary.reattemptCorrectCount : 0}
                                            </text>
                                            <text x="100" y="115" textAnchor="middle" className="fill-slate-400 text-xs">
                                                mastered
                                            </text>
                                        </>
                                    ) : (
                                        <text x="100" y="105" textAnchor="middle" className="fill-slate-400 text-xs uppercase tracking-wide">
                                            No attempts yet
                                        </text>
                                    )}
                                </svg>

                                <div className="space-y-3">
                                    {donutStrokeSegments.length === 0 && (
                                        <div className="rounded-lg border border-slate-800 bg-slate-900/40 px-3 py-2 text-sm text-slate-400">
                                            No attempts yet.
                                        </div>
                                    )}

                                    {donutStrokeSegments.map((seg) => {
                                        const status = seg.status;
                                        const meta = STATUS_META[status];
                                        const count = statusCounts[status];
                                        return (
                                            <button
                                                key={status}
                                                className={`flex w-full items-center justify-between rounded-lg border px-3 py-2 text-left text-sm transition ${
                                                    selectedStatus === status
                                                        ? "border-teal-400 bg-slate-800/80"
                                                        : "border-slate-800 bg-slate-950/40 hover:border-slate-700"
                                                }`}
                                                onClick={() => setSelectedStatus(status)}
                                                type="button"
                                            >
                                                <div className="flex items-center gap-2">
                                                    <span className="h-2 w-2 rounded-full" style={{ backgroundColor: meta.color }} />
                                                    <span className="font-semibold">{meta.label}</span>
                                                </div>
                                                <span className="text-slate-300">{count}</span>
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="mt-6">
                        <div className="flex items-center justify-between">
                            <h3 className="text-base font-semibold">Cases: {STATUS_META[selectedStatus].label}</h3>
                            <button
                                className="text-xs text-teal-300 hover:text-teal-200"
                                onClick={() => fetchCases(selectedStatus)}
                                disabled={loadingCases}
                                type="button"
                            >
                                Refresh
                            </button>
                        </div>

                        <div className="mt-3 space-y-2">
                            {loadingCases && <p className="text-sm text-slate-400">Loading cases...</p>}
                            {!loadingCases && cases.length === 0 && <p className="text-sm text-slate-400">No cases in this status yet.</p>}

                            {cases.map((item) => (
                                <div
                                    key={item.caseId}
                                    className="flex flex-col gap-2 rounded-lg border border-slate-800 bg-slate-950/50 p-3 md:flex-row md:items-center md:justify-between"
                                >
                                    <div>
                                        <p className="font-semibold">
                                            Case #{item.caseId} - {item.title}
                                        </p>
                                        <p className="text-xs text-slate-400">
                                            Last score: {item.lastScore?.toFixed(1) ?? "N/A"} ·{" "}
                                            {item.lastAttemptAt ? new Date(item.lastAttemptAt).toLocaleString() : "No attempts"}
                                        </p>
                                    </div>
                                    <button
                                        className="w-full rounded-lg bg-slate-800 px-3 py-2 text-sm font-semibold text-teal-200 hover:bg-slate-700 md:w-auto"
                                        onClick={() => navigate(`/quiz/${item.caseId}`)}
                                        type="button"
                                    >
                                        Retry
                                    </button>
                                </div>
                            ))}
                        </div>

                        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}
                    </div>
                </section>

                <MonthlyActivityHeatmap days={30} />

                <div className="col-span-1 md:col-span-2 xl:col-span-3">
                    <ProblemListPanel className="max-h-[820px]" />
                </div>
            </main>
        </div>
    );
}

function StatPill({ label, value, accent }: { label: string; value: number; accent: string }) {
    return (
        <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
            <div className="text-xs text-slate-400">{label}</div>
            <div className={`text-base font-semibold ${accent}`}>{value}</div>
        </div>
    );
}
