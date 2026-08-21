# QuestLog × Envelope Addon — synthèse technique

## Objectif

Créer un mod de compatibilité indépendant entre **Questlog** et **Envelope** pour Minecraft **1.21.1**, Java **21**, avec support **Fabric + NeoForge**.

Le mod ne doit pas nécessiter de fork de Questlog ni d'Envelope. L'objectif est d'utiliser leurs points d'extension publics autant que possible et de réserver les mixins aux interactions qu'Envelope n'expose pas directement.

## Versions étudiées

- Questlog : branche `1.21.1`, version `3.3.2`.
- Envelope : branche `1.21.1`, version `0.7.5`.
- Minecraft : `1.21.1`.
- Java : `21`.
- Loaders : Fabric et NeoForge.

Dépôts upstream :

- https://github.com/infernalstudios/Questlog/tree/1.21.1
- https://github.com/mortuusars/Envelope/tree/1.21.1

## Ce que Questlog expose

Questlog permet à un mod tiers d'enregistrer de nouveaux types d'objectifs et de récompenses avec :

```java
QuestObjectiveRegistry.register(...);
QuestRewardRegistry.register(...);
```

Les quêtes sont créées par joueur. Lorsqu'un objectif est construit, Questlog lui assigne sa quête parente et appelle `registerEventListeners()` côté serveur. Un objectif de compatibilité peut donc écouter nos propres événements et appeler `setUnits(...)` exactement comme les objectifs natifs.

Les prérequis sont des `Objective` ordinaires marqués comme prérequis. Quand tous les prérequis sont complétés, `Quest.isTriggered()` devient vrai. `QuestManager.sync()` détecte alors la transition et émet `QuestEvent.Triggered`.

Conséquence : **une lettre peut débloquer une quête simplement en complétant un prérequis custom**. Il n'est pas nécessaire de créer un second système de quêtes.

## Ce qu'Envelope expose

Envelope expose plusieurs briques utiles :

- `MailService` : accès au système postal du monde.
- `Delivery` / `DeliveryDraft` / `DeliveryManager` : création et démarrage de vraies livraisons.
- `Mail.createLetter(...)` et `Mail.createPackage(...)` : création de courriers.
- `PlayerAddress`, `BlockAddress`, `ServiceAddress` : adressage.
- `ServiceDropOffHandlerRegistry` : ajout de handlers pour des adresses de service custom.
- `MailDropOffContext` : contexte d'une livraison arrivée à destination.

Une livraison possède notamment un `owner` UUID optionnel. Lors d'un envoi normal depuis une mailbox, Envelope renseigne cet owner avec le propriétaire de la boîte. Cela permet d'attribuer correctement un courrier expédié au joueur qui l'a envoyé.

## Architecture retenue

Nom de travail : `questlog_envelope`.

```text
Envelope
   |
   +--> livraison vers un service de quête
   |       |
   |       +--> QuestMailDispatcher
   |               |
   |               +--> objectifs Questlog par joueur
   |
   +--> livraison d'une lettre de quête à un joueur
           |
           +--> détection du courrier marqué
                   |
                   +--> prérequis mail_received
                           |
                           +--> Questlog déclenche la quête nativement

Questlog
   |
   +--> récompense mail / letter
           |
           +--> Envelope DeliveryManager
                   |
                   +--> vraie livraison dans la mailbox du joueur
```

## Fonctionnalité 1 — envoyer du courrier comme objectif de quête

Type prévu :

```text
questlog_envelope:mail_delivery
```

Exemple :

```json
{
  "objectives": [
    {
      "type": "questlog_envelope:mail_delivery",
      "service": "my_pack:royal_archives",
      "required_amount": 1
    }
  ]
}
```

Flux :

1. Le joueur prépare un courrier Envelope.
2. Il l'adresse à un `ServiceAddress` de quête.
3. Envelope effectue réellement la livraison.
4. Un handler global de l'addon observe l'arrivée.
5. L'UUID `Delivery.owner` identifie le joueur expéditeur.
6. Le dispatcher notifie les objectifs `mail_delivery` de ce joueur.
7. L'objectif correspondant incrémente `units`.

### Important

Il ne faut **pas** enregistrer un `MailDropOffHandler` par instance d'objectif Questlog. Les objectifs sont instanciés par joueur, ce qui provoquerait une accumulation de handlers.

L'addon doit enregistrer un nombre contrôlé de handlers Envelope et redistribuer ensuite les événements via un dispatcher commun.

## Fonctionnalité 2 — recevoir une lettre qui débloque une quête

Type de prérequis prévu :

```text
questlog_envelope:mail_received
```

Exemple de quête :

```json
{
  "title": "Une demande mystérieuse",
  "prerequisites": [
    {
      "type": "questlog_envelope:mail_received",
      "quest": "my_pack:mysterious_request"
    }
  ],
  "objectives": [
    {
      "type": "questlog:item_obtain",
      "item": "minecraft:amethyst_shard",
      "required_amount": 8
    }
  ]
}
```

La lettre Envelope porte un marqueur indiquant l'identifiant de quête qu'elle débloque. Le premier choix d'implémentation est d'utiliser `DataComponents.CUSTOM_DATA` plutôt que d'enregistrer immédiatement un nouveau DataComponent : cela réduit le code loader-specific et conserve le marqueur dans l'`ItemStack`.

Format logique du marqueur :

```text
questlog_envelope.quest_id = "my_pack:mysterious_request"
```

Si le champ `quest` est absent du prérequis, l'addon pourra utiliser par défaut l'ID de la quête parente.

### Où détecter la réception ?

Envelope ne fournit pas d'API publique équivalente au `ServiceDropOffHandlerRegistry` pour observer une livraison réussie dans une mailbox de joueur.

`PlayerDropOffHandler` résout un `PlayerAddress` vers sa mailbox puis délègue à `BlockDropOffHandler`. `BlockDropOffHandler` insère réellement la copie livrée dans l'inbox.

Pour cette fonctionnalité, un **mixin minimal et ciblé** autour du succès de `BlockDropOffHandler.handle(...)` est donc acceptable pour le MVP. Il devra uniquement observer le résultat et publier un événement ; aucune logique métier Questlog ne doit vivre dans le mixin.

Pour les lettres générées par l'addon, le destinataire sera un `PlayerAddress`, ce qui permet de retrouver le joueur destinataire via `Delivery.getRecipient()` même si Envelope résout ensuite la mailbox en `BlockAddress`.

### Joueur hors ligne

Une livraison peut arriver alors que le joueur n'a pas d'instance Questlog active. Le design prévu est donc :

1. enregistrer l'unlock dans une `SavedData` de l'addon ;
2. si le joueur est en ligne, publier immédiatement l'événement ;
3. l'objectif `mail_received` consomme l'unlock lorsqu'il le traite ;
4. lors de `registerEventListeners()`, l'objectif vérifie également les unlocks en attente.

Ainsi une lettre reçue hors ligne pourra toujours débloquer la quête à la prochaine création/connexion du QuestManager du joueur, sans double comptage.

## Fonctionnalité 3 — récompense Questlog livrée par Envelope

Type prévu :

```text
questlog_envelope:letter
```

Exemple :

```json
{
  "rewards": [
    {
      "type": "questlog_envelope:letter",
      "sender": "my_pack:royal_archives",
      "title": "Rapport accepté",
      "text": "Votre rapport est arrivé à destination.",
      "auto_claim": true
    }
  ]
}
```

Implémentation :

1. construire la lettre avec `Mail.createLetter(...)` ;
2. résoudre le `ServiceAddress` expéditeur ;
3. adresser la lettre à `new PlayerAddress(player)` ;
4. démarrer une vraie livraison via `MailService.of(level).getDeliveryManager().startService(...)` ;
5. appeler `super.applyReward(player)` pour laisser Questlog marquer la récompense comme réclamée.

## Fonctionnalité 4 — lettre de quête envoyée par le jeu

Une extension directe de la récompense `letter` permettra d'ajouter :

```json
{
  "type": "questlog_envelope:letter",
  "sender": "my_pack:royal_archives",
  "title": "Nouvelle mission",
  "text": "Présentez-vous à l'avant-poste.",
  "grants_quest": "my_pack:outpost_mission",
  "auto_claim": true
}
```

`grants_quest` écrit le marqueur de quête sur le courrier. Lorsque la lettre arrive dans la mailbox du destinataire, le prérequis `mail_received` de cette quête est complété et Questlog la déclenche.

Cela permet des chaînes narratives :

```text
quête A terminée
  -> récompense = lettre Envelope
  -> pigeon livre la lettre
  -> réception de la lettre
  -> quête B déclenchée
```

## Adresses de service data-driven

Envelope possède un registre data-driven de `ServiceAddressDefinition`. Un modpack pourra donc créer ses propres services :

```text
my_pack:royal_archives
my_pack:mage_guild
my_pack:quartermaster
my_pack:imperial_tax_office
```

Ces adresses pourront servir de destinataires d'objectifs et d'expéditeurs de récompenses.

Pour un service créé spécialement pour les quêtes, notre handler pourra :

- `CONSUME` : accepter le courrier ;
- `reply(...)` : répondre au joueur ;
- `returned(...)` : refuser le courrier.

Pour les services natifs ou appartenant à d'autres mods, l'addon doit éviter de modifier leur comportement.

## MVP de développement

### Phase 1

- [x] Analyse de Questlog et Envelope.
- [x] Choix de Minecraft 1.21.1 / Java 21 / Fabric + NeoForge.
- [x] Architecture générale.
- [ ] Initialiser le projet multiloader.
- [ ] Enregistrer `mail_delivery`.
- [ ] Ajouter le dispatcher de courrier.
- [ ] Ajouter le marquage `QuestMailMarker` dans `CUSTOM_DATA`.
- [ ] Enregistrer `mail_received`.
- [ ] Ajouter le mixin minimal de réception.
- [ ] Ajouter la persistance des unlocks hors ligne.
- [ ] Enregistrer la récompense `letter`.

### Phase 2

- [ ] Filtrer le contenu d'un colis (items, quantité, tags).
- [ ] Réponses automatiques de services.
- [ ] Récompenses sous forme de colis.
- [ ] Conditions sur expéditeur/destinataire.
- [ ] Support d'un objectif « lire une lettre » distinct de « recevoir une lettre ».
- [ ] Suggestions adaptées dans l'éditeur Questlog.

## Principes de code

- Le code métier doit rester dans `common`.
- Les modules Fabric/NeoForge doivent être limités à l'initialisation et aux intégrations loader-specific.
- Aucun fork upstream.
- Mixins uniquement quand aucune API publique suffisante n'existe.
- Les événements de courrier sont centralisés dans un dispatcher unique.
- Toute progression de quête passe par les API et classes natives de Questlog (`Objective`, `Reward`, `setUnits`, etc.).
- Toute livraison visible en jeu passe par les primitives natives d'Envelope (`Mail`, `Delivery`, `MailService`).

## Licence

Questlog est Apache-2.0 et Envelope est GPLv3 sur les branches étudiées. Pour un addon distribué qui compile directement contre Envelope, **GPL-3.0** est le choix le plus simple/prudent pour ce projet. Cette note est une décision technique de projet, pas un avis juridique.
