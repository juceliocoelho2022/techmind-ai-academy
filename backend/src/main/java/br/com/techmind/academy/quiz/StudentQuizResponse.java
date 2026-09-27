package br.com.techmind.academy.quiz;

import java.util.List;

public record StudentQuizResponse(
        Long id,
        Long lessonId,
        String title,
        String description,
        int passingScore,
        int xpReward,
        List<Question> questions
) {
    public record Question(Long id, String prompt, int position, List<Option> options) {}
    public record Option(Long id, String text, int position) {}

    static StudentQuizResponse from(LessonQuiz quiz) {
        return new StudentQuizResponse(
                quiz.getId(),
                quiz.getLesson().getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getPassingScore(),
                quiz.getXpReward(),
                quiz.getQuestions().stream()
                        .map(question -> new Question(
                                question.getId(),
                                question.getPrompt(),
                                question.getPosition(),
                                question.getOptions().stream()
                                        .map(option -> new Option(
                                                option.getId(),
                                                option.getText(),
                                                option.getPosition()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }
}
