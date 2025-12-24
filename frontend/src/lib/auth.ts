const TOKEN_KEY = "dxvision_token";

export type UserInfo = {
    id: number;
    email: string;
    name: string;
    role: string;
};

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}
