package lk.avengers.datamigrationadapter.mapper;

import lk.avengers.datamigrationadapter.dto.MigrPolicyDataDTO;

import java.time.LocalDate;

@FunctionalInterface
public interface PolicyMapper<T> {
    void map(MigrPolicyDataDTO dto, T entity, String policyNo, LocalDate premiumDueDate);
}
