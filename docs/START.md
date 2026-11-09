# Guide de démarrage

Ce guide explique comment lancer le projet en local sous Windows. Il y a quatre éléments à démarrer : la base PostgreSQL, le service IA, le backend et l'interface web.

## Ce qu'il faut installer

- Docker Desktop (seulement pour PostgreSQL)
- Python 3.12 ou plus récent
- Java 21
- Node.js et npm

## Lancement automatique

Démarrez Docker Desktop, attendez qu'il soit prêt, puis depuis la racine du projet :

```powershell
.\start_all.ps1
```

Le tableau de bord s'ouvre ensuite sur http://localhost:4200.

## Lancement manuel

Si le script ne passe pas, on peut tout démarrer à la main. Comptez un terminal par service, et respectez l'ordre ci-dessous.

### 1. Base de données

Si le conteneur existe déjà :

```powershell
docker start postgres-test
```

Sinon, on le crée. La base `ocp_blockage` est créée en même temps :

```powershell
docker run --name postgres-test -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=ocp_blockage -p 5433:5432 -d postgres:15
```

Le mot de passe `postgres` ne sert qu'en local, pour le développement.

### 2. Service IA

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Les deux premières commandes ne servent qu'au premier lancement. Le service est prêt quand le chargement du modèle YOLOv8 est affiché.

### 3. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Le backend est prêt quand il annonce qu'il écoute sur le port 8080. Les tables sont créées automatiquement au démarrage.

### 4. Frontend

```powershell
cd frontend
npm install
npm start
```

`npm install` n'est utile qu'au premier lancement. Une fois le port 4200 annoncé, l'interface est accessible sur http://localhost:4200.

## Voir le système réagir

Sans caméra, le simulateur envoie au backend les images du dossier `test-data` :

```powershell
.\ai-service\.venv\Scripts\python.exe scripts\camera_simulator.py
```

Le tableau de bord se met à jour à chaque analyse, et les changements d'état apparaissent dans l'historique.

## En cas de problème

**Docker introuvable ou qui ne répond pas.** Docker Desktop n'est pas démarré. Ouvrez-le et attendez qu'il soit prêt.

**Le conteneur PostgreSQL a disparu.** Recréez-le avec la commande `docker run` de l'étape 1.

**Un module Python est introuvable.** L'environnement virtuel est incomplet, il suffit de réinstaller les dépendances :

```powershell
cd ai-service
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

**Le dossier `node_modules` n'existe pas.** Lancez `npm install` dans `frontend`.

**Un port est déjà utilisé.** Un ancien processus tourne encore. Pour le port 8000 par exemple :

```powershell
netstat -ano | findstr :8000
Stop-Process -Id <PID> -Force
```

## Ports utilisés

| Service | Port |
|---|---|
| PostgreSQL | 5433 |
| Service IA | 8000 |
| Backend | 8080 |
| Interface | 4200 |