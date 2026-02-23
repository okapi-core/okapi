package org.okapi.web.investigation.ctx.finders;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConfigPath {
  CONFIG_STORE store;
  String path;
}
