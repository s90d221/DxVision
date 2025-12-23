import { useEffect, useState } from "react";
import { api } from "../lib/api";

type Health = {
    status: string;
    db?: string;
    time?: string;
};

export default function QuizPage() {
    const [health, setHealth] = useState<Health | null>(null);

    useEffect(() => {
        api.get("/health")
            .then((res) => setHealth(res.data))
            .catch(() => setHealth({ status: "DOWN" }));
    }, []);

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            {/* Top bar */}
            <header className="border-b border-slate-800 px-6 py-4">
                <div className="flex items-center justify-between">
                    <div className="text-lg font-semibold tracking-tight">DxVision</div>
                    <div className="text-sm text-slate-300">
                        API: {health?.status ?? "…"} {health?.db ? `(DB: ${health.db})` : ""}
                    </div>
                </div>

                {/* Step indicator */}
                <div className="mt-3 flex gap-2 text-sm">
                    <StepBadge active label="Step 1" desc="소견 선택" />
                    <StepBadge label="Step 2" desc="병변 위치" />
                    <StepBadge label="Step 3" desc="진단 선택" />
                </div>
            </header>

            {/* Main */}
            <main className="grid grid-cols-12 gap-4 px-6 py-6">
                {/* Viewer */}
                <section className="col-span-8 rounded-xl border border-slate-800 bg-slate-900/40 p-4">
                    <div className="flex items-center justify-between">
                        <div className="font-medium">Image Viewer</div>
                        <div className="text-xs text-slate-400">Zoom / Pan / Windowing (예정)</div>
                    </div>

                    <div className="mt-4 aspect-video w-full rounded-lg bg-slate-950/60 grid place-items-center">
                        <span className="text-slate-500">영상 뷰어 자리</span>
                    </div>
                </section>

                {/* Right panel */}
                <aside className="col-span-4 rounded-xl border border-slate-800 bg-slate-900/40 p-4">
                    <div className="font-medium">Question Panel</div>
                    <p className="mt-2 text-sm text-slate-300">
                        여기서 Step별 질문/선택지/제출 버튼을 보여줍니다.
                    </p>

                    <div className="mt-4 space-y-3">
                        <button className="w-full rounded-lg bg-teal-500/90 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400">
                            제출
                        </button>
                        <div className="flex gap-2">
                            <button className="w-1/2 rounded-lg border border-slate-700 px-4 py-2 text-sm hover:bg-slate-800">
                                이전
                            </button>
                            <button className="w-1/2 rounded-lg border border-slate-700 px-4 py-2 text-sm hover:bg-slate-800">
                                다음
                            </button>
                        </div>
                    </div>
                </aside>
            </main>
        </div>
    );
}

function StepBadge({
                       label,
                       desc,
                       active,
                   }: {
    label: string;
    desc: string;
    active?: boolean;
}) {
    return (
        <div
            className={[
                "rounded-full border px-3 py-1",
                active ? "border-teal-400 text-teal-200 bg-teal-500/10" : "border-slate-700 text-slate-300",
            ].join(" ")}
        >
            <span className="font-semibold">{label}</span>
            <span className="ml-2 text-slate-400">{desc}</span>
        </div>
    );
}
