package onboarding.bff.api.resources

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import jakarta.ws.rs.core.Response
import onboarding.bff.api.generated.OnboardingsApi
import onboarding.bff.api.generated.model.ErrorResponse
import onboarding.bff.api.generated.model.ErrorResponseError
import onboarding.bff.api.generated.model.NextStepRequest
import onboarding.bff.api.generated.model.StartOnboardingRequest
import onboarding.bff.api.generated.model.SubmitStepRequest
import onboarding.bff.api.withCommonContext

private val logger = KotlinLogging.logger {}

/** Implements the interface generated from `META-INF/openapi.yaml`; flow execution arrives in feature 002. */
class OnboardingsResource : OnboardingsApi {
    @Timed(percentiles = [0.50, 0.75, 0.95, 0.99], histogram = true)
    @Counted
    override suspend fun startOnboarding(startOnboardingRequest: StartOnboardingRequest): Response =
        withCommonContext {
            logger.debug { "Request start onboarding [type: ${startOnboardingRequest.type}]" }
            notImplemented()
        }

    @Timed(percentiles = [0.50, 0.75, 0.95, 0.99], histogram = true)
    @Counted
    override suspend fun getNextStep(nextStepRequest: NextStepRequest): Response =
        withCommonContext {
            logger.debug { "Request next step" }
            notImplemented()
        }

    @Timed(percentiles = [0.50, 0.75, 0.95, 0.99], histogram = true)
    @Counted
    override suspend fun submitStep(submitStepRequest: SubmitStepRequest): Response =
        withCommonContext {
            logger.debug { "Request submit step [stepId: ${submitStepRequest.stepId}]" }
            notImplemented()
        }

    private fun notImplemented(): Response =
        Response
            .status(Response.Status.NOT_IMPLEMENTED)
            .entity(ErrorResponse(ErrorResponseError("NOT_IMPLEMENTED", "Not implemented yet.")))
            .build()
}
