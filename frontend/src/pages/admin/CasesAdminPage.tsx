import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type MasterFinding = {
    id: number;
    label: string;
};

type MasterDiagnosis = {
    id: number;
    name: string;
};

type AdminCase = {
    id: number;
    version: number;
    title: string;
    description?: string;
    modality: string;
    species: string;
    imageUrl: string;
    lesionShapeType: string;
    lesionDataJson: string;
    findings: { findingId: number; label: string; required: boolean }[];
    diagnoses: { diagnosisId: number; name: string; weight: number }[];
};

type CaseFormState = {
    title: string;
    description: string;
    modality: string;
    species: string;
    imageUrl: string;
    lesionShapeType: string;
    cx: number;
    cy: number;
    r: number;
    findingOptionIds: Set<number>;
    requiredFindingIds: Set<number>;
    diagnosisWeights: Record<number, number>;
};

const initialForm = (): CaseFormState => ({
    title: "",
    description: "",
    modality: "XRAY",
    species: "DOG",
    imageUrl: "",
    lesionShapeType: "CIRCLE",
    cx: 0.5,
    cy: 0.5,
    r: 0.2,
    findingOptionIds: new Set(),
    requiredFindingIds: new Set(),
    diagnosisWeights: {},
});

const MODALITIES = ["XRAY", "ULTRASOUND"];
const SPECIES = ["DOG", "CAT"];

export default function CasesAdminPage() {
    const [form, setForm] = useState<CaseFormState>(initialForm);
    const [findings, setFindings] = useState<MasterFinding[]>([]);
    const [diagnoses, setDiagnoses] = useState<MasterDiagnosis[]>([]);
    const [cases, setCases] = useState<AdminCase[]>([]);
    const [editingCaseId, setEditingCaseId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const selectedFindings = useMemo(() => Array.from(form.findingOptionIds), [form.findingOptionIds]);
    const requiredFindings = useMemo(() => Array.from(form.requiredFindingIds), [form.requiredFindingIds]);

    const loadData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [findingRes, diagnosisRes, casesRes] = await Promise.all([
                api.get<MasterFinding[]>("/admin/findings"),
                api.get<MasterDiagnosis[]>("/admin/diagnoses"),
                api.get<AdminCase[]>("/admin/cases"),
            ]);
            setFindings(findingRes);
            setDiagnoses(diagnosisRes);
            setCases(casesRes);
        } catch (err: any) {
            setError(err?.message || "Failed to load admin data");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const parseLesionData = (json: string) => {
        try {
            const parsed = JSON.parse(json);
            return {
                cx: typeof parsed.cx === "number" ? parsed.cx : 0.5,
                cy: typeof parsed.cy === "number" ? parsed.cy : 0.5,
                r: typeof parsed.r === "number" ? parsed.r : 0.2,
            };
        } catch {
            return { cx: 0.5, cy: 0.5, r: 0.2 };
        }
    };

    const handleToggleFinding = (id: number) => {
        setForm((prev) => {
            const nextSelected = new Set(prev.findingOptionIds);
            const nextRequired = new Set(prev.requiredFindingIds);
            if (nextSelected.has(id)) {
                nextSelected.delete(id);
                nextRequired.delete(id);
            } else {
                nextSelected.add(id);
            }
            return { ...prev, findingOptionIds: nextSelected, requiredFindingIds: nextRequired };
        });
    };

    const handleToggleRequired = (id: number) => {
        if (!form.findingOptionIds.has(id)) return;
        setForm((prev) => {
            const nextRequired = new Set(prev.requiredFindingIds);
            if (nextRequired.has(id)) {
                nextRequired.delete(id);
            } else {
                nextRequired.add(id);
            }
            return { ...prev, requiredFindingIds: nextRequired };
        });
    };

    const handleDiagnosisWeight = (id: number, value: string) => {
        setForm((prev) => {
            const next = { ...prev.diagnosisWeights };
            const parsed = parseFloat(value);
            if (Number.isNaN(parsed)) {
                delete next[id];
            } else {
                next[id] = parsed;
            }
            return { ...prev, diagnosisWeights: next };
        });
    };

    const resetForm = () => {
        setForm(initialForm());
        setEditingCaseId(null);
        setError(null);
    };

    const handleEditCase = (item: AdminCase) => {
        const lesion = parseLesionData(item.lesionDataJson);
        setForm({
            title: item.title,
            description: item.description || "",
            modality: item.modality,
            species: item.species,
            imageUrl: item.imageUrl,
            lesionShapeType: item.lesionShapeType,
            cx: lesion.cx,
            cy: lesion.cy,
            r: lesion.r,
            findingOptionIds: new Set(item.findings.map((f) => f.findingId)),
            requiredFindingIds: new Set(item.findings.filter((f) => f.required).map((f) => f.findingId)),
            diagnosisWeights: item.diagnoses.reduce<Record<number, number>>((acc, d) => {
                acc[d.diagnosisId] = d.weight;
                return acc;
            }, {}),
        });
        setEditingCaseId(item.id);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.title.trim()) {
            setError("Title is required");
            return;
        }
        if (!form.imageUrl.trim()) {
            setError("Image URL is required");
            return;
        }
        setError(null);

        const payload = {
            title: form.title,
            description: form.description,
            modality: form.modality,
            species: form.species,
            imageUrl: form.imageUrl,
            lesionShapeType: form.lesionShapeType,
            lesionData: { cx: form.cx, cy: form.cy, r: form.r },
            findingOptionIds: selectedFindings,
            requiredFindingIds: requiredFindings,
            diagnosisOptionWeights: Object.entries(form.diagnosisWeights)
                .filter(([, weight]) => weight && weight > 0)
                .map(([diagnosisId, weight]) => ({
                    diagnosisId: Number(diagnosisId),
                    weight,
                })),
        };

        try {
            if (editingCaseId) {
                await api.put(`/admin/cases/${editingCaseId}`, payload);
            } else {
                await api.post("/admin/cases", payload);
            }
            await loadData();
            resetForm();
        } catch (err: any) {
            setError(err?.message || "Save failed");
        }
    };

    const handleDeleteCase = async (caseId: number) => {
        setError(null);
        try {
            await api.delete(`/admin/cases/${caseId}`);
            await loadData();
            if (editingCaseId === caseId) {
                resetForm();
            }
        } catch (err: any) {
            setError(err?.message || "Delete failed");
        }
    };

    return (
        <AdminLayout title="Cases" description="Create, edit, and configure cases.">
            <div className="grid gap-6 xl:grid-cols-3">
                <form
                    className="xl:col-span-2 space-y-4 rounded-xl border border-slate-800 bg-slate-900/60 p-4"
                    onSubmit={handleSubmit}
                >
                    <div className="flex items-center justify-between">
                        <div className="text-lg font-semibold text-teal-200">
                            {editingCaseId ? "Edit Case" : "Create Case"}
                        </div>
                        <div className="space-x-2 text-xs text-slate-400">
                            <button
                                className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400"
                                onClick={resetForm}
                                type="button"
                            >
                                New
                            </button>
                            <button
                                className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400"
                                onClick={loadData}
                                type="button"
                            >
                                Refresh
                            </button>
                        </div>
                    </div>

                    <div className="grid gap-4 md:grid-cols-2">
                        <div>
                            <label className="text-sm text-slate-300">Title</label>
                            <input
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.title}
                                onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                                required
                            />
                        </div>
                        <div>
                            <label className="text-sm text-slate-300">Image URL</label>
                            <input
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.imageUrl}
                                onChange={(e) => setForm((prev) => ({ ...prev, imageUrl: e.target.value }))}
                                required
                            />
                        </div>
                        <div>
                            <label className="text-sm text-slate-300">Modality</label>
                            <select
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.modality}
                                onChange={(e) => setForm((prev) => ({ ...prev, modality: e.target.value }))}
                            >
                                {MODALITIES.map((m) => (
                                    <option key={m} value={m}>
                                        {m}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="text-sm text-slate-300">Species</label>
                            <select
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.species}
                                onChange={(e) => setForm((prev) => ({ ...prev, species: e.target.value }))}
                            >
                                {SPECIES.map((s) => (
                                    <option key={s} value={s}>
                                        {s}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="text-sm text-slate-300">Description</label>
                        <textarea
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                            value={form.description}
                            onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                            rows={3}
                        />
                    </div>

                    <div className="grid gap-4 md:grid-cols-3">
                        <div>
                            <label className="text-sm text-slate-300">Lesion cx (0-1)</label>
                            <input
                                type="number"
                                step="0.01"
                                min="0"
                                max="1"
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.cx}
                                onChange={(e) => setForm((prev) => ({ ...prev, cx: parseFloat(e.target.value) }))}
                            />
                        </div>
                        <div>
                            <label className="text-sm text-slate-300">Lesion cy (0-1)</label>
                            <input
                                type="number"
                                step="0.01"
                                min="0"
                                max="1"
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.cy}
                                onChange={(e) => setForm((prev) => ({ ...prev, cy: parseFloat(e.target.value) }))}
                            />
                        </div>
                        <div>
                            <label className="text-sm text-slate-300">Radius (&gt;0)</label>
                            <input
                                type="number"
                                step="0.01"
                                min="0"
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
                                value={form.r}
                                onChange={(e) => setForm((prev) => ({ ...prev, r: parseFloat(e.target.value) }))}
                            />
                        </div>
                    </div>

                    <div className="rounded-lg border border-slate-800 p-3">
                        <div className="text-sm font-semibold text-teal-200">Findings to display</div>
                        <div className="mt-2 grid gap-2 md:grid-cols-2">
                            {findings.map((f) => (
                                <label key={f.id} className="flex items-center gap-2 text-sm">
                                    <input
                                        type="checkbox"
                                        checked={form.findingOptionIds.has(f.id)}
                                        onChange={() => handleToggleFinding(f.id)}
                                    />
                                    {f.label}
                                </label>
                            ))}
                            {findings.length === 0 && <div className="text-xs text-slate-400">No findings available.</div>}
                        </div>
                        {selectedFindings.length > 0 && (
                            <div className="mt-3">
                                <div className="text-xs font-semibold text-slate-300">Required findings</div>
                                <div className="mt-1 grid gap-1 md:grid-cols-2">
                                    {selectedFindings.map((id) => {
                                        const label = findings.find((f) => f.id === id)?.label || `#${id}`;
                                        return (
                                            <label key={id} className="flex items-center gap-2 text-xs">
                                                <input
                                                    type="checkbox"
                                                    checked={form.requiredFindingIds.has(id)}
                                                    onChange={() => handleToggleRequired(id)}
                                                />
                                                {label}
                                            </label>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="rounded-lg border border-slate-800 p-3">
                        <div className="text-sm font-semibold text-teal-200">Diagnoses & weights</div>
                        <div className="mt-2 grid gap-2 md:grid-cols-2">
                            {diagnoses.map((dx) => (
                                <label key={dx.id} className="flex items-center justify-between gap-2 rounded border border-slate-800 bg-slate-900 px-3 py-2 text-sm">
                                    <span>{dx.name}</span>
                                    <input
                                        type="number"
                                        step="0.1"
                                        min="0"
                                        className="w-24 rounded border border-slate-700 bg-slate-900 px-2 py-1 text-right text-xs"
                                        value={form.diagnosisWeights[dx.id] ?? ""}
                                        onChange={(e) => handleDiagnosisWeight(dx.id, e.target.value)}
                                        placeholder="weight"
                                    />
                                </label>
                            ))}
                            {diagnoses.length === 0 && <div className="text-xs text-slate-400">No diagnoses available.</div>}
                        </div>
                    </div>

                    {error && <div className="text-sm text-red-400">{error}</div>}

                    <button
                        className="rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50"
                        type="submit"
                        disabled={loading}
                    >
                        {editingCaseId ? "Update Case" : "Create Case"}
                    </button>
                </form>

                <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
                    <div className="flex items-center justify-between">
                        <div className="text-lg font-semibold text-teal-200">Cases</div>
                        <span className="text-xs text-slate-400">{cases.length} total</span>
                    </div>
                    {loading ? (
                        <div className="mt-3 text-sm text-slate-400">Loading...</div>
                    ) : (
                        <div className="mt-3 space-y-3">
                            {cases.map((c) => (
                                <div key={c.id} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                                    <div className="flex items-center justify-between text-sm font-semibold">
                                        <div>{c.title}</div>
                                        <div className="text-xs text-slate-400">v{c.version}</div>
                                    </div>
                                    <div className="mt-1 text-xs text-slate-400">
                                        {c.modality} · {c.species}
                                    </div>
                                    <div className="mt-2 flex gap-2 text-xs">
                                        <button
                                            className="rounded border border-slate-700 px-2 py-1 hover:border-teal-400"
                                            onClick={() => handleEditCase(c)}
                                            type="button"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            className="rounded border border-red-500 px-2 py-1 text-red-300 hover:bg-red-500/10"
                                            onClick={() => handleDeleteCase(c.id)}
                                            type="button"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                            {cases.length === 0 && (
                                <div className="text-sm text-slate-400">No cases yet. Create one using the form.</div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </AdminLayout>
    );
}
