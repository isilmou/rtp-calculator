# ─────────────────────────────────────────────────────────────────────────────
# RTP Calculator — règles R8
# R8 est actif en release (réduction + optimisation + obscurcissement).
# Ce fichier ne conserve que ce qui est lu par réflexion. Tout le reste
# (UI, ViewModel, Compose, SDK) est réduit et renommé librement.
# ─────────────────────────────────────────────────────────────────────────────

# 1. Modèle sérialisé par Gson (HistoryRepository).
#    Les noms de champs sont les clés JSON stockées dans SharedPreferences
#    depuis la 1.x. Ils ne doivent JAMAIS être renommés : l'historique existant
#    deviendrait illisible et load() renverrait une liste vide en silence.
#    Périmètre volontairement réduit à trois classes du package model.
-keep class com.rtpcalculator.model.BetAnalysis { <fields>; <init>(); }
-keep class com.rtpcalculator.model.Outcome { <fields>; <init>(); }
-keep enum com.rtpcalculator.model.OddsFormat { *; }

# 2. Gson résout List<BetAnalysis> via la signature générique du TypeToken.
#    Sans cet attribut, la liste serait désérialisée en List<LinkedTreeMap>.
#    Gson 2.10 embarque déjà ces règles ; on les rend explicites pour ne pas
#    dépendre d'une future mise à jour de la dépendance.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 3. Traces de plantage lisibles dans la Play Console.
#    Les noms restent obscurcis (mapping.txt fait la traduction), mais les
#    numéros de ligne sont conservés. Le nom de fichier source est masqué.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
