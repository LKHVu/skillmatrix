package com.das.skillmatrix.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.das.skillmatrix.entity.User;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.GeneralStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findUserByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u JOIN u.department ud JOIN ud.career uc, Team t JOIN t.department td JOIN td.career tc " +
           "WHERE u.userId = :userId AND t.teamId = :teamId AND uc.careerId = tc.careerId")
    boolean checkUserInSameCareerWithTeam(@Param("userId") Long userId, @Param("teamId") Long teamId);

    boolean existsByEmailIgnoreCase(String email);
    
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);
    
    Optional<User> findByEmailIgnoreCase(String email);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :activeStatus, u.deactiveType = null, u.deactiveUntil = null, u.deActiveAt = null " +
           "WHERE u.deactiveType = 'TEMPORARY' AND u.deactiveUntil <= :now")
    int reactivateExpiredUsers(@Param("now") LocalDateTime now, @Param("activeStatus") GeneralStatus activeStatus);
}