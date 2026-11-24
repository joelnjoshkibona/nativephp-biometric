# Blutrixx Biometric

A [NativePHP Mobile](https://nativephp.com) 4 plugin for the phone's own fingerprint (or face): Android's
`BiometricPrompt`, e.g. to unlock an app. Strong or weak biometrics only; the phone's screen PIN is not offered, so keep
your own PIN or password as the second way in.

Composer package: `blutrixx/nativephp-biometric` · Repo: `joelnjoshkibona/nativephp-biometric`

## Installation

```bash
composer require blutrixx/nativephp-biometric
```

Then list `Blutrixx\Biometric\BiometricServiceProvider` in your app's `NativeServiceProvider::plugins()`.

## Use

There is no PHP API: an interface in the app's web view calls the bridge functions (`POST /_native/api/call` with
`{ method, params }`).

| Function | Params | Answer |
|---|---|---|
| `Biometric.Available` | | `{ available }`: a fingerprint or face is enrolled that the app may use |
| `Biometric.Authenticate` | `title`, `subtitle`, `cancel` (the button's word) | `{ success: true }`, `{ success: false, cancelled: true }` or `{ success: false, error }` |

Authenticate waits for the person (up to 60 seconds). Errors are `unavailable`, `busy`, `timeout` or Android's own
message; it never throws, so the caller can just fall back to its PIN.

Android: `androidx.biometric:biometric:1.1.0`, minimum SDK 26.
