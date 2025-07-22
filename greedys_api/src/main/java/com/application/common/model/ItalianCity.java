package com.application.common.model;

/**
 * Enum for major Italian cities
 * Can be extended to include more cities as needed
 */
public enum ItalianCity {
    ROMA("Roma", "Lazio", "00100"),
    MILANO("Milano", "Lombardia", "20100"),
    NAPOLI("Napoli", "Campania", "80100"),
    TORINO("Torino", "Piemonte", "10100"),
    PALERMO("Palermo", "Sicilia", "90100"),
    GENOVA("Genova", "Liguria", "16100"),
    BOLOGNA("Bologna", "Emilia-Romagna", "40100"),
    FIRENZE("Firenze", "Toscana", "50100"),
    BARI("Bari", "Puglia", "70100"),
    CATANIA("Catania", "Sicilia", "95100"),
    VENEZIA("Venezia", "Veneto", "30100"),
    VERONA("Verona", "Veneto", "37100"),
    MESSINA("Messina", "Sicilia", "98100"),
    PADOVA("Padova", "Veneto", "35100"),
    TRIESTE("Trieste", "Friuli-Venezia Giulia", "34100"),
    BRESCIA("Brescia", "Lombardia", "25100"),
    TARANTO("Taranto", "Puglia", "74100"),
    PRATO("Prato", "Toscana", "59100"),
    REGGIO_CALABRIA("Reggio Calabria", "Calabria", "89100"),
    MODENA("Modena", "Emilia-Romagna", "41100"),
    PARMA("Parma", "Emilia-Romagna", "43100"),
    PERUGIA("Perugia", "Umbria", "06100"),
    LIVORNO("Livorno", "Toscana", "57100"),
    CAGLIARI("Cagliari", "Sardegna", "09100"),
    FOGGIA("Foggia", "Puglia", "71100"),
    RIMINI("Rimini", "Emilia-Romagna", "47900"),
    SALERNO("Salerno", "Campania", "84100"),
    FERRARA("Ferrara", "Emilia-Romagna", "44100"),
    SASSARI("Sassari", "Sardegna", "07100"),
    LATINA("Latina", "Lazio", "04100"),
    GIUGLIANO_IN_CAMPANIA("Giugliano in Campania", "Campania", "80014"),
    MONZA("Monza", "Lombardia", "20900"),
    SIRACUSA("Siracusa", "Sicilia", "96100"),
    PESCARA("Pescara", "Abruzzo", "65100"),
    BERGAMO("Bergamo", "Lombardia", "24100"),
    VICENZA("Vicenza", "Veneto", "36100"),
    TERNI("Terni", "Umbria", "05100"),
    FORLÌ("Forlì", "Emilia-Romagna", "47100"),
    TRENTO("Trento", "Trentino-Alto Adige", "38100"),
    NOVARA("Novara", "Piemonte", "28100"),
    PIACENZA("Piacenza", "Emilia-Romagna", "29100"),
    ANCONA("Ancona", "Marche", "60100"),
    ANDRIA("Andria", "Puglia", "76123"),
    AREZZO("Arezzo", "Toscana", "52100"),
    UDINE("Udine", "Friuli-Venezia Giulia", "33100"),
    CESENA("Cesena", "Emilia-Romagna", "47521"),
    LECCE("Lecce", "Puglia", "73100"),
    PESARO("Pesaro", "Marche", "61100"),
    BARLETTA("Barletta", "Puglia", "76121"),
    CATANZARO("Catanzaro", "Calabria", "88100"),
    LA_SPEZIA("La Spezia", "Liguria", "19100"),
    TREVISO("Treviso", "Veneto", "31100"),
    PISA("Pisa", "Toscana", "56100"),
    COMO("Como", "Lombardia", "22100"),
    VARESE("Varese", "Lombardia", "21100"),
    BRINDISI("Brindisi", "Puglia", "72100"),
    BOLZANO("Bolzano", "Trentino-Alto Adige", "39100"),
    RAVENNA("Ravenna", "Emilia-Romagna", "48100"),
    COSENZA("Cosenza", "Calabria", "87100");
    
    private final String displayName;
    private final String region;
    private final String postalCodePrefix;
    
    ItalianCity(String displayName, String region, String postalCodePrefix) {
        this.displayName = displayName;
        this.region = region;
        this.postalCodePrefix = postalCodePrefix;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getRegion() {
        return region;
    }
    
    public String getPostalCodePrefix() {
        return postalCodePrefix;
    }
    
    /**
     * Validates if a postal code matches this city
     */
    public boolean isValidPostalCode(String postalCode) {
        if (postalCode == null) return false;
        return postalCode.startsWith(this.postalCodePrefix.substring(0, 3));
    }
    
    /**
     * Finds city by display name (case insensitive)
     */
    public static ItalianCity findByName(String name) {
        if (name == null) return null;
        
        for (ItalianCity city : values()) {
            if (city.displayName.equalsIgnoreCase(name.trim())) {
                return city;
            }
        }
        return null;
    }
}
