import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { clearToken } from "../lib/auth";

type CaseOption = {
    id: number;
    version: number;
    title: string;
    description: string;
    modality: string;
    species: string;
    imageUrl: string;
    lesionShapeType: string;
    findings: { id: number; label: string }[];
    diagnoses: { id: number; name: string }[];
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

export default function QuizPage() {
    const [quizCase, setQuizCase] = useState<CaseOption | null>(null);
    const [selectedFindings, setSelectedFindings] = useState<Set<number>>(new Set());
    const [selectedDiagnoses, setSelectedDiagnoses] = useState<Set<number>>(new Set());
    const [clickPoint, setClickPoint] = useState<{ x: number; y: number } | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const imgRef = useRef<HTMLImageElement | null>(null);
    const navigate = useNavigate();

    const fetchCase = async () => {
        setError(null);
        try {
            const data = await api.get<CaseOption>("/cases/random");
            setQuizCase(data);
            setSelectedFindings(new Set());
            setSelectedDiagnoses(new Set());
            setClickPoint(null);
        } catch (err: any) {
            setError(err?.message || "Failed to load case");
        }
    };

    useEffect(() => {
        fetchCase();
    }, []);

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
        setLoading(true);
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
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-900 text-slate-100">
            <header className="border-b border-slate-800 px-6 py-4 flex items-center justify-between">
                <div>
                    <div className="text-lg font-semibold">DxVision Quiz</div>
                    <div className="text-sm text-slate-400">Train findings → location → diagnosis</div>
                </div>
                <button
                    className="text-sm text-teal-300 hover:text-teal-200"
                    onClick={() => {
                        clearToken();
                        navigate("/login", { replace: true });
                    }}
                >
                    Logout
                </button>
            </header>

            <main className="grid gap-6 px-6 py-6 md:grid-cols-12">
                <section className="md:col-span-8 rounded-xl border border-slate-800 bg-slate-900/50 p-4">
                    <div className="flex items-center justify-between">
                        <div>
                            <div className="text-xl font-semibold">{quizCase?.title ?? "Loading..."}</div>
                            <div className="text-sm text-slate-400">{quizCase?.description}</div>
                        </div>
                        <button
                            className="rounded-lg border border-slate-700 px-3 py-1 text-sm hover:bg-slate-800"
                            onClick={fetchCase}
                            disabled={loading}
                        >
                            New random case
                        </button>
                    </div>
                    <div className="mt-4">
                        {quizCase ? (
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
                        ) : (
                            <div className="grid h-64 place-items-center rounded-lg border border-slate-800 bg-slate-950 text-slate-500">
                                Loading case...
                            </div>
                        )}
                    </div>
                    {clickPoint && (
                        <div className="mt-2 text-xs text-slate-300">
                            Click: ({clickPoint.x.toFixed(3)}, {clickPoint.y.toFixed(3)})
                        </div>
                    )}
                </section>

                <aside className="md:col-span-4 space-y-4 rounded-xl border border-slate-800 bg-slate-900/50 p-4">
                    <div>
                        <h3 className="text-sm font-semibold text-teal-200">Findings</h3>
                        <div className="mt-2 space-y-1">
                            {quizCase?.findings.map((f) => (
                                <label key={f.id} className="flex items-center gap-2 text-sm">
                                    <input
                                        type="checkbox"
                                        checked={selectedFindings.has(f.id)}
                                        onChange={() => toggleFinding(f.id)}
                                    />
                                    {f.label}
                                </label>
                            )) || <p className="text-xs text-slate-400">Loading...</p>}
                        </div>
                    </div>

                    <div>
                        <h3 className="text-sm font-semibold text-teal-200">Diagnoses</h3>
                        <div className="mt-2 space-y-1">
                            {quizCase?.diagnoses.map((d) => (
                                <label key={d.id} className="flex items-center gap-2 text-sm">
                                    <input
                                        type="checkbox"
                                        checked={selectedDiagnoses.has(d.id)}
                                        onChange={() => toggleDiagnosis(d.id)}
                                    />
                                    {d.name}
                                </label>
                            )) || <p className="text-xs text-slate-400">Loading...</p>}
                        </div>
                    </div>

                    {error && <div className="text-sm text-red-400">{error}</div>}

                    <button
                        className="w-full rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50"
                        onClick={handleSubmit}
                        disabled={loading || !quizCase}
                    >
                        {loading ? "Submitting..." : "Submit"}
                    </button>
                </aside>
            </main>
        </div>
    );
}
