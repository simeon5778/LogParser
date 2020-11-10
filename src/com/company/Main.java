package com.company;

import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final long ONE_MINUTE_IN_NANO_SECONDS = 60000000000L;
    private static final double TIME_LIMIT = 1000.000;
    private static final String RESULT_FILE_TIME_LIMIT = "resources/results/queryTimesOverTimeLimit.log";
    private static final String RESULT_FILE_MAX_OVER_MINUTE = "resources/results/maxQueriesPerMinute.log";
    private static final String UNZIPPED_FILE_PATH = "/home/simon/Desktop/projects/LogParser/resources/temporary.log";

    private static long startTime = 0;
    private static boolean needMinuteReset = true;
    private static String fileToUnzipPath = "NOT_A_VALID_FILE";


    public static void main(String[] args) throws Exception {
        loadPropertiesFromArguments(args);

        File unzippedFile = GzUnzip.decompressGzip(fileToUnzipPath, UNZIPPED_FILE_PATH);

        processLogEntriesOverTimeLimit(unzippedFile);
        processMaxAmountOfRequestsPerMinuteForEachHost(unzippedFile);

        unzippedFile.delete();
    }

    private static void loadPropertiesFromArguments(String[] args) {
        if (args[0] != null || args[0].length() > 0) {
            fileToUnzipPath = args[0];
        }
    }

    public static void processMaxAmountOfRequestsPerMinuteForEachHost(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        saveMaxAmountOfRequestsPerMinuteForEachHost(reader);
    }

    private static void saveMaxAmountOfRequestsPerMinuteForEachHost(BufferedReader reader) throws IOException {
        String logEntry;
        HashMap<String, Integer> resultIpAddresses = new HashMap<>();
        HashMap<String, Integer> ipAddresses = new HashMap<>();
        HashMap<String, String> occurringMinutesOfMaxRequestPerIpAddress = new HashMap<>();

        while ((logEntry = reader.readLine()) != null) {
            if (isSaamDbQuery(logEntry)) {

                String[] splitLogEntry = logEntry.split(" ");
                String time = splitLogEntry[1];
                String ipAddress = splitLogEntry[5];

                checkIfOneMinuteHasPassed(time);

                ipAddresses.putIfAbsent(ipAddress, 0);
                ipAddresses.put(ipAddress, ipAddresses.get(ipAddress) + 1);

                if (hasOneMinutePassed(time)) {
                    updateResultIpAddressesMapEachMinute(ipAddresses, resultIpAddresses, occurringMinutesOfMaxRequestPerIpAddress);
                    needMinuteReset = true;
                }
            }
        }
        writeResultFile(resultIpAddresses, occurringMinutesOfMaxRequestPerIpAddress);
    }

    public static void processLogEntriesOverTimeLimit(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> resultList = saveLogEntriesOverTimeLimit(reader);
        writeResultFile(resultList);
    }

    private static List<String> saveLogEntriesOverTimeLimit(BufferedReader reader) throws IOException {
        String logEntry;
        ArrayList<String> resultList = new ArrayList<>();
        while ((logEntry = reader.readLine()) != null) {

            if (isSaamDbQuery(logEntry)) {
                String[] splitLogEntry = logEntry.split(" ");
                double durationTime = Double.parseDouble(splitLogEntry[9]);

                if (durationTime >= TIME_LIMIT) {
                    resultList.add(logEntry);
                }
            }
        }
        return resultList;
    }

    private static void checkIfOneMinuteHasPassed(String time) {
        if (needMinuteReset) {
            startTime = getLogTime(time);
            needMinuteReset = false;
        }
    }

    public static void updateResultIpAddressesMapEachMinute(Map<String, Integer> ipAddresses,
                                                            Map<String, Integer> resultIpAddresses,
                                                            Map<String, String> occurringMinutesOfMaxRequestPerIpAddress) {

        ipAddresses.forEach((k, v) -> {
            resultIpAddresses.putIfAbsent(k,v);
            if (ipAddresses.get(k) > resultIpAddresses.get(k)) {
                resultIpAddresses.put(k,v);
                occurringMinutesOfMaxRequestPerIpAddress.put(k, getStartTimeAsString(startTime));
            }
        });
        ipAddresses.clear();
    }

    public static void writeResultFile(Map<String, Integer> resultIpAddresses,
                                       Map<String, String> occurringMinutesOfMaxRequestPerIpAddress) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESULT_FILE_MAX_OVER_MINUTE))) {

            resultIpAddresses.forEach((k, v)-> {
                try {
                    writer.append("HOST : IP : COUNT : TIME [")
                            .append(getHostFromIpAddress(k))
                            .append(" : ")
                            .append(k)
                            .append(" : ")
                            .append(v.toString())
                            .append(" : ")
                            .append(occurringMinutesOfMaxRequestPerIpAddress.get(k))
                            .append("]")
                            .append(System.lineSeparator());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeResultFile(List<String> resultList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESULT_FILE_TIME_LIMIT))) {
            for (String logEntry : resultList) {
                writer.append(logEntry).append("\n");
            }
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasOneMinutePassed(String time) {
        long currentTime = getLogTime(time);
        return currentTime - startTime >= ONE_MINUTE_IN_NANO_SECONDS;
    }

    public static String getHostFromIpAddress(String ipAddress) {

        for (HostEnum host : HostEnum.values()) {
            if (host.getIp().equals(ipAddress)) {
                return host.name().toLowerCase();
            }
        }
        return "unknown_host";
    }

    public static boolean isSaamDbQuery(String logEntry) {
        return logEntry.contains("saam@saam") && logEntry.contains("duration: ");
    }

    public static long getLogTime(String time) {

        String[] splitTime = time.split(":");
        int hours = Integer.parseInt(splitTime[0]);
        int minutes = Integer.parseInt(splitTime[1]);

        String[] secondsAndMilliseconds = splitTime[2].split("\\.");
        int seconds = Integer.parseInt(secondsAndMilliseconds[0]);
        int milliseconds = Integer.parseInt(secondsAndMilliseconds[1]) * 1000000;

        return LocalTime.of(hours, minutes, seconds, milliseconds).toNanoOfDay();
    }

    public static String getStartTimeAsString(long startTime) {
        return LocalTime.ofNanoOfDay(startTime).toString();
    }

}
