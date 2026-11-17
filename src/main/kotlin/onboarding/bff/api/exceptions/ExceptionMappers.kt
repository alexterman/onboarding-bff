package onboarding.bff.api.exceptions

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import onboarding.bff.api.generated.model.ErrorResponse
import onboarding.bff.api.generated.model.ErrorResponseError
import org.jboss.resteasy.reactive.server.ServerExceptionMapper

private val logger = KotlinLogging.logger {}

private const val INVALID_REQUEST = "INVALID_REQUEST"

/** Converts validation and malformed-body failures into the separate error envelope (never a `nextStep`). */
class ExceptionMappers {
    @ServerExceptionMapper
    fun mapConstraintViolation(exception: ConstraintViolationException): Response {
        logger.debug { "Request validation failed: ${exception.constraintViolations.map { it.propertyPath }}" }
        return invalidRequest()
    }

    /** Missing or wrongly typed fields (generated models have non-null required properties). */
    @ServerExceptionMapper
    fun mapMismatchedInput(exception: MismatchedInputException): Response {
        logger.debug { "Mismatched request body: ${exception.originalMessage}" }
        return invalidRequest()
    }

    @ServerExceptionMapper
    fun mapMalformedBody(exception: JsonProcessingException): Response {
        logger.debug { "Malformed request body: ${exception.originalMessage}" }
        return invalidRequest()
    }

    /** Quarkus REST wraps unparsable bodies in a 400 WebApplicationException; other statuses (404, 415, ...) pass through. */
    @ServerExceptionMapper
    fun mapWebApplicationException(exception: WebApplicationException): Response {
        if (exception.response.status != Response.Status.BAD_REQUEST.statusCode) {
            return exception.response
        }
        logger.debug { "Bad request: ${exception.message}" }
        return invalidRequest()
    }

    private fun invalidRequest(): Response =
        Response
            .status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(ErrorResponse(ErrorResponseError(INVALID_REQUEST, "The request is invalid.")))
            .build()
}
