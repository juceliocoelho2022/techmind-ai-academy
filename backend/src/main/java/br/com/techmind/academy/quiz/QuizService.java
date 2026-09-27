package br.com.techmind.academy.quiz;

import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.subscription.CourseEntitlementService;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
public class QuizService {

    private final LessonQuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseEntitlementService entitlementService;

    public QuizService(
            LessonQuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            CourseEntitlementService entitlementService
    ) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.entitlementService = entitlementService;
    }

    @Transactional(readOnly = true)
    public StudentQuizResponse getQuiz(String email, Long lessonId) {
        var user = findUser(email);
        var quiz = findActiveQuiz(lessonId);

        entitlementService.requireAccess(user, quiz.getLesson().getModule().getCourse());
        requireEnrollmentOrAdmin(user.getRole(), email, quiz);
        initializeOptions(quiz);

        return StudentQuizResponse.from(quiz);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptSummaryResponse> history(String email, Long lessonId) {
        var user = findUser(email);
        var quiz = findActiveQuiz(lessonId);

        entitlementService.requireAccess(user, quiz.getLesson().getModule().getCourse());
        requireEnrollmentOrAdmin(user.getRole(), email, quiz);

        return attemptRepository.findByQuizIdAndUserEmailOrderBySubmittedAtDesc(quiz.getId(), email)
                .stream()
                .map(QuizAttemptSummaryResponse::from)
                .toList();
    }

    @Transactional
    public QuizAttemptResponse submit(String email, Long lessonId, SubmitQuizRequest request) {
        var user = findUser(email);
        var quiz = findActiveQuiz(lessonId);
        initializeOptions(quiz);

        var course = quiz.getLesson().getModule().getCourse();
        entitlementService.requireAccess(user, course);
        var courseId = course.getId();

        var enrollment = enrollmentRepository.findForUpdateByUserEmailAndCourseId(email, courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Matricule-se na trilha antes de responder ao quiz"
                ));

        var questions = quiz.getQuestions();
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quiz sem questões");
        }

        if (request.answers().size() != questions.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responda todas as questões");
        }

        var submittedByQuestion = new HashMap<Long, Long>();
        for (var answer : request.answers()) {
            if (submittedByQuestion.put(answer.questionId(), answer.optionId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questão respondida mais de uma vez");
            }
        }

        var validQuestionIds = new HashSet<Long>();
        var results = new ArrayList<QuizAttemptResponse.QuestionResult>();
        var answerEntities = new ArrayList<QuizAttemptAnswer>();
        int correctAnswers = 0;

        for (var question : questions) {
            validQuestionIds.add(question.getId());

            var selectedOptionId = submittedByQuestion.get(question.getId());
            if (selectedOptionId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responda todas as questões");
            }

            var selected = question.getOptions().stream()
                    .filter(option -> option.getId().equals(selectedOptionId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Alternativa inválida para a questão " + question.getId()
                    ));

            var correctOption = question.getOptions().stream()
                    .filter(QuizOption::getCorrect)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Questão sem alternativa correta configurada"
                    ));

            boolean correct = Boolean.TRUE.equals(selected.getCorrect());
            if (correct) correctAnswers++;

            results.add(new QuizAttemptResponse.QuestionResult(
                    question.getId(),
                    selected.getId(),
                    correctOption.getId(),
                    correct
            ));

            answerEntities.add(
                    QuizAttemptAnswer.builder()
                            .question(question)
                            .option(selected)
                            .correct(correct)
                            .build()
            );
        }

        if (!validQuestionIds.equals(submittedByQuestion.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foram enviadas questões inválidas");
        }

        var score = BigDecimal.valueOf(correctAnswers)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);

        boolean passed = score.compareTo(BigDecimal.valueOf(quiz.getPassingScore())) >= 0;
        boolean alreadyRewarded = attemptRepository.existsByQuizIdAndUserIdAndPassedTrue(
                quiz.getId(),
                user.getId()
        );

        int xpAwarded = passed && !alreadyRewarded ? quiz.getXpReward() : 0;

        if (xpAwarded > 0) {
            enrollment.setXp(enrollment.getXp() + xpAwarded);
            enrollmentRepository.save(enrollment);
        }

        var attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(user)
                .score(score)
                .correctAnswers(correctAnswers)
                .totalQuestions(questions.size())
                .passed(passed)
                .xpAwarded(xpAwarded)
                .build();

        answerEntities.forEach(answer -> {
            answer.setAttempt(attempt);
            attempt.getAnswers().add(answer);
        });

        var saved = attemptRepository.save(attempt);

        return new QuizAttemptResponse(
                saved.getId(),
                score,
                correctAnswers,
                questions.size(),
                passed,
                xpAwarded,
                saved.getSubmittedAt(),
                results
        );
    }

    private LessonQuiz findActiveQuiz(Long lessonId) {
        var quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não disponível"));

        if (!Boolean.TRUE.equals(quiz.getActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não disponível");
        }

        return quiz;
    }

    private br.com.techmind.academy.user.User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private void requireEnrollmentOrAdmin(UserRole role, String email, LessonQuiz quiz) {
        if (role == UserRole.ADMIN) return;

        var courseId = quiz.getLesson().getModule().getCourse().getId();
        if (!enrollmentRepository.existsByUserEmailAndCourseId(email, courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Matricule-se na trilha para acessar o quiz"
            );
        }
    }

    private void initializeOptions(LessonQuiz quiz) {
        quiz.getQuestions().forEach(question -> question.getOptions().size());
    }
}
