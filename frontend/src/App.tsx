import { Navigate, Route, Routes } from "react-router-dom";
import { useEffect, useState } from "react";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import QuizPage from "./pages/QuizPage";
import ResultPage from "./pages/ResultPage";
import ProtectedRoute from "./components/ProtectedRoute";
import { clearToken, getToken, type UserInfo } from "./lib/auth";
import AdminRoute from "./components/AdminRoute";
import AdminDashboard from "./pages/admin/AdminDashboard";
import FindingsAdminPage from "./pages/admin/FindingsAdminPage";
import DiagnosesAdminPage from "./pages/admin/DiagnosesAdminPage";
import AdminCaseFormPage from "./pages/admin/AdminCaseFormPage";
import { api } from "./lib/api";

function LandingRedirect() {
    const [target, setTarget] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = getToken();
        if (!token) {
            setTarget("/login");
            setLoading(false);
            return;
        }
        api.get<UserInfo>("/auth/me")
            .then((user) => {
                setTarget(user.role === "ADMIN" ? "/admin" : "/home");
            })
            .catch(() => {
                clearToken();
                setTarget("/login");
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-slate-900 text-slate-200">
                Checking session...
            </div>
        );
    }

    return <Navigate to={target ?? "/login"} replace />;
}

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<LandingRedirect />} />

            <Route path="/login" element={<LoginPage />} />

            <Route
                path="/home"
                element={
                    <ProtectedRoute>
                        <HomePage />
                    </ProtectedRoute>
                }
            />

            <Route path="/quiz" element={<Navigate to="/quiz/random" replace />} />

            <Route
                path="/quiz/random"
                element={
                    <ProtectedRoute>
                        <QuizPage mode="random" />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/quiz/:caseId"
                element={
                    <ProtectedRoute>
                        <QuizPage mode="byId" />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/result"
                element={
                    <ProtectedRoute>
                        <ResultPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/admin"
                element={
                    <AdminRoute>
                        <AdminDashboard />
                    </AdminRoute>
                }
            />
            <Route
                path="/admin/findings"
                element={
                    <AdminRoute>
                        <FindingsAdminPage />
                    </AdminRoute>
                }
            />
            <Route
                path="/admin/diagnoses"
                element={
                    <AdminRoute>
                        <DiagnosesAdminPage />
                    </AdminRoute>
                }
            />
            <Route
                path="/admin/cases/new"
                element={
                    <AdminRoute>
                        <AdminCaseFormPage mode="create" />
                    </AdminRoute>
                }
            />
            <Route
                path="/admin/cases/:id/edit"
                element={
                    <AdminRoute>
                        <AdminCaseFormPage mode="edit" />
                    </AdminRoute>
                }
            />
            <Route path="/admin/cases" element={<Navigate to="/admin" replace />} />

            {/* 나머지는 전부 "/"로 보내서 HomeRedirect에서만 분기 */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}
