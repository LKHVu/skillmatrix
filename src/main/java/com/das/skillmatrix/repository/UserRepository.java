package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>  {

    User findUserByEmail(String email);
    
}
