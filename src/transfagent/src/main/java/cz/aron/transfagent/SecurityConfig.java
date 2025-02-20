package cz.aron.transfagent;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(value="ui.enabled", havingValue = "true")
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	private static Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Override
	protected void configure(HttpSecurity http) throws Exception {		
		 http.authorizeRequests()
         .antMatchers("/api/**","/index.html","/").access("hasRole('ROLE_USER')")
         // equivalent to <http auto-config="true">
         .and().formLogin()//.successForwardUrl("/index.html")
         .and().httpBasic()
         .and().logout()
         // CSRF is enabled by default (will discuss later)
         .and().csrf().disable();
		 log.info("Security configured");
	}
		
	@Bean
	UserDetailsManager users(DataSource dataSource) {
		UserDetails user = User.builder()
			.username("user")
			.password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
			.roles("USER")
			.build();
		UserDetails admin = User.builder()
			.username("admin")
			.password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
			.roles("USER", "ADMIN")
			.build();
		JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);		
		try {
			users.loadUserByUsername("user");
		} catch (UsernameNotFoundException e) {
			users.createUser(user);			
		}
		try {		
			users.loadUserByUsername("admin");
		} catch  (UsernameNotFoundException e) {
			users.createUser(admin);
		}
		return users;
	}

}
