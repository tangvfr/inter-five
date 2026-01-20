# URL

## authentification

UserDto(String mail,String username, @JsonIgnore @Nullable=true String password )

| url              | Méthode(GET, POST..) | filtre | code erreur   |                  type entrée                  |  type retour  |                 description                 |
|:-----------------|:--------------------:|:------:|:--------------|:---------------------------------------------:|:-------------:|:-------------------------------------------:|
| `/auth/login`    |         GET          |   -    | 404           | record(String username/mail, String password) | String(token) |                  Connexion                  |
| `/auth/register` |         POST         |   -    | 409           |                    UserDto                    | String(token) |                 Inscription                 |
| `/profil`        |         GET          |   -    |               |                 List<UserDto>                 |               |         Voire tous les utilisateurs         |
| `/profil/{id}`   |         GET          |   -    | 403, 404      |                       -                       |    UserDto    |     Récuperation du profile utilisateur     |
| `/profil/{id}`   |         PUT          |   -    | 403, 404, 409 |                    UserDto                    |    UserDto    | Changement mail/username/(password->passur) |
| `/profil/{id}`   |        DELETE        |   -    | 403, 404      |                       -                       |       -       |            Suppression du compte            |
|                  |                      |        |               |                                               |               |
|                  |                      |        |               |                                               |               |
|                  |                      |        |               |                                               |               |

## vote

| url                     | Méthode(GET, POST..) |                         filtre                         | code erreur |                type entrée                |                   type retour | description |
|:------------------------|:--------------------:|:------------------------------------------------------:|:-----------:|:-----------------------------------------:|------------------------------:|------------:| 
| `/match`                |         GET          | member (à comme participant), owner (a comme créateur) |             |                                           | Liste [Match DTO](#Match-DTO) |             |
| `/match`                |         PUT          |                                                        |             | [Creation Match DTO](#Creation-Match-DTO) |                          UUID |             |
| `/match/{matchId}`      |         GET          |                                                        |             |               path(matchID)               |       [Match DTO](#Match-DTO) |             |
| `/match/{matchId}/date` |         GET          |                                                        |             |                                           |                               |             |
| `/match/{matchId}/date` |         PUT          |                                                        |             |                array<str>                 |                               |             |
| `/match/{matchId}/hour` |         GET          |                                                        |             |                                           |                               |             |
| `/match/{matchId}/hour` |         PUT          |                                                        |             |                array<str>                 |                               |             |
|                         |                      |                                                        |             |                                           |                               |             |
|                         |                      |                                                        |             |                                           |                               |             |

### Creation Match DTO

```
title: string
dateList: array
```

### Match DTO

```
idMatch: uuid
title: string
votedDate: string
votedHour: string
```

## hist