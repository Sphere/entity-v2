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

  List<MasterEntityDocument> fuzzySearchByName(String name);

  List<MasterEntityDocument> phraseSearchByName(String name);

  List<MasterEntityDocument> fuzzyPhraseSearchByName(String name);

  List<MasterEntityDocument> fuzzySearch(String searchText);

  List<MasterEntityDocument> searchByCode(String code);

  AppResponse<EntityResult<MasterEntitySearchResponseDTO>> findEntitiesBySearchParameter(SearchDTO searchDTO);
}
