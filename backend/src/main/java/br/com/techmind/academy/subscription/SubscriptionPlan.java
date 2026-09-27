package br.com.techmind.academy.subscription;

public enum SubscriptionPlan {
    FREE(0),
    PRO(1),
    CAREER(2);

    private final int rank;

    SubscriptionPlan(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
