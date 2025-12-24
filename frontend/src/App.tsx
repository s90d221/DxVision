import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import QuizPage from "./pages/QuizPage";
import ResultPage from "./pages/ResultPage";
import ProtectedRoute from "./components/ProtectedRoute";
import { isLoggedIn } from "./lib/auth";
import AdminRoute from "./components/AdminRoute";
import AdminDashboard from "./pages/admin/AdminDashboard";
import FindingsAdminPage from "./pages/admin/FindingsAdminPage";
import DiagnosesAdminPage from "./pages/admin/DiagnosesAdminPage";
import CasesAdminPage from "./pages/admin/CasesAdminPage";

function HomeRedirect() {
    // "/"로 들어오면 로그인 상태에 따라 안전하게 분기
    return isLoggedIn() ? <Navigate to="/quiz" replace /> : <Navigate to="/login" replace />;
}

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<HomeRedirect />} />

            <Route path="/login" element={<LoginPage />} />

            <Route
                path="/quiz"
                element={
                    <ProtectedRoute>
                        <QuizPage />
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
                path="/admin/cases"
                element={
                    <AdminRoute>
                        <CasesAdminPage />
                    </AdminRoute>
                }
            />

            {/* 나머지는 전부 "/"로 보내서 HomeRedirect에서만 분기 */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}
