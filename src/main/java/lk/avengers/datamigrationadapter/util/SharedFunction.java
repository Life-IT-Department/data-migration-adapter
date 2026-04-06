package lk.avengers.datamigrationadapter.util;

import org.springframework.stereotype.Service;

@Service
public class SharedFunction {

    public boolean isEligiblePolicyStatus(String status) {

        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase("In Force")
                || "Lapsed".equalsIgnoreCase(status);
    }
}
