export type UserCaseStatus = "CORRECT" | "WRONG" | "REATTEMPT_CORRECT" | "UNSEEN" | "UNATTEMPTED";

export type CaseListItem = {
    caseId: number;
    title: string;
    modality: string;
    species: string;
    updatedAt: string;
    status: UserCaseStatus;
    lastAttemptAt: string | null;
    lastScore: number | null;
};

export type PageResponse<T> = {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
};

export type StatusMeta = { label: string; color: string; bg: string; textClass: string };

export const CASE_STATUS_META: Record<UserCaseStatus, StatusMeta> = {
    CORRECT: { label: "Correct", color: "#22c55e", bg: "bg-green-500/15", textClass: "text-emerald-200" },
    WRONG: { label: "Wrong", color: "#ef4444", bg: "bg-red-500/15", textClass: "text-red-200" },
    REATTEMPT_CORRECT: { label: "Reattempt", color: "#f59e0b", bg: "bg-amber-500/15", textClass: "text-amber-200" },
    UNSEEN: { label: "Unseen", color: "#94a3b8", bg: "bg-slate-500/20", textClass: "text-slate-300" },
    UNATTEMPTED: { label: "Unseen", color: "#94a3b8", bg: "bg-slate-500/20", textClass: "text-slate-300" },
};

export const normalizeStatus = (status: string): UserCaseStatus => {
    switch (status) {
        case "CORRECT":
        case "WRONG":
        case "REATTEMPT_CORRECT":
        case "UNSEEN":
            return status;
        case "UNATTEMPTED":
            return "UNSEEN";
        default:
            return "UNSEEN";
    }
};

export const getStatusMeta = (status: string): StatusMeta => {
    const normalized = normalizeStatus(status);
    return CASE_STATUS_META[normalized] ?? CASE_STATUS_META.UNSEEN;
};
