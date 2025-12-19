package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.RefreshToken;
import com.das.skillmatrix.entity.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
	void deleteByRefreshTokenAndUser(String refreshToken, User user);
}
