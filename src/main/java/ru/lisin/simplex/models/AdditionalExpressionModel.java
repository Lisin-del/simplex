package ru.lisin.simplex.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AdditionalExpressionModel {
    private Map<String, Double> v1;
    private Map<String, Double> v2;
    private Map<String, Double> v3;
    private Map<String, Double> v;
    private char targetFunctionSymbol = '+';
    private char restrictionSymbols = '-';

    public AdditionalExpressionModel(
            Map<String, Double> v1,
            Map<String, Double> v2,
            Map<String, Double> v3,
            Map<String, Double> v
    ) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v = v;
    }
}
