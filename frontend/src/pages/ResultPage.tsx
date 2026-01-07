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
    ];
    const hasAnyExpert = expertSections.some((section) => (section.value ?? "").trim().length > 0);

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
                    <div className="flex items-center justify-between gap-2">
                        <h3 className="text-sm font-semibold text-teal-200">전문가 해설</h3>
                        <div className="text-xs text-slate-500">제출 후에만 확인할 수 있습니다.</div>
                    </div>
                    {hasAnyExpert ? (
                        <div className="mt-3 grid gap-3 md:grid-cols-3">
                            {expertSections.map(({ label, value }) => {
                                const trimmed = (value ?? "").trim();
                                const hasValue = trimmed.length > 0;
                                return (
                                    <div
                                        key={label}
                                        className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"
                                    >
                                        <div className="text-xs font-semibold text-teal-200">{label}</div>
                                        {hasValue ? (
                                            <p className="mt-2 whitespace-pre-wrap text-sm text-slate-100">{trimmed}</p>
                                        ) : (
                                            <p className="mt-2 text-xs text-slate-500">해설 없음</p>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    ) : (
                        <div className="mt-2 rounded-lg border border-slate-800 bg-slate-900/40 p-3 text-sm text-slate-400">
                            제공된 전문가 해설이 없습니다.
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
