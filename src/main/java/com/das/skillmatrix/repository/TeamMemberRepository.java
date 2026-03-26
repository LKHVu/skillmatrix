package com.das.skillmatrix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.TeamMember;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    boolean existsByTeam_TeamIdAndUser_UserId(Long teamId, Long userId);
    void deleteAllByTeam(Team team);
    boolean existsByTeam_TeamId(Long teamId);
    long countByTeam_TeamId(Long teamId);
    @EntityGraph(attributePaths = {"user", "team", "position"})
    Page<TeamMember> findByTeam_TeamId(Long teamId, Pageable pageable);
    List<TeamMember> findByUser_UserId(Long userId);
    Optional<TeamMember> findByTeam_TeamIdAndUser_UserId(Long teamId, Long userId);
}
