package ru.kit.bioimpedance;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kit.bioimpedance.commands.CheckStatus;
import ru.kit.bioimpedance.commands.Command;
import ru.kit.bioimpedance.commands.Launch;
import ru.kit.bioimpedance.dto.Data;
import ru.kit.bioimpedance.dto.ReadyStatus;

import java.io.*;
import java.net.Socket;
import static ru.kit.bioimpedance.Util.deserializeData;
import static ru.kit.bioimpedance.Util.sendCommand;
/**
 * Created by Kit on 21.12.2016.
 */
public class OxiChecker {
    public static void main(String[] args) {
        int i = 0;
        while (i++<20){
            System.out.println(checkPulseOximeter());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkPulseOximeter() {
        try {
            try {
                System.err.println("Waiting 1 second before connection");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("Trying to create Socket");
            Socket hypoxiaSocket = new Socket("localhost", 8085);
            BufferedWriter outputH = new BufferedWriter(new OutputStreamWriter(hypoxiaSocket.getOutputStream()));
            BufferedReader brH = new BufferedReader(new InputStreamReader(hypoxiaSocket.getInputStream()));
            System.err.println("Socket created!");

            Command command = new Launch(45, true, 70, 170, 2, 120, 80, 123);
            System.err.println("Sending Launch command");
            sendCommand(command, outputH);
            System.err.println("Sending CheckStatus command");
            sendCommand(new CheckStatus(), outputH);
            System.err.println("Reading Status");
            String lineH = brH.readLine();
            System.err.println("Deserialize Data");
            Data dataH = deserializeData(lineH);
            ReadyStatus readyStatusH = (ReadyStatus) dataH;
            if (readyStatusH.isPulse()) return true;
        } catch (IOException e) {
            System.out.println("Catched IOException in checkPulseOximeter()");
            e.printStackTrace();
        }
        return false;

    }


}
