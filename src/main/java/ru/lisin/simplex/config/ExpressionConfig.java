package ru.lisin.simplex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.lisin.simplex.models.ExpressionModel;

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

}
