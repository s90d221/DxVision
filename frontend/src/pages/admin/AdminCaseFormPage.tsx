import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type LookupFinding = { id: number; label: string };
type LookupDiagnosis = { id: number; name: string };

type AdminCaseDetail = {
    id: number;
    title: string;
    description?: string;
    modality: string;
    species: string;
    imageUrl: string;
    lesionData?: { cx: number; cy: number; r: number };
    findings: { findingId: number; label: string; required: boolean }[];
    diagnoses: { diagnosisId: number; name: string; weight: number }[];
};

type LookupResponse = {
    findings: LookupFinding[];
    diagnoses: LookupDiagnosis[];
};

type AdminCaseFormPageProps = {
    mode: "create" | "edit";
};

const MODALITIES = ["XRAY", "ULTRASOUND"];
const SPECIES = ["DOG", "CAT"];

const clamp01 = (value: number) => Math.min(1, Math.max(0, value));

export default function AdminCaseFormPage({ mode }: AdminCaseFormPageProps) {
    const params = useParams();
    const navigate = useNavigate();
    const caseId = params.id;
    const isEdit = mode === "edit" && !!caseId;

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [modality, setModality] = useState(MODALITIES[0]);
    const [species, setSpecies] = useState(SPECIES[0]);
    const [lesion, setLesion] = useState({ cx: 0.5, cy: 0.5, r: 0.2 });
    const [findings, setFindings] = useState<LookupFinding[]>([]);
    const [diagnoses, setDiagnoses] = useState<LookupDiagnosis[]>([]);
    const [selectedFindingIds, setSelectedFindingIds] = useState<Set<number>>(new Set());
    const [requiredFindingIds, setRequiredFindingIds] = useState<Set<number>>(new Set());
    const [diagnosisWeights, setDiagnosisWeights] = useState<Record<number, number>>({});
    const [imageFile, setImageFile] = useState<File | null>(null);
    const [imagePreview, setImagePreview] = useState<string>("");
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [findingSearch, setFindingSearch] = useState("");
    const [diagnosisSearch, setDiagnosisSearch] = useState("");
    const imageRef = useRef<HTMLImageElement | null>(null);

    useEffect(() => {
        const loadData = async () => {
            setLoading(true);
            try {
                const lookup = await api.get<LookupResponse>("/admin/lookups");
                setFindings(lookup.findings);
                setDiagnoses(lookup.diagnoses);
                if (isEdit) {
                    const detail = await api.get<AdminCaseDetail>(`/admin/cases/${caseId}`);
                    setTitle(detail.title);
                    setDescription(detail.description || "");
                    setModality(detail.modality);
                    setSpecies(detail.species);
                    setLesion({
                        cx: detail.lesionData?.cx ?? 0.5,
                        cy: detail.lesionData?.cy ?? 0.5,
                        r: detail.lesionData?.r ?? 0.2,
                    });
                    setSelectedFindingIds(new Set(detail.findings.map((f) => f.findingId)));
                    setRequiredFindingIds(
                        new Set(detail.findings.filter((f) => f.required).map((f) => f.findingId))
                    );
                    setDiagnosisWeights(
                        detail.diagnoses.reduce<Record<number, number>>((acc, d) => {
                            acc[d.diagnosisId] = d.weight;
                            return acc;
                        }, {})
                    );
                    setImagePreview(detail.imageUrl);
                }
            } catch (err: any) {
                setError(err?.message || "Failed to load admin data");
            } finally {
                setLoading(false);
            }
        };
        void loadData();
    }, [caseId, isEdit]);

    useEffect(() => {
        return () => {
            if (imagePreview && imageFile) {
                URL.revokeObjectURL(imagePreview);
            }
        };
    }, [imagePreview, imageFile]);

    const handleFindingToggle = (id: number) => {
        setSelectedFindingIds((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
                setRequiredFindingIds((req) => {
                    const updated = new Set(req);
                    updated.delete(id);
                    return updated;
                });
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const handleRequiredToggle = (id: number) => {
        if (!selectedFindingIds.has(id)) return;
        setRequiredFindingIds((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const handleDiagnosisWeightChange = (id: number, value: string) => {
        const numeric = parseFloat(value);
        setDiagnosisWeights((prev) => {
            const next = { ...prev };
            if (Number.isNaN(numeric)) {
                delete next[id];
            } else {
                next[id] = numeric;
            }
            return next;
        });
    };

    const handleImageChange = (file: File | undefined) => {
        if (!file) return;
        setImageFile(file);
        setImagePreview(URL.createObjectURL(file));
    };

    const handleImageClick = (e: React.MouseEvent<HTMLImageElement, MouseEvent>) => {
        const rect = e.currentTarget.getBoundingClientRect();
        const cx = clamp01((e.clientX - rect.left) / rect.width);
        const cy = clamp01((e.clientY - rect.top) / rect.height);
        setLesion((prev) => ({ ...prev, cx, cy }));
    };

    const filteredFindings = useMemo(() => {
        const keyword = findingSearch.toLowerCase();
        return findings.filter((f) => f.label.toLowerCase().includes(keyword));
    }, [findingSearch, findings]);

    const filteredDiagnoses = useMemo(() => {
        const keyword = diagnosisSearch.toLowerCase();
        return diagnoses.filter((d) => d.name.toLowerCase().includes(keyword));
    }, [diagnosisSearch, diagnoses]);

    const selectedFindingsList = Array.from(selectedFindingIds);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) {
            setError("Title is required");
            return;
        }
        if (!imageFile && !isEdit) {
            setError("Image is required");
            return;
        }
        setError(null);
        setSaving(true);

        const payloadFindings = findings
            .filter((f) => selectedFindingIds.has(f.id))
            .map((f) => ({
                findingId: f.id,
                required: requiredFindingIds.has(f.id),
            }));
        const payloadDiagnoses = Object.entries(diagnosisWeights)
            .filter(([, weight]) => typeof weight === "number")
            .map(([diagnosisId, weight]) => ({
                diagnosisId: Number(diagnosisId),
                weight,
            }));

        const formData = new FormData();
        formData.append("title", title);
        formData.append("description", description);
        formData.append("modality", modality);
        formData.append("species", species);
        formData.append("lesionCx", lesion.cx.toString());
        formData.append("lesionCy", lesion.cy.toString());
        formData.append("lesionR", lesion.r.toString());
        formData.append("findings", JSON.stringify(payloadFindings));
        formData.append("diagnoses", JSON.stringify(payloadDiagnoses));

        if (imageFile) {
            formData.append("image", imageFile);
        }

        try {
            if (isEdit) {
                await api.put(`/admin/cases/${caseId}`, formData);
            } else {
                await api.post("/admin/cases", formData);
            }
            navigate("/admin");
        } catch (err: any) {
            setError(err?.message || "Failed to save case");
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <AdminLayout title="Loading case..." description=" ">
                <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-6 text-slate-300">Loading...</div>
            </AdminLayout>
        );
    }

    return (
        <AdminLayout
            title={isEdit ? "Edit Case" : "Create Case"}
            description="Configure lesion target, required findings, and diagnosis weights."
        >
            <form className="grid gap-6 lg:grid-cols-3" onSubmit={handleSubmit}>
                <div className="lg:col-span-2 space-y-4">
                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow">
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <label className="text-sm text-slate-300">Title</label>
                                <input
                                    className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-sm text-slate-300">Modality</label>
                                    <select
                                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
                                        value={modality}
                                        onChange={(e) => setModality(e.target.value)}
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
                                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
                                        value={species}
                                        onChange={(e) => setSpecies(e.target.value)}
                                    >
                                        {SPECIES.map((s) => (
                                            <option key={s} value={s}>
                                                {s}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                        </div>
                        <div className="mt-3">
                            <label className="text-sm text-slate-300">Description</label>
                            <textarea
                                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
                                rows={3}
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                            />
                        </div>
                    </div>

                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow">
                        <div className="flex items-center justify-between">
                            <div>
                                <div className="text-sm font-semibold text-teal-200">Case Image</div>
                                <div className="text-xs text-slate-400">
                                    Upload and click to set lesion center. Radius slider adjusts answer tolerance.
                                </div>
                            </div>
                            <label className="cursor-pointer rounded-lg border border-slate-700 px-3 py-1 text-xs font-semibold text-slate-100 hover:border-teal-400">
                                <input
                                    type="file"
                                    accept="image/*"
                                    className="hidden"
                                    onChange={(e) => handleImageChange(e.target.files?.[0])}
                                />
                                Upload image
                            </label>
                        </div>
                        {imagePreview ? (
                            <div className="mt-3">
                                <div className="relative overflow-hidden rounded-lg border border-slate-800 bg-slate-950">
                                    <img
                                        ref={imageRef}
                                        src={imagePreview}
                                        alt="Case"
                                        className="h-full w-full max-h-[480px] object-contain"
                                        onClick={handleImageClick}
                                    />
                                    <div
                                        className="pointer-events-none absolute"
                                        style={{
                                            left: `${lesion.cx * 100}%`,
                                            top: `${lesion.cy * 100}%`,
                                            width: `${lesion.r * 200}%`,
                                            height: `${lesion.r * 200}%`,
                                            transform: "translate(-50%, -50%)",
                                            borderRadius: "9999px",
                                            border: "2px solid rgba(94, 234, 212, 0.8)",
                                            background: "rgba(45, 212, 191, 0.12)",
                                        }}
                                    />
                                </div>
                                <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-300">
                                    <div className="flex items-center gap-2">
                                        <label className="text-xs text-slate-400">Radius</label>
                                        <input
                                            type="range"
                                            min={0.05}
                                            max={0.5}
                                            step={0.01}
                                            value={lesion.r}
                                            onChange={(e) =>
                                                setLesion((prev) => ({ ...prev, r: parseFloat(e.target.value) }))
                                            }
                                        />
                                        <span className="text-xs text-slate-400">{lesion.r.toFixed(2)}</span>
                                    </div>
                                    <div className="text-xs text-slate-400">
                                        Click on the image to set center ({lesion.cx.toFixed(2)}, {lesion.cy.toFixed(2)})
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="mt-3 rounded-lg border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
                                Upload an image to start placing the lesion marker.
                            </div>
                        )}
                    </div>
                </div>

                <div className="space-y-4">
                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow">
                        <div className="mb-3 flex items-center justify-between">
                            <div>
                                <div className="text-sm font-semibold text-teal-200">Findings</div>
                                <div className="text-xs text-slate-400">Select optional findings and mark required ones.</div>
                            </div>
                            <input
                                className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1 text-xs text-slate-100"
                                placeholder="Search"
                                value={findingSearch}
                                onChange={(e) => setFindingSearch(e.target.value)}
                            />
                        </div>
                        <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                            {filteredFindings.map((finding) => {
                                const selected = selectedFindingIds.has(finding.id);
                                const required = requiredFindingIds.has(finding.id);
                                return (
                                    <div
                                        key={finding.id}
                                        className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2 text-sm"
                                    >
                                        <div className="flex items-center gap-2">
                                            <input
                                                type="checkbox"
                                                checked={selected}
                                                onChange={() => handleFindingToggle(finding.id)}
                                            />
                                            <span>{finding.label}</span>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <label className="text-xs text-slate-400">Required</label>
                                            <input
                                                type="checkbox"
                                                checked={required}
                                                disabled={!selected}
                                                onChange={() => handleRequiredToggle(finding.id)}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                            {filteredFindings.length === 0 && (
                                <div className="text-xs text-slate-400">No findings found.</div>
                            )}
                        </div>
                        <div className="mt-3 text-xs text-slate-400">
                            Selected: {selectedFindingsList.length} | Required: {requiredFindingIds.size}
                        </div>
                    </div>

                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow">
                        <div className="mb-3 flex items-center justify-between">
                            <div>
                                <div className="text-sm font-semibold text-teal-200">Diagnoses</div>
                                <div className="text-xs text-slate-400">
                                    Assign weights. Blank entries are ignored. Weights are normalized on save.
                                </div>
                            </div>
                            <input
                                className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1 text-xs text-slate-100"
                                placeholder="Search"
                                value={diagnosisSearch}
                                onChange={(e) => setDiagnosisSearch(e.target.value)}
                            />
                        </div>
                        <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                            {filteredDiagnoses.map((dx) => {
                                const weight = diagnosisWeights[dx.id] ?? "";
                                return (
                                    <div
                                        key={dx.id}
                                        className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2 text-sm"
                                    >
                                        <div>
                                            <div className="font-semibold">{dx.name}</div>
                                            <div className="text-xs text-slate-400">ID: {dx.id}</div>
                                        </div>
                                        <input
                                            type="number"
                                            min={0}
                                            step={0.1}
                                            value={weight}
                                            onChange={(e) => handleDiagnosisWeightChange(dx.id, e.target.value)}
                                            className="w-24 rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-100"
                                        />
                                    </div>
                                );
                            })}
                            {filteredDiagnoses.length === 0 && (
                                <div className="text-xs text-slate-400">No diagnoses found.</div>
                            )}
                        </div>
                    </div>

                    {error && (
                        <div className="rounded-lg border border-red-500/50 bg-red-500/10 px-3 py-2 text-sm text-red-200">
                            {error}
                        </div>
                    )}

                    <div className="flex flex-wrap items-center gap-3">
                        <button
                            type="submit"
                            disabled={saving}
                            className="rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-900 hover:bg-teal-400 disabled:opacity-60"
                        >
                            {saving ? "Saving..." : isEdit ? "Update Case" : "Create Case"}
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate("/admin")}
                            className="rounded-lg border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-200 hover:border-teal-400"
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            </form>
        </AdminLayout>
    );
}
