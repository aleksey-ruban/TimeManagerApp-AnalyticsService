package com.alekseyruban.timemanagerapp.analytics_service.service;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit.UserCreatedEvent;
import com.alekseyruban.timemanagerapp.analytics_service.entity.User;
import com.alekseyruban.timemanagerapp.analytics_service.utils.RetryOptimisticLock;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.alekseyruban.timemanagerapp.analytics_service.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @RetryOptimisticLock
    @Transactional
    public void createUser(UserCreatedEvent event) {
        User user = new User();
        user.setDomainId(event.getUserId());
        userRepository.save(user);
    }
}
