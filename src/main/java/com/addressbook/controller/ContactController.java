package com.addressbook.controller;

import com.addressbook.model.Contact;
import com.addressbook.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for contact operations.
 * Handles HTTP requests and responses according to the exact API contract.
 */
@RestController
public class ContactController {

    @Autowired
    private ContactService contactService;

    /**
     * Handle POST /create endpoint.
     * Creates multiple contacts from JSON request body.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createContacts(@RequestBody List<Map<String, Object>> contactDataList) {
        try {
            List<Contact> createdContacts = contactService.createContacts(contactDataList);

            // Return created contacts with 201 status
            return ResponseEntity.status(HttpStatus.CREATED).body(createdContacts);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle PUT /update endpoint.
     * Updates multiple contacts from JSON request body.
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateContacts(@RequestBody List<Map<String, Object>> updateDataList) {
        try {
            List<Contact> updatedContacts = contactService.updateContacts(updateDataList);

            // Return updated contacts with 200 status
            return ResponseEntity.ok(updatedContacts);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle DELETE /delete endpoint.
     * Deletes multiple contacts by IDs from JSON request body.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteContacts(@RequestBody List<String> contactIds) {
        try {
            Map<String, Integer> result = contactService.deleteContacts(contactIds);

            // Return deletion count with 200 status
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle POST /search endpoint.
     * Searches contacts based on query from JSON request body.
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchContacts(@RequestBody Map<String, Object> queryData) {
        try {
            List<Contact> matchingContacts = contactService.searchContacts(queryData);

            // Return matching contacts with 200 status
            return ResponseEntity.ok(matchingContacts);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Optional Helper Endpoints (not part of the main contract)

    /**
     * Handle GET /contact/{id} endpoint.
     * Gets a single contact by ID.
     */
    @GetMapping("/contact/{id}")
    public ResponseEntity<?> getContactById(@PathVariable String id) {
        try {
            Optional<Contact> contact = contactService.getContactById(id);

            if (contact.isPresent()) {
                return ResponseEntity.ok(contact.get());
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Contact with ID " + id + " not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle GET /contacts endpoint.
     * Gets all contacts.
     */
    @GetMapping("/contacts")
    public ResponseEntity<?> getAllContacts() {
        try {
            List<Contact> contacts = contactService.getAllContacts();
            return ResponseEntity.ok(contacts);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle GET /stats endpoint.
     * Gets storage statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStorageStats() {
        try {
            Map<String, Object> stats = contactService.getStorageStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle GET /health endpoint.
     * Simple health check.
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "address-book");
        return ResponseEntity.ok(response);
    }
}