package uk.gov.wildfyre.SDS.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.wildfyre.SDS.HapiProperties;


@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {



    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(HapiProperties.getAppUser())
                .password(HapiProperties.getAppPassword())
                .roles("ACTUATOR");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/").permitAll().and().csrf().disable();

/* REVISIT
        http
                .authorizeRequests()
                .antMatchers("/error").permitAll()
                .antMatchers("/hawtio/**").hasRole("ACTUATOR")
                .antMatchers("/jolokia/**").hasRole("ACTUATOR")
                .and().httpBasic();
*/
    }
}
