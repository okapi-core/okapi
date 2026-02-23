/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import lombok.AllArgsConstructor;
import org.okapi.data.dao.*;
import org.okapi.data.ddb.dao.*;

@AllArgsConstructor
public class DaoModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(PendingJobsDao.class).to(PendingJobsDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(DashboardDao.class).to(DashboardDaoImpl.class).in(Scopes.SINGLETON);
    bind(DashboardPanelDao.class).to(DashboardPanelDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(FederatedSourceRepo.class).to(FederatedSourceRepoImpl.class).in(Scopes.SINGLETON);
    bind(InfraDependencyGraphDao.class).to(InfraDependencyGraphDaoImpl.class).in(Scopes.SINGLETON);
    bind(OrgDao.class).to(OrgDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(PendingJobsDao.class).to(PendingJobsDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(RelationGraphDao.class).to(RelationGraphDaoImpl.class).in(Scopes.SINGLETON);
    bind(TokenMetaDao.class).to(TokenMetaDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(UserEntityRelationsDao.class).to(UserEntityRelationsDaoImpl.class).in(Scopes.SINGLETON);
    bind(UsersDao.class).to(UsersDaoImpl.class).in(Scopes.SINGLETON);
    bind(InfraEntityNodeDao.class).to(InfraEntityNodeDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(DashboardRowDao.class).to(DashboardRowDaoDdbImpl.class).in(Scopes.SINGLETON);
    bind(DashboardVersionDao.class).to(DashboardVersionDaoImpl.class).in(Scopes.SINGLETON);
  }
}
