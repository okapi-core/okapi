package org.okapi.web.ai.tools;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.SortedMap;

@AllArgsConstructor
@Getter
public class ResourcePath {
    String pathName;
    SortedMap<String, String> tags;
}
