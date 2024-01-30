package br.com.repassa.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ProcessBarCodeThreadUtils implements Runnable {
    private int start;
    private int end;
    private List<String> results;
    private CountDownLatch latch; // Usado para aguardar a conclusão de todas as threads

    public ProcessBarCodeThreadUtils(int start, int end, List<String> results, CountDownLatch latch) {
        this.start = start;
        this.end = end;
        this.results = results;
        this.latch = latch;
    }

    @Override
    public void run() {
        List<String> threadResults = new ArrayList<>();

        for (int i = start; i <= end; i++) {
            // Simule o processamento da imagem e obtenção do código da AWS Rekognition
            String code = processImageAndGetCode("https://example.com/image" + i);
            threadResults.add(code);
        }

        // Adicione os resultados da thread à lista compartilhada
        synchronized (results) {
            results.addAll(threadResults);
        }

        // Sinalize que esta thread concluiu seu trabalho
        latch.countDown();
    }

    private String processImageAndGetCode(String imageUrl) {
        // Simule o processamento da imagem e obtenção do código da AWS Rekognition
        // Substitua esta lógica pela chamada real à AWS Rekognition
        return "Code_for_" + imageUrl;
    }
}
