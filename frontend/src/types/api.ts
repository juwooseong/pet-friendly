/** 백엔드 공통 REST 응답 래퍼 (RULES.md 3장 API Response Standard) */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error: string | null;
  timestamp: string;
}

/** 백엔드 Pageable 검색 응답 래퍼 (PageResponse<T>) */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
