import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type UserStatus = "ACTIVE" | "DISABLED";

type AdminUserStats = {
    attemptedCount: number;
    correctCount: number;
    wrongCount: number;
    reattemptCorrectCount: number;
    lastActiveAt: string | null;
};

type AdminUserListItem = {
    id: number;
    email: string;
    name: string;
    role: "ADMIN" | "USER";
    status: UserStatus;
    createdAt: string;
    stats: AdminUserStats;
};

type PageResponse<T> = {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
};

const STATUS_COLORS: Record<UserStatus, string> = {
    ACTIVE: "bg-emerald-500/20 text-emerald-200 border-emerald-500/50",
    DISABLED: "bg-red-500/20 text-red-200 border-red-500/50",
};

export default function AdminUsersPage() {
    const [users, setUsers] = useState<AdminUserListItem[]>([]);
    const [page, setPage] = useState(0);
    const [size] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [query, setQuery] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadUsers = async (pageIndex = 0, search = query) => {
        setLoading(true);
        setError(null);
        try {
            const encodedQ = encodeURIComponent(search);
            const data = await api.get<PageResponse<AdminUserListItem>>(
                `/admin/users?page=${pageIndex}&size=${size}&q=${encodedQ}`
            );
            setUsers(data.content);
            setPage(data.page);
            setTotalPages(data.totalPages);
        } catch (err: any) {
            setError(err?.message || "Failed to load users");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadUsers(0, "");
    }, []);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        loadUsers(0, query);
    };

    const formatDateTime = (value: string | null | undefined) => {
        if (!value) return "—";
        return new Date(value).toLocaleString();
    };

    return (
        <AdminLayout
            title="User Management"
            description="Search students, review their progress, and deactivate accounts."
        >
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <form className="flex flex-wrap items-center gap-2" onSubmit={handleSubmit}>
                    <input
                        className="rounded-lg border border-slate-700 bg-slate-900/60 px-3 py-2 text-sm text-slate-100 focus:border-teal-400 focus:outline-none"
                        placeholder="Search by email or name..."
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                    />
                    <button
                        className="rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-900 hover:bg-teal-400"
                        type="submit"
                    >
                        Search
                    </button>
                </form>
                <div className="text-xs text-slate-400">
                    Page {page + 1} of {Math.max(totalPages, 1)}
                </div>
            </div>

            {error && (
                <div className="mb-3 rounded-lg border border-red-500/50 bg-red-500/10 p-3 text-sm text-red-200">
                    {error}
                </div>
            )}

            <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60">
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-800 text-sm">
                        <thead className="bg-slate-800/60 text-slate-300">
                            <tr>
                                <th className="px-4 py-3 text-left">User</th>
                                <th className="px-4 py-3 text-left">Role</th>
                                <th className="px-4 py-3 text-left">Status</th>
                                <th className="px-4 py-3 text-left">Attempts</th>
                                <th className="px-4 py-3 text-left">Correct</th>
                                <th className="px-4 py-3 text-left">Wrong</th>
                                <th className="px-4 py-3 text-left">Reattempt</th>
                                <th className="px-4 py-3 text-left">Last Active</th>
                                <th className="px-4 py-3 text-left">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800 text-slate-100">
                            {users.map((user) => (
                                <tr key={user.id}>
                                    <td className="px-4 py-3">
                                        <div className="font-semibold text-slate-100">{user.name}</div>
                                        <div className="text-xs text-slate-400">{user.email}</div>
                                    </td>
                                    <td className="px-4 py-3">
                                        <span className="rounded border border-slate-700 px-2 py-1 text-xs font-semibold">
                                            {user.role}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">
                                        <span
                                            className={`rounded-full border px-2 py-1 text-xs font-semibold ${STATUS_COLORS[user.status]}`}
                                        >
                                            {user.status}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">{user.stats.attemptedCount}</td>
                                    <td className="px-4 py-3">{user.stats.correctCount}</td>
                                    <td className="px-4 py-3">{user.stats.wrongCount}</td>
                                    <td className="px-4 py-3">{user.stats.reattemptCorrectCount}</td>
                                    <td className="px-4 py-3 text-xs text-slate-400">
                                        {formatDateTime(user.stats.lastActiveAt)}
                                    </td>
                                    <td className="px-4 py-3">
                                        <Link
                                            to={`/admin/users/${user.id}`}
                                            className="rounded border border-slate-700 px-3 py-1 text-xs font-semibold hover:border-teal-400 hover:text-teal-200"
                                        >
                                            View
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                            {!loading && users.length === 0 && (
                                <tr>
                                    <td className="px-4 py-6 text-center text-slate-400" colSpan={9}>
                                        No users found. Adjust your search and try again.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
                {loading && <div className="p-4 text-sm text-slate-400">Loading users...</div>}
                <div className="flex items-center justify-between border-t border-slate-800 bg-slate-900/80 px-4 py-3 text-xs text-slate-400">
                    <div>
                        {users.length === 0
                            ? "Showing 0 of 0"
                            : `Showing ${(page * size + 1).toLocaleString()}-${(page * size + users.length).toLocaleString()}`}
                    </div>
                    <div className="flex gap-2">
                        <button
                            className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400 disabled:opacity-40"
                            disabled={page <= 0}
                            onClick={() => loadUsers(page - 1, query)}
                            type="button"
                        >
                            Prev
                        </button>
                        <button
                            className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400 disabled:opacity-40"
                            disabled={page + 1 >= totalPages}
                            onClick={() => loadUsers(page + 1, query)}
                            type="button"
                        >
                            Next
                        </button>
                    </div>
                </div>
            </div>
        </AdminLayout>
    );
}
