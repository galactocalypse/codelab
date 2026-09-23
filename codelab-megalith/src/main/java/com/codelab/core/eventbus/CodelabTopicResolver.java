package com.codelab.core.eventbus;

import com.codelab.common.spring.eventbus.CodelabSubscription;
import com.codelab.common.spring.eventbus.CodelabTopic;
import com.codelab.common.spring.persistence.CodelabModule;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class CodelabTopicResolver {

    private CodelabTopicResolver() {}

    public static String resolveTenant(Environment environment, String moduleName) {
        return environment.getRequiredProperty("app.pulsar.tenant.prefix") + moduleName;
    }
    public static String resolveTopicName(CodelabModule module, CodelabSubscription annotation, Environment environment) {
        return resolveTopicName(module.name(), annotation, environment);
    }

    public static String resolveTopicName(String moduleName, CodelabSubscription annotation, Environment environment) {
        String tenant = resolveTenant(environment, moduleName);
        String namespace = environment.getRequiredProperty("app.pulsar.namespace");
        if (!StringUtils.hasText(tenant)) {
            throw new IllegalStateException(
                    "CodelabModule '" + moduleName + "' has no tenant configured");
        }

        if (!StringUtils.hasText(namespace)) {
            throw new IllegalStateException(
                    "No namespace resolved for tenant '" + tenant + "'");
        }

        String topic = annotation.topic();
        if (!StringUtils.hasText(topic)) {
            throw new IllegalStateException(
                    "@CodelabSubscription on a consumer interface must specify a non-blank topic name");
        }

        return String.format("persistent://%s/%s/%s", tenant, namespace, topic);
    }

    /**
     * Composes the fully-qualified Pulsar topic: persistent://tenant/namespace/topic
     *
     * Tenant comes from the module (each module IS a tenant, per your original design).
     * Namespace comes from megalith-level config (roughly your env: dev/staging/prod),
     * unless the annotation explicitly overrides it — that override should be rare
     * and probably logged, since it's an escape hatch from the module's default isolation.
     */
    public static String resolveTopicName(CodelabModule module, CodelabTopic annotation, Environment environment) {
        String tenant = resolveTenant(environment, module.name());
        String namespace = environment.getRequiredProperty("app.pulsar.namespace");
        if (!StringUtils.hasText(tenant)) {
            throw new IllegalStateException(
                    "CodelabModule '" + module.name() + "' has no tenant configured");
        }

        if (!StringUtils.hasText(namespace)) {
            throw new IllegalStateException(
                    "No namespace resolved for tenant '" + tenant + "' — check module config or "
                            + "@CodelabTopic(namespace=...) override on the annotation");
        }

        String topic = annotation.value();
        if (!StringUtils.hasText(topic)) {
            throw new IllegalStateException(
                    "@CodelabTopic on a publisher interface must specify a non-blank topic name");
        }

        return String.format("persistent://%s/%s/%s", tenant, namespace, topic);
    }
}
