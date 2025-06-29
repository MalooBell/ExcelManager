package com.mblm.userservice.dto;

import java.util.Objects;

public class AuditRequest {
    private String username;
    private String action;
    private String details;

    public AuditRequest() {
    }

    public AuditRequest(String username, String action, String details) {
        this.username = username;
        this.action = action;
        this.details = details;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditRequest that = (AuditRequest) o;
        return Objects.equals(username, that.username) &&
                Objects.equals(action, that.action) &&
                Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, action, details);
    }

    @Override
    public String toString() {
        return "AuditRequest{" +
                "username='" + username + '\'' +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String action;
        private String details;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public AuditRequest build() {
            return new AuditRequest(username, action, details);
        }
    }
}