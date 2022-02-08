# Client-Server
## Gestionnaire de fichier

Application console client-serveur permettant de stocker n’importe quel type de fichier sur un serveur de stockage.

Au démarrage du serveur, celui-ci demande à l’utilisateur d’entrer les informations suivantes : l’adresse IP et le port d’écoute (un port entre 5002 et 5049).

Au lancement du client, on demande à l’utilisateur d’entrer l’adresse IP du serveur, le port du serveur.

Chaque client connaît d’avance l'adresse IP du serveur ainsi que le numéro de port écouté.

L'application utilise les threads au niveau du serveur pour réussir à supporter plusieurs clients à la fois.

Une fois la connexion établie entre le client et le serveur de stockage,l'utilisateur peut utiliser les commandes suivantes :

  -**cd** `<Nom d’un répertoire sur le serveur>` : Commande permettant de se déplacer vers un répertoire enfant ou parent (‘..’ pour se déplacer vers un répertoire parent).

  -**ls** : Commande permettant d’afficher tous les dossiers et fichiers dans le répertoire courant de l’utilisateur au niveau du serveur.

  -**mkdir** `<Nom du nouveau dossier>` : Commande permettant la création d’un dossier au niveau du serveur de stockage.

  -**Delete** `<Nom du dossier | Nom du fichier>` : Commande permettant de supprimer un dossier ou un fichier.
  
  -**upload** `<Nom du fichier>` : Commande permettant le téléversement d’un fichier, se trouvant dans le répertoire locale du client, vers le serveur de stockage.

  -**download** `<Nom du fichier> <-z>` : Commande permettant le téléchargement d’un fichier, se trouvant dans le répertoire courant de l’utilisateur au niveau du serveur de stockage, vers le répertoire local du client. Si l’usager spécifie le paramètre optionnel <-z>, le serveur compresse le fichier au format .zip avant de
l’envoyer au client.
