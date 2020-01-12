package com.thelivelock.email_generator.scoreboard.model;

import java.util.List;

public class ScoreBoard {
    private List<Match> matches;

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }
}
