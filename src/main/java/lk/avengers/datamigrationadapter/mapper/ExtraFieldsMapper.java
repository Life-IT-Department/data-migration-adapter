package lk.avengers.datamigrationadapter.mapper;

import lk.avengers.datamigrationadapter.entity.softlogicdb.ExtraFields;

@FunctionalInterface
public interface ExtraFieldsMapper<T> {
    void map(ExtraFields extra, T entity, String policyNo);
}