package com.alekseyruban.timemanagerapp.analytics_service.service;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit.UserCreatedEvent;
import com.alekseyruban.timemanagerapp.analytics_service.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.alekseyruban.timemanagerapp.analytics_service.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void createUser(UserCreatedEvent event) {
        User user = new User();
        user.setDomainId(event.getUserId());
        userRepository.save(user);
    }
}
