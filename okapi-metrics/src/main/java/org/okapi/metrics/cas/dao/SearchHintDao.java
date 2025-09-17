package org.okapi.metrics.cas.dao;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import org.okapi.metrics.cas.dto.SearchHints;

@Dao
public interface SearchHintDao {
    @Insert
    void insert(SearchHints searchHints);
}
