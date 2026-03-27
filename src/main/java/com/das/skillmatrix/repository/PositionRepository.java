package com.das.skillmatrix.repository;

import java.util.List;

import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByStatus(GeneralStatus status);
    List<Position> findByPositionIdInAndStatus(List<Long> positionIds, GeneralStatus status);
}