package org.okapi.web.service.pendingjob;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.data.dao.PendingJobsDao;
import org.okapi.data.dto.JOB_STATUS;
import org.okapi.data.dto.PendingJobDdb;
import org.okapi.web.spring.config.FederationAgentCfg;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DbFetchAssigner implements PendingJobAssigner {

  PendingJobsDao pendingJobsDao;
  FederationAgentCfg agentCfg;

  @Override
  public List<PendingJobDdb> getPendingJobs(String orgId, List<String> sources, int maxJobs) {
    var results = new java.util.ArrayList<PendingJobDdb>();
    for (var source : sources) {
      var pending =
          pendingJobsDao.getJobsBySourceAndStatus(
              orgId, source, JOB_STATUS.PENDING, agentCfg.getMaxJobsPerDispatch());
      results.addAll(pending);
    }
    return results.size() > maxJobs ? results.subList(0, maxJobs) : results;
  }
}
