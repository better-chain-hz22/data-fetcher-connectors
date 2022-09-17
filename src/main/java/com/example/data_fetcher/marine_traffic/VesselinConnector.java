package com.example.data_fetcher.marine_traffic;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class VesselinConnector {

    public VesselinConnector() throws IOException, InterruptedException {
        fetchVesselData();
    }

    private void fetchVesselData() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://vesselin.p.rapidapi.com/ais?mmsi=211873030&dist=1"))
                .header("X-RapidAPI-Key", "570f7d4295msh4b305e1d183b432p113812jsn8c5bedf351b3")
                .header("X-RapidAPI-Host", "vesselin.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }
}
