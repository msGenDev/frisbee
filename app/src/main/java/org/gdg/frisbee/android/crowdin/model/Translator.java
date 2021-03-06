package org.gdg.frisbee.android.crowdin.model;

import org.gdg.frisbee.android.api.model.Contributor;

public class Translator extends Contributor {
    public Translator(String login, String avatarUrl, int contributions) {
        super();
        setLogin(login);
        setHtmlUrl("https://crowdin.com/profile/"+login);
        setAvatarUrl(avatarUrl);
        setContributions(contributions);
    }
}
