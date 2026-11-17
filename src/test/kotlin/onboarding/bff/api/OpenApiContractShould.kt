package onboarding.bff.api

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import onboarding.bff.configs.PostgresTestResource
import onboarding.bff.configs.RestAssuredTestResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val ONBOARDINGS_PATH = "/api/v1/onboardings"

@QuarkusTest
@QuarkusTestResource(RestAssuredTestResource::class)
@QuarkusTestResource(PostgresTestResource::class)
class OpenApiContractShould {
    @Test
    fun `serve the OpenAPI contract with the onboarding paths`() {
        val paths =
            given()
                .`when`()
                .get("/swagger?format=json")
                .then()
                .statusCode(200)
                .extract()
                .path<Map<String, Any>>("paths")

        assertThat(paths.keys)
            .containsExactlyInAnyOrder(ONBOARDINGS_PATH, "$ONBOARDINGS_PATH/next-step", "$ONBOARDINGS_PATH/steps")
    }
}
