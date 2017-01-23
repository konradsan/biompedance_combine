package ru.kit.bioimpedance;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kit.bioimpedance.commands.Command;
import ru.kit.bioimpedance.commands.StartTest;
import ru.kit.bioimpedance.dto.Data;
import ru.kit.bioimpedance.dto.Inspection;
import ru.kit.bioimpedance.dto.Inspections;
import ru.kit.bioimpedance.dto.LastResearch;
import ru.kit.bioimpedance.equipment.BioimpedanceValue;

import java.io.*;
import java.net.Socket;

class Util {
    public static void sendCommand (Command command, BufferedWriter output) {
        try {
            output.write(serialize(command));
            output.newLine();
            output.flush();
        } catch (IOException ex) {
            System.err.println("sendCommand() fail! command - " + command);
            ex.printStackTrace();
        }
    }
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

    class ParseHypoxiaWaves extends Thread {

        private int count = 0;
        public void run() {
            try (Socket socket = new Socket("localhost", 8085);
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("oxi_wave.txt"), "cp1251"))
            ) {

                Command startTest = new StartTest();
                output.write(serialize(startTest));
                output.newLine();
                output.flush();

                while (true) {

                    String line = br.readLine();
                    if (line == null) {
                        System.err.println("LastResearch not found");
                    }

                    Data data = deserializeData(line);
                    LastResearch lastResearch;

                    if (data instanceof LastResearch) {

                        for (Inspection inspection : ((LastResearch) data).getInspections().values()) {
                            System.err.println(inspection);
                        }
                    } else if (data instanceof Inspections) {

                        if (count > 18000) {return;}

                        int wave = ((Inspections)data).getWave();
                        System.out.println(count + " wave: " + wave);
                        writer.write("" + wave);
                        writer.write("\r\n");
                        count++;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}



