package onboarding.bff.configs

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresTestResource : QuarkusTestResourceLifecycleManager {
    companion object {
        private val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:15.6"))
                .withDatabaseName("onboarding_test")
                .withUsername("root_test")
                .withPassword("root_test")
    }

    override fun start(): Map<String, String> {
        postgres.start()
        return mapOf(
            "quarkus.datasource.username" to postgres.username,
            "quarkus.datasource.password" to postgres.password,
            "quarkus.datasource.jdbc.url" to postgres.jdbcUrl
        )
    }

    override fun stop() {
        if (postgres.isRunning) {
            postgres.stop()
        }
    }
}
