package de.codecentric.microcks_demo.tests.infrastructure;

import io.github.microcks.web.dto.OperationOverrideDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST interface for JAX-RS-based access to certain operations of
 * <a href="https://microcks.io/documentation/references/apis/open-api/">Microcks' API</a>.
 */
@Path("/api")
public interface MicrocksApi {
    /**
     * Retrieve information about a mocked service.
     */
    @GET
    @Path("/services/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getService(@PathParam("id") String serviceId, @QueryParam("messages") boolean messages);

    /**
     * Override the configuration of a mocked service operation.
     */
    @PUT
    @Path("/services/{id}/operation")
    @Consumes("application/json")
    Response overrideServiceOperation(
        @PathParam("id") String serviceId,
        @QueryParam("operationName") String operationName,
        OperationOverrideDTO operationOverride
    );
}
