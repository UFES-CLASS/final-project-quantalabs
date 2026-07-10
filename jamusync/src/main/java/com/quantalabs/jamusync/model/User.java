package com.quantalabs.jamusync.model;

// A User IS-A Person, and a Person IS-A BaseModel. So by extending Person,
// User inherits fullName from Person AND id/createdAt from BaseModel.
public class User extends Person {
    private String username;
    private String passwordHash;
    private String role; // 'owner' or 'staff'
    private boolean isActive;

    public User() {}

    public User(int id, String username, String passwordHash, String role, boolean isActive, String createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // getId()/setId() come from BaseModel; getFullName()/setFullName() come
    // from Person; getCreatedAt()/setCreatedAt() come from BaseModel. None of
    // them need to be written again here.

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Required by BaseModel: a short description of this user.
    @Override
    public String getSummary() {
        return username + " (" + role + ")";
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
