import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";
import { type UserCaseStatus, getStatusMeta } from "../../types/case";

type UserStatus = "ACTIVE" | "DISABLED";

type AdminUserStats = {
    attemptedCount: number;
    correctCount: number;
    wrongCount: number;
    reattemptCorrectCount: number;
    lastActiveAt: string | null;
};

type AdminUserCaseProgress = {
    caseId: number;
    caseTitle: string;
    status: UserCaseStatus;
    attemptCount: number;
    lastAttemptAt: string | null;
};

type AdminUserActivity = {
    timestamp: string;
    caseId: number;
    caseTitle: string;
    status: UserCaseStatus;
    score: number;
};

type AdminUserDetail = {
    id: number;
    email: string;
    name: string;
    role: "ADMIN" | "USER";
    status: UserStatus;
    createdAt: string;
    stats: AdminUserStats;
    caseProgress: AdminUserCaseProgress[];
    recentActivities: AdminUserActivity[];
};

const USER_STATUS_META: Record<UserStatus, string> = {
    ACTIVE: "bg-emerald-500/20 text-emerald-200 border-emerald-500/50",
    DISABLED: "bg-red-500/20 text-red-200 border-red-500/50",
};

export default function AdminUserDetailPage() {
    const { userId } = useParams();
    const [data, setData] = useState<AdminUserDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    const loadUser = async () => {
        if (!userId) return;
        setLoading(true);
        setError(null);
        try {
            const detail = await api.get<AdminUserDetail>(`/admin/users/${userId}`);
            setData(detail);
        } catch (err: any) {
            setError(err?.message || "Failed to load user");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadUser();
    }, [userId]);

    const formatDateTime = (value: string | null | undefined) => {
        if (!value) return "—";
        return new Date(value).toLocaleString();
    };

    const handleDeactivate = async () => {
        if (!userId || !data || data.status === "DISABLED") return;
        if (!confirm("Deactivate this user? They will be unable to log in.")) return;

        setSaving(true);
        setError(null);
        try {
            const updated = await api.patch<AdminUserDetail>(`/admin/users/${userId}`, { status: "DISABLED" });
            setData(updated);
        } catch (err: any) {
            setError(err?.message || "Failed to update user");
        } finally {
            setSaving(false);
        }
    };

    return (
        <AdminLayout title="User Management" description="Review user progress and recent activity.">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <div className="space-y-1">
                    <div className="text-lg font-semibold text-teal-200">User Detail</div>
                    <div className="text-sm text-slate-400">
                        Progress by case, recent activity log, and account controls.
                    </div>
                </div>
                <div className="flex gap-2 text-sm">
                    <Link
                        to="/admin/users"
                        className="rounded border border-slate-700 px-3 py-2 font-semibold hover:border-teal-400 hover:text-teal-200"
                    >
                        Back to Users
                    </Link>
                    <button
                        className="rounded border border-red-500/60 px-3 py-2 font-semibold text-red-200 hover:bg-red-500/10 disabled:opacity-50"
                        disabled={saving || data?.status === "DISABLED" || data?.role === "ADMIN"}
                        onClick={handleDeactivate}
                        type="button"
                    >
                        {saving ? "Saving..." : data?.status === "DISABLED" ? "Deactivated" : "Deactivate"}
                    </button>
                </div>
            </div>

            {error && (
                <div className="mb-3 rounded-lg border border-red-500/50 bg-red-500/10 p-3 text-sm text-red-200">
                    {error}
                </div>
            )}

            {loading && <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4 text-sm">Loading...</div>}

            {data && !loading && (
                <div className="space-y-6">
                    <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                                <div className="text-xs uppercase tracking-wide text-slate-400">User</div>
                                <div className="text-xl font-semibold text-slate-100">{data.name}</div>
                                <div className="text-sm text-slate-400">{data.email}</div>
                                <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                                    <span className="rounded border border-slate-700 px-2 py-1 font-semibold">{data.role}</span>
                                    <span className={`rounded-full border px-2 py-1 font-semibold ${USER_STATUS_META[data.status]}`}>
                                        {data.status}
                                    </span>
                                    <span className="rounded border border-slate-700 px-2 py-1">
                                        Created: {formatDateTime(data.createdAt)}
                                    </span>
                                    <span className="rounded border border-slate-700 px-2 py-1">
                                        Last Active: {formatDateTime(data.stats.lastActiveAt)}
                                    </span>
                                </div>
                            </div>
                            <div className="grid grid-cols-2 gap-3 text-center md:grid-cols-4">
                                <StatCard label="Attempts" value={data.stats.attemptedCount} />
                                <StatCard label="Correct" value={data.stats.correctCount} />
                                <StatCard label="Wrong" value={data.stats.wrongCount} />
                                <StatCard label="Reattempt" value={data.stats.reattemptCorrectCount} />
                            </div>
                        </div>
                    </section>

                    <section className="rounded-xl border border-slate-800 bg-slate-900/60">
                        <div className="border-b border-slate-800 px-5 py-3 text-sm font-semibold text-slate-200">
                            Per-case Progress
                        </div>
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-slate-800 text-sm">
                                <thead className="bg-slate-800/60 text-slate-300">
                                    <tr>
                                        <th className="px-4 py-3 text-left">Case</th>
                                        <th className="px-4 py-3 text-left">Status</th>
                                        <th className="px-4 py-3 text-left">Attempts</th>
                                        <th className="px-4 py-3 text-left">Last Attempt</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-800 text-slate-100">
                                    {data.caseProgress.map((item) => (
                                        <tr key={item.caseId}>
                                            <td className="px-4 py-3">
                                                <div className="font-semibold text-slate-100">Case #{item.caseId}</div>
                                                <div className="text-xs text-slate-400">{item.caseTitle}</div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <StatusPill status={item.status} />
                                            </td>
                                            <td className="px-4 py-3">{item.attemptCount}</td>
                                            <td className="px-4 py-3 text-xs text-slate-400">
                                                {formatDateTime(item.lastAttemptAt)}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </section>

                    <section className="rounded-xl border border-slate-800 bg-slate-900/60">
                        <div className="border-b border-slate-800 px-5 py-3 text-sm font-semibold text-slate-200">
                            Recent Activity (last 20 attempts)
                        </div>
                        <div className="divide-y divide-slate-800">
                            {data.recentActivities.length === 0 && (
                                <div className="px-5 py-4 text-sm text-slate-400">No attempts yet.</div>
                            )}
                            {data.recentActivities.map((activity, idx) => (
                                <div key={`${activity.caseId}-${idx}`} className="flex flex-wrap items-center justify-between gap-3 px-5 py-3 text-sm">
                                    <div>
                                        <div className="font-semibold text-slate-100">
                                            Case #{activity.caseId}: {activity.caseTitle}
                                        </div>
                                        <div className="text-xs text-slate-400">{formatDateTime(activity.timestamp)}</div>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <StatusPill status={activity.status} />
                                        <span className="rounded border border-slate-700 px-2 py-1 text-xs text-slate-200">
                                            Score: {activity.score.toFixed(1)}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </section>
                </div>
            )}
        </AdminLayout>
    );
}

function StatusPill({ status }: { status: UserCaseStatus }) {
    const meta = getStatusMeta(status);
    return (
        <span className={`rounded-full px-2 py-1 text-xs font-semibold ${meta.bg} ${meta.textClass}`}>
            {meta.label}
        </span>
    );
}

function StatCard({ label, value }: { label: string; value: number }) {
    return (
        <div className="rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2">
            <div className="text-xs uppercase tracking-wide text-slate-400">{label}</div>
            <div className="text-lg font-semibold">{value}</div>
        </div>
    );
}
