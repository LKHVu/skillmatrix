package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.User;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u JOIN u.department ud JOIN ud.career uc, Team t JOIN t.department td JOIN td.career tc " +
           "WHERE u.userId = :userId AND t.teamId = :teamId AND uc.careerId = tc.careerId")
    boolean checkUserInSameCareerWithTeam(@Param("userId") Long userId, @Param("teamId") Long teamId);
}