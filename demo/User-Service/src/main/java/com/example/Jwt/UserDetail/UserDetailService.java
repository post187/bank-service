package com.example.Jwt.UserDetail;
import com.example.Exception.ResourceNotFoundException;
import com.example.Model.Entity.User;
import com.example.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService{
    private final UserRepository userRepository;
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Email or Username does not exist, please try again: " + username));
        return UserPrinciple.build(user);
    }

    @Transactional
    public UserDetails loadUserByPhone(String phone) {
        User user = userRepository.findUserByAuthId(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found, phone and password: " + phone));

        return UserPrinciple.build(user);
    }
}
