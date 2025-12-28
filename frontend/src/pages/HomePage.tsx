import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import { clearToken, type UserInfo } from "../lib/auth";
import GlobalHeader from "../components/GlobalHeader";

type UserCaseStatus = "CORRECT" | "WRONG" | "REATTEMPT_CORRECT";

type DashboardSummary = {
    correctCount: number;
    wrongCount: number;
    reattemptCorrectCount: number;
    xp: number;
    level: number;
    streak: number;
    correctThreshold: number;
};

type DashboardCaseItem = {
    caseId: number;
    title: string;
    status: UserCaseStatus;
    lastAttemptAt: string | null;
    lastScore: number | null;
};

const STATUS_META: Record<UserCaseStatus, { label: string; color: string; bg: string }> = {
    CORRECT: { label: "Correct", color: "#22c55e", bg: "bg-green-500/15" },
    WRONG: { label: "Wrong", color: "#ef4444", bg: "bg-red-500/15" },
    REATTEMPT_CORRECT: { label: "Reattempt", color: "#f59e0b", bg: "bg-amber-500/15" },
};

export default function HomePage() {
    const navigate = useNavigate();
    const [user, setUser] = useState<UserInfo | null>(null);
    const [summary, setSummary] = useState<DashboardSummary | null>(null);
    const [selectedStatus, setSelectedStatus] = useState<UserCaseStatus>("CORRECT");
    const [cases, setCases] = useState<DashboardCaseItem[]>([]);
    const [loadingCases, setLoadingCases] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [animationProgress, setAnimationProgress] = useState(0);
    const animationFrameRef = useRef<number | null>(null);
    const hasAnimatedRef = useRef(false);
    const isAdmin = user?.role === "ADMIN";

    useEffect(() => {
        api.get<UserInfo>("/auth/me")
            .then(setUser)
            .catch((e: unknown) => handleAuthError(e));

        api.get<DashboardSummary>("/dashboard/summary")
            .then(setSummary)
            .catch((e: unknown) => handleAuthError(e));
    }, []);

    useEffect(() => {
        fetchCases(selectedStatus);
    }, [selectedStatus]);

    const statusCounts: Record<UserCaseStatus, number> = useMemo(
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

    const availableStatuses = useMemo(
        () =>
            (["CORRECT", "WRONG", "REATTEMPT_CORRECT"] as UserCaseStatus[]).filter(
                (status) => statusCounts[status] > 0
            ),
        [statusCounts]
    );

    useEffect(() => {
        if (availableStatuses.length > 0 && !availableStatuses.includes(selectedStatus)) {
            setSelectedStatus(availableStatuses[0]);
        }
    }, [availableStatuses, selectedStatus]);

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
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
            }
        };
    }, [attemptedTotal]);

    const donutSegments = useMemo(() => {
        if (attemptedTotal <= 0 || availableStatuses.length === 0) {
            return [];
        }
        let startAngle = -90; // start at top
        return availableStatuses.map((status) => {
            const value = statusCounts[status];
            const angle = (value / attemptedTotal) * 360 * animationProgress;
            const segment = {
                status,
                start: startAngle,
                end: startAngle + angle,
                value,
                meta: STATUS_META[status],
            };
            startAngle = segment.end;
            return segment;
        });
    }, [animationProgress, attemptedTotal, availableStatuses, statusCounts]);

    const totalSolved = attemptedTotal;

    const handleAuthError = (e: unknown) => {
        const apiError = e as ApiError;
        if (apiError.status === 401 || apiError.status === 403) {
            clearToken();
            navigate("/login", { replace: true });
            return;
        }
        setError(apiError?.message || "Failed to load dashboard");
    };

    const fetchCases = async (status: UserCaseStatus) => {
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

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <GlobalHeader
                title="Student Dashboard"
                subtitle="DxVision"
                isAdmin={isAdmin}
                actions={
                    <button
                        className="rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400"
                        onClick={() => navigate("/quiz/random")}
                        type="button"
                    >
                        New Problem
                    </button>
                }
            />

            <main className="grid gap-6 px-6 py-6 md:grid-cols-[320px_1fr]">
                <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
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

                <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
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
                                    <circle
                                        cx="100"
                                        cy="100"
                                        r="72"
                                        stroke="#1f2937"
                                        strokeWidth="16"
                                        fill="none"
                                        strokeDasharray={donutSegments.length === 0 ? "6 8" : undefined}
                                    />
                                    {donutSegments.map((segment) => (
                                        <path
                                            key={segment.status}
                                            d={describeArc(100, 100, 72, segment.start, segment.end)}
                                            stroke={segment.meta.color}
                                            strokeWidth={16}
                                            fill="none"
                                            strokeLinecap="round"
                                            className={`cursor-pointer transition-opacity ${
                                                selectedStatus === segment.status ? "opacity-100" : "opacity-60"
                                            }`}
                                            onClick={() => setSelectedStatus(segment.status)}
                                        />
                                    ))}
                                    <circle cx="100" cy="100" r="45" fill="#0f172a" />
                                    {donutSegments.length > 0 ? (
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

                                <div className="space-y-3">
                                    {donutSegments.length === 0 && (
                                        <div className="rounded-lg border border-slate-800 bg-slate-900/40 px-3 py-2 text-sm text-slate-400">
                                            No attempts yet.
                                        </div>
                                    )}
                                    {donutSegments.map((segment) => {
                                        const status = segment.status;
                                        const meta = STATUS_META[status];
                                        const count = statusCounts[status];
                                        return (
                                            <button
                                                key={status}
                                                className={`flex w-full items-center justify-between rounded-lg border px-3 py-2 text-left text-sm transition ${
                                                    selectedStatus === status
                                                        ? "border-teal-400 bg-slate-800/80"
                                                        : "border-slate-800 bg-slate-900/40 hover:border-slate-700"
                                                }`}
                                                onClick={() => setSelectedStatus(status)}
                                            >
                                                <div className="flex items-center gap-2">
                                                    <span
                                                        className={`h-2 w-2 rounded-full`}
                                                        style={{ backgroundColor: meta.color }}
                                                    />
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
                            <h3 className="text-base font-semibold">
                                Cases: {STATUS_META[selectedStatus].label}
                            </h3>
                            <button
                                className="text-xs text-teal-300 hover:text-teal-200"
                                onClick={() => fetchCases(selectedStatus)}
                                disabled={loadingCases}
                            >
                                Refresh
                            </button>
                        </div>

                        <div className="mt-3 space-y-2">
                            {loadingCases && <p className="text-sm text-slate-400">Loading cases...</p>}
                            {!loadingCases && cases.length === 0 && (
                                <p className="text-sm text-slate-400">No cases in this status yet.</p>
                            )}
                            {cases.map((item) => (
                                <div
                                    key={item.caseId}
                                    className="flex flex-col gap-2 rounded-lg border border-slate-800 bg-slate-900/50 p-3 md:flex-row md:items-center md:justify-between"
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
                                    >
                                        Retry
                                    </button>
                                </div>
                            ))}
                        </div>

                        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}
                    </div>
                </section>
            </main>
        </div>
    );
}

function StatPill({ label, value, accent }: { label: string; value: number; accent: string }) {
    return (
        <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-900/40 px-3 py-2">
            <div className="text-xs text-slate-400">{label}</div>
            <div className={`text-base font-semibold ${accent}`}>{value}</div>
        </div>
    );
}

// Adapted from SVG arc helper
function polarToCartesian(cx: number, cy: number, radius: number, angleInDegrees: number) {
    const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;
    return {
        x: cx + radius * Math.cos(angleInRadians),
        y: cy + radius * Math.sin(angleInRadians),
    };
}

function describeArc(cx: number, cy: number, radius: number, startAngle: number, endAngle: number) {
    const start = polarToCartesian(cx, cy, radius, endAngle);
    const end = polarToCartesian(cx, cy, radius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

    return ["M", start.x, start.y, "A", radius, radius, 0, largeArcFlag, 0, end.x, end.y].join(" ");
}
