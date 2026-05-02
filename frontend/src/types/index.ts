export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type QuestionStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type SuggestionStatus = "PENDING" | "APPROVED" | "REJECTED";
export type SuggestionType = "NEW_QUESTION" | "NEW_TOPIC" | "EDIT_QUESTION" | "NEW_ANSWER";
export type Role = "USER" | "ADMIN";

export interface User {
  id: string;
  username: string;
  email: string;
  displayName: string;
  role: Role;
  avatarUrl?: string | null;
}

export interface Topic {
  id: string;
  name: string;
  slug: string;
  description?: string;
  colorHex?: string;
  parentId?: string | null;
  questionCount: number;
  featured: boolean;
}

export interface QuestionSummary {
  id: string;
  title: string;
  topicId: string;
  topicName: string;
  authorUsername: string;
  difficulty: Difficulty;
  likeCount: number;
  answerCount: number;
  viewCount: number;
  tags: string[];
  createdAt: string;
}

export interface QuestionDetail extends QuestionSummary {
  content: string;
  status: QuestionStatus;
  updatedAt: string;
}

export interface SearchHit {
  id: string;
  title: string;
  topicName: string;
  authorUsername: string;
  difficulty: string;
  likeCount: number;
  viewCount: number;
  score: number;
}

export interface PortfolioStats {
  posts: number;
  suggestions: number;
  acceptedSuggestions: number;
  likesReceived: number;
  reputation: number;
}

export interface UserProfile extends User {
  bio?: string;
  joinedAt: string;
  stats: PortfolioStats;
}

export interface Suggestion {
  id: string;
  type: SuggestionType;
  userId: string;
  username: string;
  questionId?: string | null;
  payload: Record<string, unknown>;
  rationale?: string;
  status: SuggestionStatus;
  reviewedByUsername?: string | null;
  reviewedAt?: string | null;
  reviewNotes?: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
