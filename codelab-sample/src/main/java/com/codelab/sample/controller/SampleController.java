package com.codelab.sample.controller;

import com.codelab.sample.model.CreateSampleRequest;
import com.codelab.sample.model.CreateSampleResponse;
import com.codelab.sample.model.GetSampleResponse;
import com.codelab.sample.service.SampleService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing sample item operations. */
@RestController
@AllArgsConstructor
@RequestMapping("/api/samples")
public class SampleController {

  private final SampleService service;

  /** Creates a sample item. */
  @PostMapping
  public CreateSampleResponse create(@RequestBody CreateSampleRequest request) {
    return service.createSample(request);
  }

  /** Reads a sample item. */
  @GetMapping("/{id}")
  public GetSampleResponse get(@PathVariable("id") Long id) {
    return service.getSample(id);
  }

  /** Enqueues the item for asynchronous processing. */
  @PostMapping("/{id}/process")
  public void submitForProcessing(@PathVariable("id") Long id) {
    service.submitForProcessing(id);
  }
}
