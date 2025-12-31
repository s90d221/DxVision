import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type Finding = {
    id: number;
    label: string;
    description?: string;
    folderId?: number | null;
    folderName?: string | null;
    orderIndex?: number;
};

type OptionFolder = {
    id: number;
    name: string;
    orderIndex: number;
};

export default function FindingsAdminPage() {
    const [findings, setFindings] = useState<Finding[]>([]);
    const [label, setLabel] = useState("");
    const [description, setDescription] = useState("");
    const [editingId, setEditingId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [folders, setFolders] = useState<OptionFolder[]>([]);
    const [selectedFolderId, setSelectedFolderId] = useState<number | null>(null);
    const [creatingFolder, setCreatingFolder] = useState(false);
    const [folderName, setFolderName] = useState("");

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

    const loadFolders = async () => {
        try {
            const data = await api.get<OptionFolder[]>("/admin/option-folders?type=FINDING");
            setFolders(data);
        } catch (err: any) {
            setError(err?.message || "Failed to load folders");
        }
    };

    useEffect(() => {
        loadFindings();
        loadFolders();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!label.trim()) {
            setError("Label is required");
            return;
        }
        setError(null);
        try {
            const payload = { label, description, folderId: selectedFolderId || null };
            if (editingId) {
                await api.put(`/admin/findings/${editingId}`, payload);
            } else {
                await api.post("/admin/findings", payload);
            }
            setLabel("");
            setDescription("");
            setSelectedFolderId(null);
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
        setSelectedFolderId(finding.folderId ?? null);
    };

    const handleDelete = async (id: number) => {
        setError(null);
        try {
            await api.delete(`/admin/findings/${id}`);
            if (editingId === id) {
                setEditingId(null);
                setLabel("");
                setDescription("");
                setSelectedFolderId(null);
            }
            loadFindings();
        } catch (err: any) {
            setError(err?.message || "Delete failed");
        }
    };

    const handleCreateFolder = async () => {
        if (!folderName.trim()) return;
        setCreatingFolder(true);
        setError(null);
        try {
            await api.post("/admin/option-folders", { type: "FINDING", name: folderName.trim() });
            setFolderName("");
            loadFolders();
        } catch (err: any) {
            setError(err?.message || "Failed to create folder");
        } finally {
            setCreatingFolder(false);
        }
    };

    const handleDeleteFolder = async (folderId: number) => {
        setError(null);
        try {
            await api.delete(`/admin/option-folders/${folderId}`);
            if (selectedFolderId === folderId) {
                setSelectedFolderId(null);
            }
            await Promise.all([loadFolders(), loadFindings()]);
        } catch (err: any) {
            setError(err?.message || "Failed to delete folder");
        }
    };

    const moveFolder = async (folderId: number, direction: -1 | 1) => {
        const sorted = [...folders].sort((a, b) => a.orderIndex - b.orderIndex);
        const index = sorted.findIndex((f) => f.id === folderId);
        const swap = sorted[index + direction];
        if (!swap) return;
        const current = sorted[index];
        await Promise.all([
            api.put(`/admin/option-folders/${current.id}`, { type: "FINDING", name: current.name, orderIndex: swap.orderIndex }),
            api.put(`/admin/option-folders/${swap.id}`, { type: "FINDING", name: swap.name, orderIndex: current.orderIndex }),
        ]);
        loadFolders();
    };

    const moveFinding = async (finding: Finding, direction: -1 | 1) => {
        const group = findings
            .filter((f) => f.folderId === finding.folderId)
            .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
        const index = group.findIndex((f) => f.id === finding.id);
        const swap = group[index + direction];
        if (!swap) return;
        await Promise.all([
            api.put(`/admin/findings/${finding.id}`, {
                label: finding.label,
                description: finding.description ?? "",
                folderId: finding.folderId ?? null,
                orderIndex: swap.orderIndex ?? 0,
            }),
            api.put(`/admin/findings/${swap.id}`, {
                label: swap.label,
                description: swap.description ?? "",
                folderId: swap.folderId ?? null,
                orderIndex: finding.orderIndex ?? 0,
            }),
        ]);
        loadFindings();
    };

    const folderOrder = useMemo(() => {
        const map = new Map<number, number>();
        folders.forEach((f) => map.set(f.id, f.orderIndex));
        return map;
    }, [folders]);

    return (
        <AdminLayout title=" " description="Admin · Manage finding options" showSectionNav>
            <div className="mb-6 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="text-lg font-semibold text-teal-200">Folders</div>
                    <div className="flex gap-2">
                        <input
                            className="w-48 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                            placeholder="New folder name"
                            value={folderName}
                            onChange={(e) => setFolderName(e.target.value)}
                        />
                        <button
                            className="rounded-lg bg-teal-500 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50"
                            onClick={() => void handleCreateFolder()}
                            disabled={creatingFolder || !folderName.trim()}
                            type="button"
                        >
                            Add
                        </button>
                    </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                    {folders.map((folder, idx) => (
                        <div
                            key={folder.id}
                            className="flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-900/70 px-3 py-2 text-sm"
                        >
                            <span className="font-semibold text-slate-100">{folder.name}</span>
                            <div className="flex items-center gap-1 text-[11px]">
                                <button
                                    className="rounded border border-slate-700 px-1 py-0.5 hover:border-teal-400"
                                    onClick={() => moveFolder(folder.id, -1)}
                                    disabled={idx === 0}
                                    type="button"
                                >
                                    ↑
                                </button>
                                <button
                                    className="rounded border border-slate-700 px-1 py-0.5 hover:border-teal-400"
                                    onClick={() => moveFolder(folder.id, 1)}
                                    disabled={idx === folders.length - 1}
                                    type="button"
                                >
                                    ↓
                                </button>
                                <button
                                    className="rounded border border-red-400 px-1.5 py-0.5 text-red-200 hover:bg-red-500/10"
                                    onClick={() => handleDeleteFolder(folder.id)}
                                    type="button"
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    ))}
                    {folders.length === 0 && <div className="text-sm text-slate-400">No folders yet.</div>}
                </div>
            </div>
            <div className="grid gap-6 lg:grid-cols-3">
                <form
                    className="space-y-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4"
                    onSubmit={handleSubmit}
                >
                    <div className="text-lg font-semibold text-teal-200">
                        {editingId ? "Edit Finding" : "Add Finding"}
                    </div>
                    <div>
                        <label className="text-sm text-slate-300">Label</label>
                        <input
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                            value={label}
                            onChange={(e) => setLabel(e.target.value)}
                            required
                        />
                    </div>
                    <div>
                        <label className="text-sm text-slate-300">Description</label>
                        <textarea
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                        />
                    </div>
                    <div>
                        <label className="text-sm text-slate-300">Folder</label>
                        <select
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                            value={selectedFolderId ?? ""}
                            onChange={(e) => setSelectedFolderId(e.target.value ? Number(e.target.value) : null)}
                        >
                            <option value="">Ungrouped</option>
                            {folders.map((folder) => (
                                <option key={folder.id} value={folder.id}>
                                    {folder.name}
                                </option>
                            ))}
                        </select>
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

                <div className="lg:col-span-2 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
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
                            {findings
                                .slice()
                                .sort((a, b) => {
                                    const folderA = a.folderId ? folderOrder.get(a.folderId) ?? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER;
                                    const folderB = b.folderId ? folderOrder.get(b.folderId) ?? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER;
                                    if (folderA !== folderB) return folderA - folderB;
                                    return (a.orderIndex ?? 0) - (b.orderIndex ?? 0);
                                })
                                .map((finding) => (
                                    <div key={finding.id} className="flex items-start justify-between py-3">
                                        <div>
                                            <div className="text-sm font-semibold">{finding.label}</div>
                                            {finding.description && (
                                                <div className="text-xs text-slate-400">{finding.description}</div>
                                            )}
                                            <div className="text-[11px] text-slate-500">
                                                Folder: {finding.folderName ?? "Ungrouped"}
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-2 text-xs">
                                            <div className="flex flex-col gap-1">
                                                <button
                                                    className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400"
                                                    onClick={() => moveFinding(finding, -1)}
                                                    type="button"
                                                >
                                                    Move ↑
                                                </button>
                                                <button
                                                    className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400"
                                                    onClick={() => moveFinding(finding, 1)}
                                                    type="button"
                                                >
                                                    Move ↓
                                                </button>
                                            </div>
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
