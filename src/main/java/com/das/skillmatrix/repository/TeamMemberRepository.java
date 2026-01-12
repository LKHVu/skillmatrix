package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.TeamMember;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    boolean existsByTeam_TeamIdAndUser_UserId(Long teamId, Long userId);
    void deleteAllByTeam(Team team);
}
