package com.adainish.smearglespixelart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RoundScoring {
    private RoundScoring() {
    }

    static int pointsForCorrectGuessOrder(int correctGuessesAlreadyAwarded) {
        return Math.max(1, 10 - Math.max(0, correctGuessesAlreadyAwarded));
    }

    static List<String> winnerNames(Map<String, Integer> pointsByPlayer) {
        int highScore = Integer.MIN_VALUE;
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pointsByPlayer.entrySet()) {
            int score = entry.getValue();
            if (score > highScore) {
                highScore = score;
                winners.clear();
                winners.add(entry.getKey());
                continue;
            }
            if (score == highScore) {
                winners.add(entry.getKey());
            }
        }
        return winners;
    }
}
