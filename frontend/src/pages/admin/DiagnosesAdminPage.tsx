import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type Diagnosis = {
    id: number;
    name: string;
    description?: string;
};

export default function DiagnosesAdminPage() {
    const [diagnoses, setDiagnoses] = useState<Diagnosis[]>([]);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [editingId, setEditingId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const loadDiagnoses = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await api.get<Diagnosis[]>("/admin/diagnoses");
            setDiagnoses(data);
        } catch (err: any) {
            setError(err?.message || "Failed to load diagnoses");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadDiagnoses();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) {
            setError("Name is required");
            return;
        }
        setError(null);
        try {
            const payload = { name, description };
            if (editingId) {
                await api.put(`/admin/diagnoses/${editingId}`, payload);
            } else {
                await api.post("/admin/diagnoses", payload);
            }
            setName("");
            setDescription("");
            setEditingId(null);
            loadDiagnoses();
        } catch (err: any) {
            setError(err?.message || "Save failed");
        }
    };

    const handleEdit = (diagnosis: Diagnosis) => {
        setEditingId(diagnosis.id);
        setName(diagnosis.name);
        setDescription(diagnosis.description || "");
    };

    const handleDelete = async (id: number) => {
        setError(null);
        try {
            await api.delete(`/admin/diagnoses/${id}`);
            if (editingId === id) {
                setEditingId(null);
                setName("");
                setDescription("");
            }
            loadDiagnoses();
        } catch (err: any) {
            setError(err?.message || "Delete failed");
        }
    };

    return (
        <AdminLayout title="Diagnoses" description="Manage diagnosis options for cases." showSectionNav>
            <div className="grid gap-6 lg:grid-cols-3">
                <form
                    className="space-y-4 rounded-xl border border-slate-800 bg-slate-900/60 p-4"
                    onSubmit={handleSubmit}
                >
                    <div className="text-lg font-semibold text-teal-200">
                        {editingId ? "Edit Diagnosis" : "Add Diagnosis"}
                    </div>
                    <div>
                        <label className="text-sm text-slate-300">Name</label>
                        <input
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
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
                                    setName("");
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
                        <div className="text-lg font-semibold text-teal-200">Existing Diagnoses</div>
                        <button
                            className="text-sm text-slate-300 hover:text-white"
                            onClick={loadDiagnoses}
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
                            {diagnoses.map((dx) => (
                                <div key={dx.id} className="flex items-start justify-between py-3">
                                    <div>
                                        <div className="text-sm font-semibold">{dx.name}</div>
                                        {dx.description && <div className="text-xs text-slate-400">{dx.description}</div>}
                                    </div>
                                    <div className="flex gap-2 text-xs">
                                        <button
                                            className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400"
                                            onClick={() => handleEdit(dx)}
                                            type="button"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            className="rounded border border-red-500 px-2 py-1 text-red-300 hover:bg-red-500/10"
                                            onClick={() => handleDelete(dx.id)}
                                            type="button"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                            {diagnoses.length === 0 && (
                                <div className="py-6 text-sm text-slate-400">No diagnoses yet.</div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </AdminLayout>
    );
}
