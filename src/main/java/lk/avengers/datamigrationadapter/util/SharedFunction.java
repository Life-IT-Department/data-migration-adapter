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
//    private static final String AMENDED = "Amended";
    private static final String IN_FORCE_PAID_UP = "In Force / Paid Up";

    public boolean isEligiblePolicyStatus(String status) {

        if (!filterByStatus) {
            return true;
        }

        return (IN_FORCE.equalsIgnoreCase(status) ||
                LAPSED.equalsIgnoreCase(status) ||
//                AMENDED.equalsIgnoreCase(status) ||
                IN_FORCE_PAID_UP.equalsIgnoreCase(status));
    }

    public PolicyNumberResponseDTO extractPolicyNumber(String policyRef) {

        if (policyRef == null || policyRef.isBlank()) {
            throw new IllegalArgumentException("Policy reference cannot be null or empty");
        }

        policyRef = policyRef.trim();

        String productCode;
        int policyNo;

        // Case 1: Format with slash(es)
        if (policyRef.contains("/")) {

            String[] parts = policyRef.split("/");

            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
            }

            productCode = parts[0].trim();

            // Join all remaining parts and remove any spaces
            StringBuilder numberBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                numberBuilder.append(parts[i].trim());
            }

            try {
                policyNo = Integer.parseInt(numberBuilder.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid policy number: " + numberBuilder, e);
            }

        }
        // Case 2: Format without slash (ULF527879)
        else {

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
