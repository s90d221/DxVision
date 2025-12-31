import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import {
    CASE_STATUS_META,
    type CaseListItem,
    type PageResponse,
    type UserCaseStatus,
    getStatusMeta,
} from "../types/case";

type FilterState = {
    modality: string;
    species: string;
    status: string;
    keyword: string;
    sort: string;
};

const STORAGE_KEY = "dxvision_problem_list_filters";

const DEFAULT_FILTERS: FilterState = {
    modality: "",
    species: "",
    status: "",
    keyword: "",
    sort: "updatedAt,desc",
};

const modalityOptions = ["XRAY", "ULTRASOUND", "CT", "MRI"];
const speciesOptions = ["DOG", "CAT"];
const statusOptions: UserCaseStatus[] = ["CORRECT", "WRONG", "REATTEMPT_CORRECT", "UNSEEN"];
const sortOptions = [
    { value: "updatedAt,desc", label: "Updated (newest)" },
    { value: "updatedAt,asc", label: "Updated (oldest)" },
    { value: "title,asc", label: "Title A→Z" },
];

function loadFilters(): FilterState {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return DEFAULT_FILTERS;
        const parsed = JSON.parse(raw) as Partial<FilterState>;
        return { ...DEFAULT_FILTERS, ...parsed };
    } catch {
        return DEFAULT_FILTERS;
    }
}

function saveFilters(filters: FilterState) {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
    } catch {
        // ignore
    }
}

export default function ProblemListPanel({ className }: { className?: string }) {
    const navigate = useNavigate();
    const [filters, setFilters] = useState<FilterState>(() => loadFilters());
    const [keywordInput, setKeywordInput] = useState(filters.keyword);
    const [page, setPage] = useState(0);
    const [size] = useState(10);
    const [items, setItems] = useState<CaseListItem[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const queryString = useMemo(() => {
        const params = new URLSearchParams();
        if (filters.modality) params.set("modality", filters.modality);
        if (filters.species) params.set("species", filters.species);
        if (filters.status) params.set("status", filters.status);
        if (filters.keyword) params.set("keyword", filters.keyword);
        if (filters.sort) params.set("sort", filters.sort);
        params.set("page", String(page));
        params.set("size", String(size));
        return params.toString();
    }, [filters, page, size]);

    const debouncedKeyword = useDebounce(keywordInput, 300);

    useEffect(() => {
        setFilters((prev) => ({ ...prev, keyword: debouncedKeyword }));
        setPage(0);
    }, [debouncedKeyword]);

    useEffect(() => {
        void loadCases();
        saveFilters(filters);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [queryString]);

    const loadCases = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await api.get<PageResponse<CaseListItem>>(`/cases?${queryString}`);
            applyResponse(data);
        } catch (err: any) {
            setError(err?.message || "Failed to load cases");
        } finally {
            setLoading(false);
        }
    };

    const applyResponse = (data: PageResponse<CaseListItem>) => {
        setItems(data.content);
        setPage(data.page);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
    };

    const handleRandomPlay = async () => {
        if (totalElements === 0) return;
        const randomIndex = Math.floor(Math.random() * totalElements);
        const targetPage = Math.floor(randomIndex / size);

        const params = new URLSearchParams(queryString);
        params.set("page", String(targetPage));

        setLoading(true);
        setError(null);
        try {
            const data = await api.get<PageResponse<CaseListItem>>(`/cases?${params.toString()}`);
            applyResponse(data);
            const localIndex = randomIndex % data.content.length;
            const chosen = data.content[localIndex];
            if (chosen) {
                navigate(`/quiz/${chosen.caseId}`);
            } else {
                setError("Failed to pick a random case from the filtered set.");
            }
        } catch (err: any) {
            setError(err?.message || "Failed to load random case");
        } finally {
            setLoading(false);
        }
    };

    const onFilterChange = (next: Partial<FilterState>) => {
        setFilters((prev) => ({ ...prev, ...next }));
        setPage(0);
    };

    const onPageChange = (nextPage: number) => {
        setPage(Math.max(0, nextPage));
    };

    const formatDate = (value?: string | null) => {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "—";
        return date.toLocaleString();
    };

    return (
        <section
            className={`flex h-full min-h-[520px] flex-col rounded-xl border border-slate-800 bg-slate-950/60 p-5 shadow-lg shadow-black/10 ${
                className ?? ""
            }`}
        >
            <div className="flex items-center justify-between gap-2">
                <h3 className="text-lg font-semibold">Problem List</h3>
                <button
                    className="text-xs text-teal-300 hover:text-teal-200"
                    onClick={() => {
                        onFilterChange(DEFAULT_FILTERS);
                        setKeywordInput("");
                    }}
                    type="button"
                >
                    Reset
                </button>
            </div>

            <div className="mt-4 space-y-3">
                <div className="grid grid-cols-2 gap-2">
                    <Select
                        label="Modality"
                        value={filters.modality}
                        onChange={(value) => onFilterChange({ modality: value })}
                        options={[{ value: "", label: "All" }, ...modalityOptions.map((m) => ({ value: m, label: m }))]}
                    />
                    <Select
                        label="Species"
                        value={filters.species}
                        onChange={(value) => onFilterChange({ species: value })}
                        options={[{ value: "", label: "All" }, ...speciesOptions.map((s) => ({ value: s, label: s }))]}
                    />
                </div>
                <Select
                    label="Status"
                    value={filters.status}
                    onChange={(value) => onFilterChange({ status: value })}
                    options={[
                        { value: "", label: "All" },
                        ...statusOptions.map((s) => ({ value: s, label: CASE_STATUS_META[s].label })),
                    ]}
                />
                <Select
                    label="Sort"
                    value={filters.sort}
                    onChange={(value) => onFilterChange({ sort: value })}
                    options={sortOptions}
                />
                <div className="space-y-1">
                    <label className="text-xs text-slate-400" htmlFor="keyword">
                        Keyword
                    </label>
                    <input
                        id="keyword"
                        value={keywordInput}
                        onChange={(e) => setKeywordInput(e.target.value)}
                        placeholder="Search title/description"
                        className="w-full rounded-lg border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-teal-400 focus:outline-none"
                    />
                </div>
            </div>

            <div className="mt-4 grow space-y-2 overflow-hidden">
                <div className="flex items-center justify-between text-xs text-slate-400">
                    <span>{loading ? "Loading..." : `${totalElements} results`}</span>
                    <span>
                        Page {totalPages === 0 ? 0 : page + 1} / {Math.max(totalPages, 1)}
                    </span>
                </div>
                <div className="h-full max-h-[420px] overflow-y-auto pr-1 md:max-h-[520px] lg:max-h-[640px]">
                    {loading && (
                        <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-3 py-2 text-sm text-slate-400">
                            Loading cases...
                        </div>
                    )}

                    {error && (
                        <div className="rounded-lg border border-red-500/50 bg-red-500/10 px-3 py-2 text-xs text-red-200">
                            {error}
                        </div>
                    )}

                    {!loading && !error && items.length === 0 && (
                        <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-3 py-2 text-sm text-slate-400">
                            No cases found for the current filters.
                        </div>
                    )}

                    <div className="space-y-2">
                        {items.map((item) => (
                            <button
                                key={item.caseId}
                                onClick={() => navigate(`/quiz/${item.caseId}`)}
                                className="block w-full rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2 text-left transition hover:border-teal-400 hover:bg-slate-900/60"
                                type="button"
                            >
                                <div className="flex items-center justify-between">
                                    <div className="font-semibold text-slate-100">{item.title}</div>
                                <StatusBadge status={item.status} />
                                </div>
                                <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-400">
                                    <span className="rounded bg-slate-800 px-2 py-0.5">{item.modality}</span>
                                    <span className="rounded bg-slate-800 px-2 py-0.5">{item.species}</span>
                                    <span>Updated: {formatDate(item.updatedAt)}</span>
                                    <span>Last attempt: {formatDate(item.lastAttemptAt)}</span>
                                    <span>
                                        Last score: {item.lastScore != null ? item.lastScore.toFixed(1) : "—"}
                                    </span>
                                </div>
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            <div className="mt-3 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                    <button
                        className="rounded border border-slate-700 px-3 py-1 text-xs font-semibold hover:border-teal-400 disabled:opacity-40"
                        onClick={() => onPageChange(page - 1)}
                        disabled={page <= 0 || loading}
                        type="button"
                    >
                        Prev
                    </button>
                    <button
                        className="rounded border border-slate-700 px-3 py-1 text-xs font-semibold hover:border-teal-400 disabled:opacity-40"
                        onClick={() => onPageChange(page + 1)}
                        disabled={page + 1 >= totalPages || loading}
                        type="button"
                    >
                        Next
                    </button>
                </div>
                <button
                    className="flex-1 rounded-lg bg-teal-500 px-3 py-2 text-sm font-semibold text-slate-950 shadow-lg shadow-teal-500/20 hover:bg-teal-400 disabled:cursor-not-allowed disabled:opacity-40"
                    onClick={handleRandomPlay}
                    disabled={totalElements === 0 || loading}
                    type="button"
                >
                    Random Play
                </button>
            </div>
            {totalElements === 0 && !loading && (
                <p className="mt-1 text-right text-xs text-slate-400">Add filters to see results. Random play is disabled.</p>
            )}
        </section>
    );
}

function StatusBadge({ status }: { status: UserCaseStatus | string }) {
    const meta = getStatusMeta(status);
    return (
        <span
            className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${meta.bg}`}
            style={{ color: meta.color }}
        >
            {meta.label}
        </span>
    );
}

function Select({
    label,
    value,
    onChange,
    options,
}: {
    label: string;
    value: string;
    onChange: (value: string) => void;
    options: { value: string; label: string }[];
}) {
    return (
        <div className="space-y-1">
            <label className="text-xs text-slate-400">{label}</label>
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="w-full rounded-lg border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-teal-400 focus:outline-none"
            >
                {options.map((opt) => (
                    <option key={opt.value || opt.label} value={opt.value}>
                        {opt.label}
                    </option>
                ))}
            </select>
        </div>
    );
}

function useDebounce<T>(value: T, delay: number): T {
    const [debounced, setDebounced] = useState(value);

    useEffect(() => {
        const handle = setTimeout(() => setDebounced(value), delay);
        return () => clearTimeout(handle);
    }, [value, delay]);

    return debounced;
}
