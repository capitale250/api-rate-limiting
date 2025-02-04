package com.capitale.ratelimit.repository;

import com.capitale.ratelimit.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

//	Optional<User> findById(Integer userId);
}
