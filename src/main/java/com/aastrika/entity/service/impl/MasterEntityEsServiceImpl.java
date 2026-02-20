package com.aastrika.entity.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.service.MasterEntityEsService;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterEntityEsServiceImpl implements MasterEntityEsService {

  private final ElasticSearchEntityRepository elasticSearchEntityRepository;
  private final MasterEntityMapper masterEntityMapper;
  private final ElasticsearchOperations elasticsearchOperations;
  private static final int DEFAULT_PAGE_SIZE = 500;

  @Override
  public void saveEntityDetailsInES(@NonNull List<EntitySheetRow> entitySheetRowList, String entityType) {

    List<MasterEntityDocument> documents = entitySheetRowList.stream()
      .map(row -> {
        MasterEntityDocument doc = masterEntityMapper.toDocument(row);
        // Set consistent ID: code_languageCode
        doc.setId(row.getCode() + "_" + row.getLanguage());
        return doc;
      })
      .toList();

    elasticSearchEntityRepository.saveAll(documents);

    log.info("Successfully saved {} documents to Elasticsearch", documents.size());
  }

  /**
   * Search by exact code match
   * Uses term query for exact matching on keyword field
   * <p>
   * Final ES JSON:
   * { "query": { "term": { "code": "YOUR_VALUE" } } }
   *
   */
  @Override
  public List<MasterEntityDocument> searchByCode(String code) {
    TermQuery termQuery = new TermQuery.Builder()
      .field("code")
      .value(code)
      .build();

    NativeQuery query = NativeQuery.builder()
      .withQuery(queryBuilder -> queryBuilder.term(termQuery))
      .build();

    return executeSearch(query);
  }


  /**
   * Dynamic search based on SearchDTO parameters.
   * - Always filters by entityType and languageCode (exact match)
   * - strict=true  → fuzzy search (typo tolerant) on specified fields
   * - strict=false → exact phrase match on specified fields
   *
   * Example ES JSON (strict=true):
   * {
   *   "query": {
   *     "bool": {
   *       "must": [
   *         { "term": { "entityType": "competency" } },
   *         { "term": { "languageCode": "en" } }
   *       ],
   *       "should": [
   *         { "match": { "code": { "query": "c1", "fuzziness": "AUTO" } } },
   *         { "match": { "name": { "query": "c1", "fuzziness": "AUTO" } } }
   *       ],
   *       "minimum_should_match": 1
   *     }
   *   }
   * }
   */
  public AppResponse<EntityResult<MasterEntitySearchResponseDTO>> findEntitiesBySearchParameter(SearchDTO searchDTO) {
    // Must: exact match on entityType
    Query entityTypeFilter = new Query.Builder()
      .match(entityTypeTermQueryBuilder ->
        entityTypeTermQueryBuilder.field("entityType").query(searchDTO.getEntityType()).fuzziness("AUTO"))
      .build();

    boolean hasLanguage = !searchDTO.getLanguage().isBlank();

    Query languageFilter = hasLanguage
      ? new Query.Builder()
          .term(t -> t.field("languageCode").value(searchDTO.getLanguage()).caseInsensitive(true))
          .build()
      : null;

    if (searchDTO.getQuery() == null || searchDTO.getQuery().isBlank()) {
      BoolQuery.Builder boolBlankBuilder = new BoolQuery.Builder().must(entityTypeFilter);
      if (hasLanguage) boolBlankBuilder.must(languageFilter);

      NativeQuery matchAllQuery = NativeQuery.builder()
          .withQuery(queryBuilder -> queryBuilder.bool(boolBlankBuilder.build()))
          .build();
      return wrapInApiResponse(executeSearch(matchAllQuery));
    }

    // Should: fuzzy or exact match on each field
    List<Query> fieldQueries = searchDTO.getField().stream()
        .map(field -> buildFieldQuery(field, searchDTO.getQuery(), searchDTO.isStrict()))
        .toList();

    BoolQuery.Builder mainBoolBuilder = new BoolQuery.Builder()
        .must(entityTypeFilter)
        .should(fieldQueries)
        .minimumShouldMatch("1");
    if (hasLanguage) mainBoolBuilder.must(languageFilter);

    BoolQuery boolQuery = mainBoolBuilder.build();

    NativeQuery query = NativeQuery.builder()
        .withQuery(queryBuilder -> queryBuilder.bool(boolQuery))
        .build();

    return wrapInApiResponse(executeSearch(query));
  }

  /**
   * @param masterEntityDocumentList
   * @return
   */
  private AppResponse<EntityResult<MasterEntitySearchResponseDTO>> wrapInApiResponse(List<MasterEntityDocument> masterEntityDocumentList) {
    List<MasterEntitySearchResponseDTO> masterEntitySearchResponseDTOList = masterEntityDocumentList != null
        ? masterEntityDocumentList.stream().map(masterEntityMapper::toSearchResponse).toList()
        : List.of();
    return AppResponse.success("api.entity.search", EntityResult.of(masterEntitySearchResponseDTOList), HttpStatus.OK);
  }

  private Query buildFieldQuery(String field, String queryText, boolean strict) {
    if (!strict) {
      // Fuzzy: handles typos
      return new Query.Builder()
          .match(fieldMatchQueryBuilder -> fieldMatchQueryBuilder.field(field).query(queryText).fuzziness("AUTO"))
          .build();
    } else {
      // Exact phrase match
      return new Query.Builder()
          .matchPhrase(fieldMatchPhraseQueryBuilder -> fieldMatchPhraseQueryBuilder.field(field).query(queryText))
          .build();
    }
  }

  /**
   * Fuzzy search by name - handles typos and multi-word search
   * Uses MatchQuery with fuzziness (not FuzzyQuery which is single-term only)
   *
   * Final ES JSON:
   * { "query": { "match": { "name": { "query": "YOUR_VALUE", "fuzziness": "AUTO" } } } }
   */
  @Override
  public List<MasterEntityDocument> fuzzySearchByName(String name) {
    // Step 1: Build MatchQuery with fuzziness
    MatchQuery matchQuery = new MatchQuery.Builder()
      .field("name")
      .query(name)
      .fuzziness("AUTO")  // AUTO: 0-2 chars = exact, 3-5 chars = 1 edit, >5 chars = 2 edits
      .build();

    // Step 2: Wrap in NativeQuery
    NativeQuery query = NativeQuery.builder()
      .withQuery(queryBuilder -> queryBuilder.match(matchQuery))
      .build();

    return executeSearch(query);
  }

  /**
   * Phrase search by name - words must appear together in order
   * Uses MatchPhraseQuery for exact phrase matching
   * slop=2 allows minor word reordering (e.g., "संचार कौशल" matches "कौशल संचार")
   *
   * Final ES JSON:
   * { "query": { "match_phrase": { "name": { "query": "YOUR_VALUE", "slop": 2 } } } }
   *
   * Note: match_phrase does NOT support fuzziness (typo tolerance).
   * For typo tolerance with phrase matching, consider using bool query with match + match_phrase.
   */
  @Override
  public List<MasterEntityDocument> phraseSearchByName(String name) {
    MatchPhraseQuery phraseQuery = new MatchPhraseQuery.Builder()
        .field("name")
        .query(name)
        .slop(2)  // allows up to 2 word position swaps
        .build();

    NativeQuery query = NativeQuery.builder()
        .withQuery(queryBuilder -> queryBuilder.matchPhrase(phraseQuery))
        .build();

    return executeSearch(query);
  }

  /**
   * Combined phrase + fuzzy search - words must appear together with typo tolerance
   * Uses BoolQuery combining:
   * - must: MatchQuery with fuzziness + AND operator (all words required, typos allowed)
   * - should: MatchPhraseQuery (boosts exact phrase matches)
   *
   * Final ES JSON:
   * {
   *   "query": {
   *     "bool": {
   *       "must": { "match": { "name": { "query": "VALUE", "fuzziness": "AUTO", "operator": "and" } } },
   *       "should": { "match_phrase": { "name": { "query": "VALUE", "slop": 2, "boost": 2.0 } } }
   *     }
   *   }
   * }
   */
  @Override
  public List<MasterEntityDocument> fuzzyPhraseSearchByName(String name) {
    // Must: all words must match (with typo tolerance)
    MatchQuery fuzzyMatch = new MatchQuery.Builder()
        .field("name")
        .query(name)
        .fuzziness("AUTO")
        .operator(Operator.And)
        .build();

    // Should: boost exact phrase matches (words in order)
    MatchPhraseQuery phraseBoost = new MatchPhraseQuery.Builder()
        .field("name")
        .query(name)
        .slop(2)
        .boost(2.0f)
        .build();

    // Combine with bool query
    BoolQuery boolQuery = new BoolQuery.Builder()
        .must(new Query.Builder().match(fuzzyMatch).build())
        .should(new Query.Builder().matchPhrase(phraseBoost).build())
        .build();

    NativeQuery query = NativeQuery.builder()
        .withQuery(q -> q.bool(boolQuery))
        .build();

    return executeSearch(query);
  }

  /**
   * Multi-field fuzzy search - searches across name, description, and competency levels
   * Uses boosting: name^3 means name matches are 3x more important than others
   */
  @Override
  public List<MasterEntityDocument> fuzzySearch(String searchText) {
    NativeQuery query = NativeQuery.builder()
      .withQuery(q -> q.multiMatch(m -> m
        .query(searchText)
        .fields("name^3", "description^2", "competencyLevel1Name", "competencyLevel2Name",
          "competencyLevel3Name", "competencyLevel4Name", "competencyLevel5Name")
        .fuzziness("AUTO")
      ))
      .build();

    return executeSearch(query);
  }



  /**
   *
   * Final ES JSON:
   *   { "query": { "term": { "code": "YOUR_VALUE" } } }
   */
  public List<MasterEntityDocument> searchByCodeWithoutLambda(String code) {

    // ============ STEP 1: Build TermQuery (exact match, no text analysis) ============
    TermQuery termQuery = new TermQuery.Builder()
      .field("code")
      .value(code)
      .build();

    // ============ STEP 2: Wrap TermQuery inside Query object ============
    // Query can hold: term, match, bool, fuzzy, range, wildcard, etc.
    Query esQuery = new Query.Builder()
      .term(termQuery)
      .build();

    // ============ STEP 3: Create NativeQuery for Spring Data ES ============
    NativeQuery nativeQuery = NativeQuery.builder()
      .withQuery(esQuery)
      .build();

    // ============ STEP 4: Execute and return results ============
    return executeSearch(nativeQuery);
  }

  /**
   * Common method to execute search and extract results
   */
  private List<MasterEntityDocument> executeSearch(NativeQuery query) {
    query.setMaxResults(DEFAULT_PAGE_SIZE);

    SearchHits<MasterEntityDocument> searchHits =
      elasticsearchOperations.search(query, MasterEntityDocument.class);

    return searchHits.getSearchHits().stream()
      .map(SearchHit::getContent)
      .toList();
  }
}
