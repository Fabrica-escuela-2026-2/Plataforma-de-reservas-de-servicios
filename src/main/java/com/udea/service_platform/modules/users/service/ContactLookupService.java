package com.udea.service_platform.modules.users.service;

import java.util.Collection;
import java.util.Map;

/**
 * Facade published by the users module.
 * Other modules resolve contact data of the other party of a reservation
 * through this contract instead of injecting UserRepository directly.
 */
public interface ContactLookupService {

    /**
     * Batch lookup (avoids N+1). Users that do not exist are simply absent
     * from the result; the role of the user does not matter.
     */
    Map<Long, ContactInfo> findContactsByIds(Collection<Long> userIds);
}
