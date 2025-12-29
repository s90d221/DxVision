import { type ReactNode } from "react";
import AdminSegmentedNav from "./AdminSegmentedNav";
import GlobalHeader from "./GlobalHeader";

type AdminLayoutProps = {
    title?: string;
    description?: string;
    headerActions?: ReactNode;
    showSectionNav?: boolean;
    children: ReactNode;
};

export default function AdminLayout({
    description,
    headerActions,
    showSectionNav = false,
    children,
}: AdminLayoutProps) {
    return (
        <div className="min-h-screen bg-slate-900 text-slate-100">
            <GlobalHeader isAdmin subtitle={description} actions={headerActions} />
            <main className="px-6 py-6">
                {showSectionNav && <AdminSegmentedNav />}
                {children}
            </main>
        </div>
    );
}
