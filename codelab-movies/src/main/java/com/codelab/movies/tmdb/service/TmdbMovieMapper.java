package com.codelab.movies.tmdb.service;

import com.codelab.movies.tmdb.entity.*;
import com.codelab.movies.tmdb.model.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Translates a parsed {@link MovieDetails} DTO into the {@link TmdbMovie} entity graph.
 *
 * <p>Pure functions only — no Spring Data, no transaction — so it is unit-testable without a
 * database. All type coercion lives here rather than in Jackson deserializers: {@code releaseDate}
 * arrives as a {@code String} that is often {@code ""} rather than null, and doing that coercion in
 * the DTOs would make them unusable outside this importer.
 *
 * <p>Conversion rules deliberately match {@code movie_fetcher/load_movies.py} so both loaders
 * produce identical rows from identical input.
 */
@Component
public class TmdbMovieMapper {

  /** TMDB can send a title that is empty or absent; both title columns are NOT NULL. */
  private static final String UNTITLED = "Untitled";

  /**
   * Maps the whole graph. {@code references} supplies the already-resolved shared reference
   * entities (genres, keywords, countries, …) — resolving them is the caller's job because it needs
   * database access.
   *
   * @param references per-movie reference entities, keyed by kind; see {@link ReferenceFactory}
   */
  public TmdbMovie toEntity(MovieDetails details, ReferenceFactory references) {
    TmdbMovie movie = new TmdbMovie();
    movie.setMovieId(details.getId());

    movie.setTitle(orUntitled(details.getTitle(), details.getOriginalTitle()));
    movie.setOriginalTitle(orUntitled(details.getOriginalTitle(), details.getTitle()));
    movie.setOverview(text(details.getOverview()));
    movie.setTagline(text(details.getTagline()));
    movie.setStatus(text(details.getStatus()));
    movie.setOriginalLanguage(text(details.getOriginalLanguage()));
    movie.setAdult(details.isAdult());
    movie.setVideo(details.isVideo());
    movie.setSoftcore(details.isSoftcore());
    movie.setImdbId(text(details.getImdbId()));
    movie.setReleaseDate(date(details.getReleaseDate()));
    movie.setRuntime(details.getRuntime());
    movie.setBudget(details.getBudget());
    movie.setRevenue(details.getRevenue());
    movie.setPopularity(details.getPopularity());
    movie.setVoteAverage(details.getVoteAverage());
    movie.setVoteCount(details.getVoteCount());
    movie.setHomepage(text(details.getHomepage()));
    movie.setPosterPath(text(details.getPosterPath()));
    movie.setBackdropPath(text(details.getBackdropPath()));
    movie.setCollection(references.collection(details.getBelongsToCollection()));

    mapGenres(details, movie, references);
    mapKeywords(details, movie, references);
    mapCompanies(details, movie, references);
    mapCountries(details, movie, references);
    mapLanguages(details, movie, references);

    mapCredits(details, movie, references);
    mapReviews(details, movie);
    mapSimilar(details, movie);
    mapRecommendations(details, movie);

    return movie;
  }

  private void mapGenres(MovieDetails details, TmdbMovie movie, ReferenceFactory references) {
    if (details.getGenres() == null) {
      return;
    }
    for (Genre genre : details.getGenres()) {
      if (genre != null && genre.getId() != 0) {
        movie.getGenres().add(references.genre(genre.getId(), text(genre.getName())));
      }
    }
  }

  private void mapKeywords(MovieDetails details, TmdbMovie movie, ReferenceFactory references) {
    KeywordsResponse keywords = details.getKeywords();
    if (keywords == null || keywords.getKeywords() == null) {
      return;
    }
    for (Keyword keyword : keywords.getKeywords()) {
      if (keyword != null && keyword.getId() != 0) {
        movie.getKeywords().add(references.keyword(keyword.getId(), text(keyword.getName())));
      }
    }
  }

  private void mapCompanies(MovieDetails details, TmdbMovie movie, ReferenceFactory references) {
    if (details.getProductionCompanies() == null) {
      return;
    }
    for (ProductionCompany company : details.getProductionCompanies()) {
      if (company == null || company.getId() == 0) {
        continue;
      }
      TmdbCompany entity = new TmdbCompany();
      entity.setCompanyId(company.getId());
      entity.setName(orEmpty(text(company.getName())));
      entity.setLogoPath(text(company.getLogoPath()));
      entity.setOriginCountry(text(company.getOriginCountry()));
      movie.getProductionCompanies().add(references.company(entity));
    }
  }

  /**
   * Maps both country lists onto the same {@link TmdbCountry} table.
   *
   * <p>{@code production_countries} is processed first on purpose: it carries the ISO code's real
   * name, whereas {@code origin_country} is a bare code list. Processing them in this order lets
   * the reference factory learn a name for a code that would otherwise only ever arrive nameless.
   * This ordering is load-bearing — reversing it would persist a null name for every country that
   * is both produced in and originates from.
   */
  private void mapCountries(MovieDetails details, TmdbMovie movie, ReferenceFactory references) {
    if (details.getProductionCountries() != null) {
      for (ProductionCountry country : details.getProductionCountries()) {
        if (country == null || text(country.getIso31661()) == null) {
          continue;
        }
        TmdbCountry entity = new TmdbCountry();
        entity.setIso31661(text(country.getIso31661()));
        entity.setName(text(country.getName()));
        movie.getProductionCountries().add(references.country(entity));
      }
    }

    if (details.getOriginCountry() != null) {
      for (String iso : details.getOriginCountry()) {
        String code = text(iso);
        if (code == null) {
          continue;
        }
        TmdbCountry entity = new TmdbCountry();
        entity.setIso31661(code);
        movie.getOriginCountries().add(references.country(entity));
      }
    }
  }

  private void mapLanguages(MovieDetails details, TmdbMovie movie, ReferenceFactory references) {
    if (details.getSpokenLanguages() == null) {
      return;
    }
    for (SpokenLanguage language : details.getSpokenLanguages()) {
      if (language == null || text(language.getIso6391()) == null) {
        continue;
      }
      TmdbLanguage entity = new TmdbLanguage();
      entity.setIso6391(text(language.getIso6391()));
      entity.setName(text(language.getName()));
      entity.setEnglishName(text(language.getEnglishName()));
      movie.getSpokenLanguages().add(references.language(entity));
    }
  }

  /**
   * Maps the flat cast/crew entries onto person + credit rows.
   *
   * <p>Every credit needs a {@code credit_id} (it is the primary key) and a person id. Entries
   * missing either are dropped, matching the Python loader. Persons are de-duplicated per movie so
   * one {@link TmdbPerson} instance backs every credit it appears in.
   */
  private void mapCredits(MovieDetails details, TmdbMovie movie, ReferenceFactory references) {
    Credits credits = details.getCredits();
    if (credits == null) {
      return;
    }

    Map<Long, TmdbPerson> persons = new LinkedHashMap<>();

    if (credits.getCast() != null) {
      for (CastMember member : credits.getCast()) {
        if (member == null || text(member.getCreditId()) == null) {
          continue;
        }
        TmdbMovieCast cast = new TmdbMovieCast();
        cast.setCreditId(text(member.getCreditId()));
        cast.setMovie(movie);
        cast.setPerson(person(persons, member, references));
        cast.setCharacter(text(member.getCharacter()));
        cast.setCastOrder(member.getOrder());
        movie.getCast().add(cast);
      }
    }

    if (credits.getCrew() != null) {
      for (CrewMember member : credits.getCrew()) {
        if (member == null || text(member.getCreditId()) == null) {
          continue;
        }
        TmdbMovieCrew crew = new TmdbMovieCrew();
        crew.setCreditId(text(member.getCreditId()));
        crew.setMovie(movie);
        crew.setPerson(person(persons, member, references));
        crew.setDepartment(orEmpty(text(member.getDepartment())));
        crew.setJob(orEmpty(text(member.getJob())));
        movie.getCrew().add(crew);
      }
    }
  }

  private TmdbPerson person(
      Map<Long, TmdbPerson> persons, CastMember member, ReferenceFactory references) {
    return person(
        persons,
        member.getId(),
        member.isAdult(),
        member.getOriginalName(),
        member.getGender(),
        member.getKnownForDepartment(),
        member.getPopularity(),
        member.getName(),
        member.getProfilePath(),
        references);
  }

  private TmdbPerson person(
      Map<Long, TmdbPerson> persons, CrewMember member, ReferenceFactory references) {
    return person(
        persons,
        member.getId(),
        member.isAdult(),
        member.getOriginalName(),
        member.getGender(),
        member.getKnownForDepartment(),
        member.getPopularity(),
        member.getName(),
        member.getProfilePath(),
        references);
  }

  private TmdbPerson person(
      Map<Long, TmdbPerson> persons,
      long id,
      boolean adult,
      String originalName,
      int gender,
      String knownForDepartment,
      double popularity,
      String name,
      String profilePath,
      ReferenceFactory references) {
    return persons.computeIfAbsent(
        id,
        key -> {
          TmdbPerson person = new TmdbPerson();
          person.setPersonId(key);
          person.setName(orEmpty(text(name)));
          person.setOriginalName(text(originalName));
          person.setGender(gender);
          person.setKnownForDepartment(text(knownForDepartment));
          person.setPopularity(popularity);
          person.setProfilePath(text(profilePath));
          person.setAdult(adult);
          return references.person(person);
        });
  }

  private void mapReviews(MovieDetails details, TmdbMovie movie) {
    PagedResult<Review> reviews = details.getReviews();
    if (reviews == null || reviews.getResults() == null) {
      return;
    }
    for (Review review : reviews.getResults()) {
      if (review == null || text(review.getId()) == null) {
        continue;
      }
      TmdbReview entity = new TmdbReview();
      entity.setReviewId(text(review.getId()));
      entity.setMovie(movie);
      entity.setAuthor(text(review.getAuthor()));

      Review.AuthorDetails author = review.getAuthorDetails();
      if (author != null) {
        entity.setAuthorName(text(author.getName()));
        entity.setAuthorUsername(text(author.getUsername()));
        entity.setAuthorRating(author.getRating());
      }

      entity.setContent(text(review.getContent()));
      entity.setCreatedAt(timestamp(review.getCreatedAt()));
      entity.setUpdatedAt(timestamp(review.getUpdatedAt()));
      entity.setUrl(text(review.getUrl()));
      movie.getReviews().add(entity);
    }
  }

  private void mapSimilar(MovieDetails details, TmdbMovie movie) {
    PagedResult<MovieSummary> similar = details.getSimilar();
    if (similar == null || similar.getResults() == null) {
      return;
    }
    Set<TmdbMovieSimilar> edges = movie.getSimilarMovies();
    int order = 0;
    for (MovieSummary summary : similar.getResults()) {
      if (summary == null || summary.getId() == 0) {
        continue;
      }
      TmdbMovieSimilar edge = new TmdbMovieSimilar();
      edge.setMovieId(movie.getMovieId());
      edge.setSimilarMovieId(summary.getId());
      edge.setMovie(movie);
      edge.setListOrder(order++);
      edges.add(edge);
    }
  }

  private void mapRecommendations(MovieDetails details, TmdbMovie movie) {
    PagedResult<MovieSummary> recommendations = details.getRecommendations();
    if (recommendations == null || recommendations.getResults() == null) {
      return;
    }
    Set<TmdbMovieRecommendation> edges = movie.getRecommendations();
    int order = 0;
    for (MovieSummary summary : recommendations.getResults()) {
      if (summary == null || summary.getId() == 0) {
        continue;
      }
      TmdbMovieRecommendation edge = new TmdbMovieRecommendation();
      edge.setMovieId(movie.getMovieId());
      edge.setRecommendedMovieId(summary.getId());
      edge.setMovie(movie);
      edge.setMediaType(text(summary.getMediaType()));
      edge.setListOrder(order++);
      edges.add(edge);
    }
  }

  /** Trims and converts blank to null, matching the loader's {@code _text}. */
  static String text(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String orUntitled(String preferred, String fallback) {
    String first = text(preferred);
    if (first != null) {
      return first;
    }
    String second = text(fallback);
    return second != null ? second : UNTITLED;
  }

  /** Blank or unparseable dates become null rather than failing the movie. */
  static LocalDate date(String value) {
    String cleaned = text(value);
    if (cleaned == null) {
      return null;
    }
    try {
      return LocalDate.parse(cleaned.length() > 10 ? cleaned.substring(0, 10) : cleaned);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /** Parses TMDB's {@code 2022-10-07T17:25:03Z} timestamps, returning null when malformed. */
  static OffsetDateTime timestamp(String value) {
    String cleaned = text(value);
    if (cleaned == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(
          cleaned.endsWith("Z") ? cleaned.substring(0, cleaned.length() - 1) + "+00:00" : cleaned);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /**
   * Resolves reference entities, de-duplicating across movies so a genre, keyword, company, country
   * or language is written once and reused.
   *
   * <p>Implemented by {@link TmdbMovieImportService} against the repositories. Kept as an interface
   * so the mapper can be tested without Spring or a database.
   */
  public interface ReferenceFactory {

    TmdbGenre genre(long id, String name);

    TmdbKeyword keyword(long id, String name);

    /** {@code candidate} may have a null {@code name}; an already-known name wins. */
    TmdbCountry country(TmdbCountry candidate);

    TmdbLanguage language(TmdbLanguage candidate);

    TmdbCompany company(TmdbCompany candidate);

    TmdbCollection collection(TmdbCollectionRef ref);

    TmdbPerson person(TmdbPerson candidate);
  }
}
