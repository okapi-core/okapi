package org.okapi.data.dao;

import org.okapi.data.dto.TeamMemberDto;

import java.util.List;

public interface TeamMemberDao {

  void addMember(String teamId, String userId);

  void removeMember(String teamId, String userId);

  List<TeamMemberDto> listMembers(String teamId);
}
