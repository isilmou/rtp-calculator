---
paths:
  - "RTP Calculator - App/**"
---

# Tests de release Android — pièges vérifiés le 19/09/2026

Complément du § 5 de `CLAUDE.md` (chaîne de publication). Chargé quand le travail touche l'app.

- Un « Run » Android Studio installe la variante **debug**, où R8 est inactif quel que soit le
  réglage (AGP désactive l'obscurcissement des builds debuggables). Un test d'obscurcissement
  exige un APK release signé via l'assistant, installé par `adb install -r`.
- Debug et release ont des signatures incompatibles : passer de l'un à l'autre impose une
  désinstallation, donc la perte des données locales. Sauvegarder `rtp_history.xml` avant.
- Les deux devices ne portent pas la version Play Store. Un test de migration de données se
  fait avec un build `debug { isMinifyEnabled = true; isDebuggable = false }` **temporaire**,
  jamais commité : signé en debug, donc installable par-dessus sans perte.
- **AdMob** : les appareils de test sont déclarés dans `MainActivity` via
  `setTestDeviceIds` (empreintes SDK, pas des secrets). L'enregistrement en console AdMob ne
  suffit pas. Ne jamais cliquer une annonce d'un build de test tant que logcat n'affiche pas
  « This request is sent from a test device ».
