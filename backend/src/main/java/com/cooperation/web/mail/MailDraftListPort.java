package com.cooperation.web.mail;

import java.util.List;

/**
 * 邮件草稿列表查询端口，供控制器获取草稿概览和项目草稿列表。
 */
public interface MailDraftListPort {

    /**
     * 查询指定用户参与的所有项目的草稿概览。
     *
     * @param userId 当前用户标识
     * @return 项目草稿概览列表
     */
    List<MailDraftDto.DraftSummaryResponse> listDraftSummariesByUser(String userId);

    /**
     * 查询指定项目的所有草稿列表。
     *
     * @param projectId 项目标识
     * @return 项目草稿列表项
     */
    List<MailDraftDto.ProjectDraftListItemResponse> listProjectDrafts(String projectId);
}
