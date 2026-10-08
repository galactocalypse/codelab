package com.codelab.movies.tmdb.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One unit of work on the movie ingest job stream: the TMDB id of a file to import. Job payloads
 * carry no business version — the contract is owned end-to-end by {@link MovieFeedRunner} and
 * {@link MovieProcessor}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieJob {
  private String id;
}
