export type UserInfo = {
    id: number;
    email: string;
    name: string;
    role: string; // 백엔드 Role enum이 문자열로 내려옴(예: "USER")
};

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