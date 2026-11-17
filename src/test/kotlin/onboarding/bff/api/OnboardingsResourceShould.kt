package onboarding.bff.api

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import onboarding.bff.api.resources.OnboardingsResource
import onboarding.bff.configs.PostgresTestResource
import onboarding.bff.configs.RestAssuredTestResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(RestAssuredTestResource::class)
@QuarkusTestResource(PostgresTestResource::class)
@TestHTTPEndpoint(OnboardingsResource::class)
class OnboardingsResourceShould {
    @Test
    fun `answer start onboarding with the error envelope while not implemented`() {
        val response = post("", """{"type": "retail"}""", 501)

        assertThat(response.errorCode()).isEqualTo("NOT_IMPLEMENTED")
        assertThat(response.asMap()).doesNotContainKey("nextStep")
    }

    @Test
    fun `answer next step with the error envelope while not implemented`() {
        val response = post("/next-step", """{"session": {"jwt": "eyJ"}}""", 501)

        assertThat(response.errorCode()).isEqualTo("NOT_IMPLEMENTED")
    }

    @Test
    fun `answer submit step accepting dynamic user input while not implemented`() {
        val response =
            post(
                "/steps",
                """
                {
                  "stepId": "verify-phone",
                  "session": {"jwt": "eyJ"},
                  "userInput": {"verification": {"code": "1234"}},
                  "additionalAttributes": {"verificationId": "abc123"}
                }
                """.trimIndent(),
                501
            )

        assertThat(response.errorCode()).isEqualTo("NOT_IMPLEMENTED")
    }

    @Test
    fun `reject a request missing required fields with the error envelope`() {
        val response = post("/steps", """{"stepId": "verify-phone", "session": {"jwt": "eyJ"}}""", 400)

        assertThat(response.errorCode()).isEqualTo("INVALID_REQUEST")
        assertThat(response.asMap()).doesNotContainKey("nextStep")
    }

    @Test
    fun `reject a malformed body with the error envelope`() {
        val response = post("", """{"type": """, 400)

        assertThat(response.errorCode()).isEqualTo("INVALID_REQUEST")
    }

    private fun post(
        path: String,
        body: String,
        expectedStatus: Int,
    ): ValidatableResponse =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .`when`()
            .post(path)
            .then()
            .statusCode(expectedStatus)

    private fun ValidatableResponse.errorCode(): String = extract().path("error.code")

    private fun ValidatableResponse.asMap(): Map<String, Any> = extract().path("$")
}
