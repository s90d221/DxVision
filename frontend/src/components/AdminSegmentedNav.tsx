import { matchPath, NavLink, useLocation } from "react-router-dom";

const NAV_ITEMS = [
    { label: "Findings", to: "/admin/findings", patterns: ["/admin/findings"] },
    { label: "Diagnoses", to: "/admin/diagnoses", patterns: ["/admin/diagnoses"] },
    { label: "New Case", to: "/admin/cases/new", patterns: ["/admin/cases/new", "/admin/cases/:id/edit"] },
];

export default function AdminSegmentedNav() {
    const location = useLocation();

    const isActive = (patterns: string[]) =>
        patterns.some((pattern) => matchPath({ path: pattern, end: true }, location.pathname) != null);

    return (
        <nav aria-label="Admin navigation" className="mb-6">
            <div className="grid overflow-hidden rounded-xl border border-slate-800 bg-slate-900/70 text-sm font-medium shadow-lg shadow-slate-950/20 sm:grid-cols-3">
                {NAV_ITEMS.map((item, index) => {
                    const active = isActive(item.patterns);
                    return (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            aria-current={active ? "page" : undefined}
                            className={({ isPending }) =>
                                [
                                    "flex h-full items-center justify-center px-4 py-3 transition-colors",
                                    index > 0 ? "border-l border-slate-800" : "",
                                    isPending ? "opacity-80" : "",
                                    active
                                        ? "bg-teal-500/15 font-semibold text-teal-100 shadow-inner shadow-teal-500/20 ring-1 ring-inset ring-teal-500/50"
                                        : "text-slate-300 hover:bg-slate-800/70 hover:text-teal-100",
                                ]
                                    .filter(Boolean)
                                    .join(" ")
                            }
                        >
                            {item.label}
                        </NavLink>
                    );
                })}
            </div>
        </nav>
    );
}
