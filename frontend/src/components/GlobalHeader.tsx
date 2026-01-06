import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import { clearToken, type UserInfo } from "../lib/auth";

type GlobalHeaderProps = {
    /** 페이지별로 바뀌는 소제목 (예: Dashboard, Quiz, Score Report, Admin · Manage cases 등) */
    subtitle?: string;
    /** 우측 상단에 추가로 넣을 액션 버튼들 (예: New Problem) */
    actions?: ReactNode;
    /** 관리자일 때만 Admin/Home 네비게이션 표시 */
    isAdmin?: boolean;
    /** 이미 불러온 사용자 정보가 있다면 전달 (Settings 패널에서 재사용) */
    user?: UserInfo | null;
    onUserChange?: (user: UserInfo) => void;
};

export default function GlobalHeader({ subtitle, actions, isAdmin, user, onUserChange }: GlobalHeaderProps) {
    const navigate = useNavigate();
    const [currentUser, setCurrentUser] = useState<UserInfo | null>(user ?? null);
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [editingName, setEditingName] = useState(false);
    const [nameInput, setNameInput] = useState(user?.name ?? "");
    const [savingName, setSavingName] = useState(false);
    const [panelError, setPanelError] = useState<string | null>(null);
    const [panelPosition, setPanelPosition] = useState<{ top: number; right: number } | null>(null);
    const [overlayTop, setOverlayTop] = useState<number>(0);

    const headerRef = useRef<HTMLElement>(null);
    const triggerRef = useRef<HTMLButtonElement>(null);
    const panelRef = useRef<HTMLDivElement>(null);

    const userInitial = useMemo(() => currentUser?.name?.[0]?.toUpperCase() ?? currentUser?.email?.[0]?.toUpperCase() ?? "U", [currentUser]);

    useEffect(() => {
        setCurrentUser(user ?? null);
        if (user?.name) setNameInput(user.name);
    }, [user]);

    useEffect(() => {
        if (user) return;
        const fetchUser = async () => {
            try {
                const info = await api.get<UserInfo>("/auth/me");
                setCurrentUser(info);
                setNameInput(info.name);
                onUserChange?.(info);
            } catch (err: unknown) {
                const apiError = err as ApiError;
                if (apiError.status === 401 || apiError.status === 403) {
                    clearToken();
                    navigate("/login", { replace: true });
                }
            }
        };
        void fetchUser();
    }, [user, navigate, onUserChange]);

    useEffect(() => {
        if (!settingsOpen) return;
        const handleOutsideClick = (event: MouseEvent) => {
            const target = event.target as Node;
            if (panelRef.current?.contains(target) || triggerRef.current?.contains(target)) return;
            closeSettings();
        };
        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                closeSettings();
            }
        };
        const updatePosition = () => {
            const trigger = triggerRef.current;
            if (!trigger) return;
            const rect = trigger.getBoundingClientRect();
            setPanelPosition({ top: rect.bottom + 8, right: window.innerWidth - rect.right });

            const headerRect = headerRef.current?.getBoundingClientRect();
            setOverlayTop(headerRect ? headerRect.bottom : 0);
        };
        updatePosition();
        window.addEventListener("resize", updatePosition);
        window.addEventListener("scroll", updatePosition, true);
        document.addEventListener("mousedown", handleOutsideClick);
        document.addEventListener("keydown", handleEscape);
        return () => {
            window.removeEventListener("resize", updatePosition);
            window.removeEventListener("scroll", updatePosition, true);
            document.removeEventListener("mousedown", handleOutsideClick);
            document.removeEventListener("keydown", handleEscape);
        };
    }, [settingsOpen]);

    const handleLogout = () => {
        clearToken();
        navigate("/login", { replace: true });
    };

    const closeSettings = () => {
        setSettingsOpen(false);
        setEditingName(false);
        setPanelError(null);
        setNameInput(currentUser?.name ?? "");
    };

    const handleSaveName = async () => {
        if (!nameInput.trim()) {
            setPanelError("Name cannot be empty.");
            return;
        }
        setSavingName(true);
        setPanelError(null);
        try {
            const updated = await api.patch<UserInfo>("/users/me", { name: nameInput.trim() });
            setCurrentUser(updated);
            onUserChange?.(updated);
            setEditingName(false);
        } catch (err: unknown) {
            const apiError = err as ApiError;
            if (apiError.status === 401 || apiError.status === 403) {
                clearToken();
                navigate("/login", { replace: true });
                return;
            }
            setPanelError(apiError?.message ?? "Failed to update name.");
        } finally {
            setSavingName(false);
        }
    };

    return (
        <header ref={headerRef} className="border-b border-slate-800 bg-slate-900/80 px-6 py-4 backdrop-blur">
            <div className="flex items-center justify-between gap-4">
                {/* Left: Brand + subtitle (2 lines) */}
                <div className="min-w-0">
                    <div className="text-lg font-semibold text-slate-100">DxVision</div>
                    {subtitle ? (
                        <div className="mt-0.5 truncate text-sm text-slate-400">{subtitle}</div>
                    ) : null}
                </div>

                {/* Right: actions + nav + settings */}
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

                    <div className="relative">
                        <button
                            ref={triggerRef}
                            className="flex items-center gap-2 rounded-lg border border-slate-700 px-3 py-1 text-sm text-slate-100 transition hover:border-teal-400 hover:text-teal-200"
                            onClick={() => setSettingsOpen((prev) => !prev)}
                            type="button"
                            aria-label="Open settings"
                        >
                            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-800 text-xs font-semibold text-teal-200">
                                {userInitial}
                            </span>
                            <span>Settings</span>
                            <svg
                                className={`h-4 w-4 transition ${settingsOpen ? "rotate-180 text-teal-300" : "text-slate-400"}`}
                                viewBox="0 0 20 20"
                                fill="currentColor"
                                aria-hidden="true"
                            >
                                <path
                                    fillRule="evenodd"
                                    d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.25 8.29a.75.75 0 01-.02-1.08z"
                                    clipRule="evenodd"
                                />
                            </svg>
                        </button>

                        {settingsOpen && panelPosition
                            ? createPortal(
                                  <>
                                      <div
                                          className="fixed left-0 right-0 z-40 bg-black/50"
                                          style={{ top: overlayTop, bottom: 0 }}
                                          onClick={closeSettings}
                                          aria-hidden
                                      />
                                      <div
                                          ref={panelRef}
                                          className="fixed z-50 w-80 rounded-lg border border-slate-800 bg-slate-900 p-4 shadow-2xl"
                                          style={{ top: panelPosition.top, right: panelPosition.right }}
                                          role="dialog"
                                          aria-label="User settings"
                                      >
                                        <div className="flex items-start justify-between gap-2">
                                            <div>
                                            <p className="text-sm font-semibold text-slate-100">{currentUser?.name ?? "User"}</p>
                                            <p className="text-xs text-slate-400">{currentUser?.email ?? "Loading..."}</p>
                                            <span className="mt-2 inline-flex rounded-full bg-slate-800 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-teal-200">
                                            {currentUser?.role ?? "USER"}
                                        </span>
                                    </div>
                                    <button
                                        className="text-xs text-slate-400 hover:text-slate-200"
                                        onClick={closeSettings}
                                        type="button"
                                        aria-label="Close settings panel"
                                    >
                                        ✕
                                    </button>
                                </div>

                                <div className="mt-4 space-y-2">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="text-xs font-semibold text-slate-200">Display name</p>
                                            {!editingName && (
                                                <p className="text-xs text-slate-400">{currentUser?.name ?? "Not set"}</p>
                                            )}
                                        </div>
                                        {!editingName && (
                                            <button
                                                className="text-xs font-semibold text-teal-300 hover:text-teal-200"
                                                onClick={() => {
                                                    setEditingName(true);
                                                    setNameInput(currentUser?.name ?? "");
                                                }}
                                                type="button"
                                            >
                                                Edit
                                            </button>
                                        )}
                                    </div>

                                    {editingName ? (
                                        <div className="space-y-2">
                                            <input
                                                className="w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-400/40"
                                                value={nameInput}
                                                onChange={(e) => setNameInput(e.target.value)}
                                                placeholder="Enter your name"
                                                disabled={savingName}
                                            />
                                            <div className="flex items-center gap-2">
                                                <button
                                                    className="rounded-md bg-teal-500 px-3 py-1 text-xs font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50"
                                                    onClick={() => void handleSaveName()}
                                                    type="button"
                                                    disabled={savingName}
                                                >
                                                    Save
                                                </button>
                                                <button
                                                    className="rounded-md border border-slate-700 px-3 py-1 text-xs font-semibold text-slate-200 hover:border-slate-500"
                                                    onClick={() => {
                                                        setEditingName(false);
                                                        setNameInput(currentUser?.name ?? "");
                                                        setPanelError(null);
                                                    }}
                                                    type="button"
                                                    disabled={savingName}
                                                >
                                                    Cancel
                                                </button>
                                            </div>
                                        </div>
                                    ) : null}

                                    {panelError && <p className="text-xs text-red-300">{panelError}</p>}
                                </div>

                                        <div className="mt-4 border-t border-slate-800 pt-3">
                                            <button
                                                className="flex w-full items-center justify-center gap-2 rounded-md border border-red-400/50 px-3 py-2 text-sm font-semibold text-red-200 transition hover:border-red-300 hover:text-red-100"
                                                onClick={handleLogout}
                                                type="button"
                                            >
                                                Logout
                                            </button>
                                        </div>
                                    </div>
                                  </>,
                                  document.body
                              )
                            : null}
                    </div>
                </div>
            </div>
        </header>
    );
}
