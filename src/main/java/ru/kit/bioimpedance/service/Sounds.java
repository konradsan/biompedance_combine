package ru.kit.bioimpedance.service;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by Kit on 15.02.2017.
 */
public class Sounds {
    private static String soundsProperties = "/ru/kit/bioimpedance/sounds.properties";
    private static final Properties properties = new Properties();
    static {
        try {
            properties.load(Sounds.class.getResourceAsStream(soundsProperties));
        } catch (IOException e) {
            System.err.println("Can not load properties file");
            e.printStackTrace();
        }
    }
    public static final String MACHINE_PROCESS_MAIN = properties.getProperty("machine_process_main");
    public static final String MACHINE_PROCESS_END = properties.getProperty("machine_process_end");
    public static final String TONOMETR_MAIN = properties.getProperty("tonometr_main");
    public static final String TONOMETR_END = properties.getProperty("tonometr_end");
    public static final String PLATES_MAIN = properties.getProperty("plates_main");
    public static final String PLATES_END = properties.getProperty("plates_end");
    public static final String HEART_MEASURE_MAIN = properties.getProperty("heart_measure_main");
    public static final String HEART_MEASURE_END = properties.getProperty("heart_measure_end");

    public static final String BASE_FEMALE_COMFORTLY_SEAT = properties.getProperty("base_female_comfortly_seat");
    public static final String BASE_FEMALE_LEGS_AND_HANDS_PRESS = properties.getProperty("base_female_legs_and_hands_press");
    public static final String BASE_FEMALE_RELAX_AND_STRAIGHT_LEFT_HAND = properties.getProperty("base_female_relax_and_straight_left_hand");
    public static final String BASE_FEMALE_TAKE_OFF_YOUR_SHOES = properties.getProperty("base_female_take_off_your_shoes");

    public static final String BASE_MALE_BASE_LOADING = properties.getProperty("base_male_base_loading");
    public static final String BASE_MALE_BASE_TEST_STARTED = properties.getProperty("base_male_base_test_started");
    public static final String BASE_MALE_BIOIMPEDANCE_ACTIVATED = properties.getProperty("base_male_bioimpedance_activated");
    public static final String BASE_MALE_PULSE_ACTIVATED = properties.getProperty("base_male_pulse_activated");
    public static final String BASE_MALE_BASE_TEST_COMPLETED = properties.getProperty("base_male_base_test_completed");

    public static final String TIME_TO_END = properties.getProperty("time_to_end");
    public static final String TIME_1 = properties.getProperty("time_1");
    public static final String TIME_2 = properties.getProperty("time_2");

}
