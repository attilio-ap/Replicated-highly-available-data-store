# Replicated, Highly-Available Data Store

## Descrizione
Questo progetto implementa un **data store** (archivio dati) **altamente disponibile** e **replicato**, scritto in **Java**.  
Il sistema offre due primitive fondamentali:

- **read(key)**: restituisce il valore associato a una chiave specificata.  
- **write(key, value)**: aggiorna/inserisce il valore associato a una chiave specificata.

L’obiettivo è garantire:
1. **Alta disponibilità**: un client può continuare a leggere e scrivere su un server anche in caso di disconnessione o *crash* di altri server.  
2. **Consistenza causale**: ogni operazione di lettura riflette un insieme coerente di scritture, rispettando le dipendenze causali tra le operazioni.  
3. **Replica multi-leader**: ogni server agisce come *leader* del proprio stato e propaga le modifiche agli altri nodi, cooperando per mantenere i dati replicati e coerenti.

---

## Architettura

### Replica Multi-Leader
Nel nostro scenario, **tutti i server** possono ricevere richieste di scrittura e agiranno quindi come *leader* della propria replica. Quando un nodo riceve un’operazione di **write**:

1. Aggiorna localmente il proprio stato.  
2. Propaga l’aggiornamento agli altri server attraverso un protocollo di comunicazione (ad esempio, usando socket TCP/UDP, gRPC o un sistema di messaggistica).

In tal modo, tutti i nodi dispongono di una **copia completa** del data store.

### Consistenza Causale
Per garantire la **consistenza causale**, ogni server tiene traccia dell’ordine (parziale) delle operazioni. In particolare:
- Ogni **write** è contrassegnata da un identificatore (timestamp o *vector clock*) che rappresenta la causalità.
- Prima di applicare un aggiornamento, un server verifica di avere applicato tutti gli aggiornamenti che lo precedono causalmente.

Ciò permette di rispettare le **relazioni di dipendenza** tra le operazioni e impedisce letture che potrebbero portare a stati incoerenti.

---

## Gestione dei Guasti (Fault Tolerance)
Il sistema è progettato per funzionare anche se:
- Alcuni **nodi** si bloccano o si disconnettono improvvisamente (*crash fault*).
- Alcuni **link** di rete diventano instabili (latenza elevata, pacchetti persi, ecc.).

**Alta disponibilità** significa che finché un client rimane connesso al proprio server (anche se isolato), può continuare a leggere e scrivere. Quando il server torna in rete, gli aggiornamenti vengono **replicati** (o **sincronizzati**) con gli altri server per ripristinare una visione comune e coerente del data store.
