export interface UserInfo {
    id: number;
    email: string;
    name: string;
    role: string; // Backend Role enum as string (e.g., "USER")
}

const TOKEN_KEY = "dxvision_token";

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}

export function isLoggedIn(): boolean {
    return !!getToken();
}
