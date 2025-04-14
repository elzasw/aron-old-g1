package cz.aron.transfagent.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.api.rest.model.UserCreate;
import cz.aron.api.rest.model.UserDef;
import cz.aron.api.rest.model.UserDelete;
import cz.aron.api.rest.model.UserEnabled;
import cz.aron.api.rest.model.UserStatus;
import cz.aron.api.rest.model.UsersResponse;

@Service
public class UserService {
	
	private static final Logger log = LoggerFactory.getLogger(UserService.class);
	
	private final JdbcTemplate jdbcTemplate;

	public UserService(JdbcTemplate jdbcTemplate) {		
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Transactional
	public UsersResponse getUsers(String query, int page, int pageSize) {
		
		var countQuery = "SELECT count(*) FROM users";
		var countParams = new Object[]{};
		var usersQuery = "SELECT * FROM users ORDER BY username LIMIT ? OFFSET ?";
		var userQueryParams = new Object[]{pageSize, page * pageSize};
		if (StringUtils.isNotBlank(query)) {
			countQuery = "SELECT count(*) FROM users WHERE username LIKE ?";
			countParams = new Object[] {query};
			usersQuery = "SELECT * FROM users WHERE username LIKE ? ORDER BY username LIMIT ? OFFSET ?";
			userQueryParams = new Object[]{query,pageSize, page * pageSize};
		}		
				
		var ur = new UsersResponse();
		var count = jdbcTemplate.queryForObject(countQuery, countParams, Integer.class);
		ur.setTotalCount(count);
		if (count>0) {
			var users = jdbcTemplate.query(usersQuery, userQueryParams,
					new BeanPropertyRowMapper<>(UserDef.class));
			ur.setItems(users);
		}		
		return ur;
	}
	
	@Transactional
	public void createUser(UserCreate userCreate) {
		var sql = "INSERT INTO USERS (username, password, enabled) VALUES (?, ?, true)";
		var hash = userCreate.getHash().startsWith("{bcrypt}") ? userCreate.getHash() : "{bcrypt}" + userCreate.getHash();
		jdbcTemplate.update(sql, ps -> {
			ps.setString(1, userCreate.getUsername());
			ps.setString(2, hash);
		});
		var roleSql = "INSERT INTO AUTHORITIES (username, authority) VALUES (?, ?)";
		jdbcTemplate.update(roleSql, ps -> {
			ps.setString(1, userCreate.getUsername());
			ps.setString(2, "ROLE_USER");
		});
		log.info("Created user {}", userCreate.getUsername());
	}
	
	@Transactional
	public void changeEnabled(UserEnabled userEnabled) {
		var sql = "UPDATE users SET enabled=? WHERE username=?";
		jdbcTemplate.update(sql, ps -> {
			ps.setBoolean(1, userEnabled.getEnabled());
			ps.setString(2, userEnabled.getUsername());
		});
		log.info("Change enabled state for user {} to {}", userEnabled.getUsername(), userEnabled.getEnabled());
	}

	@Transactional
	public void deleteUser(UserDelete userDelete) {
		var sql = "DELETE FROM authorities WHERE username=?";
		jdbcTemplate.update(sql, ps -> {
			ps.setString(1, userDelete.getUsername());
		});		
		sql = "DELETE FROM users WHERE username=?";
		jdbcTemplate.update(sql, ps -> {
			ps.setString(1, userDelete.getUsername());
		});		
		log.info("Deleted user {}", userDelete.getUsername());
	}
	
	public UserStatus getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication==null) {
			throw new IllegalStateException();
		}
		var userStatus = new UserStatus();
		userStatus.setUsername(authentication.getName());
		userStatus.setAdmin(authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));		
		return userStatus;
	}

}
