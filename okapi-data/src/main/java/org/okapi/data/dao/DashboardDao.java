package org.okapi.data.dao;

import org.okapi.data.dto.DashboardDto;
import org.okapi.data.exceptions.ResourceNotFoundException;

import java.util.Optional;

public interface DashboardDao {
    Optional<DashboardDto> get(String id);
    void updateNote(String id, String note) throws ResourceNotFoundException;
    DashboardDto save(DashboardDto dto);
    void updateDefinition(String id, String definition) throws ResourceNotFoundException;
    String getDefinition(String id) throws ResourceNotFoundException;
}
