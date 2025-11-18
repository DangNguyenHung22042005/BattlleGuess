package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class AnswerCorrectBroadcastPayload implements Payload {
    private String winnerName;
    private String correctAnswer;

    public AnswerCorrectBroadcastPayload(String winnerName, String correctAnswer) {
        this.winnerName = winnerName;
        this.correctAnswer = correctAnswer;
    }

    public String getWinnerName() { return winnerName; }
    public String getCorrectAnswer() { return correctAnswer; }
}
