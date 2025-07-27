package com.addressbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Modular Address Book service.
 * Runs on port 5000 as required by the specifications.
 */
@SpringBootApplication
public class ModularAddressBookApplication {

    public static void main(String[] args) {
        System.out.println("Starting Modular Address Book Service...");
        System.out.println("API Contract Endpoints:");
        System.out.println("  POST   /create  - Create contacts");
        System.out.println("  PUT    /update  - Update contacts");
        System.out.println("  DELETE /delete  - Delete contacts");
        System.out.println("  POST   /search  - Search contacts");
        System.out.println("\nOptional Helper Endpoints:");
        System.out.println("  GET    /health  - Health check");
        System.out.println("  GET    /stats   - Storage statistics");
        System.out.println("  GET    /contacts - Get all contacts");
        System.out.println("  GET    /contact/{id} - Get contact by ID");
        System.out.println("\nService running on http://localhost:5000");
        System.out.println("Ready to handle requests!\n");

        SpringApplication.run(ModularAddressBookApplication.class, args);
    }
}