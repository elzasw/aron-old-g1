package cz.aron.core.integration;

import java.util.function.Function;

import javax.script.ScriptEngine;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.springframework.context.annotation.Configuration;

import cz.inqool.eas.common.script.ScriptConfiguration;
import cz.inqool.eas.common.script.ScriptType;

@Configuration
public class ScriptConfigurationGroovy extends ScriptConfiguration {

	@Override
	protected Function<ScriptType, ScriptEngine> scriptEngineFactory() {
		return (ScriptType st) -> {
			if (st != ScriptType.GROOVY) {
				throw new RuntimeException("Only groovy script engine supported");
			}			           
			return new GroovyScriptEngineFactory().getScriptEngine();
		};
	}

}
