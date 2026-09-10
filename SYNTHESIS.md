# QuestLog × Envelope Addon — synthèse technique

## Objectif

Créer un addon de compatibilité indépendant entre **Questlog** et **Envelope** pour Minecraft **1.21.1**, Java **21**, avec support **Fabric + NeoForge**, sans fork des projets upstream.

Versions ciblées :

- Questlog 3.3.2 ;
- Envelope 0.7.5 ;
- Minecraft 1.21.1 ;
- Java 21 ;
- Fabric et NeoForge.

Le principe reste simple : Questlog demeure la source de vérité pour l'état des quêtes, et Envelope demeure la source de vérité pour le courrier visible et ses livraisons.

## Architecture actuelle

```text
Questlog
  |
  +--> objectifs mail_received / mail_sent
  |       |
  |       +--> événements normalisés par l'addon
  |               |
  |               +--> Envelope livraison / envoi réel
  |
  +--> récompenses letter / package
          |
          +--> construction de courrier Envelope
                  |
                  +--> livraison mailbox / pigeon / fallback
```

Le code métier vit principalement dans `common`. Les modules `fabric` et `neoforge` servent à l'initialisation et aux intégrations spécifiques au loader, notamment le networking.

Les mixins sont réservés aux points qu'Envelope ou Questlog n'exposent pas proprement via une API publique. La logique métier n'est pas placée directement dans les mixins.

## Intégration Questlog

Les types custom sont enregistrés via les registres Questlog natifs et apparaissent dans l'éditeur de quêtes.

### `questlog_envelope:mail_received`

Objectif/prérequis complété lorsqu'un courrier marqué pour une quête donnée est réellement reçu.

Le marqueur est stocké dans `DataComponents.CUSTOM_DATA` sous l'espace logique `questlog_envelope`. Si le champ `quest` de l'objectif est absent, l'ID de la quête parente est utilisé.

Les réceptions hors ligne sont persistées dans une `SavedData` de l'addon puis rejouées lorsque l'état Questlog du joueur est disponible. Le traitement est conçu pour éviter le double comptage.

### `questlog_envelope:mail_sent`

Objectif/prérequis progressé lorsqu'un joueur expédie réellement un courrier depuis Envelope.

Le filtrage supporte :

- destinataire ;
- type de courrier : lettre, colis ou indifférent ;
- texte contenu dans une lettre ;
- item contenu dans un colis ;
- quantité minimale de cet item.

Les filtres configurés sont combinés avec une sémantique AND. Les courriers de service générés automatiquement par l'addon sont exclus de cette progression.

## Récompenses Envelope

### `questlog_envelope:letter`

Construit une vraie lettre Envelope et la livre au joueur. L'éditeur expose notamment :

- expéditeur ;
- titre et texte ;
- formatage riche natif Envelope ;
- sceau de cire Envelope ;
- `grants_quest` ;
- auto-claim ;
- cercle magique optionnel.

### `questlog_envelope:package`

Construit un ou plusieurs vrais colis Envelope. L'éditeur reproduit une disposition à six slots, permet plusieurs pages physiques et supporte les sceaux Envelope. Les anciennes définitions basées sur `items` restent lisibles.

## Cercles magiques

Le cercle magique est indépendant du sceau de cire. Il possède sa propre configuration visuelle et comportementale :

- position et taille ;
- couleur d'activation ;
- durée de maintien ;
- action Questlog optionnelle ;
- commande serveur optionnelle.

Le client demande l'activation via un payload C2S dédié. Le serveur valide avant exécution :

1. le joueur destinataire ;
2. la lettre effectivement tenue ;
3. l'identifiant d'action persistant ;
4. l'état déjà utilisé ou non de l'action.

Chaque action est liée au destinataire et possède un ID unique stocké côté serveur. Une copie du courrier ne peut donc pas rejouer une action déjà consommée. Les données d'action et les métadonnées de lettre sont versionnées, avec lecture des enregistrements hérités prévus par l'implémentation actuelle.

L'état visuel activé est persistant. La texture `assets/questlog_envelope/textures/gui/magic_circle.png` est remplaçable par resource pack et le cache de teinte est invalidé lors d'un reload client sur les deux loaders.

## Livraison

Le chemin normal privilégie toujours les primitives Envelope.

- Lettre avec mailbox : livraison de service normale Envelope.
- Colis avec mailbox : livraison express avec approche finale du pigeon côté destinataire.
- Joueur sans mailbox dans l'Overworld : fallback de service-pigeon/livraison près du joueur.
- Hors Overworld : fallback direct sécurisé lorsque `MailService` n'est pas disponible.
- Expéditeur de service invalide : fallback vers l'adresse de service courrier normale d'Envelope.

Une erreur de routage ne doit pas bloquer définitivement le bouton **Collect Reward** de Questlog.

## État de l'implémentation

### Fonctionnellement implémenté

- [x] Projet multiloader Fabric + NeoForge.
- [x] Objectif/prérequis `mail_received`.
- [x] Persistance/replay des réceptions hors ligne.
- [x] Objectif/prérequis `mail_sent`.
- [x] Filtres destinataire/type/texte/item/quantité.
- [x] Exclusion des courriers automatiques de la progression d'envoi joueur.
- [x] Récompense lettre Envelope.
- [x] Récompense colis Envelope et pages multiples.
- [x] Sceaux de cire Envelope.
- [x] Édition de texte riche Envelope.
- [x] Cercles magiques configurables.
- [x] Actions cercle magique Questlog + commande serveur.
- [x] Networking C2S Fabric + NeoForge.
- [x] Validation destinataire et anti-replay persistant.
- [x] Migration/lecture des anciens enregistrements d'action pris en charge.
- [x] Fallbacks mailbox / sans mailbox / dimensions.
- [x] Traductions anglaises et françaises.
- [x] CI de build Java 21.
- [x] Smoke checks des jars et packaging des artifacts CI.
- [x] Workflow de release sur tag `v*`.
- [x] Matrice de régression documentée dans `TESTING.md`.

### Avant 0.1.0

- [ ] Exécuter la matrice `TESTING.md` en jeu sur Fabric.
- [ ] Exécuter la matrice `TESTING.md` en jeu sur NeoForge.
- [ ] Corriger les régressions détectées.
- [ ] Passer `mod_version` de `0.1.0-SNAPSHOT` à la version finale choisie.
- [ ] Compléter les notes de release dans `CHANGELOG.md`.
- [ ] Tagger exactement la même version, par exemple `v0.1.0`.

## Build, vérification et release

Le projet est standardisé sur Gradle 9.5.0, version déjà utilisée par la CI verte de la branche d'intégration.

```bash
./gradlew build --stacktrace
bash scripts/verify-build.sh
```

Le script de vérification collecte les jars distribuables dans `dist/` et vérifie la présence des métadonnées loader, du mixin partagé, des traductions et de la texture du cercle magique.

La CI exécute ces étapes sur les pull requests et les pushes vers `main` ou `dev/**`. Un tag `v*` lance le workflow de release ; celui-ci refuse la publication si le tag ne correspond pas exactement à `mod_version`.

## Principes de code

- Pas de fork upstream.
- Code métier dans `common` autant que possible.
- Adaptateurs loader-specific minimaux.
- Mixins ciblés uniquement lorsqu'une API publique suffisante n'existe pas.
- Progression de quête via les classes et événements Questlog.
- Courrier visible via les primitives Envelope.
- Actions exécutables validées côté serveur.
- Données persistantes versionnées lorsque leur évolution future est probable.

## Licence

Le projet est distribué sous **GNU GPL v3.0**. La copie complète de la licence doit être présente dans `LICENSE`.

Ce choix suit la contrainte de compatibilité/distribution avec Envelope et constitue une décision technique de projet, pas un avis juridique.
