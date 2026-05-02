import { api } from "./client";
import type {
  AuthResponse, QuestionDetail, QuestionSummary, SearchHit, Suggestion,
  Topic, UserProfile, PageResponse,
} from "@/types";

// --- Auth ---
export const authApi = {
  login: (usernameOrEmail: string, password: string) =>
    api.post<AuthResponse>("/api/auth/login", { usernameOrEmail, password }).then((r) => r.data),
  register: (username: string, email: string, password: string, displayName?: string) =>
    api.post<AuthResponse>("/api/auth/register", { username, email, password, displayName }).then((r) => r.data),
  refresh: (refreshToken: string) =>
    api.post<AuthResponse>("/api/auth/refresh", { refreshToken }).then((r) => r.data),
};

// --- Topics ---
export const topicsApi = {
  list: (featured = false) =>
    api.get<Topic[]>("/api/topics", { params: { featured } }).then((r) => r.data),
  bySlug: (slug: string) =>
    api.get<Topic>(`/api/topics/${slug}`).then((r) => r.data),
};

// --- Questions ---
export const questionsApi = {
  list: (topicId?: string, page = 0, size = 20) =>
    api.get<PageResponse<QuestionSummary>>("/api/questions", { params: { topicId, page, size } }).then((r) => r.data),
  hot: (limit = 10) =>
    api.get<QuestionSummary[]>("/api/questions/hot", { params: { limit } }).then((r) => r.data),
  byId: (id: string) =>
    api.get<QuestionDetail>(`/api/questions/${id}`).then((r) => r.data),
  create: (data: { title: string; content: string; topicId: string; difficulty?: string; tags?: string[] }) =>
    api.post<QuestionDetail>("/api/questions", data).then((r) => r.data),
  like: (id: string) => api.post(`/api/questions/${id}/likes`).then((r) => r.data),
  unlike: (id: string) => api.delete(`/api/questions/${id}/likes`).then((r) => r.data),
};

// --- Search ---
export const searchApi = {
  typeahead: (q: string, topicId?: string, limit = 10) =>
    api.get<SearchHit[]>("/api/search/typeahead", { params: { q, topicId, limit } }).then((r) => r.data),
  semantic: (q: string, limit = 10) =>
    api.get<SearchHit[]>("/api/search/semantic", { params: { q, limit } }).then((r) => r.data),
  hybrid: (q: string, limit = 10) =>
    api.get<SearchHit[]>("/api/search/hybrid", { params: { q, limit } }).then((r) => r.data),
};

// --- Users ---
export const usersApi = {
  byUsername: (username: string) =>
    api.get<UserProfile>(`/api/users/${username}`).then((r) => r.data),
  me: () => api.get<UserProfile>("/api/users/me").then((r) => r.data),
  updateMe: (data: { displayName?: string; bio?: string }) =>
    api.patch<UserProfile>("/api/users/me", data).then((r) => r.data),
  uploadAvatar: (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<{ avatarUrl: string }>("/api/users/me/avatar", fd, {
      headers: { "Content-Type": "multipart/form-data" },
    }).then((r) => r.data);
  },
};

// --- Suggestions ---
export const suggestionsApi = {
  create: (data: { type: string; questionId?: string; payload: Record<string, unknown>; rationale?: string }) =>
    api.post<Suggestion>("/api/suggestions", data).then((r) => r.data),
  mine: (page = 0, size = 20) =>
    api.get<PageResponse<Suggestion>>("/api/suggestions/me", { params: { page, size } }).then((r) => r.data),
  pending: (page = 0, size = 20) =>
    api.get<PageResponse<Suggestion>>("/api/admin/suggestions/pending", { params: { page, size } }).then((r) => r.data),
  review: (id: string, decision: "APPROVED" | "REJECTED", reviewNotes?: string) =>
    api.post<Suggestion>(`/api/admin/suggestions/${id}/review`, { decision, reviewNotes }).then((r) => r.data),
};
