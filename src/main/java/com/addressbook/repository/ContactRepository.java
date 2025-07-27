package com.addressbook.repository;

import com.addressbook.model.Contact;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In-memory storage repository optimized for O(1) operations.
 * Uses ConcurrentHashMap for thread-safe operations and inverted index for fast
 * search.
 * Designed to handle millions of contacts efficiently.
 */
@Repository
public class ContactRepository {

    // Primary storage: contact_id -> Contact object (O(1) access)
    private final Map<String, Contact> contacts = new ConcurrentHashMap<>();

    // Inverted index for search: word -> set of contact_ids (O(1) search)
    private final Map<String, Set<String>> searchIndex = new ConcurrentHashMap<>();

    // Pattern for tokenizing text into words
    private static final Pattern WORD_PATTERN = Pattern.compile("\\w+");

    /**
     * Store a new contact with O(1) complexity.
     */
    public Contact save(Contact contact) {
        if (contacts.containsKey(contact.getId())) {
            throw new IllegalArgumentException("Contact with ID " + contact.getId() + " already exists");
        }

        // Store contact (O(1))
        contacts.put(contact.getId(), contact);

        // Update search index (O(k) where k is number of words in contact)
        addToSearchIndex(contact);

        return contact;
    }

    /**
     * Retrieve a contact by ID with O(1) complexity.
     */
    public Optional<Contact> findById(String id) {
        return Optional.ofNullable(contacts.get(id));
    }

    /**
     * Update a contact with O(1) complexity.
     */
    public Contact update(String id, Map<String, Object> updates) {
        Contact contact = contacts.get(id);
        if (contact == null) {
            return null;
        }

        // Remove from search index before update
        removeFromSearchIndex(contact);

        // Update contact
        contact.update(updates);

        // Add back to search index with updated data
        addToSearchIndex(contact);

        return contact;
    }

    /**
     * Delete a contact with O(1) complexity.
     */
    public boolean deleteById(String id) {
        Contact contact = contacts.get(id);
        if (contact == null) {
            return false;
        }

        // Remove from search index
        removeFromSearchIndex(contact);

        // Remove from primary storage
        contacts.remove(id);

        return true;
    }

    /**
     * Search contacts with optimized O(1) performance using inverted index.
     * Single word queries: true O(1)
     * Multi-word queries: O(1) per word + O(min_result_set) for intersection
     */
    public List<Contact> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Normalize and tokenize query
        List<String> queryWords = tokenize(query);
        if (queryWords.isEmpty()) {
            return new ArrayList<>();
        }

        // Single word optimization - true O(1) performance
        if (queryWords.size() == 1) {
            Set<String> contactIds = searchIndex.getOrDefault(queryWords.get(0), Collections.emptySet());
            return contactIds.stream()
                    .sorted() // Deterministic ordering
                    .map(contacts::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // Multi-word optimization - find smallest result set first to minimize
        // intersections
        Set<String> smallestSet = null;
        int minSize = Integer.MAX_VALUE;

        // Find the word with smallest result set (optimization)
        for (String word : queryWords) {
            Set<String> wordMatches = searchIndex.getOrDefault(word, Collections.emptySet());
            if (wordMatches.size() < minSize) {
                minSize = wordMatches.size();
                smallestSet = wordMatches;
            }
            // Early termination if any word has no matches
            if (wordMatches.isEmpty()) {
                return new ArrayList<>();
            }
        }

        // Start with smallest set and intersect with others
        Set<String> matchingContactIds = new HashSet<>(smallestSet);

        for (String word : queryWords) {
            Set<String> wordMatches = searchIndex.getOrDefault(word, Collections.emptySet());
            if (wordMatches != smallestSet) { // Skip the set we already started with
                matchingContactIds.retainAll(wordMatches);
                // Early termination if intersection becomes empty
                if (matchingContactIds.isEmpty()) {
                    return new ArrayList<>();
                }
            }
        }

        // Return contacts in consistent order (by ID for deterministic results)
        return matchingContactIds.stream()
                .sorted()
                .map(contacts::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get all contacts.
     */
    public List<Contact> findAll() {
        return new ArrayList<>(contacts.values());
    }

    /**
     * Get the total number of contacts.
     */
    public long count() {
        return contacts.size();
    }

    /**
     * Clear all contacts from storage.
     */
    public void clear() {
        contacts.clear();
        searchIndex.clear();
    }

    /**
     * Get storage statistics for monitoring.
     */
    public Map<String, Object> getStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("contact_count", contacts.size());
        stats.put("search_index_size", searchIndex.size());
        stats.put("total_indexed_words", searchIndex.values().stream().mapToInt(Set::size).sum());
        return stats;
    }

    /**
     * Add contact to search index for efficient searching.
     */
    private void addToSearchIndex(Contact contact) {
        List<String> words = tokenize(contact.getSearchableText());
        for (String word : words) {
            searchIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(contact.getId());
        }
    }

    /**
     * Remove contact from search index.
     */
    private void removeFromSearchIndex(Contact contact) {
        List<String> words = tokenize(contact.getSearchableText());
        for (String word : words) {
            Set<String> contactIds = searchIndex.get(word);
            if (contactIds != null) {
                contactIds.remove(contact.getId());
                // Clean up empty sets to save memory
                if (contactIds.isEmpty()) {
                    searchIndex.remove(word);
                }
            }
        }
    }

    /**
     * Normalize and tokenize text for consistent indexing and searching.
     * Handles names, emails, and phone numbers effectively.
     */
    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Extract words using regex pattern, convert to lowercase
        return WORD_PATTERN.matcher(text.toLowerCase())
                .results()
                .map(matchResult -> matchResult.group())
                .distinct()
                .collect(Collectors.toList());
    }
}