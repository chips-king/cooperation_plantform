package com.cooperation.application.permission;

import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.user.UserRepository;
import com.cooperation.web.permission.PermissionDto.MemberPermissionResponse;
import com.cooperation.web.permission.PermissionDto.ProjectPermissionResponse;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 项目权限查询应用用例，提供成员权限列表查询的最小端口式契约。
 */
@Service
public class QueryProjectPermissionUseCase {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public QueryProjectPermissionUseCase(MembershipRepository membershipRepository, UserRepository userRepository) {
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "成员仓储不能为空");
        this.userRepository = Objects.requireNonNull(userRepository, "用户仓储不能为空");
    }

    /**
     * 查询项目权限列表。
     *
     * @param query 权限查询条件。
     * @return 项目权限响应。
     */
    public ProjectPermissionResponse query(Query query) {
        Objects.requireNonNull(query, "权限查询条件不能为空");

        List<Membership> memberships = membershipRepository.findByProjectId(query.projectId());

        List<MemberPermissionResponse> memberResponses = memberships.stream()
                .map(m -> {
                    String userName = userRepository.findById(m.getUserId())
                            .map(UserRepository.UserProfile::displayName)
                            .orElse("用户" + m.getUserId());
                    Set<PermissionCode> perms = m.getCustomPermissions() != null
                            ? m.getCustomPermissions().asSet()
                            : m.getRoleTemplate().defaultPermissions().asSet();
                    return new MemberPermissionResponse(
                            m.getId(),
                            m.getUserId(),
                            userName,
                            m.getRoleTemplate(),
                            perms
                    );
                })
                .toList();

        return new ProjectPermissionResponse(query.projectId(), memberResponses);
    }

    /**
     * 项目权限查询条件。
     *
     * @param operatorId 当前用户标识。
     * @param projectId 项目标识。
     */
    public record Query(Long operatorId, Long projectId) {
    }
}
