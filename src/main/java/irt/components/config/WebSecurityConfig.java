package irt.components.config;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import irt.components.services.IrtUrlAuthenticationSuccessHandler;
import irt.components.services.UserService;


@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired UserDetailsService userDetailsService;
	@Autowired UserService userService;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http
			.authorizeRequests()
			.antMatchers(HttpMethod.GET,
										"/",
										"/bom",
										"/files",
										"/css/**",
										"/js/**",
										"/calibration/**",
										"/old",
										"/old/get_fields",
										"/rma",
										"/rma/search",
										"/inventory",
										"/files/**",
										"/images/**",
										"/btr/**")
			.permitAll()

			.antMatchers(HttpMethod.POST,
											"/components",
											"/components/single",
											"/bom/search",
											"/bom/components",
											"/old/**",
											"/rma/search",
											"/rma/comments",
											"/rma/rest/**",
											"/calibration/rest/**",
											"/inventory",
											"/create/rest/**",
											"/serial_port/rest/**",
											"/btr/**")
			.permitAll()

			.anyRequest().authenticated()
				.and()
			.formLogin()
//				.loginPage("/")
				.successHandler(irtUrlAuthenticationSuccessHandler())
			.permitAll()
				.and()
			.logout()
        	.logoutSuccessUrl("/")
			.permitAll()
				.and()
	        .csrf().disable()
	        .headers()
				.frameOptions().sameOrigin()
				.httpStrictTransportSecurity().disable()
			.and()
            	.rememberMe().userDetailsService(userService);
	}

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

    	DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(encoder());
        return authProvider;
    }
     
    @Bean
    public PasswordEncoder encoder() {
        return new PasswordEncoder() {
			
			@Override
			public boolean matches(CharSequence userEntry, String encodedPassword) {

				if(encodedPassword.equals("?"))
					return  true;

				final String dbPassword = new String(Base64.getDecoder().decode(encodedPassword));

				return dbPassword.equals(userEntry);
			}
			
			@Override
			public String encode(CharSequence rawPassword) {
		        return rawPassword==null || rawPassword.equals("?")
		        		? "?"
		        				: Base64.getEncoder().encodeToString(rawPassword.toString().getBytes());
		    }
		};
    }

    public AuthenticationSuccessHandler irtUrlAuthenticationSuccessHandler(){
        return new IrtUrlAuthenticationSuccessHandler();
    }
}
