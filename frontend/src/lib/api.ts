import { getToken, clearToken } from "./auth";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    // JSON이 아닌 응답도 있을 수 있으니 Content-Type은 상황에 따라
    headers.set("Content-Type", "application/json");

    const token = getToken();
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const url = `${BASE_URL}${path}`;
    const res = await fetch(url, { ...options, headers });

    // ✅ 401은 기존대로 토큰 제거
    if (res.status === 401) {
        clearToken();
        throw new Error("Unauthorized");
    }

    // ✅ 여기가 핵심: 500일 때도 body를 읽어서 로그로 남김
    const contentType = res.headers.get("content-type") || "";
    let bodyText = "";
    let data: any = {};

    try {
        if (contentType.includes("application/json")) {
            data = await res.json();
        } else {
            bodyText = await res.text();
            data = bodyText ? { message: bodyText } : {};
        }
    } catch {
        // 파싱 실패해도 무시하고 넘어감
        data = {};
    }

    if (!res.ok) {
        // 콘솔에 상세 출력 (서버 500 원인 파악에 도움)
        console.error("[API ERROR]", {
            url,
            status: res.status,
            statusText: res.statusText,
            response: data,
        });

        const message = data?.message || `Request failed (${res.status})`;
        throw new Error(message);
    }

    return data as T;
}

export const api = {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "POST",
            body: body ? JSON.stringify(body) : undefined,
        }),
    put: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "PUT",
            body: body ? JSON.stringify(body) : undefined,
        }),
    delete: <T>(path: string) =>
        request<T>(path, {
            method: "DELETE",
        }),
};
