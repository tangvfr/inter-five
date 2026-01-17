# Architecture
## Idée de l'interface des fonctionnalité neccessaire

Méthodes des interfaces API:
- crée un compte
- obtenir un token
(
- renouvler un token
- mise a jour profil (peut 1 spécial password)
)

- création d'une sesssion (user admin)
- list des sessions (+filtre)
- obtenir une sesssion

- obtenir votes dates d'une session
- votes d'une date ppour une session (std user)
- cloture des votes dates (owner admin)
(
- relancé une session
- annulé une session
)

- obtenir votes crénaux d'une session
- vote créneaux pour une session
- clotureé votes crénaux

- obtenir la fiche de match
- histotique de match

## Découpage naif

1er microservice:
- crée un compte
- obtenir un token

2eme microservice:
- création d'une sesssion (user admin)
- list des sessions (+filtre)
- obtenir une sesssion

- obtenir votes dates d'une session
- votes d'une date ppour une session (std user)
- cloture des votes dates (owner admin)

- obtenir votes crénaux d'une session
- vote créneaux pour une session
- clotureé votes crénaux

3eme microservice:
- obtenir la fiche de match 

## Découpage par données métier

1er microservice:
- crée un compte
- obtenir un token

2eme microservice:
- création d'une session (user admin)
- list des sessions (+filtre)
- obtenir une session

- obtenir votes dates d'une session
- votes d'une date pour une session (std user)
- cloture des votes dates (owner admin)

- obtenir votes créneaux d'une session
- vote créneaux pour une session
- cloturé votes créneaux

3eme microservice:

- obtenir la fiche de match 

### Idée URL & DTO
/session/ GET liste session       ?owned=true
/session/{id}/   =>
{
    id
    titre
    state
    finalDate: date?
    finalHour: datetime?  
}

/session/{id}/date  GET   liste dates
{
    date: -
    vote: bool?
}
/session/{id}/date/{aDate}  PUT   liste de dates {["aDate": bool]}

/session/{id}/date/{aDate}/period  GET   liste dates
/session/{id}/date/{aDate}/period/{hour}  PUT   vote d'une période de date
{["aPeriod": bool]}

/session/{id}/date/{aDate}/period/{hour}/members GET  participant retenu ?waiting=false
/session/{id}/date/{aDate}/period/{hour}/members GET  participant en attente ?waiting=true



/session/{id}/period  GET  liste dates
/session/{id}/members GET  paricipant retenu ?waiting=false  ?status: 


{type} = hour, date
/session/{id}/sondage/{type} GET  liste des possibilité de votes
["str1", "str2"]
/session/{id}/sondage/{type} PUT voté pour une possibilié
{value: bool} 
