package com.addressbook;

import com.addressbook.model.Contact;
import com.addressbook.repository.ContactRepository;
import com.addressbook.service.ContactService;

import java.util.*;

/**
 * Performance demonstration for O(1) search optimization.
 */
public class PerformanceTest {

    public static void main(String[] args) {
        System.out.println("=== PERFORMANCE TEST: O(1) Search Optimization ===\n");

        ContactRepository repository = new ContactRepository();
        ContactService service = new ContactService();

        // Use reflection to inject repository
        try {
            var field = ContactService.class.getDeclaredField("repository");
            field.setAccessible(true);
            field.set(service, repository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repository", e);
        }

        // Test with increasing dataset sizes
        int[] testSizes = { 1000, 5000, 10000, 50000 };

        for (int size : testSizes) {
            testSearchPerformance(service, size);
        }

        // Demonstrate multi-word search optimization
        testMultiWordSearchOptimization(service);
    }

    private static void testSearchPerformance(ContactService service, int numContacts) {
        System.out.println("--- Testing with " + numContacts + " contacts ---");

        // Clear previous data
        service.clearAllContacts();

        // Create test data
        List<Map<String, Object>> contacts = new ArrayList<>();
        for (int i = 0; i < numContacts; i++) {
            Map<String, Object> contact = new HashMap<>();
            contact.put("name", "Person" + i + " Smith");
            contact.put("phone", "555" + String.format("%07d", i));
            contact.put("email", "person" + i + "@example.com");
            contacts.add(contact);
        }

        // Time contact creation
        long startTime = System.nanoTime();
        service.createContacts(contacts);
        long createTime = System.nanoTime() - startTime;

        System.out.println("  Created " + numContacts + " contacts in: " +
                (createTime / 1_000_000) + "ms");

        // Test single word search (should be true O(1))
        Map<String, Object> searchQuery = new HashMap<>();
        searchQuery.put("query", "Smith");

        // Warm up JVM
        for (int i = 0; i < 10; i++) {
            service.searchContacts(searchQuery);
        }

        // Measure search performance
        startTime = System.nanoTime();
        List<Contact> results = service.searchContacts(searchQuery);
        long searchTime = System.nanoTime() - startTime;

        System.out.println("  Searched " + numContacts + " contacts in: " +
                (searchTime / 1_000_000.0) + "ms");
        System.out.println("  Found " + results.size() + " matches");
        System.out.println("  Search time per contact: " +
                (searchTime / (double) numContacts) + " nanoseconds");

        // Test more specific search
        searchQuery.put("query", "Person1000");
        startTime = System.nanoTime();
        results = service.searchContacts(searchQuery);
        searchTime = System.nanoTime() - startTime;

        System.out.println("  Specific search time: " +
                (searchTime / 1_000_000.0) + "ms (found " + results.size() + ")");

        System.out.println();
    }

    private static void testMultiWordSearchOptimization(ContactService service) {
        System.out.println("--- Multi-word Search Optimization Test ---");

        // Clear and create diverse test data
        service.clearAllContacts();

        List<Map<String, Object>> contacts = new ArrayList<>();

        // Create contacts with different name patterns
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> contact = new HashMap<>();
            if (i < 10) {
                // Few contacts with "Manager" title
                contact.put("name", "Manager Person" + i);
            } else if (i < 100) {
                // More contacts with "Engineer" title
                contact.put("name", "Engineer Person" + i);
            } else {
                // Many contacts with "Developer" title
                contact.put("name", "Developer Person" + i);
            }
            contact.put("phone", "555" + String.format("%07d", i));
            contact.put("email", "person" + i + "@company.com");
            contacts.add(contact);
        }

        service.createContacts(contacts);

        // Test multi-word search with different selectivity
        String[][] testQueries = {
                { "Manager", "Person" }, // Very selective (10 matches)
                { "Engineer", "Person" }, // Moderately selective (90 matches)
                { "Developer", "Person" }, // Less selective (900 matches)
                { "Person", "company" } // Least selective (1000 matches)
        };

        for (String[] queryWords : testQueries) {
            String query = String.join(" ", queryWords);
            Map<String, Object> searchQuery = new HashMap<>();
            searchQuery.put("query", query);

            // Warm up
            for (int i = 0; i < 5; i++) {
                service.searchContacts(searchQuery);
            }

            long startTime = System.nanoTime();
            List<Contact> results = service.searchContacts(searchQuery);
            long searchTime = System.nanoTime() - startTime;

            System.out.println("  Query: \"" + query + "\"");
            System.out.println("    Found " + results.size() + " matches in " +
                    (searchTime / 1_000_000.0) + "ms");
            System.out.println("    Performance: " +
                    (searchTime < 1_000_000 ? "Excellent" : searchTime < 5_000_000 ? "Good" : "Needs optimization"));
        }

        System.out.println();
    }
}