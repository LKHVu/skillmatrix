package com.das.skillmatrix.repository.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.das.skillmatrix.dto.request.DepartmentFilterRequest;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;

import jakarta.persistence.criteria.Predicate;

public class DepartmentSpecification {

    public static Specification<Department> filterDepartmentsByCareer(
            Long careerId,
            DepartmentFilterRequest request
    ) {
        return (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("career").get("careerId"), careerId));

            // Status
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            } else {
                predicates.add(root.get("status").in(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
            }

            // Keyword
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            // Date Modified filter
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
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                }
                if (end != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
