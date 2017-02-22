import javafx.concurrent.Task;
import org.json.JSONObject;
import ru.kit.bioimpedance.dto.Inspection;
import ru.kit.bioimpedance.dto.LastResearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    private static ExecutorService checkingQueueExecutor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        checkingQueueExecutor.submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("1 task");
                return null;
            }
        });
        checkingQueueExecutor.submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("2 task");
                return null;
            }
        });
    }
}


