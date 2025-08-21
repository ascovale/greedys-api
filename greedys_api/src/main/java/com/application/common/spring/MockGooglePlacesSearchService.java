package com.application.common.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.service.google.GooglePlacesSearchService;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.AddressType;
import com.google.maps.model.Geometry;
import com.google.maps.model.LatLng;
import com.google.maps.model.OpeningHours;
import com.google.maps.model.PlaceDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock Google Places Search Service per sviluppo minimal
 * Si attiva solo quando google.maps.enabled=false
 */
@Service
@Primary
@ConditionalOnProperty(name = "google.maps.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockGooglePlacesSearchService extends GooglePlacesSearchService {

    public MockGooglePlacesSearchService() {
        super();
        log.warn("🔧 MOCK: GooglePlacesSearchService attivato - modalità sviluppo minimal");
    }

    @Override
    public List<PlaceDetails> findRestaurantsOnMaps(String restaurantName, String address) throws Exception {
        log.info("🔧 MOCK: Ricerca ristoranti chiamata");
        log.info("   🏪 Nome: {}", restaurantName);
        log.info("   📍 Indirizzo: {}", address);
        
        List<PlaceDetails> mockResults = new ArrayList<>();
        
        // Crea 1-3 risultati mock
        int numResults = ThreadLocalRandom.current().nextInt(1, 4);
        for (int i = 0; i < numResults; i++) {
            PlaceDetails mockPlace = createMockPlaceDetails(restaurantName, address, i);
            mockResults.add(mockPlace);
        }
        
        log.info("✅ MOCK: Trovati {} ristoranti (mock)", mockResults.size());
        return mockResults;
    }

    @Override
    public PlaceDetails findRestaurantOnMaps(String restaurantName, String address) throws Exception {
        log.info("🔧 MOCK: Ricerca primo ristorante chiamata");
        List<PlaceDetails> results = findRestaurantsOnMaps(restaurantName, address);
        PlaceDetails result = results.isEmpty() ? null : results.get(0);
        
        if (result != null) {
            log.info("✅ MOCK: Primo ristorante trovato: {}", result.name);
        } else {
            log.info("⚠️ MOCK: Nessun ristorante trovato");
        }
        
        return result;
    }

    @Override
    public PlaceDetails getPlaceDetailsByPlaceId(String placeId) throws Exception {
        log.info("🔧 MOCK: Dettagli posto chiamato per placeId: {}", placeId);
        
        // Crea dettagli mock basati sul placeId
        PlaceDetails mockPlace = createMockPlaceDetailsFromId(placeId);
        
        log.info("✅ MOCK: Dettagli posto generati: {}", mockPlace.name);
        return mockPlace;
    }

    @Override
    public List<PlaceDetails> findRestaurantsOnMapFromRestaurant(Restaurant restaurant) throws Exception {
        log.info("🔧 MOCK: Ricerca ristoranti da entità Restaurant chiamata");
        log.info("   🏪 Restaurant ID: {}", restaurant.getId());
        log.info("   📍 Nome: {}", restaurant.getName());
        
        String address = buildAddressFromRestaurant(restaurant);
        List<PlaceDetails> results = findRestaurantsOnMaps(restaurant.getName(), address);
        
        if (results.isEmpty()) {
            log.warn("⚠️ MOCK: Simulazione 'ristorante non trovato' per testing");
            String errorMessage = String.format("Ristorante non trovato su Google Maps (MOCK) - Nome: %s, Indirizzo: %s", 
                restaurant.getName(), address);
            throw new RuntimeException(errorMessage);
        }
        
        log.info("✅ MOCK: Trovati {} ristoranti per Restaurant entity", results.size());
        return results;
    }

    @Override
    public boolean isRestaurant(PlaceDetails place) {
        // In modalità mock, consideriamo sempre i luoghi come ristoranti
        log.debug("🔧 MOCK: isRestaurant = true (sempre vero in modalità mock)");
        return true;
    }

    /**
     * Crea PlaceDetails mock per testing
     */
    private PlaceDetails createMockPlaceDetails(String restaurantName, String address, int variant) {
        PlaceDetails place = new PlaceDetails();
        
        // Informazioni base
        place.placeId = "mock_place_id_" + Math.abs((restaurantName + address + variant).hashCode());
        place.name = restaurantName + (variant > 0 ? " (" + (variant + 1) + ")" : "");
        place.formattedAddress = address != null ? address : "Via Mock, 123, 00100 Roma RM, Italia";
        place.formattedPhoneNumber = "+39 06 1234567" + variant;
        // Website omesso per evitare URL deprecato
        
        // Rating mock
        place.rating = 3.5f + (ThreadLocalRandom.current().nextFloat() * 1.5f); // 3.5-5.0
        place.userRatingsTotal = ThreadLocalRandom.current().nextInt(50, 500);
        
        // Tipi (AddressType invece di PlaceType)
        place.types = new AddressType[]{ AddressType.ESTABLISHMENT };
        
        // Geometria (coordinate mock Roma)
        place.geometry = new Geometry();
        place.geometry.location = new LatLng(
            41.9028 + (ThreadLocalRandom.current().nextDouble(-0.1, 0.1)), // Roma lat ± variazione
            12.4964 + (ThreadLocalRandom.current().nextDouble(-0.1, 0.1))  // Roma lng ± variazione
        );
        
        // Componenti indirizzo
        place.addressComponents = createMockAddressComponents();
        
        // Orari di apertura (semplificato)
        place.openingHours = createMockOpeningHours();
        
        log.debug("🔧 MOCK: PlaceDetails creato - ID: {}, Nome: {}", place.placeId, place.name);
        return place;
    }

    /**
     * Crea PlaceDetails mock da placeId
     */
    private PlaceDetails createMockPlaceDetailsFromId(String placeId) {
        PlaceDetails place = new PlaceDetails();
        
        place.placeId = placeId;
        place.name = "Ristorante Mock " + placeId.substring(Math.max(0, placeId.length() - 6));
        place.formattedAddress = "Via Mock, 456, 00100 Roma RM, Italia";
        place.formattedPhoneNumber = "+39 06 9876543";
        // Website omesso per evitare URL deprecato
        
        place.rating = 4.2f;
        place.userRatingsTotal = 150;
        place.types = new AddressType[]{ AddressType.ESTABLISHMENT };
        
        // Geometria
        place.geometry = new Geometry();
        place.geometry.location = new LatLng(41.9028, 12.4964); // Roma
        
        return place;
    }

    /**
     * Crea componenti indirizzo mock
     */
    private AddressComponent[] createMockAddressComponents() {
        List<AddressComponent> components = new ArrayList<>();
        
        AddressComponent streetNumber = new AddressComponent();
        streetNumber.longName = "123";
        streetNumber.shortName = "123";
        streetNumber.types = new AddressComponentType[]{ AddressComponentType.STREET_NUMBER };
        components.add(streetNumber);
        
        AddressComponent route = new AddressComponent();
        route.longName = "Via Mock";
        route.shortName = "Via Mock";
        route.types = new AddressComponentType[]{ AddressComponentType.ROUTE };
        components.add(route);
        
        AddressComponent locality = new AddressComponent();
        locality.longName = "Roma";
        locality.shortName = "Roma";
        locality.types = new AddressComponentType[]{ AddressComponentType.LOCALITY };
        components.add(locality);
        
        AddressComponent country = new AddressComponent();
        country.longName = "Italia";
        country.shortName = "IT";
        country.types = new AddressComponentType[]{ AddressComponentType.COUNTRY };
        components.add(country);
        
        return components.toArray(new AddressComponent[0]);
    }

    /**
     * Crea orari di apertura mock (semplificato)
     */
    private OpeningHours createMockOpeningHours() {
        OpeningHours hours = new OpeningHours();
        hours.openNow = true;
        // In modalità mock non definiamo periodi complessi
        return hours;
    }

    /**
     * Costruisce indirizzo da Restaurant entity
     */
    private String buildAddressFromRestaurant(Restaurant restaurant) {
        StringBuilder addressBuilder = new StringBuilder();
        
        if (restaurant.getAddress() != null && !restaurant.getAddress().trim().isEmpty()) {
            addressBuilder.append(restaurant.getAddress());
        }
        
        if (restaurant.getCity() != null && !restaurant.getCity().trim().isEmpty()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(restaurant.getCity());
        }
        
        if (restaurant.getStateProvince() != null && !restaurant.getStateProvince().trim().isEmpty()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(restaurant.getStateProvince());
        }
        
        String address = addressBuilder.toString();
        return address.isEmpty() ? "Indirizzo non disponibile" : address;
    }
}
