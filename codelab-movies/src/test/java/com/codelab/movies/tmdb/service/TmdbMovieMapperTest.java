package com.codelab.movies.tmdb.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codelab.movies.tmdb.entity.TmdbCollection;
import com.codelab.movies.tmdb.entity.TmdbCompany;
import com.codelab.movies.tmdb.entity.TmdbCountry;
import com.codelab.movies.tmdb.entity.TmdbGenre;
import com.codelab.movies.tmdb.entity.TmdbKeyword;
import com.codelab.movies.tmdb.entity.TmdbLanguage;
import com.codelab.movies.tmdb.entity.TmdbMovie;
import com.codelab.movies.tmdb.entity.TmdbPerson;
import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.model.TmdbCollectionRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Mapper tests. No Spring, no database — the mapper is deliberately a set of pure functions so the
 * awkward parts of the TMDB payload can be exercised directly.
 */
class TmdbMovieMapperTest {

  private final TmdbMovieMapper mapper = new TmdbMovieMapper();
  private final RecordingReferences references = new RecordingReferences();

  @Test
  void mapsTheSampleMovie() throws IOException {
    TmdbMovie movie = mapper.toEntity(sample(), references);

    assertThat(movie.getMovieId()).isEqualTo(1704105L);
    assertThat(movie.getTitle()).isEqualTo("Limiar");
    assertThat(movie.getOriginalTitle()).isEqualTo("Limiar");
    assertThat(movie.getStatus()).isEqualTo("Released");
    assertThat(movie.getOriginalLanguage()).isEqualTo("pt");
    assertThat(movie.isAdult()).isFalse();
    assertThat(movie.isVideo()).isFalse();
    assertThat(movie.isSoftcore()).isFalse();

    // The sample has release_date "" and imdb_id null.
    assertThat(movie.getReleaseDate()).isNull();
    assertThat(movie.getImdbId()).isNull();

    // Empty-string text fields become null rather than being written as "".
    assertThat(movie.getOverview()).isNull();
    assertThat(movie.getTagline()).isNull();
    assertThat(movie.getHomepage()).isNull();

    assertThat(movie.getBudget()).isEqualTo(2384L);
    assertThat(movie.getRevenue()).isZero();
    assertThat(movie.getPopularity()).isEqualTo(0.8699);
    assertThat(movie.getVoteAverage()).isZero();
    assertThat(movie.getVoteCount()).isZero();
    assertThat(movie.getRuntime()).isZero();
  }

  @Test
  void mapsNestedCollections() throws IOException {
    TmdbMovie movie = mapper.toEntity(sample(), references);

    assertThat(movie.getGenres()).hasSize(2);
    assertThat(movie.getKeywords()).hasSize(2);
    assertThat(movie.getCast()).hasSize(3);
    assertThat(movie.getCrew()).hasSize(11);
    assertThat(movie.getSpokenLanguages()).hasSize(1);

    // Empty in the sample.
    assertThat(movie.getProductionCompanies()).isEmpty();
    assertThat(movie.getProductionCountries()).isEmpty();
    assertThat(movie.getReviews()).isEmpty();

    assertThat(movie.getSimilarMovies()).hasSize(19);
    assertThat(movie.getRecommendations()).hasSize(20);
  }

  @Test
  void mapsCastAndCrewOntoSharedPeople() throws IOException {
    TmdbMovie movie = mapper.toEntity(sample(), references);

    // 3 cast + 11 crew, but Samuel Rendeiro and Diogo Maia each appear twice in crew, so the
    // distinct person count is lower than the credit count.
    assertThat(references.peoplePassed).hasSize(12);

    var cast = movie.getCast().iterator().next();
    assertThat(cast.getMovie()).isSameAs(movie);
    assertThat(cast.getPerson().getName()).isEqualTo("Bryan Carvalho");
    assertThat(cast.getCharacter()).isEqualTo("Ícaro");
    assertThat(cast.getCastOrder()).isZero();

    // department and job are NOT NULL; the sample sends both for every crew member.
    movie
        .getCrew()
        .forEach(
            crew -> {
              assertThat(crew.getDepartment()).isNotBlank();
              assertThat(crew.getJob()).isNotBlank();
              assertThat(crew.getMovie()).isSameAs(movie);
            });
  }

  @Test
  void keepsOnePersonInstanceForRepeatedCredits() throws IOException {
    TmdbMovie movie = mapper.toEntity(sample(), references);

    // Samuel Rendeiro is credited twice in crew (Writer, Director) and must resolve to a single
    // TmdbPerson, or the re-import path would duplicate the person row.
    long rendeiro =
        movie.getCrew().stream()
            .filter(crew -> crew.getPerson().getName().equals("Samuel Rendeiro"))
            .map(crew -> crew.getPerson().getPersonId())
            .distinct()
            .count();
    assertThat(rendeiro).isEqualTo(1);

    long distinct =
        movie.getCrew().stream().map(crew -> crew.getPerson().getPersonId()).distinct().count();
    // 11 crew credits, two people credited twice (Writer + Director each) -> 9 distinct ids.
    assertThat(distinct).isEqualTo(9);
    // One lookup per person across the whole movie — a duplicate here means a duplicate row.
    assertThat(references.peoplePassed).doesNotHaveDuplicates();
  }

  @Test
  void mapsTheCollectionBlock() throws IOException {
    MovieDetails details = sample();
    TmdbCollectionRef ref = new TmdbCollectionRef();
    ref.setId(1078977);
    ref.setName("The Next Generation: Patlabor Collection");
    ref.setPosterPath("/h0OkS3OjRM6QkE7Re7LSg3PX3l.jpg");
    details.setBelongsToCollection(ref);

    TmdbMovie movie = mapper.toEntity(details, references);

    TmdbCollection collection = movie.getCollection();
    assertThat(collection).isNotNull();
    assertThat(collection.getCollectionId()).isEqualTo(1078977L);
    assertThat(collection.getName()).isEqualTo("The Next Generation: Patlabor Collection");
    assertThat(collection.getPosterPath()).isEqualTo("/h0OkS3OjRM6QkE7Re7LSg3PX3l.jpg");
    assertThat(collection.getBackdropPath()).isNull();
  }

  @Test
  void handlesAnEmptyMarkerFile() {
    // TMDB writes {} for movies it could not fetch. Nothing should blow up.
    TmdbMovie movie = mapper.toEntity(new MovieDetails(), references);

    assertThat(movie.getMovieId()).isZero();
    assertThat(movie.getTitle()).isEqualTo("Untitled");
    assertThat(movie.getOriginalTitle()).isEqualTo("Untitled");
    assertThat(movie.getGenres()).isEmpty();
    assertThat(movie.getCast()).isEmpty();
    assertThat(movie.getCollection()).isNull();
  }

  @Test
  void fallsBackBetweenTitlesAndFinallyToUntitled() {
    MovieDetails noTitle = new MovieDetails();
    noTitle.setOriginalTitle("Only Original");
    assertThat(mapper.toEntity(noTitle, references).getTitle()).isEqualTo("Only Original");

    MovieDetails neither = new MovieDetails();
    neither.setTitle("   ");
    neither.setOriginalTitle("");
    assertThat(mapper.toEntity(neither, references).getTitle()).isEqualTo("Untitled");
  }

  @Test
  void processesProductionCountriesBeforeOriginCountries() throws IOException {
    // The sample's origin_country is ["PT"] with no name; processing countries in the wrong order
    // would leave PT permanently nameless.
    TmdbMovie movie = mapper.toEntity(sample(), references);

    assertThat(references.countryCalls).hasSize(1);
    assertThat(movie.getOriginCountries()).hasSize(1);
    assertThat(movie.getOriginCountries().iterator().next().getIso31661()).isEqualTo("PT");
    assertThat(movie.getOriginCountries().iterator().next().getName()).isNull();
  }

  @Test
  void parsesAndRejectsDates() {
    assertThat(TmdbMovieMapper.date("2016-12-16")).isEqualTo(LocalDate.of(2016, 12, 16));
    // Some payloads carry a timestamp where a date is expected.
    assertThat(TmdbMovieMapper.date("2016-12-16T00:00:00Z")).isEqualTo(LocalDate.of(2016, 12, 16));
    assertThat(TmdbMovieMapper.date("")).isNull();
    assertThat(TmdbMovieMapper.date("not a date")).isNull();
    assertThat(TmdbMovieMapper.date(null)).isNull();
  }

  @Test
  void parsesTimestamps() {
    assertThat(TmdbMovieMapper.timestamp("2022-10-07T17:25:03Z").toString())
        .isEqualTo("2022-10-07T17:25:03Z");
    assertThat(TmdbMovieMapper.timestamp("2022-10-07T17:25:03+00:00").toString())
        .isEqualTo("2022-10-07T17:25:03Z");
    assertThat(TmdbMovieMapper.timestamp("")).isNull();
    assertThat(TmdbMovieMapper.timestamp("garbage")).isNull();
    assertThat(TmdbMovieMapper.timestamp(null)).isNull();
  }

  @Test
  void mapsNullCollectionsRatherThanThrowing() {
    MovieDetails details = new MovieDetails();
    details.setId(1);
    // Every list left null, which is what a sparse payload looks like.

    TmdbMovie movie = mapper.toEntity(details, references);

    assertThat(movie.getGenres()).isEmpty();
    assertThat(movie.getKeywords()).isEmpty();
    assertThat(movie.getCast()).isEmpty();
    assertThat(movie.getCrew()).isEmpty();
    assertThat(movie.getReviews()).isEmpty();
    assertThat(movie.getSimilarMovies()).isEmpty();
    assertThat(movie.getRecommendations()).isEmpty();
  }

  private static MovieDetails sample() throws IOException {
    try (InputStream in = TmdbMovieMapperTest.class.getResourceAsStream("/1704105.json")) {
      return new ObjectMapper().readValue(in, MovieDetails.class);
    }
  }

  /** Records what the mapper asked for, and hands back the candidate it was given. */
  private static final class RecordingReferences implements TmdbMovieMapper.ReferenceFactory {

    private final List<Long> peoplePassed = new ArrayList<>();
    private final List<TmdbCountry> countryCalls = new ArrayList<>();

    @Override
    public TmdbGenre genre(long id, String name) {
      TmdbGenre genre = new TmdbGenre();
      genre.setGenreId(id);
      genre.setName(name);
      return genre;
    }

    @Override
    public TmdbKeyword keyword(long id, String name) {
      TmdbKeyword keyword = new TmdbKeyword();
      keyword.setKeywordId(id);
      keyword.setName(name);
      return keyword;
    }

    @Override
    public TmdbCountry country(TmdbCountry candidate) {
      countryCalls.add(candidate);
      return candidate;
    }

    @Override
    public TmdbLanguage language(TmdbLanguage candidate) {
      return candidate;
    }

    @Override
    public TmdbCompany company(TmdbCompany candidate) {
      return candidate;
    }

    @Override
    public TmdbCollection collection(TmdbCollectionRef ref) {
      if (ref == null) {
        return null;
      }
      TmdbCollection collection = new TmdbCollection();
      collection.setCollectionId(ref.getId());
      collection.setName(ref.getName());
      collection.setPosterPath(ref.getPosterPath());
      collection.setBackdropPath(ref.getBackdropPath());
      return collection;
    }

    @Override
    public TmdbPerson person(TmdbPerson candidate) {
      peoplePassed.add(candidate.getPersonId());
      return candidate;
    }
  }
}
