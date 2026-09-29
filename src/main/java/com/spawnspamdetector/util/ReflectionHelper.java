package com.spawnspamdetector.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


/**
 * Reflection helpers used by optional client integrations.
 */
public final class ReflectionHelper {

    private ReflectionHelper() {
    }

    public static Class<?> loadClassRequired(String className) throws ReflectionException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException("Class not found: " + className, e);
        }
    }

    public static Object getStaticField(Class<?> clazz, String fieldName) throws ReflectionException {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            throw new ReflectionException("Failed to get field '" + fieldName + "' from " + clazz.getName(), e);
        }
    }

    public static Object invokeRequired(Object target, String methodName, Class<?>[] parameterTypes,
            Object... args) throws ReflectionException {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new ReflectionException(
                "Failed to invoke method '" + methodName + "' on " + target.getClass().getName(), e);
        }
    }

    public static Object invokeStaticRequired(Class<?> ownerClass, String methodName,
            Class<?>[] parameterTypes, Object... args) throws ReflectionException {
        try {
            Method method = ownerClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new ReflectionException(
                "Failed to invoke static method '" + methodName + "' on " + ownerClass.getName(), e);
        }
    }

    public static class ReflectionException extends Exception {

        public ReflectionException(String message) {
            super(message);
        }

        public ReflectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
