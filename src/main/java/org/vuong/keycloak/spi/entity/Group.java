package org.vuong.keycloak.spi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects; // Import Objects for equals/hashCode
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "groups")
@Table(name = "groups")
public class Group implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private String description;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "parent_path")
    private String parentPath;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Column(name = "realm_id")
    private String realmId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "groups_roles", joinColumns = @JoinColumn(name = "groups_id"), inverseJoinColumns = @JoinColumn(name = "roles_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Fix for "cannot view users in group details" - crucial for Set operations
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        // Equality based on the primary key 'id'
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        // Hash code based on the primary key 'id'
        return Objects.hash(id);
    }
}
