package com.addressbook.service;

import com.addressbook.model.Contact;
import com.addressbook.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service layer that handles business logic for contact operations.
 * Provides validation, error handling, and coordinates with repository.
 */
@Service
public class ContactService {

    @Autowired
    private ContactRepository repository;

    /**
     * Create multiple contacts from provided data.
     */
    public List<Contact> createContacts(List<Map<String, Object>> contactDataList) {
        if (contactDataList == null || contactDataList.isEmpty()) {
            throw new IllegalArgumentException("Contact data list cannot be empty");
        }

        List<Contact> createdContacts = new ArrayList<>();

        for (Map<String, Object> contactData : contactDataList) {
            // Validate required fields
            validateContactData(contactData, true);

            // Create contact object
            Contact contact = new Contact(
                    (String) contactData.get("name"),
                    (String) contactData.get("phone"),
                    (String) contactData.get("email"));

            try {
                // Store contact
                Contact savedContact = repository.save(contact);
                createdContacts.add(savedContact);
            } catch (IllegalArgumentException e) {
                // Handle duplicate ID (very unlikely with UUID4)
                throw new IllegalArgumentException("Failed to create contact: " + e.getMessage());
            }
        }

        return createdContacts;
    }

    /**
     * Update multiple contacts with provided data.
     */
    public List<Contact> updateContacts(List<Map<String, Object>> updateDataList) {
        if (updateDataList == null || updateDataList.isEmpty()) {
            throw new IllegalArgumentException("Update data list cannot be empty");
        }

        List<Contact> updatedContacts = new ArrayList<>();

        for (Map<String, Object> updateData : updateDataList) {
            // Validate required ID field
            if (!updateData.containsKey("id")) {
                throw new IllegalArgumentException("Contact ID is required for updates");
            }

            String contactId = (String) updateData.get("id");

            // Validate ID format
            validateUUID(contactId);

            // Extract update fields (exclude ID)
            Map<String, Object> updateFields = new HashMap<>(updateData);
            updateFields.remove("id");

            if (updateFields.isEmpty()) {
                throw new IllegalArgumentException("No update fields provided for contact " + contactId);
            }

            // Validate update fields
            validateContactData(updateFields, false);

            // Update contact
            Contact updatedContact = repository.update(contactId, updateFields);

            if (updatedContact == null) {
                throw new IllegalArgumentException("Contact with ID " + contactId + " not found");
            }

            updatedContacts.add(updatedContact);
        }

        return updatedContacts;
    }

    /**
     * Delete multiple contacts by their IDs.
     */
    public Map<String, Integer> deleteContacts(List<String> contactIds) {
        if (contactIds == null || contactIds.isEmpty()) {
            throw new IllegalArgumentException("Contact IDs list cannot be empty");
        }

        int deletedCount = 0;

        for (String contactId : contactIds) {
            if (contactId == null) {
                throw new IllegalArgumentException("Contact ID cannot be null");
            }

            // Validate ID format
            validateUUID(contactId);

            // Delete contact (returns true if deleted, false if not found)
            if (repository.deleteById(contactId)) {
                deletedCount++;
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("deleted", deletedCount);
        return result;
    }

    /**
     * Search contacts based on query.
     */
    public List<Contact> searchContacts(Map<String, Object> queryData) {
        if (queryData == null || !queryData.containsKey("query")) {
            throw new IllegalArgumentException("Search query is required");
        }

        Object queryObj = queryData.get("query");
        if (!(queryObj instanceof String)) {
            throw new IllegalArgumentException("Search query must be a string");
        }

        String query = (String) queryObj;

        // Search contacts
        return repository.search(query);
    }

    /**
     * Get a single contact by ID.
     */
    public Optional<Contact> getContactById(String contactId) {
        validateUUID(contactId);
        return repository.findById(contactId);
    }

    /**
     * Get all contacts.
     */
    public List<Contact> getAllContacts() {
        return repository.findAll();
    }

    /**
     * Get storage statistics for monitoring.
     */
    public Map<String, Object> getStorageStats() {
        return repository.getStorageStats();
    }

    /**
     * Clear all contacts from storage (for testing).
     */
    public void clearAllContacts() {
        repository.clear();
    }

    /**
     * Validate contact data fields.
     */
    private void validateContactData(Map<String, Object> contactData, boolean requireAll) {
        if (contactData == null) {
            throw new IllegalArgumentException("Contact data cannot be null");
        }

        String[] fields = { "name", "phone", "email" };
        Set<String> allowedFields = new HashSet<>(Arrays.asList(fields));
        allowedFields.add("id");

        // Check for unexpected fields
        Set<String> providedFields = contactData.keySet();
        Set<String> unexpectedFields = new HashSet<>(providedFields);
        unexpectedFields.removeAll(allowedFields);

        if (!unexpectedFields.isEmpty()) {
            throw new IllegalArgumentException("Unexpected fields: " + unexpectedFields);
        }

        // Validate required/optional fields
        for (String field : fields) {
            Object value = contactData.get(field);

            if (requireAll) {
                if (value == null) {
                    throw new IllegalArgumentException("Required field '" + field + "' is missing");
                }
            }

            if (value != null) {
                if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                    throw new IllegalArgumentException("Field '" + field + "' must be a non-empty string");
                }
            }
        }
    }

    /**
     * Validate UUID format.
     */
    private void validateUUID(String uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuid);
        }
    }
}