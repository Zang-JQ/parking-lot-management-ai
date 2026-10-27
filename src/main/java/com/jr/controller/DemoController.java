package com.jr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Controller
public class DemoController {

    /**
     * 允许渲染的页面白名单。
     *
     * 原来这里是 {@code @RequestMapping("/{url}")} 直接把路径当模板名返回，
     * 等于任意单段路径都能渲染任意模板（/dashboard 就能绕过 .html 的角色校验）。
     * 现在只放行这 19 个页面，其它一律 404。
     */
    private static final Set<String> PAGES = Set.of(
            "login", "register",
            "userSpots", "userEntry", "userExit", "userRecords", "userMembership",
            "memberCenter", "memberEntry", "memberExit", "memberRecords", "memberRenew",
            "dashboard", "spots", "rules", "records", "members", "reports", "settings"
    );

    /**
     * 页面渲染：同时支持 /dashboard 和 /dashboard.html 两种写法。
     * 真正的访问控制在 SecurityConfig 里（角色没权限会被拦下，到这里就说明已放行）。
     */
    @RequestMapping("/{url}")
    public String url(@PathVariable String url) {
        String name = url.endsWith(".html") ? url.substring(0, url.length() - 5) : url;
        if (!PAGES.contains(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return name;
    }

    /**
     * 根路径：不能直接放业务页面，否则任何人打开 127.0.0.1:8899 就跳过登录了。
     *   未登录 → 登录页
     *   已登录 → 按角色回各自工作台（带上 username，页面靠它拿 userid）
     */
    @RequestMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login.html";
        }
        String roles = String.valueOf(auth.getAuthorities());
        String username = auth.getName();
        if (roles.contains("ROLE_ADMIN")) {
            return "redirect:/dashboard.html?username=" + username;
        }
        if (roles.contains("ROLE_MEMBER")) {
            return "redirect:/memberCenter.html?username=" + username;
        }
        return "redirect:/userSpots.html?username=" + username;
    }

    @RequestMapping("/favicon.ico")
    @ResponseBody
    public String ioc() {
        return "";
    }
}
