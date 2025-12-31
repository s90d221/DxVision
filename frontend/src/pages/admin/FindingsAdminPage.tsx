import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type Finding = {
    id: number;
    label: string;
    description?: string | null;
};

type Folder = {
    id: number;
    name: string;
    sortOrder: number;
    systemDefault?: boolean;
    items: Finding[];
};

export default function FindingsAdminPage() {
    const [folders, setFolders] = useState<Folder[]>([]);
    const [expandedFolders, setExpandedFolders] = useState<Set<number>>(new Set());
    const [label, setLabel] = useState("");
    const [description, setDescription] = useState("");
    const [selectedFolders, setSelectedFolders] = useState<Set<number>>(new Set());
    const [editingId, setEditingId] = useState<number | null>(null);
    const [folderName, setFolderName] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [savingFolder, setSavingFolder] = useState(false);

    const orderedFolders = useMemo(
        () => [...folders].sort((a, b) => a.sortOrder - b.sortOrder),
        [folders]
    );

    const loadFolders = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await api.get<Folder[]>("/admin/folders?type=FINDING");
            setFolders(data);
            setExpandedFolders(new Set(data.map((f) => f.id)));
            const folderIds = data.map((f) => f.id);
            const hasSelected = Array.from(selectedFolders).some((id) => folderIds.includes(id));
            if (data.length > 0 && (!hasSelected || selectedFolders.size === 0)) {
                setSelectedFolders(new Set([data[0].id]));
            } else if (!hasSelected) {
                setSelectedFolders(new Set());
            }
        } catch (err: any) {
            setError(err?.message || "Failed to load findings");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadFolders();
    }, []);

    const resetForm = () => {
        setEditingId(null);
        setLabel("");
        setDescription("");
        if (folders.length > 0) {
            setSelectedFolders(new Set([folders[0].id]));
        } else {
            setSelectedFolders(new Set());
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!label.trim()) {
            setError("Label is required");
            return;
        }
        if (selectedFolders.size === 0) {
            setError("Select at least one folder");
            return;
        }
        setError(null);
        try {
            const payload = { label, description, folderIds: Array.from(selectedFolders) };
            if (editingId) {
                await api.put(`/admin/findings/${editingId}`, payload);
            } else {
                await api.post("/admin/findings", payload);
            }
            resetForm();
            await loadFolders();
        } catch (err: any) {
            setError(err?.message || "Save failed");
        }
    };

    const handleEdit = (finding: Finding) => {
        setEditingId(finding.id);
        setLabel(finding.label);
        setDescription(finding.description || "");
        const folderIds = orderedFolders
            .filter((folder) => folder.items.some((item) => item.id === finding.id))
            .map((f) => f.id);
        const fallbackId = orderedFolders[0]?.id;
        setSelectedFolders(new Set(folderIds.length > 0 ? folderIds : fallbackId ? [fallbackId] : []));
    };

    const handleDelete = async (id: number) => {
        setError(null);
        try {
            await api.delete(`/admin/findings/${id}`);
            if (editingId === id) {
                resetForm();
            }
            await loadFolders();
        } catch (err: any) {
            setError(err?.message || "Delete failed");
        }
    };

    const toggleFolderSelection = (id: number) => {
        setSelectedFolders((prev) => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    const toggleExpanded = (id: number) => {
        setExpandedFolders((prev) => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    const handleFolderCreate = async () => {
        if (!folderName.trim()) return;
        setSavingFolder(true);
        setError(null);
        try {
            await api.post("/admin/folders", { type: "FINDING", name: folderName, sortOrder: folders.length });
            setFolderName("");
            await loadFolders();
        } catch (err: any) {
            setError(err?.message || "Failed to create folder");
        } finally {
            setSavingFolder(false);
        }
    };

    const handleFolderRename = async (folder: Folder) => {
        const nextName = prompt("Rename folder", folder.name);
        if (!nextName || !nextName.trim() || nextName === folder.name) return;
        setSavingFolder(true);
        setError(null);
        try {
            await api.put(`/admin/folders/${folder.id}`, { type: "FINDING", name: nextName.trim(), sortOrder: folder.sortOrder });
            await loadFolders();
        } catch (err: any) {
            setError(err?.message || "Rename failed");
        } finally {
            setSavingFolder(false);
        }
    };

    const handleFolderDelete = async (folder: Folder) => {
        if (folder.systemDefault) return;
        if (!confirm(`Delete folder "${folder.name}"? Items will move to default.`)) return;
        setSavingFolder(true);
        setError(null);
        try {
            await api.delete(`/admin/folders/${folder.id}`);
            await loadFolders();
        } catch (err: any) {
            setError(err?.message || "Delete failed");
        } finally {
            setSavingFolder(false);
        }
    };

    const handleReorder = async (folderId: number, direction: -1 | 1) => {
        const index = orderedFolders.findIndex((f) => f.id === folderId);
        const targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= orderedFolders.length) return;
        const swapped = [...orderedFolders];
        [swapped[index], swapped[targetIndex]] = [swapped[targetIndex], swapped[index]];
        const orders = swapped.map((f, idx) => ({ id: f.id, sortOrder: idx }));
        setFolders(swapped);
        try {
            await api.put("/admin/folders/reorder", { type: "FINDING", orders });
        } catch (err: any) {
            setError(err?.message || "Reorder failed");
        }
    };

    return (
        <AdminLayout title=" " description="Admin · Manage finding options" showSectionNav>
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
                        <div className="text-sm font-semibold text-slate-200">Folders</div>
                        <div className="mt-2 flex flex-wrap gap-2">
                            {orderedFolders.map((folder) => (
                                <label
                                    key={folder.id}
                                    className="flex items-center gap-2 rounded-full border border-slate-700 px-3 py-1 text-xs"
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedFolders.has(folder.id)}
                                        onChange={() => toggleFolderSelection(folder.id)}
                                    />
                                    {folder.name}
                                </label>
                            ))}
                            {orderedFolders.length === 0 && (
                                <span className="text-xs text-slate-400">No folders yet.</span>
                            )}
                        </div>
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
                                onClick={resetForm}
                                type="button"
                            >
                                Cancel
                            </button>
                        )}
                    </div>
                </form>

                <div className="lg:col-span-2 space-y-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="text-lg font-semibold text-teal-200">Folders & Findings</div>
                        <div className="flex flex-wrap items-center gap-2">
                            <input
                                className="w-40 rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm"
                                value={folderName}
                                onChange={(e) => setFolderName(e.target.value)}
                                placeholder="New folder name"
                            />
                            <button
                                className="rounded-lg border border-slate-700 px-3 py-1 text-sm hover:border-teal-400 disabled:opacity-60"
                                onClick={handleFolderCreate}
                                type="button"
                                disabled={savingFolder}
                            >
                                Add folder
                            </button>
                            <button
                                className="text-sm text-slate-300 hover:text-white"
                                onClick={() => loadFolders()}
                                disabled={loading}
                                type="button"
                            >
                                Refresh
                            </button>
                        </div>
                    </div>
                    {loading ? (
                        <div className="mt-3 text-sm text-slate-400">Loading...</div>
                    ) : (
                        <div className="space-y-3">
                            {orderedFolders.map((folder) => (
                                <div key={folder.id} className="rounded-lg border border-slate-800 bg-slate-900/50">
                                    <div className="flex items-center justify-between px-3 py-2">
                                        <button
                                            className="flex items-center gap-2 text-sm font-semibold text-slate-100"
                                            onClick={() => toggleExpanded(folder.id)}
                                            type="button"
                                        >
                                            <span className="text-xs text-slate-400">
                                                {expandedFolders.has(folder.id) ? "▾" : "▸"}
                                            </span>
                                            {folder.name}
                                            {folder.systemDefault && (
                                                <span className="rounded bg-slate-800 px-2 py-0.5 text-[10px] text-slate-300">
                                                    Default
                                                </span>
                                            )}
                                        </button>
                                        <div className="flex items-center gap-2 text-xs">
                                            <button
                                                className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400 disabled:opacity-40"
                                                onClick={() => handleReorder(folder.id, -1)}
                                                type="button"
                                                disabled={orderedFolders[0]?.id === folder.id}
                                            >
                                                ↑
                                            </button>
                                            <button
                                                className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400 disabled:opacity-40"
                                                onClick={() => handleReorder(folder.id, 1)}
                                                type="button"
                                                disabled={orderedFolders[orderedFolders.length - 1]?.id === folder.id}
                                            >
                                                ↓
                                            </button>
                                            <button
                                                className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400"
                                                onClick={() => handleFolderRename(folder)}
                                                type="button"
                                            >
                                                Rename
                                            </button>
                                            {!folder.systemDefault && (
                                                <button
                                                    className="rounded border border-red-500 px-2 py-1 text-red-300 hover:bg-red-500/10"
                                                    onClick={() => handleFolderDelete(folder)}
                                                    type="button"
                                                >
                                                    Delete
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                    {expandedFolders.has(folder.id) && (
                                        <div className="space-y-1 border-t border-slate-800 px-3 py-2">
                                            {folder.items.map((finding) => (
                                                <div
                                                    key={`${folder.id}-${finding.id}`}
                                                    className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2"
                                                >
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
                                            {folder.items.length === 0 && (
                                                <div className="text-xs text-slate-500">No findings in this folder.</div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            ))}
                            {orderedFolders.length === 0 && (
                                <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4 text-sm text-slate-400">
                                    No folders yet. Create one to start organizing findings.
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </AdminLayout>
    );
}
