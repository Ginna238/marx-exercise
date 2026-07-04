package cn.ginna.marxexercise.admin;

import cn.ginna.marxexercise.common.entity.User;
import cn.ginna.marxexercise.common.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "ginna_238"));
        if (admin == null) {
            admin = new User();
            admin.setUsername("ginna_238");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("Ginna_238");
            admin.setRole("ADMIN");
            userMapper.insert(admin);
            log.info("超级管理员已创建: ginna_238 / admin123");
        } else if (!"ADMIN".equals(admin.getRole())) {
            admin.setRole("ADMIN");
            userMapper.updateById(admin);
            log.info("用户 ginna_238 已升级为超级管理员");
        }
    }
}
