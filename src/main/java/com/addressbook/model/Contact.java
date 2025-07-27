package com.addressbook.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import java.util.UUID;

/**
 * Contact model representing a single contact in the address book.
 * Maintains field order as per API contract: id, name, phone, email
 */
@JsonPropertyOrder({ "id", "name", "phone", "email" })
public class Contact {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    // Default constructor for Jackson
    public Contact() {
    }

    // Constructor for creating new contacts (generates UUID)
    public Contact(String name, String phone, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    // Constructor with ID (for updates or existing contacts)
    public Contact(String id, String name, String phone, String email) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    /**
     * Update contact fields with provided values.
     */
    public void update(Map<String, Object> updates) {
        if (updates.containsKey("name")) {
            this.name = (String) updates.get("name");
        }
        if (updates.containsKey("phone")) {
            this.phone = (String) updates.get("phone");
        }
        if (updates.containsKey("email")) {
            this.email = (String) updates.get("email");
        }
    }

    /**
     * Get all searchable text from the contact for indexing.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getSearchableText() {
        return (name + " " + phone + " " + email).toLowerCase();
    }

    /**
     * Check if contact matches the search query.
     */
    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        return getSearchableText().contains(query.toLowerCase());
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return String.format("Contact{id='%s', name='%s', phone='%s', email='%s'}",
                id, name, phone, email);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Contact contact = (Contact) obj;
        return id != null ? id.equals(contact.id) : contact.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}