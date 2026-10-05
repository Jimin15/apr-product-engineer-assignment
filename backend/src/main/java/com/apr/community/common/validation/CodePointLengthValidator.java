package com.apr.community.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodePointLengthValidator implements ConstraintValidator<CodePointLength, String> {

    private int min;
    private int max;

    @Override
    public void initialize(CodePointLength annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = countCodePoints(value);
        return length >= min && length <= max;
    }

    /** strip() 은 @NotBlank(isBlank) 와 같은 유니코드 공백 기준이다. */
    public static int countCodePoints(String value) {
        String stripped = value.strip();
        return stripped.codePointCount(0, stripped.length());
    }
}
