package org.vuong.keycloak.spi.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.*;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserResource {

    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public UserResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    @OPTIONS
    @Path("{any:.*}")
    public Response preflight() {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    @GET
    @Path("/by-ids")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getUsers(
            @QueryParam("ids") List<String> userIds
    ) {
        checkRealmAccess();

        RealmModel realm = session.getContext().getRealm();
        UserProvider userProvider = session.users();

        // userLocalStorage(): Get keycloak specific local storage for users.  No cache in front, this api talks directly to database configured for Keycloak
        // users(): Get a cached view of all users in system including  users loaded by UserStorageProviders
        // searchForUser(): Support Attributes since v15.1.0
        List<UserRepresentation> users = userIds
                .stream()
                .map((id) -> userProvider.getUserById(realm, id))
                .filter(Objects::nonNull)
                .map(this::toFullRepresentation)
                .toList();

        return Response.ok(users).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserById(@PathParam("id") String id) {
        checkRealmAccess();

        UserModel user = session.users().getUserById(session.getContext().getRealm(), id);
        if (user == null) return Response.status(Response.Status.NOT_FOUND).build();

        return Response.ok(toFullRepresentation(user)).build();
    }

    @GET
    @Path("/{id}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserRoles(@PathParam("id") String id) {
        checkRealmAccess();

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, id);
        if (user == null) return Response.status(Response.Status.NOT_FOUND).build();

        List<String> roles = user.getRoleMappingsStream()
                .map(RoleModel::getName)
                .toList();

        return Response.ok(roles).build();
    }

    @GET
    @Path("/{id}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserGroups(@PathParam("id") String id) {
        checkRealmAccess();

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, id);
        if (user == null) return Response.status(Response.Status.NOT_FOUND).build();

        List<String> groups = user.getGroupsStream()
                .map(GroupModel::getName)
                .toList();

        return Response.ok(groups).build();
    }


    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser() {
        if (auth == null) throw new NotAuthorizedException("Bearer");

        RealmModel realm = session.getContext().getRealm();
        UserModel user = auth.getUser();

        return Response.ok(toFullRepresentation(user)).build();
    }


    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response countUsers() {
        checkRealmAccess();

        RealmModel realm = session.getContext().getRealm();
        int count = session.users().getUsersCount(realm);

        return Response.ok(count).build();
    }

    @GET
    @Path("/hello")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response sayHello() {
        checkRealmAccess();
        return Response.ok("Hello").build();
    }

    private void checkRealmAccess() {
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        } else if (auth.getToken().getRealmAccess() == null || !auth.getToken().getRealmAccess().isUserInRole("admin")) {
            throw new ForbiddenException("Does not have permission to fetch users");
        }
    }

    public UserRepresentation toFullRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        rep.setUsername(user.getUsername());
        rep.setEmail(user.getEmail());
        rep.setFirstName(user.getFirstName());
        rep.setLastName(user.getLastName());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setCreatedTimestamp(user.getCreatedTimestamp());

        // Get attributes (custom or standard)
        Map<String, List<String>> attrs = user.getAttributes();
        if (attrs != null) {
            rep.setAttributes(attrs);
        }

        // Add roles
        List<String> roleModels = user.getRealmRoleMappingsStream()
                .map(RoleModel::getName)
                .toList();
        rep.setRealmRoles(roleModels);

        return rep;
    }
}
