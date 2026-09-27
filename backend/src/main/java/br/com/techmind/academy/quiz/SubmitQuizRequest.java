package br.com.techmind.academy.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitQuizRequest(
        @NotEmpty List<@Valid Answer> answers
) {
    public record Answer(
            @NotNull Long questionId,
            @NotNull Long optionId
    ) {}
}
