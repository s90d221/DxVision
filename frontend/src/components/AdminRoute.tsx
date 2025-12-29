import { type ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../lib/api";
import { clearToken, getToken, type UserInfo } from "../lib/auth";

export default function AdminRoute({ children }: { children: ReactNode }) {
    const [status, setStatus] = useState<"loading" | "unauth" | "user" | "admin">("loading");

    useEffect(() => {
        const token = getToken();
        if (!token) {
            setStatus("unauth");
            return;
        }

        api.get<UserInfo>("/auth/me")
            .then((user) => {
                setStatus(user.role === "ADMIN" ? "admin" : "user");
            })
            .catch(() => {
                clearToken();
                setStatus("unauth");
            });
    }, []);

    if (status === "loading") {
        return (
            <div className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-200">
                Loading admin...
            </div>
        );
    }

    if (status === "unauth") {
        return <Navigate to="/login" replace />;
    }

    if (status === "user") {
        return <Navigate to="/home" replace />;
    }

    return <>{children}</>;
}
