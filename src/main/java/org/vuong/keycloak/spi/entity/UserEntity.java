package org.vuong.keycloak.spi.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity(name = "users")
@Table(name = "users")
public class UserEntity {

    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    private Boolean locked;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    // Getters and setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public Boolean getLocked() { return locked; }

    public void setLocked(Boolean locked) { this.locked = locked; }

    public Date getCreatedAt() { return createdAt; }

    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}