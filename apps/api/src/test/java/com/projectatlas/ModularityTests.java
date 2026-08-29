package com.projectatlas;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	@Test
	void moduleBoundariesAreValid() {
		ApplicationModules.of(AtlasApplication.class).verify();
	}

}
