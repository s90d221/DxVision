import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { setToken } from "../lib/auth";

type AuthResponse = {
    token: string;
};

export default function LoginPage() {
    const [mode, setMode] = useState<"login" | "signup">("login");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [name, setName] = useState("");
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        try {
            const path = mode === "login" ? "/auth/login" : "/auth/signup";
            const payload = mode === "login" ? { email, password } : { email, password, name: name || email };
            const res = await api.post<AuthResponse>(path, payload);
            setToken(res.token);
            navigate("/home", { replace: true });
        } catch (err: any) {
            setError(err?.message || "Authentication failed");
        }
    };

    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-900 text-slate-100">
            <div className="w-full max-w-md rounded-2xl bg-slate-800/70 p-6 shadow-xl">
                <div className="mb-4 flex justify-between">
                    <button
                        className={`w-1/2 rounded-lg px-4 py-2 text-sm font-semibold ${
                            mode === "login" ? "bg-teal-500 text-slate-950" : "bg-slate-700 text-slate-200"
                        }`}
                        onClick={() => setMode("login")}
                        type="button"
                    >
                        Login
                    </button>
                    <button
                        className={`w-1/2 rounded-lg px-4 py-2 text-sm font-semibold ${
                            mode === "signup" ? "bg-teal-500 text-slate-950" : "bg-slate-700 text-slate-200"
                        }`}
                        onClick={() => setMode("signup")}
                        type="button"
                    >
                        Signup
                    </button>
                </div>

                <form className="space-y-4" onSubmit={handleSubmit}>
                    <div>
                        <label className="text-sm text-slate-300">Email</label>
                        <input
                            className="mt-1 w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-2 text-sm"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    {mode === "signup" && (
                        <div>
                            <label className="text-sm text-slate-300">Name</label>
                            <input
                                className="mt-1 w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-2 text-sm"
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                            />
                        </div>
                    )}

                    <div>
                        <label className="text-sm text-slate-300">Password</label>
                        <input
                            className="mt-1 w-full rounded-lg border border-slate-600 bg-slate-900 px-3 py-2 text-sm"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    {error && <p className="text-sm text-red-400">{error}</p>}

                    <button
                        type="submit"
                        className="w-full rounded-lg bg-teal-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400"
                    >
                        {mode === "login" ? "Login" : "Create Account"}
                    </button>
                </form>
            </div>
        </div>
    );
}
