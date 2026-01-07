import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import GlobalHeader from "../components/GlobalHeader";
import { ApiError, api } from "../lib/api";
import { clearToken, type UserInfo } from "../lib/auth";

type AttemptResult = {
    attemptId: number;
    caseId: number;
    caseVersion: number;
    findingsScore: number;
    locationScore: number;
    diagnosisScore: number;
    finalScore: number;
    explanation: string;
    expertFindingExplanation?: string | null;
    expertDiagnosisExplanation?: string | null;
    expertLocationExplanation?: string | null;
    locationGrade: string;
    correctFindings: string[];
    correctDiagnoses: string[];
};

export default function ResultPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const [result] = useState<AttemptResult | null>(() => {
        if (location.state) return location.state as AttemptResult;
        const stored = localStorage.getItem("dxvision_last_attempt");
        return stored ? (JSON.parse(stored) as AttemptResult) : null;
    });
    const [user, setUser] = useState<UserInfo | null>(null);

    useEffect(() => {
        if (!result) {
            navigate("/home", { replace: true });
        }
    }, [result, navigate]);

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

    if (!result) return null;

    const expertSections = [
        { label: "소견 해설", value: result.expertFindingExplanation },
        { label: "진단 해설", value: result.expertDiagnosisExplanation },
        { label: "위치 해설", value: result.expertLocationExplanation },
    ].filter((section): section is { label: string; value: string } => {
        return !!section.value && section.value.trim().length > 0;
    });

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <GlobalHeader
                subtitle={`Score Report of Case #${result.caseId} (version ${result.caseVersion})`}
                isAdmin={isAdmin}
                user={user}
                onUserChange={setUser}
                actions={
                    <div className="flex flex-wrap gap-2">
                        <button
                            className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                            onClick={() => navigate(`/quiz/${result.caseId}`, { replace: true })}
                            type="button"
                        >
                            Retry this case
                        </button>
                        <button
                            className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                            onClick={() => navigate("/quiz/random", { replace: true })}
                            type="button"
                        >
                            New problem
                        </button>
                        {!isAdmin && (
                            <button
                                className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                                onClick={() => navigate("/home", { replace: true })}
                                type="button"
                            >
                                Back to home
                            </button>
                        )}
                    </div>
                }
            />

            <main className="px-6 py-6 space-y-4">
                <div className="h-px w-full bg-slate-800/70" />
                <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-4 grid gap-4 md:grid-cols-2">
                    <ScoreCard label="Findings" value={result.findingsScore} />
                    <ScoreCard label="Location" value={result.locationScore} extra={result.locationGrade} />
                    <ScoreCard label="Diagnosis" value={result.diagnosisScore} />
                    <ScoreCard label="Final Score" value={result.finalScore} highlight />
                </section>

                <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                    <h3 className="text-sm font-semibold text-teal-200">Explanation</h3>
                    <pre className="mt-2 whitespace-pre-wrap text-sm text-slate-200">{result.explanation}</pre>
                </section>

                <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                    <h3 className="text-sm font-semibold text-teal-200">전문가 해설</h3>
                    {expertSections.length === 0 ? (
                        <div className="mt-2 text-sm text-slate-400">해설 없음</div>
                    ) : (
                        <div className="mt-3 space-y-4">
                            {expertSections.map((section) => (
                                <div key={section.label}>
                                    <div className="text-sm font-semibold text-slate-200">{section.label}</div>
                                    <pre className="mt-1 whitespace-pre-wrap text-sm text-slate-200">
                                        {section.value}
                                    </pre>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-4 grid gap-4 md:grid-cols-2">
                    <div>
                        <h4 className="text-sm font-semibold text-teal-200">Correct Findings</h4>
                        <ul className="mt-2 list-disc pl-4 text-sm text-slate-200">
                            {result.correctFindings.map((f) => (
                                <li key={f}>{f}</li>
                            ))}
                        </ul>
                    </div>
                    <div>
                        <h4 className="text-sm font-semibold text-teal-200">Correct Diagnoses</h4>
                        <ul className="mt-2 list-disc pl-4 text-sm text-slate-200">
                            {result.correctDiagnoses.map((d) => (
                                <li key={d}>{d}</li>
                            ))}
                        </ul>
                    </div>
                </section>
            </main>
        </div>
    );
}

function ScoreCard({
                       label,
                       value,
                       extra,
                       highlight,
                   }: {
    label: string;
    value: number;
    extra?: string;
    highlight?: boolean;
}) {
    return (
        <div
            className={`rounded-lg border p-4 ${
                highlight ? "border-teal-400 bg-teal-500/10" : "border-slate-800 bg-slate-950/40"
            }`}
        >
            <div className="text-sm text-slate-300">{label}</div>
            <div className="text-2xl font-bold text-teal-200">{value.toFixed(1)}</div>
            {extra && <div className="text-xs text-slate-400">Grade: {extra}</div>}
        </div>
    );
}
