import { type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { clearToken } from "../lib/auth";

type GlobalHeaderProps = {
    title?: string;
    subtitle?: string;
    actions?: ReactNode;
    isAdmin?: boolean;
};

export default function GlobalHeader({ title, subtitle, actions, isAdmin }: GlobalHeaderProps) {
    const navigate = useNavigate();

    const handleLogout = () => {
        clearToken();
        navigate("/login", { replace: true });
    };

    return (
        <header className="border-b border-slate-800 bg-slate-900/80 px-6 py-4 backdrop-blur">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                    {title && <div className="text-lg font-semibold">{title}</div>}
                    {subtitle && <div className="text-sm text-slate-400">{subtitle}</div>}
                </div>
                <div className="flex flex-wrap items-center justify-end gap-2 text-sm">
                    {actions}
                    {isAdmin && (
                        <>
                            <Link
                                className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                                to="/home"
                            >
                                Home
                            </Link>
                            <Link
                                className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                                to="/admin"
                            >
                                Admin
                            </Link>
                        </>
                    )}
                    <button
                        className="rounded-lg border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                        onClick={handleLogout}
                        type="button"
                    >
                        Logout
                    </button>
                </div>
            </div>
        </header>
    );
}
