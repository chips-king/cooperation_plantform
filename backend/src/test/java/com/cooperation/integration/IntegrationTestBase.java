package com.cooperation.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基础类，统一提供 Spring Boot 测试上下文和 Testcontainers MySQL 容器。
 *
 * <p>后续集成测试类继承本类后，可直接复用同一个 MySQL 测试容器，并通过动态属性注入
 * Spring DataSource 配置。</p>
 */
@Testcontainers
@SpringBootTest
public abstract class IntegrationTestBase {

    /** MySQL 测试容器镜像版本，固定版本避免测试环境随上游 latest 变化而漂移。 */
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.39");

    /** 测试数据库名称，用于隔离集成测试与本地开发数据库。 */
    private static final String TEST_DATABASE_NAME = "cooperation_test";

    /** 测试数据库用户名，仅在容器内部测试环境使用。 */
    private static final String TEST_DATABASE_USERNAME = "test_user";

    /** 测试数据库密码，仅在容器内部测试环境使用。 */
    private static final String TEST_DATABASE_PASSWORD = "test_password";

    /** 静态 MySQL 容器会在测试类之间复用，减少后续集成测试启动成本。 */
    @Container
    protected static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName(TEST_DATABASE_NAME)
            .withUsername(TEST_DATABASE_USERNAME)
            .withPassword(TEST_DATABASE_PASSWORD);

    /**
     * 将 Testcontainers 动态分配的 JDBC 连接信息注册到 Spring 测试上下文。
     *
     * @param registry Spring 动态属性注册器，用于覆盖测试环境 DataSource 配置
     */
    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
    }
}
