package org.vuong.keycloak.spi.adapter;

import org.keycloak.component.ComponentModel; // Import ComponentModel
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId; // Import StorageId
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.repository.RoleRepository; // Keep this if used for other RoleModel methods

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class RoleAdapter implements RoleModel {

    private static final Logger log = LoggerFactory.getLogger(RoleAdapter.class); // Initialize Logger

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ComponentModel model; // Add ComponentModel
    private final Role role;
    private final RoleRepository roleRepository; // Keep this if you need it for other RoleModel methods

    public RoleAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, Role role, RoleRepository roleRepository) {
        this.session = session;
        this.realm = realm;
        this.model = model; // Assign ComponentModel
        this.role = role;
        this.roleRepository = roleRepository;
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        log.debug("Setting description for role {}: {}", role.getName(), description);
        role.setDescription(description);
        // You should have a way to persist this change.
        // If RoleRepository.save() can handle merge, you might call it here.
        // For simplicity, we'll assume persistence is handled by the provider later.
        // roleRepository.save(role); // Uncomment if you want immediate persistence
    }

    @Override
    public String getId() {
        // Keycloak expects the ID to be the Keycloak-generated UUID, which you store in 'keycloakId'.
        // If 'keycloakId' is null (e.g., for roles not yet synced by Keycloak or created internally),
        // we fallback to StorageId.keycloakId which creates a composite ID using your internal ID.
        if (role.getKeycloakId() != null) {
            return role.getKeycloakId();
        }
        // This is a fallback if the role's keycloakId was not yet set.
        // It forms a composite ID like "f:provider_id:internal_id".
        return StorageId.keycloakId(model, String.valueOf(role.getId()));
    }

    @Override
    public void setName(String name) {
        log.debug("Setting name for role {}: {}", role.getName(), name);
        role.setName(name);
        // roleRepository.save(role); // Uncomment if you want immediate persistence
    }

    @Override
    public boolean isComposite() {
        // Assuming your custom storage does not natively support composite roles directly in the 'Role' entity.
        // If it did, you'd check a relationship here.
        return false;
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        // If isComposite() is false, this method typically does nothing for custom storage.
        log.warn("Attempted to add composite role '{}' to non-composite role '{}'. No action taken.", role.getName(), getName());
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        // If isComposite() is false, this method typically does nothing for custom storage.
        log.warn("Attempted to remove composite role '{}' from non-composite role '{}'. No action taken.", role.getName(), getName());
    }

    @Override
    public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
        // If isComposite() is false, return an empty stream.
        return Stream.empty();
    }

    @Override
    public boolean isClientRole() {
        // A role is a client role if its clientId is not null.
        return role.getClientId() != null;
    }

    @Override
    public String getContainerId() {
        // If it's a client role, return the client ID. Otherwise, return the realm ID.
        if (isClientRole()) {
            return role.getClientId();
        }
        return realm.getId();
    }

    @Override
    public RoleContainerModel getContainer() {
        // If it's a client role, return the ClientModel. Otherwise, return the RealmModel.
        if (isClientRole()) {
            // Need to retrieve the ClientModel from KeycloakSession using the stored client ID.
            return session.clients().getClientById(realm, role.getClientId());
        }
        return realm;
    }

    @Override
    public boolean hasRole(RoleModel roleModel) {
        // This method checks if *this* role is a composite of the given 'roleModel'.
        // Since isComposite() returns false, this should also return false.
        return false;
    }

    // --- Attribute methods (as per your original code, assuming no generic attributes for Role) ---

    @Override
    public void setSingleAttribute(String name, String value) {
        log.warn("RoleAdapter: setSingleAttribute('{}', '{}') not implemented for role '{}'.", name, value, getName());
        // Implement if your Role entity supports generic attributes
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        log.warn("RoleAdapter: setAttribute('{}', {}) not implemented for role '{}'.", name, values, getName());
        // Implement if your Role entity supports generic attributes
    }

    @Override
    public void removeAttribute(String name) {
        log.warn("RoleAdapter: removeAttribute('{}') not implemented for role '{}'.", name, getName());
        // Implement if your Role entity supports generic attributes
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        log.warn("RoleAdapter: getAttributeStream('{}') not implemented for role '{}'. Returning empty stream.", name, getName());
        // Implement if your Role entity supports generic attributes
        return Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        log.warn("RoleAdapter: getAttributes() not implemented for role '{}'. Returning empty map.", getName());
        // Implement if your Role entity supports generic attributes
        return Collections.emptyMap();
    }

    @Override
    public int hashCode() {
        return Objects.hash(role.getId(), role.getKeycloakId(), role.getRealmId(), role.getClientId(), role.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RoleAdapter that = (RoleAdapter) obj;
        // Compare based on internal ID, Keycloak ID, and definition.
        // It's crucial for Keycloak to correctly identify roles.
        // If the Keycloak ID is present, that's the primary comparison.
        if (this.role.getKeycloakId() != null && that.role.getKeycloakId() != null) {
            return this.role.getKeycloakId().equals(that.role.getKeycloakId());
        }
        // Fallback to internal ID + distinguishing attributes if Keycloak ID isn't set for both
        return Objects.equals(this.role.getId(), that.role.getId()) &&
                Objects.equals(this.role.getName(), that.role.getName()) &&
                Objects.equals(this.role.getRealmId(), that.role.getRealmId()) &&
                Objects.equals(this.role.getClientId(), that.role.getClientId());
    }
}