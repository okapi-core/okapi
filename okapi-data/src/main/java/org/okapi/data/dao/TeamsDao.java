package org.okapi.data.dao;

import org.okapi.data.dto.TeamDto;
import org.okapi.data.exceptions.TeamNotFoundException;

import java.util.List;
import java.util.Optional;

public interface TeamsDao {

  TeamDto create(TeamDto team);

  Optional<TeamDto> get(String teamId);

  List<TeamDto> listByOrgId(String orgId);

  void delete(String teamId);

  TeamDto update(String teamId, String teamName, String description) throws TeamNotFoundException;
}
