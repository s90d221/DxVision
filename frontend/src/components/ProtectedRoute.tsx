import { type ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import { clearToken, getToken, type UserInfo } from "../lib/auth";

export default function ProtectedRoute({ children }: { children: ReactNode }) {
    const [loading, setLoading] = useState(true);
    const [authorized, setAuthorized] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [redirectToLogin, setRedirectToLogin] = useState(false);

    useEffect(() => {
        const token = getToken();
        if (!token) {
            setAuthorized(false);
            setRedirectToLogin(true);
            setLoading(false);
            return;
        }

        api
            .get<UserInfo>("/auth/me")
            .then(() => {
                setAuthorized(true);
                setError(null);
                setRedirectToLogin(false);
            })
            .catch((e: unknown) => {
                const apiError = e as ApiError;
                console.error("[ProtectedRoute] /auth/me failed:", apiError);
                const shouldRedirect = apiError.status === 401 || apiError.status === 403;
                if (shouldRedirect) {
                    clearToken();
                    setRedirectToLogin(true);
                }
                setAuthorized(false);
                setError(apiError?.message || "Auth check failed");
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

    if (!authorized && redirectToLogin) {
        return <Navigate to="/login" replace />;
    }

    if (!authorized) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-slate-900 text-slate-200">
                <div className="rounded-xl border border-slate-700 bg-slate-800/60 p-4">
                    <div className="text-sm font-semibold">Authentication required</div>
                    {error && <div className="mt-2 text-xs text-red-300">{error}</div>}
                    <div className="mt-3 text-xs text-slate-400">Please try again.</div>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
