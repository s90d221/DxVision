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

const hasAbsoluteProtocol = (value: string) => /^https?:\/\//i.test(value);

const getApiOrigin = () => {
    if (hasAbsoluteProtocol(BASE_URL)) {
        return new URL(BASE_URL).origin;
    }
    if (typeof window !== "undefined") {
        return window.location.origin;
    }
    return "";
};

export const resolveAssetUrl = (value?: string | null) => {
    if (!value) return "";
    if (hasAbsoluteProtocol(value)) return value;
    if (value.startsWith("blob:") || value.startsWith("data:")) return value;
    const origin = getApiOrigin();
    if (!origin || !value.startsWith("/")) return value;
    return `${origin}${value}`;
};

function buildUrl(path: string): string {
    const normalizedBase = BASE_URL.endsWith("/") ? BASE_URL.slice(0, -1) : BASE_URL;
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    return `${normalizedBase}${normalizedPath}`;
}

const isFormData = (body: unknown): body is FormData =>
    typeof FormData !== "undefined" && body instanceof FormData;

const hasContentType = (headers: Headers) =>
    headers.has("Content-Type") || headers.has("content-type");

const isBinaryBody = (body: unknown): boolean => {
    if (!body) return false;
    if (typeof Blob !== "undefined" && body instanceof Blob) return true;
    if (typeof ArrayBuffer !== "undefined" && body instanceof ArrayBuffer) return true;
    if (typeof ArrayBuffer !== "undefined" && ArrayBuffer.isView && ArrayBuffer.isView(body as any)) return true;
    return false;
};

async function parseResponse(res: Response): Promise<any> {
    if (res.status === 204) return null;

    const contentType = res.headers.get("content-type") || "";
    try {
        if (contentType.includes("application/json")) {
            return await res.json();
        }
        const text = await res.text();
        return text ? { message: text } : {};
    } catch {
        return {};
    }
}

// 필요하면 true로 켜서 네트워크 디버깅
const DEBUG_REQUEST = false;

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    const token = getToken();

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const url = buildUrl(path);
    const body = options.body;

    // Content-Type 규칙
    // 1) 이미 지정되어 있으면 유지
    // 2) FormData면 절대 지정하지 않음 (브라우저가 boundary 포함 자동 설정)
    // 3) Binary/Stream이면 강제하지 않음
    // 4) 그 외 바디가 있으면 JSON으로 간주하고 application/json 지정
    if (!hasContentType(headers) && body != null) {
        if (!isFormData(body) && !isBinaryBody(body)) {
            headers.set("Content-Type", "application/json");
        }
    }

    if (DEBUG_REQUEST) {
        console.log("[API REQ]", {
            url,
            method: options.method || "GET",
            contentType: headers.get("Content-Type") || headers.get("content-type") || "(auto)",
            bodyType: isFormData(body) ? "FormData" : isBinaryBody(body) ? "Binary/Blob" : body == null ? "None" : typeof body,
        });
    }

    const res = await fetch(url, { ...options, headers, body });
    const data = await parseResponse(res);

    if (res.status === 401 || res.status === 403) {
        clearToken();
        throw new ApiError("Unauthorized", res.status, data);
    }

    if (!res.ok) {
        console.error("[API ERROR]", {
            url,
            method: options.method || "GET",
            status: res.status,
            statusText: res.statusText,
            requestContentType: headers.get("Content-Type") || headers.get("content-type") || "(auto)",
            requestBodyType: isFormData(body) ? "FormData" : isBinaryBody(body) ? "Binary/Blob" : body == null ? "None" : typeof body,
            response: data,
        });

        const message = (data && (data.message as string)) || `Request failed (${res.status})`;
        throw new ApiError(message, res.status, data);
    }

    return data as T;
}

export const api = {
    get: <T>(path: string) => request<T>(path),

    post: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "POST",
            body: body instanceof FormData ? body : body != null ? JSON.stringify(body) : undefined,
        }),

    put: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "PUT",
            body: body instanceof FormData ? body : body != null ? JSON.stringify(body) : undefined,
        }),

    delete: <T>(path: string) =>
        request<T>(path, {
            method: "DELETE",
        }),

    patch: <T>(path: string, body?: unknown) =>
        request<T>(path, {
            method: "PATCH",
            body: body instanceof FormData ? body : body != null ? JSON.stringify(body) : undefined,
        }),
};
