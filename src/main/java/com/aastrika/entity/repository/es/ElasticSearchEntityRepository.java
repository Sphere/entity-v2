package com.aastrika.entity.repository.es;

import com.aastrika.entity.document.MasterEntityDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticSearchEntityRepository extends ElasticsearchRepository<MasterEntityDocument, String> {

    List<MasterEntityDocument> findByType(String type);

    List<MasterEntityDocument> findByStatus(String status);

    List<MasterEntityDocument> findByTypeAndStatus(String type, String status);

    List<MasterEntityDocument> findByNameContaining(String name);
}

