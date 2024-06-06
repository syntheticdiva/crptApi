package com.example.crptApi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CrptApi {
    // Константа, содержащая URL-адрес для создания документов
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    // Объект Semaphore, используемый для управления количеством одновременных запросов к API
    private final Semaphore semaphore;
    // AtomicLong, отслеживающий время последнего запроса, чтобы соблюдать ограничения на частоту запросов
    private final AtomicLong lastRequestTime;
    // Временная единица, используемая для управления частотой запросов
    private final TimeUnit timeUnit;

    // Конструктор, инициализирующий объект CrptApi с заданным временным интервалом и ограничением на количество запросов
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.semaphore = new Semaphore(requestLimit);
        this.lastRequestTime = new AtomicLong(0L);
    }

    // Метод, отвечающий за отправку запроса на создание документа в Crpt api
    public void createDocument(Document document, String signature) {
        try {
            // Получение разрешения на выполнение запроса с помощью semaphore.acquire()
            semaphore.acquire();

            // Вычисление времени, прошедшего с момента последнего запроса
            long currentTime = Instant.now().toEpochMilli();
            long elapsedTime = currentTime - lastRequestTime.get();

            // Ожидание необходимого времени, чтобы соблюсти ограничение на частоту запросов
            if (elapsedTime < this.timeUnit.toMillis(1)) {
                Thread.sleep(this.timeUnit.toMillis(1) - elapsedTime);
            }
            // Обновление времени последнего запроса
            lastRequestTime.set(currentTime);

            // Создание RestTemplate для выполнения HTTP-запроса
            RestTemplate restTemplate = new RestTemplate();

            // Формирование HttpHeaders с необходимыми заголовками
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("X-Signature", signature);

            // Преобразование объекта Document в JSON-строку с помощью ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(document), headers);

            // Выполнение POST-запроса к API (точке входа) с помощью restTemplate.exchange()
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, String.class);

            // Обработка ответа
            System.out.println("Response: " + response.getBody());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }
    public Document loadDocumentFromJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(getClass().getResourceAsStream("/document.json"), Document.class);
    }

    public void shutdown() {
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        try {
            Document document = api.loadDocumentFromJson();
            api.createDocument(document, "your-signature-here");
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        } finally {
            api.shutdown();
        }
    }

    // Вложенный класс, представляющий структуру данных для документа, который будет отправлен в Crpt  API
    private static class Document {

        // Вложенные классы для описания структуры документа
        public static class Description {
            @JsonProperty("participantInn")
            public String participantInn;
        }

        public static class Product {
            @JsonProperty("certificate_document")
            public String certificateDocument;

            @JsonProperty("certificate_document_date")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            public Date certificateDocumentDate;

            @JsonProperty("certificate_document_number")
            public String certificateDocumentNumber;

            @JsonProperty("owner_inn")
            public String ownerInn;

            @JsonProperty("producer_inn")
            public String producerInn;

            @JsonProperty("production_date")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            public Date productionDate;

            @JsonProperty("tnved_code")
            public String tnvedCode;

            @JsonProperty("uit_code")
            public String uitCode;

            @JsonProperty("uitu_code")
            public String uituCode;
        }

        @JsonProperty("description")
        public Description description;

        @JsonProperty("doc_id")
        public String docId;

        @JsonProperty("doc_status")
        public String docStatus;

        @JsonProperty("doc_type")
        public String docType;

        @JsonProperty("importRequest")
        public boolean importRequest;

        @JsonProperty("owner_inn")
        public String ownerInn;

        @JsonProperty("participant_inn")
        public String participantInn;

        @JsonProperty("producer_inn")
        public String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        public Date productionDate;

        @JsonProperty("production_type")
        public String productionType;

        @JsonProperty("products")
        public List<Product> products;

        @JsonProperty("reg_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        public Date regDate;

        @JsonProperty("reg_number")
        public String regNumber;
    }

    }
