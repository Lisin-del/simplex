package ru.lisin.simplex.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SimplexController {
	
	@GetMapping("/home")
	public RedirectView getHomePage() {
		RedirectView redirect = new RedirectView();
		redirect.setUrl("/html/HomePage.html");
//		return "redirect:/html/HomePage.html";
		return redirect; 
	}
	
}
