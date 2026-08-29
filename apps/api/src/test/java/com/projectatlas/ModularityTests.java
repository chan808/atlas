package com.projectatlas;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularityTests {

	@Test
	void moduleBoundariesAreValid() {
		ApplicationModules modules = ApplicationModules.of(AtlasApplication.class);

		modules.verify();
		assertTrue(modules.getModuleByName("inquiry").isPresent());
	}

}
