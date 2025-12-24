import { clearToken, getToken } from "./auth";

export class ApiError extends Error {
    status?: number;
    body?: unknown;

    constructor(message: string, status?: number, body?: unknown) {
        super(message);
        this.status = status;
        this.body = body;
    }
}

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) || "/api/v1";

function buildUrl(path: string): string {
    const normalizedBase = BASE_URL.endsWith("/") ? BASE_URL.slice(0, -1) : BASE_URL;
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    return `${normalizedBase}${normalizedPath}`;
}

const isFormData = (body: unknown): body is FormData => typeof FormData !== "undefined" && body instanceof FormData;

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});

    const token = getToken();
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const url = buildUrl(path);
    const body = options.body;
    if (!isFormData(body)) {
        headers.set("Content-Type", "application/json");
    }

    const res = await fetch(url, { ...options, headers, body });

    const contentType = res.headers.get("content-type") || "";
    let data: any = {};

    try {
        if (contentType.includes("application/json")) {
            data = await res.json();
        } else {
            const text = await res.text();
            data = text ? { message: text } : {};
        }
    } catch {
        data = {};
    }

    if (res.status === 401 || res.status === 403) {
        clearToken();
        throw new ApiError("Unauthorized", res.status, data);
    }

    if (!res.ok) {
        console.error("[API ERROR]", {
            url,
            status: res.status,
            statusText: res.statusText,
            response: data,
        });
        const message = data?.message || `Request failed (${res.status})`;
        throw new ApiError(message, res.status, data);
    }

    return data as T;
}

export const api = {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "POST",
            body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
        }),
    put: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "PUT",
            body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
        }),
    delete: <T>(path: string) =>
        request<T>(path, {
            method: "DELETE",
        }),
};
