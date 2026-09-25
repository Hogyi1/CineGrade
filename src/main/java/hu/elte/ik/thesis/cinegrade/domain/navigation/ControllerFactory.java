package hu.elte.ik.thesis.cinegrade.domain.navigation;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ControllerFactory implements Callback<Class<?>, Object> {

    private final ConcurrentHashMap<Class<?>, Supplier<?>> registry = new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger(ControllerFactory.class);

    public void register(Class<?> clazz, Supplier<?> supplier) {
        registry.put(clazz, supplier);
        logger.debug("New controller registered: {}", clazz.getName());
    }

    @Override
    public Object call(Class<?> clazz) {
        logger.debug("Creating controller {}", clazz.getName());
        Supplier<?> supplier = registry.get(clazz);
        if (supplier != null) {
            return supplier.get();
        }

        logger.debug("Controller {} haven't been registered trying default constructor", clazz.getName());
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, ex);
        }
    }
}
