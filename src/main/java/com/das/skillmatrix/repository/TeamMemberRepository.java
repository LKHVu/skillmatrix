package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.TeamMember;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    boolean existsByTeamIdAndUserId(Long teamId, Long userId);
}
