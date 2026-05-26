import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * 后端统一响应结构。
 */
interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  message?: string;
  code?: string;
  errorCode?: string;
}

/**
 * 前端统一 HTTP 异常，保留状态码和业务错误码。
 */
export class HttpRequestError extends Error {
  readonly status?: number;
  readonly code?: string;

  /**
   * 创建 HTTP 请求异常。
   *
   * @param message 面向用户或调用方的错误说明
   * @param status HTTP 状态码
   * @param code 后端业务错误码
   */
  constructor(message: string, status?: number, code?: string) {
    super(message);
    this.name = 'HttpRequestError';
    this.status = status;
    this.code = code;
  }
}

/**
 * 读取当前登录令牌。
 *
 * @returns 本地保存的访问令牌，未登录时返回 null
 */
function getAccessToken(): string | null {
  return window.localStorage.getItem('access_token');
}

/**
 * 读取当前登录用户标识。
 *
 * @returns 本地保存的用户标识，未登录或数据异常时返回 null
 */
function getCurrentUserId(): string | null {
  const rawUser = window.localStorage.getItem('current_user');
  if (!rawUser) {
    return null;
  }

  try {
    const user = JSON.parse(rawUser) as { id?: number | string };
    return user.id === undefined || user.id === null || user.id === '' ? null : String(user.id);
  } catch {
    return null;
  }
}

/**
 * 为请求补充鉴权头。
 *
 * @param config Axios 请求配置
 * @returns 补充鉴权信息后的请求配置
 */
function attachAuthorization(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const token = getAccessToken();
  const userId = getCurrentUserId();

  // 存在令牌时才写入请求头，避免向匿名接口发送空 Authorization。
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // 当前后端控制器仍通过 X-User-Id 识别操作者，登录后统一由 HTTP 层补齐。
  if (userId && !config.headers['X-User-Id']) {
    config.headers['X-User-Id'] = userId;
  }

  return config;
}

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000, // 30 秒用于普通接口请求，上传下载会在业务 Service 中单独调整。
});

httpClient.interceptors.request.use(attachAuthorization);

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // 统一保留原始错误对象，后续业务 Service 可基于状态码做页面提示。
    return Promise.reject(error);
  },
);

/**
 * 判断响应体是否符合后端统一响应结构。
 *
 * @param value 待检查响应体
 * @returns 符合统一响应结构时返回 true
 */
function isApiEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return typeof value === 'object'
    && value !== null
    && 'success' in value;
}

/**
 * 将 Axios 错误转换为前端统一 HTTP 异常。
 *
 * @param error Axios 抛出的请求异常
 * @returns 包含状态码和业务错误码的 HTTP 异常
 */
function normalizeError(error: AxiosError): HttpRequestError {
  const responseData = error.response?.data;

  // 后端返回统一错误结构时，优先使用业务消息和错误码。
  if (isApiEnvelope(responseData)) {
    return new HttpRequestError(
      responseData.message || error.message,
      error.response?.status,
      responseData.code ?? responseData.errorCode,
    );
  }

  return new HttpRequestError(error.message, error.response?.status, error.code);
}

/**
 * 发送 HTTP 请求并解包后端统一响应。
 *
 * @param config Axios 请求配置
 * @returns 后端统一响应中的业务数据
 */
export async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await httpClient.request<ApiEnvelope<T> | T>(config);
    const responseData = response.data;

    // 后端统一响应成功时只向业务层返回 data，失败时交给统一异常处理。
    if (isApiEnvelope<T>(responseData)) {
      if (responseData.success) {
        return responseData.data as T;
      }

      throw new HttpRequestError(responseData.message || '请求失败', response.status, responseData.code ?? responseData.errorCode);
    }

    return responseData as T;
  } catch (error) {
    if (error instanceof HttpRequestError) {
      throw error;
    }

    if (axios.isAxiosError(error)) {
      throw normalizeError(error);
    }

    throw error;
  }
}
