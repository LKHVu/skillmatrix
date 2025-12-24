package com.das.skillmatrix.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{

	private final UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {	
		User userInDB = this.userRepository.findUserByEmail(email);
		if (userInDB == null) throw new UsernameNotFoundException("User isn't found");
		return org.springframework.security.core.userdetails.User
				.withUsername(userInDB.getEmail())
                .password(userInDB.getPasswordHash())
                .roles(userInDB.getRole())
                .build();
	}
	
}
