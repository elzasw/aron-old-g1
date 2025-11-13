package cz.aron.customizations.generic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.customizations.soaplzen.ImportArchDescNameBuilderImpl;

@Service
@ConditionalOnProperty(value = "result.archdescnamebuilderelza", havingValue = "default")
public class ImportArchdescNameBuilderDefault extends ImportArchDescNameBuilderImpl {

}
