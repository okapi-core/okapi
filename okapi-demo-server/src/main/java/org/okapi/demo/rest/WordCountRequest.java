package org.okapi.demo.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WordCountRequest {
    String sentenceId;
    String text;
}
