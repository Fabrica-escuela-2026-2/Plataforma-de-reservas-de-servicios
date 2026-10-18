package com.udea.service_platform.modules.categories.service;

import com.udea.service_platform.modules.categories.dto.CategoriaRequest;
import com.udea.service_platform.modules.categories.dto.CategoriaResponse;

import java.util.List;

public interface CategoriaService {

    CategoriaResponse createCategory(CategoriaRequest request);

    List<CategoriaResponse> getAllCategories();
}
