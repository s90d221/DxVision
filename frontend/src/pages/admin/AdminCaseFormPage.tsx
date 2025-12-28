import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import AdminLayout from "../../components/AdminLayout";
import { api } from "../../lib/api";

type LookupFinding = { id: number; label: string };
type LookupDiagnosis = { id: number; name: string };

type LesionType = "CIRCLE" | "RECT";

type CircleLesion = { type: "CIRCLE"; cx: number; cy: number; r: number };
type RectLesion = { type: "RECT"; x: number; y: number; w: number; h: number };
type LesionState = CircleLesion | RectLesion;

type AdminCaseDetail = {
    id: number;
    title: string;
    description?: string;
    modality: string;
    species: string;
    imageUrl: string;
    lesionShapeType?: LesionType;
    lesionData?: {
        type?: string;
        cx?: number;
        cy?: number;
        r?: number;
        x?: number;
        y?: number;
        w?: number;
        h?: number;
    };
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

const MODALITIES = ["XRAY", "ULTRASOUND", "CT", "MRI"];
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
    const [lesionMode, setLesionMode] = useState<LesionType>("CIRCLE");
    const [circleLesion, setCircleLesion] = useState<CircleLesion>({ type: "CIRCLE", cx: 0.5, cy: 0.5, r: 0.2 });
    const [rectLesion, setRectLesion] = useState<RectLesion>({ type: "RECT", x: 0.3, y: 0.3, w: 0.2, h: 0.2 });
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
    const imageContainerRef = useRef<HTMLDivElement | null>(null);
    const dragStartRef = useRef<{ x: number; y: number } | null>(null);
    const [draggingRect, setDraggingRect] = useState(false);
    const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });

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
                    const incomingTypeRaw = (detail.lesionData?.type || detail.lesionShapeType || "CIRCLE").toUpperCase();
                    const incomingType: LesionType = incomingTypeRaw === "RECT" ? "RECT" : "CIRCLE";
                    const nextCircle: CircleLesion = {
                        type: "CIRCLE",
                        cx: clamp01(detail.lesionData?.cx ?? 0.5),
                        cy: clamp01(detail.lesionData?.cy ?? 0.5),
                        r: clamp01(detail.lesionData?.r ?? 0.2),
                    };
                    const nextRect: RectLesion = {
                        type: "RECT",
                        x: clamp01(detail.lesionData?.x ?? 0.3),
                        y: clamp01(detail.lesionData?.y ?? 0.3),
                        w: clamp01(detail.lesionData?.w ?? 0.2),
                        h: clamp01(detail.lesionData?.h ?? 0.2),
                    };
                    setCircleLesion(nextCircle);
                    setRectLesion(nextRect);
                    setLesionMode(incomingType === "RECT" ? "RECT" : "CIRCLE");
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

    const updateContainerSize = useCallback(() => {
        if (!imageContainerRef.current) return;
        const rect = imageContainerRef.current.getBoundingClientRect();
        setContainerSize({ width: rect.width, height: rect.height });
    }, []);

    useEffect(() => {
        const container = imageContainerRef.current;
        if (!container) return;
        updateContainerSize();
        const resizeObserver = new ResizeObserver(() => updateContainerSize());
        resizeObserver.observe(container);
        return () => resizeObserver.disconnect();
    }, [imagePreview, updateContainerSize]);

    const getNormalizedPoint = (clientX: number, clientY: number) => {
        if (!imageContainerRef.current) {
            return { x: 0.5, y: 0.5 };
        }
        const rect = imageContainerRef.current.getBoundingClientRect();
        const safeWidth = rect.width || 1;
        const safeHeight = rect.height || 1;
        return {
            x: clamp01((clientX - rect.left) / safeWidth),
            y: clamp01((clientY - rect.top) / safeHeight),
        };
    };

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

    const handlePointerDown = (e: ReactPointerEvent<HTMLDivElement>) => {
        e.preventDefault();
        if (!imageContainerRef.current) return;
        const norm = getNormalizedPoint(e.clientX, e.clientY);
        imageContainerRef.current.setPointerCapture(e.pointerId);
        if (lesionMode === "CIRCLE") {
            setCircleLesion((prev) => ({ ...prev, cx: norm.x, cy: norm.y }));
            dragStartRef.current = null;
        } else {
            dragStartRef.current = { x: norm.x, y: norm.y };
            setRectLesion({ type: "RECT", x: norm.x, y: norm.y, w: 0.001, h: 0.001 });
            setDraggingRect(true);
        }
    };

    const handlePointerMove = (e: ReactPointerEvent<HTMLDivElement>) => {
        if (!draggingRect || lesionMode !== "RECT" || !imageContainerRef.current) return;
        e.preventDefault();
        const norm = getNormalizedPoint(e.clientX, e.clientY);
        const start = dragStartRef.current ?? { x: norm.x, y: norm.y };
        const x = clamp01(Math.min(start.x, norm.x));
        const y = clamp01(Math.min(start.y, norm.y));
        const w = clamp01(Math.abs(norm.x - start.x));
        const h = clamp01(Math.abs(norm.y - start.y));
        setRectLesion({ type: "RECT", x, y, w, h });
    };

    const handlePointerUp = (e: ReactPointerEvent<HTMLDivElement>) => {
        if (imageContainerRef.current?.hasPointerCapture(e.pointerId)) {
            imageContainerRef.current.releasePointerCapture(e.pointerId);
        }
        if (draggingRect && lesionMode === "RECT") {
            handlePointerMove(e);
            setDraggingRect(false);
            dragStartRef.current = null;
            setRectLesion((prev) => {
                if (prev.w < 0.01 || prev.h < 0.01) {
                    return { ...prev, w: Math.max(prev.w, 0.05), h: Math.max(prev.h, 0.05) };
                }
                return prev;
            });
            return;
        }
        setDraggingRect(false);
        dragStartRef.current = null;
    };

    const handlePointerCancel = (e: ReactPointerEvent<HTMLDivElement>) => {
        handlePointerUp(e);
    };

    // Normalized lesion coordinates (0..1) are scaled to the measured container size so the overlay remains
    // accurate and perfectly circular even when the displayed image is resized.
    const circleOverlayStyle = useMemo(() => {
        const { width, height } = containerSize;
        if (width <= 0 || height <= 0) {
            return {
                left: `${circleLesion.cx * 100}%`,
                top: `${circleLesion.cy * 100}%`,
                width: `${circleLesion.r * 200}%`,
                height: `${circleLesion.r * 200}%`,
                transform: "translate(-50%, -50%)",
            };
        }
        const minDimension = Math.min(width, height);
        const radiusPx = circleLesion.r * minDimension;
        const diameterPx = radiusPx * 2;
        return {
            left: `${circleLesion.cx * width}px`,
            top: `${circleLesion.cy * height}px`,
            width: `${diameterPx}px`,
            height: `${diameterPx}px`,
            transform: "translate(-50%, -50%)",
        };
    }, [circleLesion, containerSize]);

    const rectOverlayStyle = useMemo(() => {
        const { width, height } = containerSize;
        if (width <= 0 || height <= 0) {
            return {
                left: `${rectLesion.x * 100}%`,
                top: `${rectLesion.y * 100}%`,
                width: `${rectLesion.w * 100}%`,
                height: `${rectLesion.h * 100}%`,
            };
        }
        return {
            left: `${rectLesion.x * width}px`,
            top: `${rectLesion.y * height}px`,
            width: `${rectLesion.w * width}px`,
            height: `${rectLesion.h * height}px`,
        };
    }, [rectLesion, containerSize]);

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

        const currentLesion: LesionState = lesionMode === "CIRCLE" ? circleLesion : rectLesion;
        if (lesionMode === "RECT" && (currentLesion.w <= 0 || currentLesion.h <= 0)) {
            setSaving(false);
            setError("Please drag to create a rectangle with width/height greater than 0.");
            return;
        }

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
        formData.append("lesionType", lesionMode);
        if (lesionMode === "CIRCLE") {
            formData.append("lesionCx", (currentLesion as CircleLesion).cx.toString());
            formData.append("lesionCy", (currentLesion as CircleLesion).cy.toString());
            formData.append("lesionR", (currentLesion as CircleLesion).r.toString());
        } else {
            const rect = currentLesion as RectLesion;
            // Provide fallback center for backward compatibility
            formData.append("lesionCx", (rect.x + rect.w / 2).toString());
            formData.append("lesionCy", (rect.y + rect.h / 2).toString());
            formData.append("lesionR", Math.max(rect.w, rect.h) / 2 + "");
            formData.append("lesionX", rect.x.toString());
            formData.append("lesionY", rect.y.toString());
            formData.append("lesionW", rect.w.toString());
            formData.append("lesionH", rect.h.toString());
        }
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
                        <div className="flex items-center justify-between gap-3">
                            <div>
                                <div className="text-sm font-semibold text-teal-200">Case Image</div>
                                <div className="text-xs text-slate-400">
                                    Choose a selection mode and click/drag on the image.
                                </div>
                            </div>
                            <div className="flex flex-wrap items-center gap-2">
                                <div className="flex overflow-hidden rounded-lg border border-slate-700 text-xs">
                                    {(["CIRCLE", "RECT"] as LesionType[]).map((modeOption) => (
                                        <button
                                            type="button"
                                            key={modeOption}
                                            className={`px-3 py-1 font-semibold ${
                                                lesionMode === modeOption
                                                    ? "bg-teal-500 text-slate-900"
                                                    : "bg-slate-900 text-slate-200 hover:bg-slate-800"
                                            }`}
                                            onClick={() => setLesionMode(modeOption)}
                                        >
                                            {modeOption === "CIRCLE" ? "Circle" : "Rectangle"}
                                        </button>
                                    ))}
                                </div>
                                <button
                                    type="button"
                                    className="rounded-lg border border-slate-700 px-3 py-1 text-xs font-semibold text-slate-100 hover:border-teal-400"
                                    onClick={() =>
                                        lesionMode === "CIRCLE"
                                            ? setCircleLesion({ type: "CIRCLE", cx: 0.5, cy: 0.5, r: 0.2 })
                                            : setRectLesion({ type: "RECT", x: 0.3, y: 0.3, w: 0.2, h: 0.2 })
                                    }
                                >
                                    Clear selection
                                </button>
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
                        </div>
                        {imagePreview ? (
                            <div className="mt-3">
                                <div
                                    ref={imageContainerRef}
                                    className="relative overflow-hidden rounded-lg border border-slate-800 bg-slate-950"
                                    style={{ touchAction: "none", cursor: "crosshair" }}
                                    onPointerDown={handlePointerDown}
                                    onPointerMove={handlePointerMove}
                                    onPointerUp={handlePointerUp}
                                    onPointerLeave={handlePointerUp}
                                    onPointerCancel={handlePointerCancel}
                                >
                                    {/* Prevent default image dragging so the cursor never shows the "not allowed" icon while drawing. */}
                                    <img
                                        ref={imageRef}
                                        src={imagePreview}
                                        alt="Case"
                                        className="h-full w-full max-h-[480px] select-none object-contain"
                                        draggable={false}
                                        onDragStart={(event) => event.preventDefault()}
                                        onLoad={updateContainerSize}
                                    />
                                    {lesionMode === "CIRCLE" && (
                                        <div
                                            className="pointer-events-none absolute"
                                            style={{
                                                ...circleOverlayStyle,
                                                borderRadius: "9999px",
                                                border: "2px solid rgba(94, 234, 212, 0.8)",
                                                background: "rgba(45, 212, 191, 0.12)",
                                            }}
                                        />
                                    )}
                                    {lesionMode === "RECT" && (
                                        <div
                                            className="pointer-events-none absolute"
                                            style={{
                                                ...rectOverlayStyle,
                                                borderRadius: "12px",
                                                border: "2px solid rgba(94, 234, 212, 0.8)",
                                                background: "rgba(45, 212, 191, 0.12)",
                                            }}
                                        />
                                    )}
                                </div>
                                <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-300">
                                    {lesionMode === "CIRCLE" ? (
                                        <>
                                            <div className="flex items-center gap-2">
                                                <label className="text-xs text-slate-400">Radius</label>
                                                <input
                                                    type="range"
                                                    min={0.05}
                                                    max={0.5}
                                                    step={0.01}
                                                    value={circleLesion.r}
                                                    onChange={(e) =>
                                                        setCircleLesion((prev) => ({
                                                            ...prev,
                                                            r: parseFloat(e.target.value),
                                                        }))
                                                    }
                                                />
                                                <span className="text-xs text-slate-400">{circleLesion.r.toFixed(2)}</span>
                                            </div>
                                            <div className="text-xs text-slate-400">
                                                Click the image to set center ({circleLesion.cx.toFixed(2)},{" "}
                                                {circleLesion.cy.toFixed(2)})
                                            </div>
                                        </>
                                    ) : (
                                        <>
                                            <div className="text-xs text-slate-400">
                                                Drag to draw a rectangle. Position ({rectLesion.x.toFixed(2)},{" "}
                                                {rectLesion.y.toFixed(2)}) size {rectLesion.w.toFixed(2)} ×{" "}
                                                {rectLesion.h.toFixed(2)}
                                            </div>
                                            {draggingRect && (
                                                <div className="rounded bg-teal-500/10 px-2 py-1 text-xs text-teal-200">
                                                    Dragging...
                                                </div>
                                            )}
                                        </>
                                    )}
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
