package org.okapi.data.ddb.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TagsList {

  public TagsList(List<String> tags) {
    this.tags = tags;
  }

  public TagsList() {
    this(new ArrayList<>());
  }

  List<String> tags;

  public void addTag(String tag) {
    tags.add(tag);
  }

  public void removeTag(String tag) {
    tags.remove(tag);
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<String> asList() {
    return Collections.unmodifiableList(tags);
  }

  public static TagsList of(List<String> tags) {
    return new TagsList(tags);
  }

  public static TagsList of(String... tags) {
    TagsList tl = new TagsList();
    for (String tag : tags) {
      tl.addTag(tag);
    }
    return tl;
  }
}
