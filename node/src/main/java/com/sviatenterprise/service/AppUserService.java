package com.sviatenterprise.service;

import com.sviatenterprise.entity.AppUser;

public interface AppUserService {
    String registerUser(AppUser appUser);

    String setEmail(AppUser appUser, String email);
}
