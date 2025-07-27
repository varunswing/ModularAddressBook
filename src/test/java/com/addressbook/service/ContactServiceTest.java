package com.addressbook.service;

import com.addressbook.model.Contact;
import com.addressbook.repository.ContactRepository;
import com.addressbook.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Comprehensive test cases for ContactService business logic layer.
 */
public class ContactServiceTest {

    private ContactService contactService;
    private ContactRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ContactRepository();
        contactService = new ContactService();
        // Use reflection to set the repository (since it's @Autowired in real
        // implementation)
        try {
            var field = ContactService.class.getDeclaredField("repository");
            field.setAccessible(true);
            field.set(contactService, repository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repository", e);
        }
    }

    @Test
    @DisplayName("Create contacts with valid data should succeed")
    void testCreateContactsSuccess() {
        List<Map<String, Object>> contactData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"),
                createContactMap("Bob Jones", "2345678901", "bob@example.com"));

        List<Contact> created = contactService.createContacts(contactData);

        assertEquals(2, created.size());
        assertNotNull(created.get(0).getId());
        assertNotNull(created.get(1).getId());
        assertEquals("Alice Smith", created.get(0).getName());
        assertEquals("Bob Jones", created.get(1).getName());
    }

    @Test
    @DisplayName("Create contacts with null data should throw exception")
    void testCreateContactsNullData() {
        assertThrows(IllegalArgumentException.class, () -> contactService.createContacts(null));
    }

    @Test
    @DisplayName("Create contacts with empty list should throw exception")
    void testCreateContactsEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> contactService.createContacts(new ArrayList<>()));
    }

    @Test
    @DisplayName("Create contacts with missing required fields should throw exception")
    void testCreateContactsMissingFields() {
        List<Map<String, Object>> invalidData = Arrays.asList(
                createContactMap("Alice Smith", null, "alice@example.com") // Missing phone
        );

        assertThrows(IllegalArgumentException.class, () -> contactService.createContacts(invalidData));
    }

    @Test
    @DisplayName("Create contacts with empty fields should throw exception")
    void testCreateContactsEmptyFields() {
        List<Map<String, Object>> invalidData = Arrays.asList(
                createContactMap("", "1234567890", "alice@example.com") // Empty name
        );

        assertThrows(IllegalArgumentException.class, () -> contactService.createContacts(invalidData));
    }

    @Test
    @DisplayName("Create contacts with unexpected fields should throw exception")
    void testCreateContactsUnexpectedFields() {
        Map<String, Object> invalidContact = new HashMap<>();
        invalidContact.put("name", "Alice Smith");
        invalidContact.put("phone", "1234567890");
        invalidContact.put("email", "alice@example.com");
        invalidContact.put("address", "123 Main St"); // Unexpected field

        assertThrows(IllegalArgumentException.class,
                () -> contactService.createContacts(Arrays.asList(invalidContact)));
    }

    @Test
    @DisplayName("Update contacts with valid data should succeed")
    void testUpdateContactsSuccess() {
        // First create a contact
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"));
        List<Contact> created = contactService.createContacts(createData);
        String contactId = created.get(0).getId();

        // Now update it
        List<Map<String, Object>> updateData = Arrays.asList(
                createUpdateMap(contactId, "phone", "9999999999"));

        List<Contact> updated = contactService.updateContacts(updateData);

        assertEquals(1, updated.size());
        assertEquals("Alice Smith", updated.get(0).getName()); // Name unchanged
        assertEquals("9999999999", updated.get(0).getPhone()); // Phone updated
        assertEquals("alice@example.com", updated.get(0).getEmail()); // Email unchanged
    }

    @Test
    @DisplayName("Update contacts without ID should throw exception")
    void testUpdateContactsMissingId() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "New Name"); // Missing ID

        assertThrows(IllegalArgumentException.class, () -> contactService.updateContacts(Arrays.asList(updateData)));
    }

    @Test
    @DisplayName("Update non-existent contact should throw exception")
    void testUpdateNonExistentContact() {
        List<Map<String, Object>> updateData = Arrays.asList(
                createUpdateMap("non-existent-id", "name", "New Name"));

        assertThrows(IllegalArgumentException.class, () -> contactService.updateContacts(updateData));
    }

    @Test
    @DisplayName("Update contact with invalid UUID should throw exception")
    void testUpdateContactInvalidUuid() {
        List<Map<String, Object>> updateData = Arrays.asList(
                createUpdateMap("invalid-uuid", "name", "New Name"));

        assertThrows(IllegalArgumentException.class, () -> contactService.updateContacts(updateData));
    }

    @Test
    @DisplayName("Update contact with no update fields should throw exception")
    void testUpdateContactNoFields() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("id", UUID.randomUUID().toString());

        assertThrows(IllegalArgumentException.class, () -> contactService.updateContacts(Arrays.asList(updateData)));
    }

    @Test
    @DisplayName("Delete contacts should succeed")
    void testDeleteContactsSuccess() {
        // Create contacts first
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"),
                createContactMap("Bob Jones", "2345678901", "bob@example.com"));
        List<Contact> created = contactService.createContacts(createData);

        // Delete them
        List<String> idsToDelete = Arrays.asList(
                created.get(0).getId(),
                created.get(1).getId());

        Map<String, Integer> result = contactService.deleteContacts(idsToDelete);

        assertEquals(2, result.get("deleted"));
        assertEquals(0, contactService.getAllContacts().size());
    }

    @Test
    @DisplayName("Delete non-existent contacts should return zero count")
    void testDeleteNonExistentContacts() {
        List<String> idsToDelete = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        Map<String, Integer> result = contactService.deleteContacts(idsToDelete);

        assertEquals(0, result.get("deleted"));
    }

    @Test
    @DisplayName("Delete with null ID should throw exception")
    void testDeleteContactsNullId() {
        List<String> idsToDelete = Arrays.asList((String) null);

        assertThrows(IllegalArgumentException.class, () -> contactService.deleteContacts(idsToDelete));
    }

    @Test
    @DisplayName("Delete with invalid UUID should throw exception")
    void testDeleteContactsInvalidUuid() {
        List<String> idsToDelete = Arrays.asList("invalid-uuid");

        assertThrows(IllegalArgumentException.class, () -> contactService.deleteContacts(idsToDelete));
    }

    @Test
    @DisplayName("Search contacts should return correct results")
    void testSearchContactsSuccess() {
        // Create test data
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"),
                createContactMap("Bob Jones", "2345678901", "bob@example.com"),
                createContactMap("Charlie Smith", "3456789012", "charlie@example.com"));
        contactService.createContacts(createData);

        // Search for "Smith"
        Map<String, Object> queryData = new HashMap<>();
        queryData.put("query", "Smith");

        List<Contact> results = contactService.searchContacts(queryData);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Alice Smith")));
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Charlie Smith")));
    }

    @Test
    @DisplayName("Search without query should throw exception")
    void testSearchContactsMissingQuery() {
        Map<String, Object> queryData = new HashMap<>(); // Missing query

        assertThrows(IllegalArgumentException.class, () -> contactService.searchContacts(queryData));
    }

    @Test
    @DisplayName("Search with null query data should throw exception")
    void testSearchContactsNullData() {
        assertThrows(IllegalArgumentException.class, () -> contactService.searchContacts(null));
    }

    @Test
    @DisplayName("Search with non-string query should throw exception")
    void testSearchContactsNonStringQuery() {
        Map<String, Object> queryData = new HashMap<>();
        queryData.put("query", 123); // Non-string query

        assertThrows(IllegalArgumentException.class, () -> contactService.searchContacts(queryData));
    }

    @Test
    @DisplayName("Get contact by ID should work")
    void testGetContactById() {
        // Create a contact
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"));
        List<Contact> created = contactService.createContacts(createData);
        String contactId = created.get(0).getId();

        Optional<Contact> found = contactService.getContactById(contactId);

        assertTrue(found.isPresent());
        assertEquals("Alice Smith", found.get().getName());
    }

    @Test
    @DisplayName("Get contact by invalid ID should throw exception")
    void testGetContactByInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> contactService.getContactById("invalid-uuid"));
    }

    @Test
    @DisplayName("Get all contacts should return all contacts")
    void testGetAllContacts() {
        // Create test data
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"),
                createContactMap("Bob Jones", "2345678901", "bob@example.com"));
        contactService.createContacts(createData);

        List<Contact> all = contactService.getAllContacts();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("Clear all contacts should work")
    void testClearAllContacts() {
        // Create test data
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"));
        contactService.createContacts(createData);

        assertEquals(1, contactService.getAllContacts().size());

        contactService.clearAllContacts();

        assertEquals(0, contactService.getAllContacts().size());
    }

    @Test
    @DisplayName("Get storage stats should return metrics")
    void testGetStorageStats() {
        // Create test data
        List<Map<String, Object>> createData = Arrays.asList(
                createContactMap("Alice Smith", "1234567890", "alice@example.com"));
        contactService.createContacts(createData);

        Map<String, Object> stats = contactService.getStorageStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("contact_count"));
        assertEquals(1, stats.get("contact_count"));
    }

    // Helper methods
    private Map<String, Object> createContactMap(String name, String phone, String email) {
        Map<String, Object> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("phone", phone);
        contact.put("email", email);
        return contact;
    }

    private Map<String, Object> createUpdateMap(String id, String field, String value) {
        Map<String, Object> update = new HashMap<>();
        update.put("id", id);
        update.put(field, value);
        return update;
    }
}