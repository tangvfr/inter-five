# URL

## authentification

```java
UserDto(String mail, String username, @JsonIgnore @Nullable=true String password)
```

| url              | Méthode(GET, POST..) | filtre | code erreur   |                  type entrée                  |  type retour  |                 description                 |
|:-----------------|:--------------------:|:------:|:--------------|:---------------------------------------------:|:-------------:|:-------------------------------------------:|
| `/auth/login`    |         GET          |   -    | 404           | record(String username/mail, String password) | String(token) |                  Connexion                  |
| `/auth/register` |         POST         |   -    | 409           |                    UserDto                    | String(token) |                 Inscription                 |
| `/profil`        |         GET          |   -    |               |                 List<UserDto>                 |               |         Voire tous les utilisateurs         |
| `/profil/{id}`   |         GET          |   -    | 403, 404      |                       -                       |    UserDto    |     Récuperation du profile utilisateur     |
| `/profil/{id}`   |         PUT          |   -    | 403, 404, 409 |                    UserDto                    |    UserDto    | Changement mail/username/(password->passur) |
| `/profil/{id}`   |        DELETE        |   -    | 403, 404      |                       -                       |       -       |            Suppression du compte            |

## vote
| url                             | Méthode(GET, POST..) |                  description                   |                         filtre                         |                                  code erreur                                   |                type entrée                |                   type retour | 
|:--------------------------------|:--------------------:|:----------------------------------------------:|:------------------------------------------------------:|:------------------------------------------------------------------------------:|:-----------------------------------------:|------------------------------:|
| `/match`                        |         GET          |                                                | member (à comme participant), owner (a comme créateur) |                                                                                |                                           | Liste [Match DTO](#Match-DTO) |             
| `/match`                        |         PUT          |                                                |                                                        |                                                                                | [Creation Match DTO](#Creation-Match-DTO) |                          UUID |             
| `/match/{matchId}`              |         GET          |                                                |                                                        |                              404 id match invalid                              |               path(matchID)               |       [Match DTO](#Match-DTO) |             
| `/match/{matchId}/date`         |         GET          | voir la liste des dates proposé pour un match  |                                                        |                              404 id match invalid                              |              `array<string>`              |                               |             
| `/match/{matchId}/date/{aDate}` |         PUT          |         vote pour une date d'un match          |                                                        |                   404 id match invalid,   404 aDate invalid                    |              `array<string>`              |                               |             
| `/match/{matchId}/hour`         |         PUT          |                                                |                                                        |          403 bad step, 403 pas dispo a la date, 404 id match invalid           |              `array<string>`              |                               |             
| `/match/{matchId}/hour`         |         GET          | voir la liste des heures proposé pour un match |                                                        |          403 bad step, 403 pas dispo a la date, 404 id match invalid           |                                           |                               |             
| `/match/{matchId}/hour/{aHour}` |                      |                                                |                                                        | 403 bad step, 403 pas dispo a la date, 404 id match invalid, 404 dHour invalid |                                           |                               |             
|                                 |                      |                                                |                                                        |                                                                                |                                           |                               |             

### Creation Match DTO

```
title: string
dateList: array<string>
```

### Match DTO

```
idMatch: uuid
title: string
votedDate: string
votedHour: string
```

## hist

| url               | Méthode(GET, POST..) | filtre | code erreur |            type entrée            |   type retour    |                      description                      |
|:------------------|:--------------------:|:------:|:------------|:---------------------------------:|:----------------:|:-----------------------------------------------------:|
| `/match/resultat` |         GET          |   -    | 404         |                                   | feuille de route | une fois les votes fini; donne la feuille de resultat |
| `/match/resultat` |         PUT          |        | 400         | le match avec tous les votes fini |        -         |                 sauvegarder le match                  |
