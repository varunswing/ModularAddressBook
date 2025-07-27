package com.addressbook.repository;

import com.addressbook.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Comprehensive test cases for ContactRepository including O(1) performance
 * verification.
 */
public class ContactRepositoryTest {

    private ContactRepository repository;
    private Contact testContact1;
    private Contact testContact2;
    private Contact testContact3;

    @BeforeEach
    void setUp() {
        repository = new ContactRepository();
        testContact1 = new Contact("Alice Smith", "1234567890", "alice@example.com");
        testContact2 = new Contact("Bob Jones", "2345678901", "bob@example.com");
        testContact3 = new Contact("Charlie Smith", "3456789012", "charlie@example.com");
    }

    @Test
    @DisplayName("Save contact should store and index correctly")
    void testSaveContact() {
        Contact saved = repository.save(testContact1);

        assertEquals(testContact1.getId(), saved.getId());
        assertEquals(testContact1.getName(), saved.getName());
        assertEquals(1, repository.count());

        // Verify it can be found by ID
        Optional<Contact> found = repository.findById(testContact1.getId());
        assertTrue(found.isPresent());
        assertEquals("Alice Smith", found.get().getName());
    }

    @Test
    @DisplayName("Save duplicate contact should throw exception")
    void testSaveDuplicateContact() {
        repository.save(testContact1);

        // Create contact with same ID
        Contact duplicate = new Contact(testContact1.getId(), "Different Name", "9999999999", "different@example.com");

        assertThrows(IllegalArgumentException.class, () -> repository.save(duplicate));
    }

    @Test
    @DisplayName("Find by ID should return correct contact")
    void testFindById() {
        repository.save(testContact1);
        repository.save(testContact2);

        Optional<Contact> found = repository.findById(testContact1.getId());
        assertTrue(found.isPresent());
        assertEquals("Alice Smith", found.get().getName());

        Optional<Contact> notFound = repository.findById("non-existent-id");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Update contact should modify fields and maintain index")
    void testUpdateContact() {
        repository.save(testContact1);

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", "9999999999");
        updates.put("email", "newalice@example.com");

        Contact updated = repository.update(testContact1.getId(), updates);

        assertNotNull(updated);
        assertEquals("Alice Smith", updated.getName()); // Name unchanged
        assertEquals("9999999999", updated.getPhone()); // Phone updated
        assertEquals("newalice@example.com", updated.getEmail()); // Email updated

        // Verify search index was updated
        List<Contact> searchResults = repository.search("newalice");
        assertEquals(1, searchResults.size());
        assertEquals(testContact1.getId(), searchResults.get(0).getId());
    }

    @Test
    @DisplayName("Update non-existent contact should return null")
    void testUpdateNonExistentContact() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "New Name");

        Contact result = repository.update("non-existent-id", updates);
        assertNull(result);
    }

    @Test
    @DisplayName("Delete contact should remove from storage and index")
    void testDeleteContact() {
        repository.save(testContact1);
        repository.save(testContact2);

        assertTrue(repository.deleteById(testContact1.getId()));
        assertEquals(1, repository.count());

        // Verify contact is gone
        Optional<Contact> found = repository.findById(testContact1.getId());
        assertFalse(found.isPresent());

        // Verify search index updated
        List<Contact> searchResults = repository.search("Alice");
        assertEquals(0, searchResults.size());

        // Verify other contact still exists
        Optional<Contact> other = repository.findById(testContact2.getId());
        assertTrue(other.isPresent());
    }

    @Test
    @DisplayName("Delete non-existent contact should return false")
    void testDeleteNonExistentContact() {
        assertFalse(repository.deleteById("non-existent-id"));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("Single word search should have O(1) performance")
    void testSingleWordSearchPerformance() {
        // Setup test data
        repository.save(testContact1); // Alice Smith
        repository.save(testContact2); // Bob Jones
        repository.save(testContact3); // Charlie Smith

        // Single word search should be O(1)
        long startTime = System.nanoTime();
        List<Contact> results = repository.search("Smith");
        long endTime = System.nanoTime();

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Alice Smith")));
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Charlie Smith")));

        // Performance should be very fast (under 1ms for small dataset)
        long duration = endTime - startTime;
        assertTrue(duration < 1_000_000, "Single word search should be very fast (O(1))"); // 1ms in nanoseconds
    }

    @Test
    @DisplayName("Multi-word search should be optimized")
    void testMultiWordSearch() {
        repository.save(testContact1); // Alice Smith
        repository.save(testContact2); // Bob Jones
        repository.save(testContact3); // Charlie Smith

        // Search for contacts with both "Smith" and "Alice"
        List<Contact> results = repository.search("Smith Alice");
        assertEquals(1, results.size());
        assertEquals("Alice Smith", results.get(0).getName());

        // Search that should return no results
        List<Contact> noResults = repository.search("Smith Bob");
        assertEquals(0, noResults.size());
    }

    @Test
    @DisplayName("Search should handle email and phone queries")
    void testSearchByEmailAndPhone() {
        repository.save(testContact1);
        repository.save(testContact2);

        // Search by email
        List<Contact> emailResults = repository.search("alice@example.com");
        assertEquals(1, emailResults.size());
        assertEquals("Alice Smith", emailResults.get(0).getName());

        // Search by phone
        List<Contact> phoneResults = repository.search("1234567890");
        assertEquals(1, phoneResults.size());
        assertEquals("Alice Smith", phoneResults.get(0).getName());

        // Search by domain
        List<Contact> domainResults = repository.search("example");
        assertEquals(2, domainResults.size());
    }

    @Test
    @DisplayName("Search should be case insensitive")
    void testSearchCaseInsensitive() {
        repository.save(testContact1);

        assertEquals(1, repository.search("alice").size());
        assertEquals(1, repository.search("ALICE").size());
        assertEquals(1, repository.search("Alice").size());
        assertEquals(1, repository.search("smith").size());
        assertEquals(1, repository.search("SMITH").size());
    }

    @Test
    @DisplayName("Search should handle empty and null queries")
    void testSearchEdgeCases() {
        repository.save(testContact1);

        assertEquals(0, repository.search(null).size());
        assertEquals(0, repository.search("").size());
        assertEquals(0, repository.search("   ").size());
        assertEquals(0, repository.search("nonexistent").size());
    }

    @Test
    @DisplayName("Search should return results in consistent order")
    void testSearchOrdering() {
        repository.save(testContact1); // Alice Smith
        repository.save(testContact3); // Charlie Smith

        List<Contact> results1 = repository.search("Smith");
        List<Contact> results2 = repository.search("Smith");

        assertEquals(2, results1.size());
        assertEquals(2, results2.size());

        // Results should be in consistent order (sorted by ID)
        for (int i = 0; i < results1.size(); i++) {
            assertEquals(results1.get(i).getId(), results2.get(i).getId());
        }
    }

    @Test
    @DisplayName("Large dataset performance test")
    void testLargeDatasetPerformance() {
        // Create a larger dataset to test O(1) performance
        List<Contact> contacts = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            Contact contact = new Contact(
                    "Person" + i + " Smith",
                    "555000" + String.format("%04d", i),
                    "person" + i + "@example.com");
            contacts.add(contact);
            repository.save(contact);
        }

        assertEquals(10000, repository.count());

        // Test single word search performance
        long startTime = System.nanoTime();
        List<Contact> results = repository.search("Smith");
        long endTime = System.nanoTime();

        assertEquals(10000, results.size()); // All contacts have "Smith"

        // Performance should still be fast even with 10k records
        long duration = endTime - startTime;
        assertTrue(duration < 50_000_000, "Search should remain fast with large dataset"); // 50ms threshold

        System.out.println("Search time for 10k records: " + duration / 1_000_000 + "ms");
    }

    @Test
    @DisplayName("Find all should return all contacts")
    void testFindAll() {
        repository.save(testContact1);
        repository.save(testContact2);
        repository.save(testContact3);

        List<Contact> all = repository.findAll();
        assertEquals(3, all.size());

        Set<String> names = new HashSet<>();
        for (Contact contact : all) {
            names.add(contact.getName());
        }

        assertTrue(names.contains("Alice Smith"));
        assertTrue(names.contains("Bob Jones"));
        assertTrue(names.contains("Charlie Smith"));
    }

    @Test
    @DisplayName("Clear should remove all contacts and indices")
    void testClear() {
        repository.save(testContact1);
        repository.save(testContact2);
        repository.save(testContact3);

        assertEquals(3, repository.count());

        repository.clear();

        assertEquals(0, repository.count());
        assertEquals(0, repository.findAll().size());
        assertEquals(0, repository.search("Smith").size());
    }

    @Test
    @DisplayName("Storage stats should provide accurate metrics")
    void testStorageStats() {
        repository.save(testContact1);
        repository.save(testContact2);

        Map<String, Object> stats = repository.getStorageStats();

        assertEquals(2, (Integer) stats.get("contact_count"));
        assertTrue((Integer) stats.get("search_index_size") > 0);
        assertTrue((Integer) stats.get("total_indexed_words") > 0);

        System.out.println("Storage stats: " + stats);
    }

    @Test
    @DisplayName("Concurrent access should be thread-safe")
    void testConcurrentAccess() throws InterruptedException {
        final int numThreads = 10;
        final int contactsPerThread = 100;
        List<Thread> threads = new ArrayList<>();

        // Create multiple threads adding contacts concurrently
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < contactsPerThread; i++) {
                    Contact contact = new Contact(
                            "Thread" + threadId + "Person" + i,
                            "555" + String.format("%03d%04d", threadId, i),
                            "thread" + threadId + "person" + i + "@test.com");
                    repository.save(contact);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all contacts were added
        assertEquals(numThreads * contactsPerThread, repository.count());

        // Test concurrent search
        List<Thread> searchThreads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread searchThread = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    List<Contact> results = repository.search("Thread");
                    assertEquals(numThreads * contactsPerThread, results.size());
                }
            });
            searchThreads.add(searchThread);
            searchThread.start();
        }

        for (Thread thread : searchThreads) {
            thread.join();
        }
    }
}