package com.model.fill;

import com.model.fill.fillspace.beanRegister.RegisterCenter;

/**
 * @author machunfei
 */
public abstract class ModelUp {
    public ModelUp() {
        RegisterCenter.extracted(this);
    }
}