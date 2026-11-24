<?php

namespace Blutrixx\Biometric;

use Illuminate\Support\ServiceProvider;

/** No PHP of its own: an app's interface calls Biometric.Available and Biometric.Authenticate through the bridge. */
class BiometricServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        //
    }

    public function boot(): void
    {
        //
    }
}
