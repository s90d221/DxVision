import { Link } from "react-router-dom";

export default function CasesPageHeader() {
    return (
        <div className="mb-4 flex flex-col gap-3 rounded-xl bg-slate-900/40 p-4 md:flex-row md:items-center md:justify-between md:p-0">
            <div>
                <div className="text-lg font-semibold text-teal-200">Cases</div>
                <div className="text-sm text-slate-400">
                    Manage answer keys, findings, diagnoses, and lesion targets.
                </div>
            </div>
            <Link
                className="inline-flex items-center justify-center rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 shadow-lg shadow-teal-500/20 hover:bg-teal-400"
                to="/admin/cases/new"
            >
                New Case
            </Link>
        </div>
    );
}
