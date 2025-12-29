import { type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { clearToken } from "../lib/auth";

type GlobalHeaderProps = {
    /** 페이지별로 바뀌는 소제목 (예: Dashboard, Quiz, Score Report, Admin · Manage cases 등) */
    subtitle?: string;
    /** 우측 상단에 추가로 넣을 액션 버튼들 (예: New Problem) */
    actions?: ReactNode;
    /** 관리자일 때만 Admin/Home 네비게이션 표시 */
    isAdmin?: boolean;
};

export default function GlobalHeader({ subtitle, actions, isAdmin }: GlobalHeaderProps) {
    const navigate = useNavigate();

    const handleLogout = () => {
        clearToken();
        navigate("/login", { replace: true });
    };

    return (
        <header className="border-b border-slate-800 bg-slate-900/80 px-6 py-4 backdrop-blur">
            <div className="flex items-center justify-between gap-4">
                {/* Left: Brand + subtitle (2 lines) */}
                <div className="min-w-0">
                    <div className="text-lg font-semibold text-slate-100">DxVision</div>
                    {subtitle ? (
                        <div className="mt-0.5 truncate text-sm text-slate-400">{subtitle}</div>
                    ) : null}
                </div>

                {/* Right: actions + nav + logout */}
                <div className="flex flex-wrap items-center justify-end gap-2 text-sm">
                    {actions}

                    {isAdmin ? (
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
                    ) : null}

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
