package com.red3.app.common.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.red3.app.common.entity.User;
import com.red3.app.common.filter.JWTAuthenticationFilter;
import com.red3.app.common.impl.UserDetailsServiceImpl;
import com.red3.app.common.service.AUserService;
import com.red3.app.common.util.R;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;

/**
 * @className: com.red3.app.common.config.SecurityConfig
 * @description: TODO
 * @author: zxl
 * @create: 2021-04-30 0:26
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    AUserService aUserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable().exceptionHandling()
//                .exceptionHandling().authenticationEntryPoint(new UnauthorizedEntryPoint())
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .authorizeRequests()//Accept:text/html,application/xhtml+x
//                .antMatchers("/login", "/all/**").permitAll()//??????????????????????????????/user?????????URL
//                .antMatchers("/user/**").hasRole("USER")//???????????????USER?????????????????????/user?????????URL
                .anyRequest().permitAll()
                .and()
                .formLogin()

                .permitAll()
                // ??????????????????json
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                        Authentication authentication) throws IOException, ServletException {
                        // ??????jwt token??????????????????
                        String name = request.getParameter("name").toString();
                        User user = aUserService.getOne(Wrappers.<User>lambdaQuery().eq(User::getUser_name, name));
                        Jwts.claims().clear();

                        String token = SecurityConstant.TOKEN_SPLIT + Jwts.builder()
//                        ?????? ???????????????
                                .setSubject(request.getParameter("name"))
//                        ???????????????,??????????????????????????????
                                .claim(SecurityConstant.AUTHORITIES, user.getIdentity())
                                //????????????
                                .setExpiration(new Date(System.currentTimeMillis() + 7 * 60 * 1000))
//                                ?????????????????????
                                .signWith(SignatureAlgorithm.HS512, SecurityConstant.JWT_SIGN_KEY)
                                .compact();
                        PrintWriter writer = response.getWriter();
                        // ??????????????????????????????
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("token",token);
                        new ObjectMapper().writeValue(writer, R.ok(map));
                        writer.flush();
                        writer.close();
                    }
                })
                // ??????????????????json
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                        AuthenticationException exception) throws IOException,
                            ServletException {

                        PrintWriter writer = response.getWriter();
                        // ??????????????????????????????
                        new ObjectMapper().writeValue(writer, R.error("??????"));
                        writer.flush();
                        writer.close();
                    }
                })
                .loginProcessingUrl("/login")
                .usernameParameter("name")
                .passwordParameter("password")
//                .successHandler(new AjaxAuthSuccessHandler())
//                .failureHandler(new AjaxAuthFailHandler())
                .permitAll();
        //??????
        http
                .cors().configurationSource(corsConfigurationSource())
                .and()
        .addFilter(new JWTAuthenticationFilter(authenticationManager(),7))
        ;
// ????????????
        http.headers().cacheControl();
    }


    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");    //???????????????*????????????????????????????????????????????????ip???????????????????????????localhost???8080?????????????????????????????????
        corsConfiguration.addAllowedHeader("*");//header???????????????header????????????????????????token???????????????*?????????token???
        corsConfiguration.addAllowedMethod("*");    //????????????????????????PSOT???GET???
        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }


}
