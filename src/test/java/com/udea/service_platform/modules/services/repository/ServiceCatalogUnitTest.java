package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.modules.services.model.Service;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-09 - Consulta y filtrado del catálogo público.
 * Unit tests for {@link ServiceSpecification#buildFilter}.
 */
@ExtendWith(MockitoExtension.class)
class ServiceCatalogUnitTest {

    @Mock
    private Root<Service> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> activoPath;

    @Mock
    private Path<BigDecimal> precioPath;

    @Mock
    private Predicate activoPredicate;

    @Mock
    private Predicate minPredicate;

    @Mock
    private Predicate maxPredicate;

    @Mock
    private Predicate andPredicate;

    @Test
    void buildFilter_withNullBounds_createsSingleActivePredicate() {
        doReturn(activoPath).when(root).get("activo");
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(activoPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Specification<Service> spec = ServiceSpecification.buildFilter(null, null, null, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(andPredicate, result);

        ArgumentCaptor<Predicate[]> andCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(andCaptor.capture());
        assertEquals(1, andCaptor.getValue().length);

        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    /**
     * CP-UT-19 - minPrice = 20000 and maxPrice = null.
     * Must use inclusive lower bound (greaterThanOrEqualTo) and combine
     * [activo, minPrice] via the varargs overload cb.and(Predicate[]).
     */
    @Test
    void buildFilter_withPriceRange_createsInclusiveBounds() {
        doReturn(activoPath).when(root).get("activo");
        doReturn(precioPath).when(root).get("precio");
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(activoPredicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(minPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Specification<Service> spec =
                ServiceSpecification.buildFilter(null, null, new BigDecimal("20000"), null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(andPredicate, result);

        verify(cb).greaterThanOrEqualTo(precioPath, new BigDecimal("20000"));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));

        ArgumentCaptor<Predicate[]> andCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(andCaptor.capture());
        assertEquals(2, andCaptor.getValue().length);
    }

    @Test
    void buildFilter_withMaxPrice_createsInclusiveUpperBound() {
        doReturn(activoPath).when(root).get("activo");
        doReturn(precioPath).when(root).get("precio");
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(activoPredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(maxPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Specification<Service> spec =
                ServiceSpecification.buildFilter(null, null, null, new BigDecimal("50000"));
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(andPredicate, result);

        verify(cb).lessThanOrEqualTo(precioPath, new BigDecimal("50000"));
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));

        ArgumentCaptor<Predicate[]> andCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(andCaptor.capture());
        assertEquals(2, andCaptor.getValue().length);
    }

    @Test
    void buildFilter_withMinAndMaxPrice_createsBothBounds() {
        doReturn(activoPath).when(root).get("activo");
        doReturn(precioPath).when(root).get("precio");
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(activoPredicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(minPredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(BigDecimal.class))).thenReturn(maxPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Specification<Service> spec = ServiceSpecification.buildFilter(
                null, null, new BigDecimal("20000"), new BigDecimal("50000"));
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(andPredicate, result);

        verify(cb).greaterThanOrEqualTo(precioPath, new BigDecimal("20000"));
        verify(cb).lessThanOrEqualTo(precioPath, new BigDecimal("50000"));

        ArgumentCaptor<Predicate[]> andCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(andCaptor.capture());
        assertEquals(3, andCaptor.getValue().length);
    }
}
