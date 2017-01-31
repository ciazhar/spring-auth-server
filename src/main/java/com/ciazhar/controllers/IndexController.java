
package com.ciazhar.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Created by hafidz on 31/01/17.
 */
@RestController
public class IndexController {

    @RequestMapping("/user")
    public Principal user(Principal principal) {
        return principal;
    }

}
