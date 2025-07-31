package org.vuong.keycloak.spi.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.*;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Produces(MediaType.APPLICATION_JSON)
public class AdminUserResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator permissions;
    private final AdminEventBuilder adminEvent;

    public AdminUserResource(KeycloakSession session, RealmModel realm,
                             AdminPermissionEvaluator permissions, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.permissions = permissions;
        this.adminEvent = adminEvent;
    }

    @GET
    @Path("/ping")
    public Response ping() {
        return Response.ok("pong from admin").build();
    }

    @GET
    @Path("/by-ids")
    public Response getUsersByIds(@QueryParam("ids") List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing required query parameter: ids").build();
        }

        UserProvider userProvider = session.users();
        List<UserRepresentation> results = ids.stream()
                .map(id -> userProvider.getUserById(realm, id))
                .filter(Objects::nonNull)
                .map(this::toRepresentation)
                .collect(Collectors.toList());

        return Response.ok(results).build();
    }

    private UserRepresentation toRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        rep.setUsername(user.getUsername());
        rep.setEmail(user.getEmail());
        rep.setFirstName(user.getFirstName());
        rep.setLastName(user.getLastName());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setCreatedTimestamp(user.getCreatedTimestamp());

        // Add attributes
        rep.setAttributes(user.getAttributes());

        // Add realm roles
        List<String> roles = user.getRealmRoleMappingsStream()
                .map(RoleModel::getName)
                .collect(Collectors.toList());
        rep.setRealmRoles(roles);

        return rep;
    }
}