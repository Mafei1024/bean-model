package com.model.fill.fillspace.beanRegister;

import com.model.annotation.ModelScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
public class Register implements ImportSelector, ApplicationContextAware {
    private static Map<String, Object> NEW_BEAN_MAP_FIELD;
    private static ApplicationContext applicationContext;

    private static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        MergedAnnotations annotations = importingClassMetadata.getAnnotations();
        AtomicInteger fieldLength = new AtomicInteger(0);
        // 初始化容量,防止使用时，无端扩容，增加等待时长
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String modelPath : new ArrayList<>(Arrays.asList(Arrays.stream(
                annotations.get(ModelScan.class).getStringArray("value"))
                .distinct()
                .toArray(String[]::new))).toArray(new String[0])) {
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources(modelPath.replaceAll("\\.", "/"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (resources == null) {
                continue;
            }
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (url == null) {
                    continue;
                }
                String filePath = null;
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    try {
                        filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                if (filePath != null) {
                    selectPackage(filePath, modelPath, fieldLength);
                }
            }
        }
        NEW_BEAN_MAP_FIELD = new ConcurrentHashMap<>(fieldLength.get());
        return new String[0];
    }

    private void selectPackage(String filePath, String modelPath, AtomicInteger fieldLength) {
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles =
                dir.listFiles(
                        file -> (file.isDirectory()) || (file.getName().endsWith(".class")));
        if (dirfiles == null) {
            return;
        }
        for (File dirfile : dirfiles) {
            if (dirfile.isDirectory()) {
                selectPackage(
                        filePath + "/" + dirfile.getName(), modelPath + "." + dirfile.getName(), fieldLength);
            } else {
                String className = dirfile.getName().substring(0, dirfile.getName().length() - 6);
                try {
                    Class<?> clazz = Thread.currentThread()
                            .getContextClassLoader()
                            .loadClass(modelPath + "." + className);
                    for (Field declaredField : clazz.getDeclaredFields()) {
                        if (declaredField.getAnnotation(Resource.class) != null || declaredField.getAnnotation(Autowired.class) != null) {
                            fieldLength.getAndAdd(1);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        if (Register.applicationContext == null) {
            Register.applicationContext = applicationContext;
        }
        if (NEW_BEAN_MAP_FIELD == null) {
            NEW_BEAN_MAP_FIELD = new ConcurrentHashMap<>(64);
        }
    }

    protected static void extracted(Object cast, Class<?> aClass) {
        Field[] declaredFields = aClass.getDeclaredFields();
        Consumer<Field> setField = (f) -> {
            Object bean = null, name = aClass.getName() + "_" + f.getName();
            if (NEW_BEAN_MAP_FIELD.containsKey(name)) {
                bean = NEW_BEAN_MAP_FIELD.get(name);
            }
            try {
                if (bean != null) {
                    f.set(cast, bean);
                    return;
                }
                Class<?> type = f.getType();
                if (Collection.class.isAssignableFrom(type)) {
                    Type[] actualTypeArguments = ((ParameterizedType) f.getGenericType()).getActualTypeArguments();
                    Class<?> elementType = (Class<?>) actualTypeArguments[0];
                    bean = Register.getBean(f.getType(), elementType);
                    f.set(cast, bean);
                    NEW_BEAN_MAP_FIELD.put(name.toString(), bean);
                    return;
                }
                if (Map.class.isAssignableFrom(type)) {
                    Type[] actualTypeArguments = ((ParameterizedType) f.getGenericType()).getActualTypeArguments();
                    Class<?> elementType = (Class<?>) actualTypeArguments[1];
                    bean = Register.getBean(f.getType(), elementType);
                    f.set(cast, bean);
                    NEW_BEAN_MAP_FIELD.put(name.toString(), bean);
                    return;
                }
                bean = Register.getBean(f.getType(), null);
                NEW_BEAN_MAP_FIELD.put(name.toString(), bean);
                f.set(cast, bean);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        };
        BiConsumer<Field, String> setFieldByName = (declaredField, beanName) -> {
            Object bean = null, name = aClass.getName() + "_" + declaredField.getName();
            if (NEW_BEAN_MAP_FIELD.containsKey(name)) {
                bean = NEW_BEAN_MAP_FIELD.get(name);
            }
            if (bean == null) {
                bean = Register.getBean(beanName);
                NEW_BEAN_MAP_FIELD.put(name.toString(), bean);
            }
            try {
                declaredField.set(cast, bean);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        };
        for (Field declaredField : declaredFields) {
            Autowired autowired = declaredField.getAnnotation(Autowired.class);
            declaredField.setAccessible(true);
            if (autowired != null) {
                Qualifier annotation = declaredField.getAnnotation(Qualifier.class);
                if (annotation == null) {
                    setField.accept(declaredField);
                    continue;
                }
                setFieldByName.accept(declaredField, annotation.value());
            } else {
                Resource resource = declaredField.getAnnotation(Resource.class);
                if (resource == null) {
                    continue;
                }
                if (resource.name().isEmpty()) {
                    setField.accept(declaredField);
                    continue;
                }
                setFieldByName.accept(declaredField, resource.name());
            }
            declaredField.setAccessible(false);
        }
    }

    private static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    private static Object getBean(Class<?> clazz, Class<?> type) {
        if (type != null) {
            String[] beanNamesForTypes = getApplicationContext().getBeanNamesForType(type);
            if (List.class.isAssignableFrom(clazz)) {
                return new ArrayList<Object>() {{
                    for (String beanNamesForType : beanNamesForTypes) {
                        add(getBean(beanNamesForType));
                    }
                }};
            }
            if (Set.class.isAssignableFrom(clazz)) {
                return new HashSet<Object>() {{
                    for (String beanNamesForType : beanNamesForTypes) {
                        add(getBean(beanNamesForType));
                    }
                }};
            }
            if (Map.class.isAssignableFrom(clazz)) {
                return new HashMap<String, Object>() {{
                    for (String beanNamesForType : beanNamesForTypes) {
                        put(beanNamesForType, getBean(beanNamesForType));
                    }
                }};

            }
            return null;
        }
        return getApplicationContext().getBean(clazz);
    }
}
