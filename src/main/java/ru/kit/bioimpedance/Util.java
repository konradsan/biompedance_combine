package ru.kit.bioimpedance;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kit.bioimpedance.commands.Command;
import ru.kit.bioimpedance.dto.Data;
import ru.kit.bioimpedance.equipment.BioimpedanceValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Util {
    public static Data deserializeData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Data.class);
    }

    public static Command deserializeCommand(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Command.class);
    }

    public static String serialize(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    public static BioimpedanceValue parseFile(String fileName) throws IOException {
        System.out.println(new File(fileName).getAbsolutePath());

        double fm = 0, tbw = 0, mm = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] sublines = line.split("\\s+");

                if (sublines[0].equals("FFM")) {
                    fm = Double.parseDouble(sublines[1].replace(",", "."));
                } else if (sublines[0].equals("TBW")) {
                    tbw = Double.parseDouble(sublines[1].replace(",", "."));
                } else if (sublines[0].equals("MM")) {
                    mm = Double.parseDouble(sublines[1].replace(",", "."));
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new BioimpedanceValue(fm, tbw, mm);
    }
}



