# Privacy Policy — Preflight

_Last updated: 14 June 2026_

Preflight is an on-device tool for checking Android apps before you submit them.
This policy explains, in plain terms, how it handles your data.

## The short version

Preflight does not collect, transmit, or store your personal data on any server.
There is no account, no analytics, and no advertising. Everything happens on your
device.

## What happens to the things you scan

- **APK files.** When you scan an APK, it is read on your device to extract its
  manifest and structure. The file is copied into the app's private cache only for
  the duration of the scan and is deleted immediately afterward. The APK is never
  uploaded. APK analysis works with no internet connection.
- **GitHub repositories.** If you choose to review a repository by pasting its
  link, the app fetches that public repository's manifest files directly from
  GitHub over an anonymous, unauthenticated connection. No credentials are sent or
  stored, and only public repositories are reachable. The repository owner and
  name are sent to GitHub solely to retrieve those files, as is necessary for any
  request to GitHub.
- **Self-audit and sample.** These read data already on your device (the app
  itself, or a bundled example) and send nothing anywhere.

## Scan history

Results from past scans are saved in the app's private storage on your device so
you can revisit them and see score changes over time. This history never leaves
your device and is removed when you uninstall the app.

## Permissions

Preflight requests only the `INTERNET` permission, which is used solely for the
GitHub repository fetch described above. It uses the system file picker to read an
APK you explicitly select, so it does not request broad storage access.

## Third parties

The only third-party service the app contacts is **GitHub**, and only when you
ask it to review a repository. GitHub's handling of those requests is governed by
[GitHub's Privacy Statement](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).

## Verifying these claims

Preflight is open source under the MIT license. You can read the full code to
confirm everything described here at
<https://github.com/Abhijeet6395/Preflight--The-App-Reviewer>, and you can run the
app with networking disabled to confirm that APK analysis sends nothing.

## Contact

Questions about this policy can be sent to: abhijee0123@gmail.com
