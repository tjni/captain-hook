package com.github.tjni.captainhook.dagger.modules;

import dagger.Module;
import dagger.Provides;
import java.time.Clock;
import javax.inject.Singleton;

@Module
public class SingletonModule {
  @Provides
  @Singleton
  public static Clock provideClock() {
    return Clock.systemDefaultZone();
  }

  private SingletonModule() {}
}
