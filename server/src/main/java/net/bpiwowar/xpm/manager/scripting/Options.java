package net.bpiwowar.xpm.manager.scripting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks parameter that are maps (string -> object) than
 * can receive arguments as options
 *
 * <code>
 *     method(... @Options Map options...)
 * </code>
 *
 * when called from python with
 *
 * <code>
 *     method(..., a=1, b="yop")
 * </code>
 *
 * will receive a map with {a: 1, b: yop} in the map options
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Options {
}
