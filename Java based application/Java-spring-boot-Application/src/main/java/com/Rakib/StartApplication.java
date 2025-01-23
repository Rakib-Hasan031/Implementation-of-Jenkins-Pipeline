package com.Rakib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@Controller
public class StartApplication {

    @GetMapping("/")
    public String index(final Model model) {
        model.addAttribute("title", "I have successfuly built a simple Sprint Boot based Java application using Maven");
        model.addAttribute("msg", "Jenkins Pipeline for Java based application using Maven, SonarQube, Argo CD, Helm and Kubernetes. This application is deployed on to Kubernetes using Argo CD");
        return "index";
    }

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }

}
