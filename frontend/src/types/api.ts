/**
 * 后端字段级校验错误。
 */
export interface FieldErrorItem {
  field: string;
  message: string;
}

/**
 * 后端统一响应结构。
 */
export interface ApiResponse<T> {
  success: boolean;
  code: string | null;
  message: string;
  data: T;
  fieldErrors: FieldErrorItem[] | null;
}

/**
 * 后端分页响应结构。
 */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

/**
 * 前端统一请求异常，供页面层展示状态码、错误码和字段错误。
 */
export interface ApiError extends Error {
  status?: number;
  code?: string;
  fieldErrors?: FieldErrorItem[];
}

/**
 * 分页查询参数。
 */
export interface PageQuery {
  page?: number;
  size?: number;
}

/**
 * 空响应占位类型。
 */
export type EmptyResponse = null;
