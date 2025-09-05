package irt.components.config;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import irt.components.services.IrtUrlAuthenticationSuccessHandler;
import irt.components.services.UserService;

@Configuration
public class WebSecurityConfig {

	@Autowired UserService userService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    	http.authorizeHttpRequests(
    			request->
    			request.requestMatchers(HttpMethod.GET,
										"/",
										"/bom",
										"/files",
										"/files/**",
										"/css/**",
										"/js/**",
										"/calibration/**",
										"/old",
										"/old/get_fields",
										"/eco",
										"/rma/**",
										"/calibration/rest/**",
										"/calibration/biasing/rest/**",
										"/inventory",
										"/images/**",
										"/wo/**",
										"/wip/**",
										"/production/**",
										"/btr/**")
    			.permitAll()
    			.requestMatchers(HttpMethod.POST,
											"/components",
											"/components/single",
											"/bom/search",
											"/bom/components",
											"/old/**",
											"/eco",
											"/eco/get_files/**",
											"/eco/show_img",
											"/rma/**",
											"/calibration/rest/**",
											"/serial_port/rest/**",
											"/inventory",
											"/create/rest/**",
											"/calibration/biasing/rest/save",
											"/wo/**",
											"/btr/**")
    			.permitAll()
    			.anyRequest().authenticated())
    	.formLogin(
    			form->
    			form
    			.successHandler(new IrtUrlAuthenticationSuccessHandler()).permitAll())
		.logout(logout->logout.logoutSuccessUrl("/").permitAll())
	    .csrf(csrf->csrf.disable())
	    .rememberMe(rememberMe->rememberMe.userDetailsService(userService).tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(21)));

		return http.build();
	}

    @Bean
    public ProviderManager authenticationProvider() {

    	DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
        authProvider.setPasswordEncoder(encoder());
        return new ProviderManager(authProvider);
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
}
