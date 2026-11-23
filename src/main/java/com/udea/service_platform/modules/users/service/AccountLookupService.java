package com.udea.service_platform.modules.users.service;

/**
 * Facade published by the users module.
 * Other modules query the state of an account through this contract
 * instead of injecting UserRepository directly.
 */
public interface AccountLookupService {

    /**
     * True only when the user exists and its account is active.
     * Unknown users are reported as not active.
     */
    boolean isAccountActive(Long userId);
}
