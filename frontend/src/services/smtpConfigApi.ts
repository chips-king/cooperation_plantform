import { request } from './http';

/**
 * SMTP 配置。
 */
export interface SmtpConfig {
  id: number;
  name: string;
  host: string;
  port: number;
  username: string;
  fromAddress: string;
  imapHost: string;
  imapPort: number;
  sslEnabled: boolean;
  starttlsEnabled: boolean;
  isDefault: boolean;
  createdBy: number;
}

/**
 * 保存 SMTP 配置请求。
 */
export interface SaveSmtpConfigRequest {
  name: string;
  host: string;
  port: number;
  username: string;
  password: string;
  fromAddress: string;
  imapHost: string;
  imapPort: number;
  sslEnabled: boolean;
  starttlsEnabled: boolean;
  isDefault: boolean;
}

/**
 * 测试 SMTP 配置结果。
 */
export interface TestSmtpResponse {
  success: boolean;
  message: string;
}

/**
 * 查询当前用户的所有 SMTP 配置。
 */
export function listSmtpConfigs(): Promise<SmtpConfig[]> {
  return request<SmtpConfig[]>({ url: '/smtp-configs', method: 'GET' });
}

/**
 * 新增 SMTP 配置。
 */
export function createSmtpConfig(data: SaveSmtpConfigRequest): Promise<SmtpConfig> {
  return request<SmtpConfig>({ url: '/smtp-configs', method: 'POST', data });
}

/**
 * 修改 SMTP 配置。
 */
export function updateSmtpConfig(id: number, data: SaveSmtpConfigRequest): Promise<SmtpConfig> {
  return request<SmtpConfig>({ url: `/smtp-configs/${id}`, method: 'PUT', data });
}

/**
 * 删除 SMTP 配置。
 */
export function deleteSmtpConfig(id: number): Promise<void> {
  return request<void>({ url: `/smtp-configs/${id}`, method: 'DELETE' });
}

/**
 * 发送测试邮件验证 SMTP 配置。
 */
export function testSmtpConfig(id: number, testRecipient: string): Promise<TestSmtpResponse> {
  return request<TestSmtpResponse>({
    url: `/smtp-configs/${id}/test`,
    method: 'POST',
    data: { testRecipient },
  });
}

/**
 * 设为默认 SMTP 配置。
 */
export function setDefaultSmtpConfig(id: number): Promise<void> {
  return request<void>({ url: `/smtp-configs/${id}/default`, method: 'PUT' });
}
