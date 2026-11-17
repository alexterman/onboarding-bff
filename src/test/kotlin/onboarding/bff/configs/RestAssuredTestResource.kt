package onboarding.bff.configs

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.restassured.RestAssured
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter

class RestAssuredTestResource : QuarkusTestResourceLifecycleManager {
    init {
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())
    }

    override fun start(): Map<String, String> = emptyMap()

    override fun stop() {
        // empty
    }
}
