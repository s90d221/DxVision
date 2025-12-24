import { Link } from "react-router-dom";
import AdminLayout from "../../components/AdminLayout";

const cards = [
    { title: "Findings", description: "Manage finding options students can select.", to: "/admin/findings" },
    { title: "Diagnoses", description: "Manage diagnosis options for cases.", to: "/admin/diagnoses" },
    { title: "Cases", description: "Create or edit cases and configure correctness.", to: "/admin/cases" },
];

export default function AdminDashboard() {
    return (
        <AdminLayout title="Admin Dashboard" description="Manage content and options for DxVision">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {cards.map((card) => (
                    <Link
                        key={card.title}
                        to={card.to}
                        className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow hover:border-teal-400 hover:shadow-lg"
                    >
                        <div className="text-lg font-semibold text-teal-200">{card.title}</div>
                        <div className="mt-2 text-sm text-slate-300">{card.description}</div>
                    </Link>
                ))}
            </div>
        </AdminLayout>
    );
}
