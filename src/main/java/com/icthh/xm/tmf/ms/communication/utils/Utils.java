package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

    public static int parseIntOrException(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("error.parameter.invalid", "Invalid parameter value " + value);
        }
    }
}
