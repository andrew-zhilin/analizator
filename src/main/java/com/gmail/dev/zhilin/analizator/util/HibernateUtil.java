package com.gmail.dev.zhilin.analizator.util;

import org.hibernate.Hibernate;

public class HibernateUtil {

    public static <T> T nullIfNotInitialized(T entity) {
        return Hibernate.isInitialized(entity) ? entity : null;
    }

}
