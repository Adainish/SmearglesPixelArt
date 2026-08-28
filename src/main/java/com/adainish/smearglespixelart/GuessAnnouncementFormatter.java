package com.adainish.smearglespixelart;

final class GuessAnnouncementFormatter {
    private GuessAnnouncementFormatter() {
    }

    static String correctGuessAnnouncement(String playerName, int points, int totalPoints) {
        return "<green><bold>" + MiniMessageText.escape(playerName) + "</bold></green> "
            + "<gray>got the answer for</gray> <gold>" + points + "</gold> <gray>point" + (points == 1 ? "" : "s")
            + " (total: " + totalPoints + ").</gray>";
    }
}
