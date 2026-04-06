package lk.avengers.datamigrationadapter.mapper;

import lk.avengers.datamigrationadapter.entity.softlogicdb.FundCurrentBalanceEntity;

@FunctionalInterface
public interface FundCurrentBalanceMapper<T> {
    void map(FundCurrentBalanceEntity fund, T entity);
}
