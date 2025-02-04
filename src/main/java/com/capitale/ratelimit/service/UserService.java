package com.capitale.ratelimit.service;


import com.capitale.ratelimit.exception.UserNotFoundException;
import com.capitale.ratelimit.model.User;
import com.capitale.ratelimit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
	
	@Autowired
	UserRepository userRepository;
	
	@Cacheable(value="userList", key="#user")
	public User getUser(String  user) {
		User userOp = userRepository.findById(Integer.parseInt(user)).get();
		if(userOp.getId()!=null) {
			return userOp;
		} else {
			throw new UserNotFoundException("user not found");
		}
	}
	
	@CacheEvict(value="userList", allEntries = true)
	@Scheduled(fixedDelayString = "${caching.spring.userListTTL}", initialDelay = 50000)
	public void deleteUserList() {
		LOG.info("Evict User List");
	}
}
