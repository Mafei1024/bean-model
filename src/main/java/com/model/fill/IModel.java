package com.model.fill;

import com.model.fill.fillspace.Fill;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author machunfei
 */
public interface IModel<T> {
    default T model() {
        IModel cast;
        try {
            Class<? extends IModel> thisClass = this.getClass();
            Constructor<? extends IModel> declaredConstructor =
                    thisClass.getDeclaredConstructor();
            IModel newAutowriteBean = declaredConstructor.newInstance();
            cast = thisClass.cast(newAutowriteBean);
        } catch (NoSuchMethodException
                | InvocationTargetException
                | InstantiationException
                | IllegalAccessException e) {
            throw new RuntimeException(" unable to init this bean");
        }
        Fill.extracted(cast);
        return (T) cast;
    }
}
