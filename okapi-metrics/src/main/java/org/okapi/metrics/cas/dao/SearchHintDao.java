package org.okapi.metrics.cas.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import org.okapi.metrics.cas.dto.SearchHints;

@Dao
public interface SearchHintDao {
    @Insert
    void insert(SearchHints searchHints);

    @Select(customWhereClause = "tenant_id = :tenantId AND shard_key = :shardKey AND start_minute >= :start AND start_minute <= :end")
    PagingIterable<SearchHints> scan(String tenantId, int shardKey, long start, long end);
}
