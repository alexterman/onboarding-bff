package onboarding.bff.api

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import onboarding.bff.configs.PostgresTestResource
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test

private const val ONBOARDINGS_PATH = "/api/v1/onboardings"

@QuarkusTest
@QuarkusTestResource(PostgresTestResource::class)
class OnboardingsResourceShould {
    @Test
    fun `answer start onboarding with the error envelope while not implemented`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"type": "retail"}""")
            .`when`()
            .post(ONBOARDINGS_PATH)
            .then()
            .statusCode(501)
            .body("error.code", equalTo("NOT_IMPLEMENTED"))
            .body("$", not(hasKey("nextStep")))
    }

    @Test
    fun `answer next step with the error envelope while not implemented`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"session": {"jwt": "eyJ"}}""")
            .`when`()
            .post("$ONBOARDINGS_PATH/next-step")
            .then()
            .statusCode(501)
            .body("error.code", equalTo("NOT_IMPLEMENTED"))
    }

    @Test
    fun `answer submit step accepting dynamic user input while not implemented`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "stepId": "verify-phone",
                  "session": {"jwt": "eyJ"},
                  "userInput": {"verification": {"code": "1234"}},
                  "additionalAttributes": {"verificationId": "abc123"}
                }
                """.trimIndent()
            )
            .`when`()
            .post("$ONBOARDINGS_PATH/steps")
            .then()
            .statusCode(501)
            .body("error.code", equalTo("NOT_IMPLEMENTED"))
    }

    @Test
    fun `reject a request missing required fields with the error envelope`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"stepId": "verify-phone", "session": {"jwt": "eyJ"}}""")
            .`when`()
            .post("$ONBOARDINGS_PATH/steps")
            .then()
            .statusCode(400)
            .body("error.code", equalTo("INVALID_REQUEST"))
            .body("$", not(hasKey("nextStep")))
    }

    @Test
    fun `reject a malformed body with the error envelope`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"type": """)
            .`when`()
            .post(ONBOARDINGS_PATH)
            .then()
            .statusCode(400)
            .body("error.code", equalTo("INVALID_REQUEST"))
    }

    @Test
    fun `serve the OpenAPI contract with the onboarding paths`() {
        given()
            .`when`()
            .get("/swagger?format=json")
            .then()
            .statusCode(200)
            .body("paths.keySet()", org.hamcrest.Matchers.hasItems(
                ONBOARDINGS_PATH, "$ONBOARDINGS_PATH/next-step", "$ONBOARDINGS_PATH/steps"
            ))
    }
}
