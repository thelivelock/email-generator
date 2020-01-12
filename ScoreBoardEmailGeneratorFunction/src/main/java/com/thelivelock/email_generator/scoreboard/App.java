package com.thelivelock.email_generator.scoreboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.thelivelock.email_generator.scoreboard.model.Match;
import com.thelivelock.email_generator.scoreboard.model.ScoreBoard;
import com.thelivelock.email_generator.scoreboard.model.Team;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

    private static final String NoLogoTeam = "No Logo";

    private static final Map<String, String> teamUrl = Map.ofEntries(
            Map.entry("La Lakers", "https://www.nba.com/.element/img/1.0/teamsites/logos/teamlogos_500x500/lal.png"),
            Map.entry("Golden State Warriors", "https://www.freepnglogos.com/uploads/warriors-png-logo/golden-state-warriors-png-logo-9.png"),
            Map.entry(NoLogoTeam, "http://pngimages.net/sites/default/files/basketball-logo-png-image-80566.png")
    );

    public Object handleRequest(final Object input, final Context context) {
        ScoreBoard scoreBoard = getScoreBoardFromLambdaInput(input);
        // todo check if scoreboard null
        setTeamLogo(scoreBoard);
        String html = convertToHtml(scoreBoard);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        String output = String.format("{ \"html\": \"%s\"}", html);
        return new GatewayResponse(output, headers, 200);
    }

    private String convertToHtml(ScoreBoard scoreBoard) {
        TemplateLoader loader = new ClassPathTemplateLoader("/", ".html");
        Handlebars handlebars = new Handlebars(loader);
        Template template = null;
        String htmlScoreBoard = "";
        try {
            template = handlebars.compile("scoreboard");
            htmlScoreBoard = template.apply(scoreBoard);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return htmlScoreBoard;
    }

    private void setTeamLogo(ScoreBoard scoreBoard) {
        List<Match> matches = scoreBoard.getMatches();
        matches.forEach(this::setTeamLogo);
    }

    private void setTeamLogo(Match match) {
        Team home = match.getHome();
        setTeamLogo(home);
        Team away = match.getAway();
        setTeamLogo(away);
    }

    private void setTeamLogo(Team team) {
        if (teamUrl.containsKey(team.getName())) {
            team.setLogoUrl(teamUrl.get(team.getName()));
        }
        else {
            team.setLogoUrl(teamUrl.get(NoLogoTeam));
        }
    }

    private ScoreBoard getScoreBoardFromLambdaInput(Object lambdaInput) {
        ScoreBoard scoreBoard = null;
        if (!(lambdaInput instanceof LinkedHashMap)) {
            return null;
        }

        LinkedHashMap<String, String> lambdaMapInput = (LinkedHashMap<String, String>) lambdaInput;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (lambdaMapInput.get("body") == null) {
                return null;
            }

            String body = lambdaMapInput.get("body");
            body = java.net.URLDecoder.decode(body, StandardCharsets.UTF_8.name());
            scoreBoard = objectMapper.readValue(body, ScoreBoard.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scoreBoard;
    }
}
