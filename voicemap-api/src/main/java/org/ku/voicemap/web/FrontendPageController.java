package org.ku.voicemap.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendPageController {

    @GetMapping({"/voice-chat", "/oauth/callback"})
    public String voiceChat() {
        return "forward:/voice-chat.html";
    }
}
