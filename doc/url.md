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

| url                             | verbe |                  description                   |                                 filtre                                 | code erreur                                     | type entrée                               | type retour                                  | 
|:--------------------------------|:-----:|:----------------------------------------------:|:----------------------------------------------------------------------:|:------------------------------------------------|:------------------------------------------|:---------------------------------------------|
| `/match`                        |  GET  |            voir la liste des matchs            | member (à comme participant), owner (a comme créateur), (?page ?limit) |                                                 |                                           | HTTP 200 Liste [Match DTO](#Match-DTO)       |             
| `/match`                        | POST  |           crée une session de match            |                                                                        | `#A`,                                           | [Creation Match DTO](#Creation-Match-DTO) | HTTP 201 URL avec UUID                       |             
| `/match/{matchId}`              |  GET  |             information d'un match             |                                                                        | `#A`, `4imi`                                    |                                           | HTTP 200 [Match DTO](#Match-DTO)             |             
| `/match/{matchId}`              |  PUT  |            permet d'annulé le match            |                                                                        | `#A`, `4imi`, `#O`                              |                                           | HTTP 200 [Match DTO](#Match-DTO)             |             
| `/match/{matchId}/date`         |  GET  | voir la liste des dates proposé pour un match  |                                                                        | `4imi`                                          |                                           | `map<string, (int, int)>`                    |             
| `/match/{matchId}/date/{aDate}` | POST  |         vote pour une date d'un match          |                                                                        | `#A`, `4imi`, `4bst`, 404 aDate invalid         | `bool`                                    | HTTP 200 `map<string, (int, int)>`           |             
| `/match/{matchId}/date`         |  PUT  |     cloture le vote de la date d'un match      |                                                                        | `#A`, `4imi`, `#O`,  `4bs`,                     |                                           | HTTP 204 ou HTTP 200 [Match DTO](#Match-DTO) |
| `/match/{matchId}/hour`         |  GET  | voir la liste des heures proposé pour un match |                                                                        | `4imi`, `4bs`                                   |                                           | `map<string, (int, int)>`                    |             
| `/match/{matchId}/hour/{aHour}` | POST  |         vote pour une date d'un match          |                                                                        | `#A`, `4imi`, `4bst`, `43nd`, 404 aHour invalid | `bool`                                    | HTTP 200 `map<string, (int, int)>`           |             
| `/match/{matchId}/hour`         |  PUT  |     cloture le vote de l'heure d'un match      |                                                                        | `#A`, `4imi`, `#O`, `4bs`                       |                                           | HTTP 204 ou HTTP 200 [Match DTO](#Match-DTO) |

### Légendes
#### Code erreur
- `#A`: 401 néccéssite d'etre conntecté
- `4imi`: 404 id match invalid
- `4bst`: 403 bad step
- `43nd`: 403 pas dispo a la date
- `#O`: 403 n'est pas le créateur du match

### Creation Match DTO

```
title: string
dateList: array<string>
```

### Match DTO

```
idMatch: uuid
title: string
cancel: bool
step: string
winningDate: string
winningHour: string
```

## hist

| url                      | verbe |                description                 |              filtre               | code erreur  | type entrée | type retour   | 
|:-------------------------|:-----:|:------------------------------------------:|:---------------------------------:|:-------------|:------------|:--------------|
| `/match/{matchId}/sheet` |  GET  |           Voir la fiche de match           |                                   | `4imi`,`4bs` |             |               |             
| `/profil/{id}/match`     |  GET  | Voir les matchs auquel on était disponible | player, (?page ?limit), dateOrder | ?            |             | array<string> |             

### Filtres
#### Filtre `player`
- `true` = titulaire
- `false` = en attente

#### Filtre `dateOrder` ?? pas sur car iumplqiue de save lka date en double pour etre efficase, et le retour array<string> = array des uuid des match
- `ASC` = du plus vieux au plus récent
- `DSC` = du plus récent au plus vieux
- null | empty = ASC

### Match Sheet DTO
```
idMatch: uuid
title: string
cancel: bool
step: string
winningDate: string
winningHour: string
```