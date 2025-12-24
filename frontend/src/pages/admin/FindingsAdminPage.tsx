import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type Finding = {
    id: number;
    label: string;
    description?: string;
};

export default function FindingsAdminPage() {
    const [findings, setFindings] = useState<Finding[]>([]);
    const [label, setLabel] = useState("");
    const [description, setDescription] = useState("");
    const [editingId, setEditingId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const loadFindings = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await api.get<Finding[]>("/admin/findings");
            setFindings(data);
        } catch (err: any) {
            setError(err?.message || "Failed to load findings");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadFindings();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!label.trim()) {
            setError("Label is required");
            return;
        }
        setError(null);
        try {
            const payload = { label, description };
            if (editingId) {
                await api.put(`/admin/findings/${editingId}`, payload);
            } else {
                await api.post("/admin/findings", payload);
            }
            setLabel("");
            setDescription("");
            setEditingId(null);
            loadFindings();
        } catch (err: any) {
            setError(err?.message || "Save failed");
        }
    };

    const handleEdit = (finding: Finding) => {
        setEditingId(finding.id);
        setLabel(finding.label);
        setDescription(finding.description || "");
    };

    const handleDelete = async (id: number) => {
        setError(null);
        try {
            await api.delete(`/admin/findings/${id}`);
            if (editingId === id) {
                setEditingId(null);
                setLabel("");
                setDescription("");
            }
            loadFindings();
        } catch (err: any) {
            setError(err?.message || "Delete failed");
        }
    };

    return (
        <AdminLayout title="Findings" description="Manage finding options shown to students.">
            <div className="grid gap-6 lg:grid-cols-3">
                <form
                    className="space-y-4 rounded-xl border border-slate-800 bg-slate-900/60 p-4"
                    onSubmit={handleSubmit}
                >
                    <div className="text-lg font-semibold text-teal-200">
                        {editingId ? "Edit Finding" : "Add Finding"}
                    </div>
                    <div>
                        <label className="text-sm text-slate-300">Label</label>
                        <input
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                            value={label}
                            onChange={(e) => setLabel(e.target.value)}
                            required
                        />
                    </div>
                    <div>
                        <label className="text-sm text-slate-300">Description</label>
                        <textarea
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                        />
                    </div>
                    {error && <div className="text-sm text-red-400">{error}</div>}
                    <div className="flex gap-2">
                        <button
                            className="rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400"
                            type="submit"
                        >
                            {editingId ? "Update" : "Create"}
                        </button>
                        {editingId && (
                            <button
                                className="rounded-lg border border-slate-700 px-3 py-2 text-sm hover:border-teal-400"
                                onClick={() => {
                                    setEditingId(null);
                                    setLabel("");
                                    setDescription("");
                                }}
                                type="button"
                            >
                                Cancel
                            </button>
                        )}
                    </div>
                </form>

                <div className="lg:col-span-2 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
                    <div className="flex items-center justify-between">
                        <div className="text-lg font-semibold text-teal-200">Existing Findings</div>
                        <button
                            className="text-sm text-slate-300 hover:text-white"
                            onClick={loadFindings}
                            disabled={loading}
                            type="button"
                        >
                            Refresh
                        </button>
                    </div>
                    {loading ? (
                        <div className="mt-3 text-sm text-slate-400">Loading...</div>
                    ) : (
                        <div className="mt-3 divide-y divide-slate-800">
                            {findings.map((finding) => (
                                <div key={finding.id} className="flex items-start justify-between py-3">
                                    <div>
                                        <div className="text-sm font-semibold">{finding.label}</div>
                                        {finding.description && (
                                            <div className="text-xs text-slate-400">{finding.description}</div>
                                        )}
                                    </div>
                                    <div className="flex gap-2 text-xs">
                                        <button
                                            className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400"
                                            onClick={() => handleEdit(finding)}
                                            type="button"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            className="rounded border border-red-500 px-2 py-1 text-red-300 hover:bg-red-500/10"
                                            onClick={() => handleDelete(finding.id)}
                                            type="button"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                            {findings.length === 0 && (
                                <div className="py-6 text-sm text-slate-400">No findings yet.</div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </AdminLayout>
    );
}
