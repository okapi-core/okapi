package org.okapi.web.investigation.ctx.finders;

import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LogPath {
  String path;
  TreeMap<String, String> tags;
}
