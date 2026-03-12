package com.aastrika.entity.service;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;

import java.util.List;

public interface MasterEntityEsService {

  void saveEntityDetailsInES(List<EntitySheetRow> entitySheetRowList, String entityType);

  List<MasterEntityDocument> phraseSearchByName(String name);

  List<MasterEntityDocument> fuzzyPhraseSearchByName(String name);

  AppResponse<EntityResult<MasterEntitySearchResponseDTO>> findEntitiesBySearchParameter(SearchDTO searchDTO);
}
