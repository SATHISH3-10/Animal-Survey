package com.example.vilage_wise.models;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String officerBadgeId;
    private String assignedVillage;
    private String designation;
    private String role;   // "ADMIN" or "OFFICER"
    private String status; // "PENDING", "APPROVED", "REJECTED"
    private String password;
    private long createdAt;
    private long approvedAt;
    private String approvedBy;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String fullName, String email, String phone, 
                String officerBadgeId, String assignedVillage, String designation, 
                String role, String status, long createdAt, long approvedAt, String approvedBy) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.officerBadgeId = officerBadgeId;
        this.assignedVillage = assignedVillage;
        this.designation = designation;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName != null ? fullName : "";
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOfficerBadgeId() {
        return officerBadgeId != null ? officerBadgeId : "";
    }

    public void setOfficerBadgeId(String officerBadgeId) {
        this.officerBadgeId = officerBadgeId;
    }

    public String getAssignedVillage() {
        return assignedVillage != null ? assignedVillage : "";
    }

    public void setAssignedVillage(String assignedVillage) {
        this.assignedVillage = assignedVillage;
    }

    public String getDesignation() {
        return designation != null ? designation : "Village Survey Officer";
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getRole() {
        return role != null ? role : "OFFICER";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(long approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy != null ? approvedBy : "";
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
