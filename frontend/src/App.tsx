import { Routes, Route, Navigate } from "react-router-dom";
import QuizPage from "./pages/QuizPage";

export default function App() {
    return (
        <Routes>
            <Route path="/quiz" element={<QuizPage />} />
            <Route path="*" element={<Navigate to="/quiz" replace />} />
        </Routes>
    );
}
