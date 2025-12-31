package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.UpskillDocument;

@Repository
public interface UpskillDocumentRepository extends JpaRepository<UpskillDocument, Long> {
    
}
