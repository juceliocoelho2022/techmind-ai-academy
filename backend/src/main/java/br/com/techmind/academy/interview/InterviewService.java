package br.com.techmind.academy.interview;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewService {
    public InterviewFeedback evaluate(InterviewRequest request) {
        int score = Math.min(95, 60 + Math.min(30, request.answer().length() / 10));
        return new InterviewFeedback(
                score,
                "Resposta objetiva e com boa direção técnica. A versão atual usa um avaliador local; depois conectaremos um LLM.",
                List.of("Clareza", "Vocabulário técnico", "Boa estrutura"),
                List.of("Adicionar trade-offs", "Citar exemplo real", "Explicar impacto da decisão"),
                "Como você garantiria idempotência em uma API de pagamentos?"
        );
    }
}
