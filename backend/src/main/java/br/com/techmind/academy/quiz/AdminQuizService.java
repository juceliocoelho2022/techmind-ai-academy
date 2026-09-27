package br.com.techmind.academy.quiz;

import br.com.techmind.academy.learning.LessonRepository;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

@Service
public class AdminQuizService {

    private final LessonRepository lessonRepository;
    private final LessonQuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    public AdminQuizService(
            LessonRepository lessonRepository,
            LessonQuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            UserRepository userRepository
    ) {
        this.lessonRepository = lessonRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminQuizResponse findByLesson(String email, Long lessonId) {
        requireAdmin(email);

        var quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não cadastrado"));

        initializeOptions(quiz);
        return AdminQuizResponse.from(quiz);
    }

    @Transactional
    public AdminQuizResponse save(String email, Long lessonId, AdminQuizRequest request) {
        requireAdmin(email);

        var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada"));

        validateQuestions(request);

        var quiz = quizRepository.findByLessonId(lessonId)
                .orElseGet(() -> LessonQuiz.builder().lesson(lesson).build());

        if (quiz.getId() != null && attemptRepository.existsByQuizId(quiz.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este quiz já possui tentativas e sua estrutura está protegida"
            );
        }

        quiz.setTitle(request.title().trim());
        quiz.setDescription(normalize(request.description()));
        quiz.setPassingScore(request.passingScore());
        quiz.setXpReward(request.xpReward());
        quiz.setActive(request.active());

        var questions = new ArrayList<QuizQuestion>();

        for (int questionIndex = 0; questionIndex < request.questions().size(); questionIndex++) {
            var source = request.questions().get(questionIndex);

            var question = QuizQuestion.builder()
                    .prompt(source.prompt().trim())
                    .position(questionIndex + 1)
                    .build();

            var options = new ArrayList<QuizOption>();
            for (int optionIndex = 0; optionIndex < source.options().size(); optionIndex++) {
                var option = source.options().get(optionIndex);
                options.add(
                        QuizOption.builder()
                                .text(option.text().trim())
                                .position(optionIndex + 1)
                                .correct(option.correct())
                                .build()
                );
            }

            question.replaceOptions(options);
            questions.add(question);
        }

        quiz.replaceQuestions(questions);
        var saved = quizRepository.saveAndFlush(quiz);
        initializeOptions(saved);

        return AdminQuizResponse.from(saved);
    }

    @Transactional
    public void delete(String email, Long lessonId) {
        requireAdmin(email);

        var quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não cadastrado"));

        if (attemptRepository.existsByQuizId(quiz.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O quiz possui tentativas e não pode ser excluído"
            );
        }

        quizRepository.delete(quiz);
    }

    private void validateQuestions(AdminQuizRequest request) {
        if (request.questions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adicione pelo menos uma questão");
        }

        for (int i = 0; i < request.questions().size(); i++) {
            var question = request.questions().get(i);

            if (question.options().size() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A questão " + (i + 1) + " precisa de pelo menos duas alternativas"
                );
            }

            long correctOptions = question.options().stream()
                    .filter(AdminQuizRequest.AdminQuizOptionRequest::correct)
                    .count();

            if (correctOptions != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A questão " + (i + 1) + " deve ter exatamente uma alternativa correta"
                );
            }
        }
    }

    private void initializeOptions(LessonQuiz quiz) {
        quiz.getQuestions().forEach(question -> question.getOptions().size());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao administrador");
        }
    }
}
