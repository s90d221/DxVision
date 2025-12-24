import { getToken, clearToken } from "./auth";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    headers.set("Content-Type", "application/json");
    const token = getToken();
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
    if (res.status === 401) {
        clearToken();
        throw new Error("Unauthorized");
    }

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
        const message = data?.message || "Request failed";
        throw new Error(message);
    }
    return data as T;
}

export const api = {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: "POST", body: body ? JSON.stringify(body) : undefined }),
};
