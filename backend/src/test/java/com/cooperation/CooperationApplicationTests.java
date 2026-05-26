package com.cooperation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 后端应用部署态上下文加载测试，使用真实 MySQL 容器验证迁移和接口启动链路。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CooperationApplicationTests {

    /** MySQL 测试镜像版本，固定版本避免数据库行为随 latest 漂移。 */
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.39");

    /** 测试数据库名称，仅用于应用上下文启动测试。 */
    private static final String TEST_DATABASE_NAME = "cooperation_context_test";

    /** 测试数据库用户，仅在容器内部使用。 */
    private static final String TEST_DATABASE_USERNAME = "context_user";

    /** 测试数据库密码，仅在容器内部使用。 */
    private static final String TEST_DATABASE_PASSWORD = "context_password";

    /** MySQL 容器提供真实数据库，避免应用测试退回内存仓储。 */
    @Container
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName(TEST_DATABASE_NAME)
            .withUsername(TEST_DATABASE_USERNAME)
            .withPassword(TEST_DATABASE_PASSWORD);

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 注册测试容器连接信息，覆盖本机环境中的数据库账号。
     *
     * @param registry Spring 动态属性注册器
     */
    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
        registry.add("app.mail.enabled", () -> "false");
    }

    /**
     * 验证 Spring 应用上下文可以在真实 MySQL 上完成启动。
     */
    @Test
    void contextLoads() {
    }

    /**
     * 验证首页项目列表在空数据库初始态可以正常返回空分页。
     */
    @Test
    void projectsEndpointReturnsSuccessWithEmptyDatabase() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("dev-token-1");
        headers.set("X-User-Id", "1");

        ResponseEntity<String> response = restTemplate.exchange(
                "/projects?page=1&size=20",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"success\":true");
    }
}
