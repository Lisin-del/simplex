package ru.lisin.simplex.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.lisin.simplex.models.ExpressionModel;

@Controller
public class SimplexController {
    @Autowired
    @Qualifier(value = "targetFunction")
    ExpressionModel targetFunction;

    @Autowired
    @Qualifier(value = "funcRestriction1")
    ExpressionModel funcRestriction1;

	@Autowired
	@Qualifier(value = "funcRestriction2")
	ExpressionModel funcRestriction2;

	@Autowired
	@Qualifier(value = "funcRestriction3")
	ExpressionModel funcRestriction3;

    @GetMapping("/home")
    public String getHomePage() {
        return "redirect:/html/HomePage.html";
    }

	// КОСТЫЛЬ
    @PostMapping("/process")
    public String processSimplexExpression(
            @RequestParam double x01,
            @RequestParam double x02,
            @RequestParam double x03,
            @RequestParam double x04,
            @RequestParam double x05,

            @RequestParam double x11,
            @RequestParam double x12,
            @RequestParam double x13,
            @RequestParam double x14,
            @RequestParam double x15,
            @RequestParam double result1,

            @RequestParam double x21,
            @RequestParam double x22,
            @RequestParam double x23,
            @RequestParam double x24,
            @RequestParam double x25,
            @RequestParam double result2,

            @RequestParam double x31,
            @RequestParam double x32,
            @RequestParam double x33,
            @RequestParam double x34,
            @RequestParam double x35,
            @RequestParam double result3
    ) {
		targetFunction.setX1(x01);
		targetFunction.setX2(x02);
		targetFunction.setX3(x03);
		targetFunction.setX4(x04);
		targetFunction.setX5(x05);

		funcRestriction1.setX1(x11);
		funcRestriction1.setX2(x12);
		funcRestriction1.setX3(x13);
		funcRestriction1.setX4(x14);
		funcRestriction1.setX5(x15);
		funcRestriction1.setResult(result1);

		funcRestriction2.setX1(x21);
		funcRestriction2.setX2(x22);
		funcRestriction2.setX3(x23);
		funcRestriction2.setX4(x24);
		funcRestriction2.setX5(x25);
		funcRestriction2.setResult(result2);

		funcRestriction3.setX1(x31);
		funcRestriction3.setX2(x32);
		funcRestriction3.setX3(x33);
		funcRestriction3.setX4(x34);
		funcRestriction3.setX5(x35);
		funcRestriction3.setResult(result3);

        return "";
    }

}
