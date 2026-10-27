package com.jr.controller;

import com.jr.entity.SysUser;
import com.jr.service.AiAssistantService;
import com.jr.service.SysUserService;
import com.jr.util.ResultHelper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jr.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@RestController
@RequestMapping("SysUser/")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ResultHelper  resultHelper;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AiAssistantService aiAssistantService;

    /*按用户名查用户：页面跳转后拿 userid/realname 用。
      注意：必须返回 ResultHelper（包一层 t），前端回调 data.t.xxx 才能直接用*/
    @RequestMapping("findByUsername")
    public ResultHelper findByUsername(String username){
        SysUser user = sysUserMapper.selectByUsername(username);
        resultHelper.setBool(user != null);
        resultHelper.setT(user);
        return resultHelper;
    }


    /*=======================================*/
    @RequestMapping("doLogin")
    public ResultHelper doLogin(String username, String password, HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            // 关键：把认证状态保存进 Session，否则跳转页面时 Security 不认你登录过
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
            repository.saveContext(context, request, response);

            SysUser user = sysUserMapper.selectByUsername(username);
            session.setAttribute("loginUser", user);
            resultHelper.setBool(true);
            resultHelper.setT(user);
        } catch (Exception e) {
            resultHelper.setBool(false);
        }
        return resultHelper;
    }

    /*========================================*/

    @RequestMapping("logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 0. 退出登录时顺手清掉 AI 助手的对话记忆（记忆按 userId 存，否则会一直留着）
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            SysUser loginUser = sysUserMapper.selectByUsername(auth.getName());
            if (loginUser != null) {
                aiAssistantService.clearMemory(loginUser.getId());
            }
        }

        // 1. 清 Security 认证态
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        // 2. 再清 Session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // 3. 回登录页
        response.sendRedirect("/login.html");
    }

    /*========================================*/


    @RequestMapping("findstatus")
    public String findstatus(SysUser sysUser){
        String findstats = sysUserService.findstats(sysUser.getUsername());
        return findstats;
    }

    @RequestMapping("updatestatsopen")
    public Boolean updatestatsopen(SysUser sysUser){
        Boolean b = sysUserService.updatestatsopen(sysUser.getUsername());
        return b;
    }

    @RequestMapping("updatestatsclose")
    public Boolean updatestatsclose(SysUser sysUser){
        Boolean b = sysUserService.updatestatsclose(sysUser.getUsername());
        return b;
    }

    @RequestMapping("findSysUserById")
    public SysUser findSysUserById(Integer id){
        return sysUserService.findSysUserById(id);
    }

    @RequestMapping("addUser")
    public Boolean addUser(SysUser sysUser){
        // 注册时直接 BCrypt 加密入库，注册完即可登录（不再依赖启动时的密码迁移）
        sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        return sysUserService.addUser(sysUser);
    }


}
