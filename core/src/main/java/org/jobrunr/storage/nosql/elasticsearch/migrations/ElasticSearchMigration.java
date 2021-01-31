package org.jobrunr.storage.nosql.elasticsearch.migrations;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class ElasticSearchMigration {

    public abstract void runMigration(RestHighLevelClient restHighLevelClients) throws IOException;

    public static boolean indexExists(RestHighLevelClient restHighLevelClient, String name) {
        try {
            return restHighLevelClient.indices().exists(new GetIndexRequest(name), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static void createIndex(RestHighLevelClient client, String name) {
        createIndex(client, new CreateIndexRequest(name), 0);
    }

    public static void createIndex(RestHighLevelClient client, CreateIndexRequest createIndexRequest) {
        createIndex(client, createIndexRequest, 0);
    }

    private static void createIndex(RestHighLevelClient client, CreateIndexRequest createIndexRequest, int retry) {
        sleep(retry * 500);
        try {
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() == 400) {
                if (e.getMessage().contains("resource_already_exists_exception")) {
                    // why: since we're distributed, multiple StorageProviders are trying to create indices
                    return;
                } else if (e.status().getStatus() == 400 && retry < 5) {
                    createIndex(client, createIndexRequest, retry + 1);
                } else {
                    throw new StorageException("Retried 5 times to setup ElasticSearch Indices", e);
                }
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    void deleteIndex(RestHighLevelClient client, String name) throws IOException {
        client.indices().delete(new DeleteIndexRequest(name), RequestOptions.DEFAULT);
    }

    protected static Map<String, Object> mapping(BiConsumer<StringBuilder, Map<String, Object>>... consumers) {
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        jsonMap.put("properties", properties);

        for (BiConsumer<StringBuilder, Map<String, Object>> consumer : consumers) {
            StringBuilder sb = new StringBuilder();
            Map<String, Object> fieldProperties = new HashMap<>();
            consumer.accept(sb, fieldProperties);
            properties.put(sb.toString(), fieldProperties);

        }
        return jsonMap;
    }

    private static void sleep(long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }
}