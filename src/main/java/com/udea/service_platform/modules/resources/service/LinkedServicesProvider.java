package com.udea.service_platform.modules.resources.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Dependency inversion: resources asks for the services linked to its
 * resources without importing the services module.
 */
public interface LinkedServicesProvider {

    Map<Long, List<LinkedService>> findLinkedServices(Collection<Long> idsRecursos);
}
