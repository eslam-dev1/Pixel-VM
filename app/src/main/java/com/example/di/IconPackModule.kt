package com.example.di

import android.content.Context
import com.example.domain.repository.IconPackResolver

/**
 * Hilt Dependency Injection module providing the [IconPackResolver] as a Singleton.
 *
 * Pattern equivalent in standard Dagger/Hilt:
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object IconPackModule {
 *     @Provides
 *     @Singleton
 *     fun provideIconPackResolver(@ApplicationContext context: Context): IconPackResolver {
 *         return IconPackResolver.getInstance(context)
 *     }
 * }
 * ```
 *
 * By providing [IconPackResolver] as a Singleton bound to the application context,
 * its internal [android.util.LruCache] remains alive across the entire application lifecycle,
 * eliminating repeated XML parsing when navigating between Home, Drawer, Recents, and Settings.
 */
object IconPackModule {

    /**
     * Provides the Singleton instance of [IconPackResolver].
     *
     * @param context [Context] used to obtain the application context.
     * @return The application-wide singleton [IconPackResolver].
     */
    fun provideIconPackResolver(context: Context): IconPackResolver {
        return IconPackResolver.getInstance(context)
    }
}
