package com.moodmatch.service;

import com.moodmatch.entity.AppUser;

public interface CurrentUserProvider {

    AppUser getCurrentUser();
}
