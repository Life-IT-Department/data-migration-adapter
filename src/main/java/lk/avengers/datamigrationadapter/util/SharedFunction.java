package lk.avengers.datamigrationadapter.util;

import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SharedFunction {

    @Value("${filter.by.status}")
    private boolean filterByStatus;

    private static final String IN_FORCE = "In Force";
    private static final String LAPSED = "Lapsed";

    public boolean isEligiblePolicyStatus(String status) {

        if (!filterByStatus) {
            return true;
        }

        return (IN_FORCE.equalsIgnoreCase(status) || LAPSED.equalsIgnoreCase(status));
    }

    public PolicyNumberResponseDTO extractPolicyNumber(String policyRef) {

        if (policyRef == null || policyRef.isBlank()) {
            throw new IllegalArgumentException("Policy reference cannot be null or empty");
        }

        policyRef = policyRef.trim();

        String productCode;
        int policyNo;

        // Case 1: Format with slash (ASP/1082429)
        if (policyRef.contains("/")) {

            String[] parts = policyRef.split("/");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
            }

            productCode = parts[0].trim();

            try {
                policyNo = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid policy number: " + parts[1], e);
            }

        }
        // Case 2: Format without slash (ULF527879)
        else {

            // Expect: 3 letters + digits
            if (!policyRef.matches("[A-Z]{3}\\d+")) {
                throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
            }

            productCode = policyRef.substring(0, 3);

            try {
                policyNo = Integer.parseInt(policyRef.substring(3));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid policy number: " + policyRef, e);
            }
        }

        return new PolicyNumberResponseDTO(productCode, policyNo);
    }
}
