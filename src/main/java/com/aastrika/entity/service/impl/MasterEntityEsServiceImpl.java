package com.aastrika.entity.service.impl;

 import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.service.MasterEntityEsService;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
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
  private final OpenSearchOperations openSearchOperations;
  private static final int DEFAULT_PAGE_SIZE = 500;

  @Override
  public void saveEntityDetailsInES(@NonNull List<EntitySheetRow> entitySheetRowList, String entityType, String userId) {
    List<MasterEntityDocument> documents = entitySheetRowList.stream()
        .map(row -> {
          MasterEntityDocument doc = masterEntityMapper.toDocument(row);
          doc.setId(row.getCode() + "_" + row.getLanguage());
          doc.setCreatedAt(new Date());
          doc.setCreatedBy(userId);
          return doc;
        })
        .toList();

    elasticSearchEntityRepository.saveAll(documents);
    log.info("Successfully saved {} documents to OpenSearch", documents.size());
  }

  /**
   * Dynamic search based on SearchDTO parameters.
   * - Always filters by entityType (fuzzy) and languageCode (exact)
   * - strict=false → fuzzy match on specified fields
   * - strict=true  → exact phrase match on specified fields
   *
   * Example query (strict=false):
   * {
   *   "bool": {
   *     "must": [ { "match": { "entityType": { "query": "COMPETENCY", "fuzziness": "AUTO" } } },
   *               { "term":  { "languageCode": "en" } } ],
   *     "should": [ { "match": { "name": { "query": "value", "fuzziness": "AUTO" } } } ],
   *     "minimum_should_match": 1
   *   }
   * }
   */
  @Override
  public AppResponse<EntityResult<MasterEntitySearchResponseDTO>> findEntitiesBySearchParameter(SearchDTO searchDTO) {
    QueryBuilder entityTypeFilter = QueryBuilders
        .matchQuery("entityType", searchDTO.getEntityType().name())
        .fuzziness(Fuzziness.AUTO);

    boolean hasLanguage = searchDTO.getLanguage() != null && !searchDTO.getLanguage().isBlank();

    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(entityTypeFilter);
    if (hasLanguage) {
      boolQuery.must(QueryBuilders.termQuery("languageCode", searchDTO.getLanguage().toLowerCase()));
    }

    if (searchDTO.getQuery() != null && !searchDTO.getQuery().isBlank()) {
      searchDTO.getField().stream()
          .map(field -> buildFieldQuery(field, searchDTO.getQuery(), searchDTO.isStrict()))
          .forEach(boolQuery::should);
      boolQuery.minimumShouldMatch(1);
    }

    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withQuery(boolQuery)
        .build();

    return wrapInApiResponse(executeSearch(query));
  }

  private QueryBuilder buildFieldQuery(String field, String queryText, boolean strict) {
    if (!strict) {
      return QueryBuilders.matchQuery(field, queryText).fuzziness(Fuzziness.AUTO);
    } else {
      return QueryBuilders.matchPhraseQuery(field, queryText);
    }
  }

  /**
   * Phrase search by name — words must appear together in order.
   * slop=2 allows minor word reordering (e.g., "संचार कौशल" matches "कौशल संचार").
   *
   * Query: { "match_phrase": { "name": { "query": "VALUE", "slop": 2 } } }
   * Note: match_phrase does not support fuzziness.
   */
  @Override
  public List<MasterEntityDocument> phraseSearchByName(String name) {
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchPhraseQuery("name", name).slop(2))
        .build();
    return executeSearch(query);
  }

  /**
   * Combined fuzzy + phrase search — typo-tolerant with phrase boost.
   *
   * Query:
   * {
   *   "bool": {
   *     "must":   { "match": { "name": { "query": "VALUE", "fuzziness": "AUTO", "operator": "AND" } } },
   *     "should": { "match_phrase": { "name": { "query": "VALUE", "slop": 2, "boost": 2.0 } } }
   *   }
   * }
   */
  @Override
  public List<MasterEntityDocument> fuzzyPhraseSearchByName(String name) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("name", name)
            .fuzziness(Fuzziness.AUTO)
            .operator(org.opensearch.index.query.Operator.AND))
        .should(QueryBuilders.matchPhraseQuery("name", name)
            .slop(2)
            .boost(2.0f));

    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withQuery(boolQuery)
        .build();
    return executeSearch(query);
  }

  private List<MasterEntityDocument> executeSearch(NativeSearchQuery query) {
    query.setMaxResults(DEFAULT_PAGE_SIZE);
    SearchHits<MasterEntityDocument> searchHits =
        openSearchOperations.search(query, MasterEntityDocument.class);
    return searchHits.getSearchHits().stream()
        .map(SearchHit::getContent)
        .toList();
  }

  private AppResponse<EntityResult<MasterEntitySearchResponseDTO>> wrapInApiResponse(
      List<MasterEntityDocument> docs) {
    List<MasterEntitySearchResponseDTO> dtos = docs != null
        ? docs.stream().map(masterEntityMapper::toSearchResponse).toList()
        : List.of();
    return AppResponse.success("api.entity.search", EntityResult.of(dtos), HttpStatus.OK);
  }
}
