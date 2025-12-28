import { type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { clearToken } from "../lib/auth";

type AdminLayoutProps = {
    title: string;
    description?: string;
    children: ReactNode;
};

export default function AdminLayout({ title, description, children }: AdminLayoutProps) {
    const navigate = useNavigate();

    const handleLogout = () => {
        clearToken();
        navigate("/login", { replace: true });
    };

    return (
        <div className="min-h-screen bg-slate-900 text-slate-100">
            <header className="border-b border-slate-800 bg-slate-900/80 px-6 py-4 backdrop-blur">
                <div className="flex items-center justify-between">
                    <div>
                        <div className="text-xl font-semibold text-teal-300">DxVision Admin</div>
                        <div className="text-sm text-slate-400">{description}</div>
                    </div>
                    <div className="flex items-center gap-3 text-sm">
                        <button
                            className="rounded border border-slate-700 px-3 py-1 hover:border-teal-400 hover:text-teal-200"
                            onClick={handleLogout}
                            type="button"
                        >
                            Logout
                        </button>
                    </div>
                </div>
                <div className="mt-3 text-lg font-semibold">{title}</div>
            </header>
            <main className="px-6 py-6">{children}</main>
        </div>
    );
}
