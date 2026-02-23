package org.okapi.web.investigation.ctx.finders;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.TreeMap;

@AllArgsConstructor
@Getter
public class TracePath {
    String path;
    TreeMap<String, String> tags;
}
