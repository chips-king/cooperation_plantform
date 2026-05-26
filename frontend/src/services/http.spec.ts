import axios, { AxiosError, type AxiosResponse } from 'axios';
import { beforeEach, describe, expect, it } from 'vitest';

import { request } from './http';

/**
 * 创建 Axios 适配器响应，避免测试依赖真实网络。
 *
 * @param config 请求配置
 * @param data 响应体数据
 * @returns Axios 标准响应对象
 */
function createResponse(config: AxiosResponse['config'], data: unknown): AxiosResponse {
  return {
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  };
}

describe('http request', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('解析统一成功响应并返回业务数据', async () => {
    const result = await request<{ id: string }>({
      url: '/projects/p1',
      method: 'GET',
      adapter: async (config) => createResponse(config, {
        success: true,
        data: { id: 'p1' },
        message: 'ok',
      }),
    });

    expect(result).toEqual({ id: 'p1' });
  });

  it('存在本地令牌时自动注入 Authorization 请求头', async () => {
    window.localStorage.setItem('access_token', 'token-1');

    await request({
      url: '/projects',
      method: 'GET',
      adapter: async (config) => {
        expect(config.headers.Authorization).toBe('Bearer token-1');
        return createResponse(config, { success: true, data: [] });
      },
    });
  });

  it('存在本地用户时自动注入 X-User-Id 请求头', async () => {
    window.localStorage.setItem('current_user', JSON.stringify({
      id: 1001,
      displayName: '管理员',
      email: 'admin@example.com',
    }));

    await request({
      url: '/projects',
      method: 'GET',
      adapter: async (config) => {
        expect(config.headers['X-User-Id']).toBe('1001');
        return createResponse(config, { success: true, data: [] });
      },
    });
  });

  it('将后端错误响应转换为带状态码和错误码的异常', async () => {
    await expect(request({
      url: '/projects',
      method: 'GET',
      adapter: async (config) => {
        throw new AxiosError(
          'Request failed',
          'ERR_BAD_REQUEST',
          config,
          undefined,
          {
            ...createResponse(config, {
              success: false,
              code: 'FORBIDDEN',
              message: '没有权限访问项目',
            }),
            status: 403,
            statusText: 'Forbidden',
          },
        );
      },
    })).rejects.toMatchObject({
      message: '没有权限访问项目',
      status: 403,
      code: 'FORBIDDEN',
    });
  });
});
