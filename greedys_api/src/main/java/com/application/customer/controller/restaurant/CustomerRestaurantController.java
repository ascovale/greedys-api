package com.application.customer.controller.restaurant;

/**
 * REFACTORING COMPLETED - CustomerRestaurantController è stato diviso in 4 controller specializzati:
 * 
 * 1. {@link CustomerRestaurantInfoController} - Gestisce le informazioni generali sui ristoranti:
 *    - getRestaurants() - Tutti i ristoranti
 *    - getOpenDays() - Giorni aperti del ristorante  
 *    - getClosedDays() - Giorni chiusi del ristorante
 *    - getDaySlots() - Slot giornalieri del ristorante
 *    - searchRestaurants() - Ricerca ristoranti per nome
 *    - getRestaurantTypesNames() - Tipi di ristorante
 *    - getServices() - Servizi del ristorante (deprecato)
 * 
 * 2. {@link CustomerRestaurantServiceController} - Gestisce i servizi del ristorante:
 *    - getActiveEnabledServicesInPeriod() - Servizi attivi in un periodo
 *    - getActiveEnabledServicesInDate() - Servizi attivi in una data specifica
 * 
 * 3. {@link CustomerRoomController} - Gestisce le stanze del ristorante:
 *    - getRooms() - Stanze del ristorante
 * 
 * 4. {@link CustomerTableController} - Gestisce i tavoli delle stanze:
 *    - getTables() - Tavoli di una stanza
 * 
 * 5. {@link CustomerSlotController} - Gestisce gli slot temporali:
 *    - getAllSlotsByRestaurantId() - Tutti gli slot del ristorante
 *    - getSlotById() - Slot per ID
 * 
 * IMPORTANTE: Questa classe è ora deprecata e deve essere rimossa dopo che tutti i client
 * sono stati aggiornati per utilizzare i nuovi controller specializzati.
 * 
 * @deprecated Utilizzare i controller specializzati elencati sopra al posto di questa classe.
 */

@Deprecated
public class CustomerRestaurantController {
    // Questa classe è stata divisa in controller specializzati.
    // Vedere la documentazione sopra per i nuovi controller da utilizzare.
}
