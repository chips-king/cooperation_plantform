package com.cooperation.application.member;

import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.web.member.MemberDto.CreateInvitationResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 创建邀请应用用例，提供 Web 层可调用的最小端口式契约。
 */
@Service
public class CreateInvitationUseCase {

    /**
     * 创建邀请。
     *
     * @param command 创建邀请命令。
     * @return 创建后的邀请链接信息。
     */
    public CreateInvitationResponse create(Command command) {
        Objects.requireNonNull(command, "创建邀请命令不能为空");
        String code = "INVITE-" + command.groupId() + "-" + command.projectId();
        return new CreateInvitationResponse(
                null,
                command.groupId(),
                command.projectId(),
                command.mode(),
                code,
                "/invitations/" + code
        );
    }

    /**
     * 创建邀请命令。
     *
     * @param operatorId 操作用户标识。
     * @param groupId 小组标识。
     * @param projectId 项目标识。
     * @param mode 邀请模式。
     * @param roleTemplate 加入后角色模板。
     */
    public record Command(Long operatorId, Long groupId, Long projectId, String mode, RoleTemplate roleTemplate) {
    }
}
