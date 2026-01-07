import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import { clearToken, type UserInfo } from "../lib/auth";
import GlobalHeader from "../components/GlobalHeader";

type OptionItem = { id: number; label: string; description?: string | null };
type OptionFolder = { id: number; name: string; sortOrder: number; systemDefault?: boolean; items: OptionItem[] };

type CaseOption = {
    id: number;
    version: number;
    title: string;
    description: string;
    modality: string;
    species: string;
    imageUrl: string;
    lesionShapeType: string;
    findings: { id: number; label: string; description?: string | null }[];
    diagnoses: { id: number; name: string; description?: string | null }[];
    findingFolders?: OptionFolder[];
    diagnosisFolders?: OptionFolder[];
};

type AttemptResult = {
    attemptId: number;
    caseId: number;
    caseVersion: number;
    findingsScore: number;
    locationScore: number;
    diagnosisScore: number;
    finalScore: number;
    explanation: string;
    locationGrade: string;
    correctFindings: string[];
    correctDiagnoses: string[];
};

type QuizPageProps = {
    mode?: "random" | "byId";
};

export default function QuizPage({ mode = "random" }: QuizPageProps) {
    const { caseId: caseIdParam } = useParams();
    const numericCaseId = caseIdParam ? Number(caseIdParam) : undefined;
    const caseId = mode === "byId" && Number.isFinite(numericCaseId) ? numericCaseId : undefined;

    const [quizCase, setQuizCase] = useState<CaseOption | null>(null);
    const [selectedFindings, setSelectedFindings] = useState<Set<number>>(new Set());
    const [selectedDiagnoses, setSelectedDiagnoses] = useState<Set<number>>(new Set());
    const [findingFolders, setFindingFolders] = useState<OptionFolder[]>([]);
    const [diagnosisFolders, setDiagnosisFolders] = useState<OptionFolder[]>([]);
    const [openFindingFolders, setOpenFindingFolders] = useState<Set<number>>(new Set());
    const [openDiagnosisFolders, setOpenDiagnosisFolders] = useState<Set<number>>(new Set());
    const [clickPoint, setClickPoint] = useState<{ x: number; y: number } | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loadingCase, setLoadingCase] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [user, setUser] = useState<UserInfo | null>(null);
    const imgRef = useRef<HTMLImageElement | null>(null);
    const navigate = useNavigate();

    const fetchCase = async () => {
        setError(null);
        setLoadingCase(true);
        try {
            const endpoint = caseId ? `/cases/${caseId}` : "/cases/random";
            const data = await api.get<CaseOption>(endpoint);
            setQuizCase(data);
            const fallbackFindings: OptionItem[] = (data.findings ?? []).map((f) => ({
                id: f.id,
                label: f.label,
                description: f.description ?? undefined,
            }));
            const fallbackDiagnoses: OptionItem[] = (data.diagnoses ?? []).map((d) => ({
                id: d.id,
                label: d.name,
                description: d.description ?? undefined,
            }));
            const mappedFindingFolders = buildFolderList(data.findingFolders, fallbackFindings, "Findings", -1);
            const mappedDiagnosisFolders = buildFolderList(data.diagnosisFolders, fallbackDiagnoses, "Diagnoses", -2);
            setFindingFolders(mappedFindingFolders);
            setDiagnosisFolders(mappedDiagnosisFolders);
            setOpenFindingFolders(new Set(mappedFindingFolders.map((folder) => folder.id)));
            setOpenDiagnosisFolders(new Set(mappedDiagnosisFolders.map((folder) => folder.id)));
            setSelectedFindings(new Set());
            setSelectedDiagnoses(new Set());
            setClickPoint(null);
        } catch (err: unknown) {
            const apiError = err as ApiError;
            if (apiError.status === 401 || apiError.status === 403) {
                clearToken();
                navigate("/login", { replace: true });
                return;
            }
            setError(apiError?.message || "Failed to load case");
        } finally {
            setLoadingCase(false);
        }
    };

    useEffect(() => {
        fetchCase();
    }, [caseId, mode]);

    useEffect(() => {
        api.get<UserInfo>("/auth/me")
            .then(setUser)
            .catch((err: unknown) => {
                const apiError = err as ApiError;
                if (apiError.status === 401 || apiError.status === 403) {
                    clearToken();
                    navigate("/login", { replace: true });
                }
            });
    }, [navigate]);

    const isAdmin = user?.role === "ADMIN";
    const subtitle =
        mode === "byId" && caseId ? `Retry Case #${caseId}` : `New Case #${caseId}`;

    const buildFolderList = (
        folders: OptionFolder[] | undefined,
        fallbackItems: OptionItem[],
        fallbackName: string,
        fallbackId: number
    ): OptionFolder[] => {
        if (folders && folders.length > 0) {
            return folders.map((folder) => ({
                ...folder,
                items: (folder.items ?? []).map((item) => ({
                    id: item.id,
                    label: item.label,
                    description: item.description ?? undefined,
                })),
            }));
        }
        if (fallbackItems.length > 0) {
            return [
                {
                    id: fallbackId,
                    name: fallbackName,
                    sortOrder: 0,
                    systemDefault: true,
                    items: fallbackItems,
                },
            ];
        }
        return [];
    };

    const toggleFolderOpen = (id: number, type: "finding" | "diagnosis") => {
        if (type === "finding") {
            setOpenFindingFolders((prev) => {
                const next = new Set(prev);
                if (next.has(id)) {
                    next.delete(id);
                } else {
                    next.add(id);
                }
                return next;
            });
        } else {
            setOpenDiagnosisFolders((prev) => {
                const next = new Set(prev);
                if (next.has(id)) {
                    next.delete(id);
                } else {
                    next.add(id);
                }
                return next;
            });
        }
    };

    const toggleFinding = (id: number) => {
        const next = new Set(selectedFindings);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        setSelectedFindings(next);
    };

    const toggleDiagnosis = (id: number) => {
        const next = new Set(selectedDiagnoses);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        setSelectedDiagnoses(next);
    };

    const handleImageClick = (e: React.MouseEvent<HTMLImageElement>) => {
        if (!imgRef.current) return;
        const rect = imgRef.current.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width;
        const y = (e.clientY - rect.top) / rect.height;
        setClickPoint({ x: Math.min(Math.max(x, 0), 1), y: Math.min(Math.max(y, 0), 1) });
    };

    const handleSubmit = async () => {
        if (!quizCase) return;
        if (!clickPoint) {
            setError("Please click the image to mark the lesion location.");
            return;
        }
        setSubmitting(true);
        setError(null);
        try {
            const payload = {
                caseId: quizCase.id,
                caseVersion: quizCase.version,
                findingIds: Array.from(selectedFindings),
                diagnosisIds: Array.from(selectedDiagnoses),
                clickX: clickPoint.x,
                clickY: clickPoint.y,
            };
            const res = await api.post<AttemptResult>("/attempts", payload);
            localStorage.setItem("dxvision_last_attempt", JSON.stringify(res));
            navigate("/result", { state: res });
        } catch (err: any) {
            if (err?.message === "Unauthorized") {
                clearToken();
                navigate("/login", { replace: true });
                return;
            }
            setError(err?.message || "Submit failed");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <GlobalHeader
                subtitle={subtitle}
                isAdmin={isAdmin}
                user={user}
                onUserChange={setUser}
                actions={
                    <div className="flex flex-wrap items-center justify-end gap-2">
                        {isAdmin && (
                            <button
                                className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                                onClick={() => navigate("/admin")}
                                type="button"
                            >
                                Back to Admin Cases
                            </button>
                        )}
                        {isAdmin && mode === "byId" && quizCase && (
                            <button
                                className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                                onClick={() => navigate(`/admin/cases/${quizCase.id}/edit`)}
                                type="button"
                            >
                                Back to Edit Case
                            </button>
                        )}
                        {!isAdmin && (
                            <button
                                className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                                onClick={() => navigate("/home")}
                                type="button"
                            >
                                Home
                            </button>
                        )}
                        <button
                            className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                            onClick={() => {
                                navigate("/quiz/random");
                                if (mode === "random") {
                                    fetchCase();
                                }
                            }}
                            type="button"
                        >
                            New problem
                        </button>
                    </div>
                }
            />

            <main className="grid gap-6 px-6 py-6 md:grid-cols-12">
                <div className="md:col-span-12 h-px w-full bg-slate-800/70" />
                <section className="md:col-span-8 rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                    <div className="flex items-center justify-between">
                        <div>
                            <div className="text-xl font-semibold">{quizCase?.title ?? "Loading..."}</div>
                            <div className="text-sm text-slate-400">{quizCase?.description}</div>
                        </div>
                        <button
                            className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                            onClick={fetchCase}
                            disabled={submitting || loadingCase}
                        >
                            Reload case
                        </button>
                    </div>
                    <div className="mt-4">
                        {loadingCase && (
                            <div className="grid h-64 place-items-center rounded-lg border border-slate-800 bg-slate-950 text-slate-500">
                                Loading case...
                            </div>
                        )}
                        {!loadingCase && quizCase && (
                            <div className="relative w-full overflow-hidden rounded-lg border border-slate-800 bg-slate-950">
                                <img
                                    ref={imgRef}
                                    src={quizCase.imageUrl}
                                    alt={quizCase.title}
                                    className="w-full select-none"
                                    onClick={handleImageClick}
                                />
                                {clickPoint && (
                                    <div
                                        className="pointer-events-none absolute h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-teal-400 bg-teal-300/70"
                                        style={{ left: `${clickPoint.x * 100}%`, top: `${clickPoint.y * 100}%` }}
                                    />
                                )}
                            </div>
                        )}
                        {!loadingCase && !quizCase && (
                            <div className="grid h-64 place-items-center rounded-lg border border-slate-800 bg-slate-950 text-slate-500">
                                Failed to load case.
                            </div>
                        )}
                    </div>
                    {clickPoint && (
                        <div className="mt-2 text-xs text-slate-300">
                            Click: ({clickPoint.x.toFixed(3)}, {clickPoint.y.toFixed(3)})
                        </div>
                    )}
                </section>

                <aside className="md:col-span-4 space-y-4 rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                    <FolderedOptionList
                        title="Findings"
                        folders={findingFolders}
                        selectedIds={selectedFindings}
                        onToggle={toggleFinding}
                        openFolders={openFindingFolders}
                        onToggleFolder={(id) => toggleFolderOpen(id, "finding")}
                        disabled={submitting}
                    />

                    <FolderedOptionList
                        title="Diagnoses"
                        folders={diagnosisFolders}
                        selectedIds={selectedDiagnoses}
                        onToggle={toggleDiagnosis}
                        openFolders={openDiagnosisFolders}
                        onToggleFolder={(id) => toggleFolderOpen(id, "diagnosis")}
                        disabled={submitting}
                    />

                    {error && <div className="text-sm text-red-400">{error}</div>}

                    <button
                        className="w-full rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50"
                        onClick={handleSubmit}
                        disabled={submitting || !quizCase}
                    >
                        {submitting ? "Submitting..." : "Submit"}
                    </button>
                </aside>
            </main>
        </div>
    );
}

function FolderedOptionList({
    title,
    folders,
    selectedIds,
    onToggle,
    openFolders,
    onToggleFolder,
    disabled = false,
}: {
    title: string;
    folders: OptionFolder[];
    selectedIds: Set<number>;
    onToggle: (id: number) => void;
    openFolders: Set<number>;
    onToggleFolder: (id: number) => void;
    disabled?: boolean;
}) {
    return (
        <div>
            <h3 className="text-sm font-semibold text-teal-200">{title}</h3>
            <div className="mt-2 space-y-2">
                {folders.map((folder) => (
                    <div key={folder.id} className="rounded-lg border border-slate-800 bg-slate-900/40">
                        <button
                            className="flex w-full items-center justify-between px-3 py-2 text-left text-sm font-semibold"
                            onClick={() => onToggleFolder(folder.id)}
                            type="button"
                        >
                            <span className="flex items-center gap-2">
                                <span className="h-2 w-2 rounded-full bg-teal-400/70" />
                                {folder.name}
                            </span>
                            <span className="text-xs text-slate-400">{openFolders.has(folder.id) ? "Hide" : "Show"}</span>
                        </button>
                        {openFolders.has(folder.id) && (
                            <div className="border-t border-slate-800 px-3 py-2">
                                {folder.items.map((item) => (
                                    <label
                                        key={`${folder.id}-${item.id}`}
                                        className="flex items-center gap-2 py-1 text-sm text-slate-100"
                                    >
                                        <input
                                            type="checkbox"
                                            checked={selectedIds.has(item.id)}
                                            onChange={() => onToggle(item.id)}
                                            disabled={disabled}
                                        />
                                        <span>{item.label}</span>
                                    </label>
                                ))}
                                {folder.items.length === 0 && (
                                    <div className="py-1 text-xs text-slate-500">No options in this folder.</div>
                                )}
                            </div>
                        )}
                    </div>
                ))}
                {folders.length === 0 && (
                    <p className="text-xs text-slate-400">No options loaded yet.</p>
                )}
            </div>
        </div>
    );
}
