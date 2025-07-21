package com.model.fill.fillspace;

import com.model.fill.fillspace.beanRegister.RegisterCenter;

public interface Fill {
    static void extracted(Object cast) {
        RegisterCenter.extracted(cast);
    }
}
