import { type ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../lib/api";
import { clearToken, getToken, type UserInfo } from "../lib/auth";

export default function AdminRoute({ children }: { children: ReactNode }) {
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

        api.get<UserInfo>("/auth/me")
            .then((user) => {
                if (user.role === "ADMIN") {
                    setAuthorized(true);
                    setError(null);
                } else {
                    setAuthorized(false);
                    setError("Forbidden");
                }
            })
            .catch((e: any) => {
                console.error("[AdminRoute] /auth/me failed:", e);
                setAuthorized(false);
                setError(e?.message || "Auth check failed");
                if (e?.message === "Unauthorized") {
                    clearToken();
                }
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-slate-900 text-slate-200">
                Loading admin...
            </div>
        );
    }

    if (!authorized) {
        return (
            <Navigate
                to="/quiz"
                replace
                state={{
                    error: error || "Forbidden",
                }}
            />
        );
    }

    return <>{children}</>;
}
