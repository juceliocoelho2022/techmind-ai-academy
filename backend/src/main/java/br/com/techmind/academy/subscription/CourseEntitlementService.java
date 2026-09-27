package br.com.techmind.academy.subscription;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CourseEntitlementService {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public CourseEntitlementService(
            UserRepository userRepository,
            SubscriptionService subscriptionService
    ) {
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan currentPlan(String email) {
        var user = findUser(email);
        if (user.getRole() == UserRole.ADMIN) {
            return SubscriptionPlan.CAREER;
        }

        return subscriptionService.currentPlanForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(String email, Course course) {
        var user = findUser(email);
        return hasAccess(user, course);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(User user, Course course) {
        if (user.getRole() == UserRole.ADMIN) return true;

        var currentPlan = subscriptionService.currentPlanForUser(user.getId());
        var requiredPlan = requiredPlan(course);

        return currentPlan.rank() >= requiredPlan.rank();
    }

    @Transactional(readOnly = true)
    public void requireAccess(String email, Course course) {
        var user = findUser(email);
        requireAccess(user, course);
    }

    @Transactional(readOnly = true)
    public void requireAccess(User user, Course course) {
        if (user.getRole() == UserRole.ADMIN) return;

        var currentPlan = subscriptionService.currentPlanForUser(user.getId());
        var requiredPlan = requiredPlan(course);

        if (currentPlan.rank() < requiredPlan.rank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta trilha requer o plano " + requiredPlan.name()
                            + ". Seu plano atual é " + currentPlan.name()
            );
        }
    }

    private SubscriptionPlan requiredPlan(Course course) {
        return course.getRequiredPlan() == null
                ? SubscriptionPlan.FREE
                : course.getRequiredPlan();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));
    }
}
