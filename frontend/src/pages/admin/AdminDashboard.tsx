import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type AdminCaseListItem = {
    id: number;
    title: string;
    modality: string;
    species: string;
    version: number;
    updatedAt: string;
};

type PageResponse<T> = {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
};

export default function AdminDashboard() {
    const [cases, setCases] = useState<AdminCaseListItem[]>([]);
    const [page, setPage] = useState(0);
    const [size] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const loadCases = async (nextPage = 0) => {
        setLoading(true);
        setError(null);
        try {
            const data = await api.get<PageResponse<AdminCaseListItem>>(`/admin/cases?page=${nextPage}&size=${size}`);
            setCases(data.content);
            setPage(data.page);
            setTotalPages(data.totalPages);
        } catch (err: any) {
            setError(err?.message || "Failed to load cases");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCases(0);
    }, []);

    const handleDelete = async (id: number) => {
        if (!confirm("Delete this case? This cannot be undone.")) return;
        try {
            await api.delete(`/admin/cases/${id}`);
            await loadCases(page);
        } catch (err: any) {
            setError(err?.message || "Failed to delete case");
        }
    };

    return (
        <AdminLayout
            title="Case Console"
            description="Create, edit, and publish cases for students. Admin access only."
        >
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <div className="space-y-1">
                    <div className="text-lg font-semibold text-teal-200">Cases</div>
                    <div className="text-sm text-slate-400">
                        Manage answer keys, findings, diagnoses, and lesion targets.
                    </div>
                </div>
                <div className="flex flex-wrap gap-2">
                    <Link
                        to="/admin/findings"
                        className="rounded-lg border border-slate-700 px-3 py-2 text-sm font-semibold text-slate-200 hover:border-teal-400"
                    >
                        Findings
                    </Link>
                    <Link
                        to="/admin/diagnoses"
                        className="rounded-lg border border-slate-700 px-3 py-2 text-sm font-semibold text-slate-200 hover:border-teal-400"
                    >
                        Diagnoses
                    </Link>
                    <button
                        onClick={() => navigate("/admin/cases/new")}
                        className="rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-900 hover:bg-teal-400"
                        type="button"
                    >
                        New Case
                    </button>
                </div>
            </div>

            {error && <div className="mb-3 rounded-lg border border-red-500/50 bg-red-500/10 p-3 text-sm text-red-200">{error}</div>}

            <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60">
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-800 text-sm">
                        <thead className="bg-slate-800/60 text-slate-300">
                            <tr>
                                <th className="px-4 py-3 text-left">ID</th>
                                <th className="px-4 py-3 text-left">Title</th>
                                <th className="px-4 py-3 text-left">Modality</th>
                                <th className="px-4 py-3 text-left">Species</th>
                                <th className="px-4 py-3 text-left">Version</th>
                                <th className="px-4 py-3 text-left">Updated</th>
                                <th className="px-4 py-3 text-left">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800 text-slate-100">
                            {cases.map((item) => (
                                <tr key={item.id}>
                                    <td className="px-4 py-3">#{item.id}</td>
                                    <td className="px-4 py-3 font-semibold">{item.title}</td>
                                    <td className="px-4 py-3 text-xs">{item.modality}</td>
                                    <td className="px-4 py-3 text-xs">{item.species}</td>
                                    <td className="px-4 py-3">v{item.version}</td>
                                    <td className="px-4 py-3 text-xs text-slate-400">
                                        {new Date(item.updatedAt).toLocaleString()}
                                    </td>
                                    <td className="px-4 py-3">
                                        <div className="flex flex-wrap gap-2">
                                            <Link
                                                to={`/quiz/${item.id}`}
                                                className="rounded border border-teal-500/60 px-3 py-1 text-xs font-semibold text-teal-200 hover:bg-teal-500/10"
                                            >
                                                Test this case
                                            </Link>
                                            <Link
                                                to={`/admin/cases/${item.id}/edit`}
                                                className="rounded border border-slate-700 px-3 py-1 text-xs font-semibold hover:border-teal-400 hover:text-teal-200"
                                            >
                                                Edit
                                            </Link>
                                            <button
                                                onClick={() => handleDelete(item.id)}
                                                className="rounded border border-red-500/50 px-3 py-1 text-xs font-semibold text-red-200 hover:bg-red-500/10"
                                                type="button"
                                            >
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!loading && cases.length === 0 && (
                                <tr>
                                    <td className="px-4 py-6 text-center text-slate-400" colSpan={7}>
                                        No cases yet. Create the first case to get started.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
                {loading && <div className="p-4 text-sm text-slate-400">Loading cases...</div>}
                <div className="flex items-center justify-between border-t border-slate-800 bg-slate-900/80 px-4 py-3 text-xs text-slate-400">
                    <div>
                        Page {page + 1} of {Math.max(totalPages, 1)}
                    </div>
                    <div className="flex gap-2">
                        <button
                            className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400 disabled:opacity-40"
                            disabled={page <= 0}
                            onClick={() => loadCases(page - 1)}
                            type="button"
                        >
                            Prev
                        </button>
                        <button
                            className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400 disabled:opacity-40"
                            disabled={page + 1 >= totalPages}
                            onClick={() => loadCases(page + 1)}
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
