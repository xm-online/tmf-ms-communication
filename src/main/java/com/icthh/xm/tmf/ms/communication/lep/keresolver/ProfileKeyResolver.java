package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import static java.util.Optional.ofNullable;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ProfileKeyResolver implements LepKeyResolver {
    private static final String PROFILE_KEY = "profile";

    private String getProfile(LepMethod method) {
        HttpServletRequest request =
            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        return ofNullable(request.getHeader(PROFILE_KEY))
            .or(() -> ofNullable(method.getParameter(PROFILE_KEY, String.class)))
            .orElse(Strings.EMPTY);
    }

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(getProfile(method));
    }
}
