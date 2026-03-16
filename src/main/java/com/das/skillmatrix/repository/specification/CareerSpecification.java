package com.das.skillmatrix.repository.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.das.skillmatrix.dto.request.CareerFilterRequest;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.GeneralStatus;

import jakarta.persistence.criteria.Predicate;

public class CareerSpecification {

    public static Specification<Career> filterCareers(CareerFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //1. Keyword search
            if (StringUtils.hasText(request.getKeyword())) {
                String keywordPattern = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordPattern);
                Predicate typePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("careerType")), keywordPattern);
                predicates.add(criteriaBuilder.or(namePredicate, typePredicate));
            }

            //2. Status filter
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            } else {
                predicates.add(root.get("status").in(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
            }

            //3. Date Modified filter
            if (StringUtils.hasText(request.getDateModified())) {
                LocalDateTime start = null;
                LocalDateTime end = null;
                LocalDateTime now = LocalDateTime.now();

                switch (request.getDateModified().toLowerCase()) {
                    case "last 7 days":
                        start = now.minusDays(7);
                        break;
                    case "last 30 days":
                        start = now.minusDays(30);
                        break;
                    case "this year":
                        start = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
                        end = LocalDateTime.of(now.getYear(), 12, 31, 23, 59, 59);
                        break;
                    case "last year":
                        start = LocalDateTime.of(now.getYear() - 1, 1, 1, 0, 0);
                        end = LocalDateTime.of(now.getYear() - 1, 12, 31, 23, 59, 59);
                        break;
                    case "any time":
                    default:
                        break;
                }

                if (start != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start));
                }
                if (end != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end));
                }
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}