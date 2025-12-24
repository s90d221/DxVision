import { type ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../lib/api";
import { clearToken, getToken } from "../lib/auth";
import type { UserInfo } from "../lib/auth";

export default function ProtectedRoute({ children }: { children: ReactNode }) {
    const [loading, setLoading] = useState(true);
    const [authorized, setAuthorized] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const token = getToken();
        if (!token) {
            setAuthorized(false);
            setLoading(false);
            return;
        }

        api
            .get<UserInfo>("/auth/me")
            .then(() => {
                setAuthorized(true);
                setError(null);
            })
            .catch((e: any) => {
                // ✅ 500이면 토큰 문제가 아닐 수도 있으니, 원인 파악 전까지는 토큰 삭제하지 말고 메시지 노출
                console.error("[ProtectedRoute] /auth/me failed:", e);
                setAuthorized(false);
                setError(e?.message || "Auth check failed");

                // 401일 때만 clearToken 하고 싶으면 api.ts에서 Unauthorized로 throw하므로 여기서 분기 가능
                if (e?.message === "Unauthorized") {
                    clearToken();
                }
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-slate-900 text-slate-200">
                Loading...
            </div>
        );
    }

    if (!authorized) {
        // ✅ 개발 중 원인 확인을 위해 에러를 화면에 잠깐 보여줌
        return (
            <div className="flex min-h-screen items-center justify-center bg-slate-900 text-slate-200">
                <div className="rounded-xl border border-slate-700 bg-slate-800/60 p-4">
                    <div className="text-sm font-semibold">Not authorized</div>
                    {error && <div className="mt-2 text-xs text-red-300">{error}</div>}
                    <div className="mt-3 text-xs text-slate-400">Redirecting to login...</div>
                    <Navigate to="/login" replace />
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
