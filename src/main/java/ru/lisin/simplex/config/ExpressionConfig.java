package ru.lisin.simplex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.lisin.simplex.models.AdditionalExpressionModel;
import ru.lisin.simplex.models.ExpressionModel;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ExpressionConfig {

    @Bean(name = "targetFunction")
    public ExpressionModel getTargetFunction() {
        return new ExpressionModel(1.0, 1.0, 1.0, 1.0, 1.0, 0.0);
    }

    @Bean(name = "funcRestriction1")
    public ExpressionModel getFunctionRestriction1() {
        return new ExpressionModel(1.0, 1.0, 2.0, 0.0, 0.0, 4.0);
    }

    @Bean(name = "funcRestriction2")
    public ExpressionModel getFunctionRestriction2() {
        return new ExpressionModel(0.0, -2.0, -2.0, 1.0, 1.0, -6.0);
    }

    @Bean(name = "funcRestriction3")
    public ExpressionModel getFunctionRestriction3() {
        return new ExpressionModel(1.0, -1.0, 6.0, 1.0, 0.0, 12);
    }

    @Bean
    public AdditionalExpressionModel getAdditionalVariables() {
        Map<String, Double> v1 = new HashMap<>() {{
            put("b", 4.0);
            put("x1", 1.0);
            put("x2", 1.0);
            put("x3", 2.0);
            put("x4", 0.0);
            put("x5", 0.0);
        }};
        Map<String, Double> v2 = new HashMap<>() {{
            put("b", 6.0);
            put("x1", 0.0);
            put("x2", 2.0);
            put("x3", 2.0);
            put("x4", -1.0);
            put("x5", -1.0);
        }};
        Map<String, Double> v3 = new HashMap<>() {{
            put("b", 12.0);
            put("x1", 1.0);
            put("x2", -1.0);
            put("x3", 6.0);
            put("x4", 1.0);
            put("x5", 0.0);
        }};
        Map<String, Double> v = new HashMap<>() {{
            put("v1", 1.0);
            put("v2", 1.0);
            put("v3", 1.0);
            put("coeffb", 0.0);
            put("coeffx1", 0.0);
            put("coeffx2", 0.0);
            put("coeffx3", 0.0);
            put("coeffx4", 0.0);
            put("coeffx5", 0.0);
        }};
        return new AdditionalExpressionModel(v1, v2, v3, v);
    }
}
