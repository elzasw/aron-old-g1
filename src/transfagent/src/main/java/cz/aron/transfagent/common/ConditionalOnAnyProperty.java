package cz.aron.transfagent.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;


/**
 * Spring {@link Conditional} annotation to test any not empty property exist.
 */


@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionalOnAnyProperty.OnAnyPropertyCondition.class)
public @interface ConditionalOnAnyProperty {

	String[] name();
	
	public static class OnAnyPropertyCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

			Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnAnyProperty.class.getName());

			String[] properties = (String[]) attributes.get("name");

			Environment env = context.getEnvironment();

			for (String property : properties) {
				if (env.containsProperty(property)) {
					return true;
				}
			}

			return false;
		}

	}
	
}
