package com.fileprocessor.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fileprocessor.entity.User;
import com.fileprocessor.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        List<com.fileprocessor.entity.Role> roles = userMapper.selectRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream()
                .map(com.fileprocessor.entity.Role::getRoleCode)
                .collect(Collectors.toList());

        return UserPrincipal.create(user, roleCodes);
    }
}
