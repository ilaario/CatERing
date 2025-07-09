# Istruzioni: Esecuzione del progetto CatERing
## Prerequisiti
- Java JDK 11 o superiore
- Apache Maven
- Visual Studio Code (opzionale)
- Estensione Java Extension Pack (opzionale)
### Database
- SQLite - Istruzioni per l'installazione
- Per visualizzare il contenuto del DB (suggerimento):
  1. Apri Visual Studio Code
  2. Vai sulle estensioni e installa SQLite Viewer
  3. Da Visual Studio Code fai doppio click sul file catering.db per visualizzarne il contenuto


## 1. Aprire il progetto

1. Apri Visual Studio Code
2. Vai su File > Open Folder...
3. Seleziona la cartella che contiene il file pom.xml

Se da terminale, segui semplicemente il passo 3.

## 2. Compilare il progetto
### Metodo A – Terminale

``` mvn clean compile```

### Metodo B – Interfaccia grafica Maven

1. Apri la vista laterale o in basso denominata Maven (la posizione puo' cambiare a seconda della versione di VS Code)
2. Espandi ```catering > Lifecycle```
3. Fai clic su ```compile```

## 3. Eseguire il progetto
### Metodo A – Da terminale

```mvn exec:java```

### Metodo B – Da Visual Studio Code

1. Apri la classe ```CatERing.java```
2. Clicca sull’icona ▶️ accanto al metodo main oppure sul bottone ```Run``` sopra al metodo main

## 4. Eseguire i test
### Metodo A – Terminale

```mvn test```

### Metodo B – Interfaccia grafica Maven

1. Apri la vista laterale o in basso denominata Maven (la posizione puo' cambiare a seconda della versione di VS Code)
2. Espandi ```catering > Lifecycle```
3. Fai clic su ```test```