package br.com.techmind.academy.interview;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewServiceTest {

    private final InterviewService service = new InterviewService();

    @Test
    void shouldReturnStructuredFeedback() {
        var request = new InterviewRequest(
                "Java Backend Pleno",
                "Eu usaria uma chave de idempotência persistida e devolveria a resposta já processada para requisições repetidas."
        );

        var feedback = service.evaluate(request);

        assertThat(feedback.score()).isBetween(60, 95);
        assertThat(feedback.strengths()).isNotEmpty();
        assertThat(feedback.improvements()).contains("Adicionar trade-offs");
        assertThat(feedback.nextQuestion()).contains("idempotência");
    }
}
