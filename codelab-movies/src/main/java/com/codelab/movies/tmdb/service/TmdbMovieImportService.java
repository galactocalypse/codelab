package com.codelab.movies.tmdb.service;

import com.codelab.movies.tmdb.entity.*;
import com.codelab.movies.tmdb.model.MovieDetails;
import com.codelab.movies.tmdb.model.TmdbCollectionRef;
import com.codelab.movies.tmdb.repository.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Persists a parsed {@link MovieDetails} and its nested objects into the normalized TMDB schema.
 *
 * <p>Ids are assigned by TMDB rather than generated, which is what makes this idempotent: importing
 * the same file twice converges on the same rows instead of duplicating them. That also makes the
 * operation resumable — a run interrupted halfway can simply be repeated.
 *
 * <p>One transaction per movie. A whole-directory run must not hold a single transaction open
 * across 1.2M movies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbMovieImportService implements TmdbMovieMapper.ReferenceFactory {

  private final TmdbMovieRepository movieRepository;
  private final TmdbGenreRepository genreRepository;
  private final TmdbKeywordRepository keywordRepository;
  private final TmdbCompanyRepository companyRepository;
  private final TmdbCountryRepository countryRepository;
  private final TmdbLanguageRepository languageRepository;
  private final TmdbCollectionRepository collectionRepository;
  private final TmdbPersonRepository personRepository;
  private final TmdbMovieMapper mapper;

  private final BoundedCache<Long, TmdbGenre> genres = new BoundedCache<>();
  private final BoundedCache<Long, TmdbKeyword> keywords = new BoundedCache<>();
  private final BoundedCache<Long, TmdbCompany> companies = new BoundedCache<>();
  private final BoundedCache<String, TmdbCountry> countries = new BoundedCache<>();
  private final BoundedCache<String, TmdbLanguage> languages = new BoundedCache<>();
  private final BoundedCache<Long, TmdbCollection> collections = new BoundedCache<>();
  private final BoundedCache<Long, TmdbPerson> people = new BoundedCache<>();

  /**
   * Maps and stores one movie. TMDB writes {@code {}} for movies it could not fetch; those carry no
   * id and are skipped rather than treated as errors.
   *
   * @return true if the movie was written, false if the input was an empty marker
   */
  @Transactional(transactionManager = "movies.transactionManager")
  public boolean persist(MovieDetails details) {
    if (details == null || details.getId() == 0) {
      return false;
    }

    // A rollback leaves reference entities that were only ever inserted inside this transaction
    // sitting in the caches; every later movie would then link against rows that do not exist.
    // Registering up front means the caches are dropped for every rollback path — including a
    // constraint violation that only surfaces when the transaction commits, after this method
    // has already returned.
    registerCacheClearOnRollback();

    TmdbMovie movie = mapper.toEntity(details, this);

    // findById rather than relying on save(): with an assigned id and no @GeneratedValue, save()
    // would merge() -- an extra SELECT per movie across 1.2M rows. Branching explicitly keeps the
    // insert path on persist().
    TmdbMovie existing = movieRepository.findById(movie.getMovieId()).orElse(null);
    if (existing == null) {
      movieRepository.save(movie);
    } else {
      apply(movie, existing);
    }

    return true;
  }

  private void registerCacheClearOnRollback() {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != TransactionSynchronization.STATUS_COMMITTED) {
              log.warn("Movie import rolled back; clearing reference caches to match");
              clearReferenceCaches();
            }
          }
        });
  }

  private void clearReferenceCaches() {
    genres.clear();
    keywords.clear();
    companies.clear();
    countries.clear();
    languages.clear();
    collections.clear();
    people.clear();
  }

  /**
   * Re-import path: copies the incoming scalar fields onto the already-persisted row and replaces
   * its children.
   *
   * <p>The {@code @ManyToMany} sets are assigned fresh collections so Hibernate diffs the
   * association — identical content produces no DML, changed content produces precise
   * DELETE/INSERT. That matters because those join tables (e.g. {@code
   * tmdb_movie_origin_countries}) use composite primary keys: mutating in place with {@code
   * clear()} + {@code addAll()} would re-insert a row with the same key before the orphan delete
   * flushes, tripping the PK.
   *
   * <p>The {@code orphanRemoval} {@code @OneToMany}s (cast, crew, reviews, similar,
   * recommendations) are mutated in place instead — replacing the collection reference is rejected
   * by Hibernate for orphanly-owned children ("no longer referenced by the owning entity"). Their
   * rows use generated ids, so clearing and re-adding never collides.
   */
  private void apply(TmdbMovie incoming, TmdbMovie existing) {
    existing.setTitle(incoming.getTitle());
    existing.setOriginalTitle(incoming.getOriginalTitle());
    existing.setOverview(incoming.getOverview());
    existing.setTagline(incoming.getTagline());
    existing.setStatus(incoming.getStatus());
    existing.setOriginalLanguage(incoming.getOriginalLanguage());
    existing.setAdult(incoming.isAdult());
    existing.setVideo(incoming.isVideo());
    existing.setSoftcore(incoming.isSoftcore());
    existing.setImdbId(incoming.getImdbId());
    existing.setReleaseDate(incoming.getReleaseDate());
    existing.setRuntime(incoming.getRuntime());
    existing.setBudget(incoming.getBudget());
    existing.setRevenue(incoming.getRevenue());
    existing.setPopularity(incoming.getPopularity());
    existing.setVoteAverage(incoming.getVoteAverage());
    existing.setVoteCount(incoming.getVoteCount());
    existing.setHomepage(incoming.getHomepage());
    existing.setPosterPath(incoming.getPosterPath());
    existing.setBackdropPath(incoming.getBackdropPath());
    existing.setCollection(incoming.getCollection());

    existing.setGenres(incoming.getGenres());
    existing.setKeywords(incoming.getKeywords());
    existing.setProductionCompanies(incoming.getProductionCompanies());
    existing.setProductionCountries(incoming.getProductionCountries());
    existing.setSpokenLanguages(incoming.getSpokenLanguages());
    existing.setOriginCountries(incoming.getOriginCountries());

    replaceChildren(
        existing.getCast(), parented(incoming.getCast(), TmdbMovieCast::setMovie, existing));
    replaceChildren(
        existing.getCrew(), parented(incoming.getCrew(), TmdbMovieCrew::setMovie, existing));
    replaceChildren(
        existing.getReviews(), parented(incoming.getReviews(), TmdbReview::setMovie, existing));
    replaceChildren(
        existing.getSimilarMovies(),
        parented(incoming.getSimilarMovies(), TmdbMovieSimilar::setMovie, existing));
    replaceChildren(
        existing.getRecommendations(),
        parented(incoming.getRecommendations(), TmdbMovieRecommendation::setMovie, existing));

    movieRepository.save(existing);
  }

  /** Points freshly-mapped children at the persisted parent so the merged graph is not orphaned. */
  private static <T> Set<T> parented(
      Set<T> children, BiConsumer<T, TmdbMovie> setter, TmdbMovie parent) {
    children.forEach(child -> setter.accept(child, parent));
    return children;
  }

  private static <T> void replaceChildren(Set<T> existing, Set<T> incoming) {
    existing.clear();
    existing.addAll(incoming);
  }

  // --- ReferenceFactory: resolve each shared reference once and reuse it across movies ---

  @Override
  public TmdbGenre genre(long id, String name) {
    return genres.get(
        id,
        () -> {
          TmdbGenre genre = genreRepository.findById(id).orElseGet(TmdbGenre::new);
          genre.setGenreId(id);
          genre.setName(name == null ? "" : name);
          return genreRepository.save(genre);
        });
  }

  @Override
  public TmdbKeyword keyword(long id, String name) {
    return keywords.get(
        id,
        () -> {
          TmdbKeyword keyword = keywordRepository.findById(id).orElseGet(TmdbKeyword::new);
          keyword.setKeywordId(id);
          keyword.setName(name == null ? "" : name);
          return keywordRepository.save(keyword);
        });
  }

  @Override
  public TmdbCompany company(TmdbCompany candidate) {
    return companies.get(
        candidate.getCompanyId(),
        () -> {
          TmdbCompany company =
              companyRepository.findById(candidate.getCompanyId()).orElseGet(TmdbCompany::new);
          company.setCompanyId(candidate.getCompanyId());
          company.setName(orEmpty(candidate.getName()));
          company.setLogoPath(candidate.getLogoPath());
          company.setOriginCountry(candidate.getOriginCountry());
          return companyRepository.save(company);
        });
  }

  /**
   * A country can arrive first as a bare code from {@code origin_country} and only gain its name
   * later from {@code production_countries}, so a cached row with no name is upgraded in place
   * rather than replaced.
   */
  @Override
  public TmdbCountry country(TmdbCountry candidate) {
    return countries.get(
        candidate.getIso31661(),
        () -> {
          TmdbCountry country =
              countryRepository.findById(candidate.getIso31661()).orElseGet(TmdbCountry::new);
          country.setIso31661(candidate.getIso31661());
          country.setName(candidate.getName());
          return countryRepository.save(country);
        },
        cached -> {
          if (cached.getName() == null && candidate.getName() != null) {
            cached.setName(candidate.getName());
            countryRepository.save(cached);
          }
        });
  }

  @Override
  public TmdbLanguage language(TmdbLanguage candidate) {
    return languages.get(
        candidate.getIso6391(),
        () -> {
          TmdbLanguage language =
              languageRepository.findById(candidate.getIso6391()).orElseGet(TmdbLanguage::new);
          language.setIso6391(candidate.getIso6391());
          if (candidate.getName() != null) {
            language.setName(candidate.getName());
          }
          if (candidate.getEnglishName() != null) {
            language.setEnglishName(candidate.getEnglishName());
          }
          return languageRepository.save(language);
        });
  }

  @Override
  public TmdbCollection collection(TmdbCollectionRef ref) {
    if (ref == null || ref.getId() == 0) {
      return null;
    }
    return collections.get(
        ref.getId(),
        () -> {
          TmdbCollection collection =
              collectionRepository.findById(ref.getId()).orElseGet(TmdbCollection::new);
          collection.setCollectionId(ref.getId());
          collection.setName(orEmpty(TmdbMovieMapper.text(ref.getName())));
          collection.setPosterPath(TmdbMovieMapper.text(ref.getPosterPath()));
          collection.setBackdropPath(TmdbMovieMapper.text(ref.getBackdropPath()));
          return collectionRepository.save(collection);
        });
  }

  /**
   * People are cached like every other reference. Unlike the others they are not small — over a
   * million distinct ids across the full dataset, touched by every movie — which is why the cache
   * is bounded rather than unbounded. A miss costs one SELECT, the same cost this path would pay
   * per credit without any cache at all.
   */
  @Override
  public TmdbPerson person(TmdbPerson candidate) {
    return people.get(
        candidate.getPersonId(),
        () -> {
          TmdbPerson person =
              personRepository.findById(candidate.getPersonId()).orElseGet(TmdbPerson::new);
          person.setPersonId(candidate.getPersonId());
          person.setName(orEmpty(candidate.getName()));
          person.setOriginalName(candidate.getOriginalName());
          person.setGender(candidate.getGender());
          person.setKnownForDepartment(candidate.getKnownForDepartment());
          person.setPopularity(candidate.getPopularity());
          person.setProfilePath(candidate.getProfilePath());
          person.setAdult(candidate.isAdult());
          return personRepository.save(person);
        });
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  /**
   * Concurrent, self-evicting cache for reference entities.
   *
   * <p>Unbounded maps would be a heap leak on a full import: keywords and companies each run to
   * 100k+ distinct values and people to over a million. On reaching {@code limit} the whole cache
   * is dropped rather than evicting a single entry — an LRU would keep the working set warm but
   * needs per-access bookkeeping on a path that runs tens of millions of times, and a periodic
   * reset costs only re-reads for the entries that come back.
   */
  private static final class BoundedCache<K, V> {

    private static final int LIMIT = 100_000;
    private static final int STRIPE_COUNT = 64;

    private final ConcurrentHashMap<K, V> entries = new ConcurrentHashMap<>();
    private final Object[] stripes = new Object[STRIPE_COUNT];

    BoundedCache() {
      for (int i = 0; i < STRIPE_COUNT; i++) {
        stripes[i] = new Object();
      }
    }

    V get(K key, Supplier<V> loader) {
      return get(key, loader, null);
    }

    /**
     * @param onCachedHit run against an already-cached value, for the case where a later input can
     *     add information an earlier one lacked
     */
    V get(K key, Supplier<V> loader, java.util.function.Consumer<V> onCachedHit) {
      if (entries.size() >= LIMIT) {
        log.warn("Reference cache hit its {} entry limit; clearing", LIMIT);
        entries.clear();
      }

      V cached = entries.get(key);
      if (cached != null) {
        if (onCachedHit != null) {
          onCachedHit.accept(cached);
        }
        return cached;
      }

      // One loader at a time per stripe. With concurrency > 1, two consumers could both miss
      // this key and both insert the same reference row; serialising loads per key means the
      // second one instead finds the winner's entry and skips the database entirely. The
      // double-checked get keeps the hot path lock-free.
      synchronized (stripeFor(key)) {
        cached = entries.get(key);
        if (cached != null) {
          if (onCachedHit != null) {
            onCachedHit.accept(cached);
          }
          return cached;
        }

        V loaded;
        try {
          loaded = loader.get();
        } catch (DataIntegrityViolationException collision) {
          // Backstop for writers outside this JVM: the database holds the loser on the unique
          // index until the winner commits, so re-running the loader re-reads the now-committed
          // row instead of inserting again.
          log.debug("Reference insert collided with a concurrent writer; re-reading", collision);
          loaded = loader.get();
        }
        entries.put(key, loaded);
        return loaded;
      }
    }

    void clear() {
      entries.clear();
    }

    private Object stripeFor(K key) {
      int hash = key.hashCode();
      hash ^= hash >>> 16;
      return stripes[(hash & 0x7fffffff) % STRIPE_COUNT];
    }
  }
}
