package org.okapi.resources;

import jakarta.validation.Valid;
import org.okapi.rest.search.SearchResourcesRequest;
import org.okapi.rest.search.SearchResourcesResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceSearchController {

  ChResourceSearchService service;

  @PostMapping("/query")
  public SearchResourcesResponse query(@RequestBody @Valid SearchResourcesRequest searchRequest) {
    return service.search(searchRequest);
  }
}
