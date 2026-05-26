package com.cooperation.application.member;

import com.cooperation.web.member.MemberDto.InvitationDetailResponse;
import org.springframework.stereotype.Service;

/**
 * 查询邀请应用用例，提供邀请码详情查询的最小端口式契约。
 */
@Service
public class QueryInvitationUseCase {

    /**
     * 查询邀请详情。
     *
     * @param code 邀请码。
     * @return 邀请详情。
     */
    public InvitationDetailResponse detail(String code) {
        return new InvitationDetailResponse(null, null, null, null, null, null, "valid");
    }
}
