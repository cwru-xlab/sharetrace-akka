package org.sharetrace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
    get = {"is*", "has*", "get*", "*"},
    strictBuilder = true,
    typeAbstract = "*",
    depluralize = true,
    visibility = ImplementationVisibility.PACKAGE,
    newBuilder = "create",
    jdkOnly = true,
    deepImmutablesDetection = true,
    defaults = @Value.Immutable(copy = false, lazyhash = true))
public @interface ImmutableStyle {}
