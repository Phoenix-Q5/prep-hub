import { api } from "./client";
import type { Suggestion, PageResponse } from "@/types";

export interface DashboardStats {
  totalUsers: number;
  totalQuestions: number;
  publishedQuestions: number;
  pendingSuggestions: number;
}

export interface BulkUploadResult {
  totalRows: number;
  successCount: number;
  errorCount: number;
  errors: { row: number; title: string; error: string }[];
}

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  displayName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

export const adminApi = {
  // Dashboard
  stats: () => api.get<DashboardStats>("/api/admin/stats").then((r) => r.data),

  // Bulk upload
  uploadJson: (questions: unknown[]) =>
    api.post<BulkUploadResult>("/api/admin/upload/json", { questions }).then((r) => r.data),

  uploadFile: (file: File, format: "json-file" | "csv" | "excel") => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post<BulkUploadResult>(`/api/admin/upload/${format}`, fd, {
      headers: { "Content-Type": "multipart/form-data" },
    }).then((r) => r.data);
  },

  // Templates
  templateUrl: (format: "excel" | "csv" | "json") => `/api/admin/templates/${format}`,

  // Users
  listUsers: (page = 0, size = 50) =>
    api.get<PageResponse<AdminUser>>("/api/admin/users", { params: { page, size } }).then((r) => r.data),

  toggleUser: (id: string) =>
    api.patch(`/api/admin/users/${id}/toggle`).then((r) => r.data),

  changeRole: (id: string, role: string) =>
    api.patch(`/api/admin/users/${id}/role`, { role }).then((r) => r.data),

  // Suggestions (reuse from endpoints)
  pendingSuggestions: (page = 0, size = 20) =>
    api.get<PageResponse<Suggestion>>("/api/admin/suggestions/pending", { params: { page, size } }).then((r) => r.data),

  reviewSuggestion: (id: string, decision: "APPROVED" | "REJECTED", reviewNotes?: string) =>
    api.post<Suggestion>(`/api/admin/suggestions/${id}/review`, { decision, reviewNotes }).then((r) => r.data),
};
