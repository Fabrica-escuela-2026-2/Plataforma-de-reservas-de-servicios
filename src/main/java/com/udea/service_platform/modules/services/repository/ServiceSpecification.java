package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.modules.categories.model.Categoria;
import com.udea.service_platform.modules.services.model.Service;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ServiceSpecification {

    private ServiceSpecification() {
    }

    public static Specification<Service> buildFilter(Long providerId, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            predicates.add(cb.equal(root.get("activo"), true));

            if (providerId != null) {
                predicates.add(cb.equal(root.get("idProveedor"), providerId));
            }

            if (category != null && !category.isBlank()) {
                Join<Service, Categoria> categoriaJoin =
                        root.join("categoria", JoinType.LEFT);
                predicates.add(cb.equal(
                        cb.lower(categoriaJoin.<String>get("nombre")),
                        category.toLowerCase()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("precio"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precio"), maxPrice));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
