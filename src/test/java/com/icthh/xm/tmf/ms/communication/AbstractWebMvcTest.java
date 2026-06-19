package com.icthh.xm.tmf.ms.communication;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Abstract test for extension for any WebMvc test.
 * Marks test with junit @Tag
 */
@ExtendWith(SpringExtension.class)
@Tag("com.icthh.xm.tmf.ms.communication.AbstractWebMvcTest")
public abstract class AbstractWebMvcTest {
}
