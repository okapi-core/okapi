package org.okapi.web.auth;

import static org.okapi.data.dao.RelationGraphDao.makeRelation;
import static org.okapi.data.ddb.attributes.ENTITY_TYPE.*;
import static org.okapi.data.ddb.attributes.RELATION_TYPE.*;

import java.util.Arrays;
import java.util.List;
import org.okapi.data.ddb.attributes.EdgeSeq;

public class PathWays {

  public static final List<EdgeSeq> DASH_EDIT_PATH_WAY =
      List.of(
          new EdgeSeq(
              Arrays.asList(
                  makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_EDIT))));

  public static final List<EdgeSeq> DASH_READ_PATH_WAY =
      List.of(
          new EdgeSeq(
              Arrays.asList(
                  makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_READ))));
}
