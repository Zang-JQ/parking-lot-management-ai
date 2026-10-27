package com.jr.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.jr.entity.SysUser;
import com.jr.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. 放行：登录/注册页（带不带 .html 都放行）+ 静态资源
                        .requestMatchers("/login.html", "/register.html", "/login", "/register",
                                "/js/**", "/css/**", "/favicon.ico").permitAll()
                        // 2. 放行：登录、注册、退出这几个接口本身
                        .requestMatchers("/SysUser/doLogin", "/SysUser/addUser", "/SysUser/logout").permitAll()
                        // 3. 管理员页面：只有管理员能进（带 .html 和不带的都要管，否则 /dashboard 能绕过）
                        .requestMatchers("/dashboard.html", "/dashboard",
                                "/reports.html", "/reports",
                                "/settings.html", "/settings",
                                "/members.html", "/members",
                                "/records.html", "/records",
                                "/spots.html", "/spots",
                                "/rules.html", "/rules").hasRole("ADMIN")
                        // 4. 普通用户页面：只有普通用户能进
                        .requestMatchers("/user*.html", "/user*").hasRole("USER")
                        // 5. 会员页面：只有会员能进
                        .requestMatchers("/member*.html", "/member*").hasRole("MEMBER")
                        // 6. 其余（根路径 + 所有 Ajax 业务接口）一律必须登录
                        //    之前这里是 permitAll()，等于不登录就能调入场/出场/查会员库，是最大的安全漏洞
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 未登录访问受保护资源 → 跳登录页
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/login.html"))
                        // 登录了但角色无权 → 跳登录页
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/login.html"))
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    // BCrypt 密码编码器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 认证管理器：登录接口里手动调用它校验身份
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CommandLineRunner passwordMigration(PasswordEncoder encoder, SysUserMapper sysUserMapper) {
        return args -> {
            List<SysUser> users = sysUserMapper.selectAll();
            for (SysUser u : users) {
                String pwd = u.getPassword();
                if (pwd != null && !pwd.startsWith("$2")) {
                    u.setPassword(encoder.encode(pwd));
                    sysUserMapper.updatePassword(u);
                }
            }
            System.out.println("===== 密码迁移完成 =====");
        };
    }
}
