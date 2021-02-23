package com.samuelfrazee.jsettlersrest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NicknameController {

    @GetMapping("/nickname")
    public Nickname nickname() {
        return new Nickname(JsettlersrestApplication.jsettlersClient.getNickname());
    }

}
