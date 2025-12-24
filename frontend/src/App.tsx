import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import QuizPage from "./pages/QuizPage";
import ResultPage from "./pages/ResultPage";
import ProtectedRoute from "./components/ProtectedRoute";

export default function App() {
    return (
        <Routes>
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
            <Route path="*" element={<Navigate to="/quiz" replace />} />
        </Routes>
    );
}
