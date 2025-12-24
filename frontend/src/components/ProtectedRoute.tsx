import { ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../lib/api";
import { UserInfo, clearToken, getToken } from "../lib/auth";

export default function ProtectedRoute({ children }: { children: ReactNode }) {
    const [loading, setLoading] = useState(true);
    const [authorized, setAuthorized] = useState(false);

    useEffect(() => {
        const token = getToken();
        if (!token) {
            setAuthorized(false);
            setLoading(false);
            return;
        }
        api.get<UserInfo>("/auth/me")
            .then(() => {
                setAuthorized(true);
            })
            .catch(() => {
                clearToken();
                setAuthorized(false);
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <div className="flex h-screen items-center justify-center text-slate-700">Loading...</div>;
    }
    if (!authorized) {
        return <Navigate to="/login" replace />;
    }
    return <>{children}</>;
}
