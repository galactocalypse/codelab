package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Credits {
  private List<CastMember> cast;
  private List<CrewMember> crew;
}
