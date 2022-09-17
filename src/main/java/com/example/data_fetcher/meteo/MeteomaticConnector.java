package com.example.data_fetcher.meteo;

import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class MeteomaticConnector {

    public static final String USERNAME = "tamarabraun_braun";
    public static final String PASSWORD = "H1mll84PV8";
    public static String URL_BASE = "https://api.meteomatics.com/";
    public static String URL_MAX_WAVES = "/significant_wave_height_max_1d_sot:idx";

    public static String URL_TROP_CYCLONE_PROBABILITY = "/prob_tropical_cyclone:p";
    public static String OUTPUT_FILENAME_WAVES = "meteo_waves";
    public static String OUTPUT_FILENAME_CYCLONES = "meteo_cyclones";
    public static String PORT_FILE = "ports.csv";
    private static final String SEMICOLON_DELIMITER = ";";
    private static final String JSON_FORMAT = "json";
    private static final String CSV_FORMAT = "csv";
    private final double[][] portCoordinates;
    private List<String> headers;

    List<List<String>> records = new ArrayList<>();

    {
        try (BufferedReader br = new BufferedReader(new FileReader(PORT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(SEMICOLON_DELIMITER);
                records.add(Arrays.asList(values));
            }
            headers = records.remove(0);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MeteomaticConnector() throws Exception {
        portCoordinates = new double[records.size()][2];
        readCoordinatesFromPortCsv();
        fetchMeteoData();
    }

    private void fetchMeteoData() throws Exception {
        String currentDate = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
        String futureDate = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().plusDays(5L).truncatedTo(ChronoUnit.DAYS));
        //expected format: 2022-09-17T00:00:00Z--2022-09-24T00:00:00Z:P1D
        String date = currentDate + "Z--" + futureDate + "Z:P1D";


        for (double[] portCoordinate : portCoordinates) {
            final StringBuilder sbhw = fetchWaveHeight(portCoordinate[0], portCoordinate[1],
                    date, CSV_FORMAT).append(";");
            writeToOutputFile(sbhw, OUTPUT_FILENAME_WAVES + "_" + currentDate + "." + CSV_FORMAT);
        }

        for (double[] portCoordinate : portCoordinates) {
            final StringBuilder sbcp =
                    fetchCycloneProb(portCoordinate[0], portCoordinate[1],
                            "2022-09-17T06:00:00ZP2D:PT12H", CSV_FORMAT).append(";");
            writeToOutputFile(sbcp, OUTPUT_FILENAME_CYCLONES + "_" + currentDate + "." + CSV_FORMAT);
        }


    }

    private static void writeToOutputFile(StringBuilder sbhw, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
        writer.write(sbhw.toString());
        writer.flush();
    }

    private StringBuilder fetchWaveHeight(double lat, double lon, String date, String format)
            throws IOException, InterruptedException {
        URL url_max_waves = new URL(URL_BASE + date + URL_MAX_WAVES + "/" + lat + "," + lon + "/" + format);
        HttpURLConnection conn = getHttpURLConnection(url_max_waves);
        TimeUnit.SECONDS.sleep(2L);
        return getStringBuilder(conn);
    }

    private static StringBuilder getStringBuilder(HttpURLConnection conn) throws IOException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = streamReader.readLine()) != null) {
            responseStrBuilder.append(inputStr).append(";");
        }
        return responseStrBuilder;
    }

    private static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        String encoding = Base64.getEncoder()
                .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Basic " + encoding);

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }
        return conn;
    }

    private StringBuilder fetchCycloneProb (double lat, double lon, String date, String format) throws IOException, InterruptedException {
        URL url_cyclone_prob = new URL(
                URL_BASE + date + URL_TROP_CYCLONE_PROBABILITY + "/" + lat + "," + lon + "/" + format);
        HttpURLConnection conn = getHttpURLConnection(url_cyclone_prob);
        TimeUnit.SECONDS.sleep(2L);
        return getStringBuilder(conn);
    }

    private void readCoordinatesFromPortCsv() {

        for (int i = 0; i < records.size(); i++) {
            List<String> record = records.get(i);
            double lat = Double.parseDouble(record.get(4));
            double lon = Double.parseDouble(record.get(5));
            portCoordinates[i][0] =lat;
            portCoordinates[i][1] =lon;
        }
        System.out.println("ports reading done!");
    }
}
