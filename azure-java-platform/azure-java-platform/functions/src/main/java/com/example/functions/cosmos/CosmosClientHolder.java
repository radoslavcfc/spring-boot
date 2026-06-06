package main.java.com.example.functions.cosmos;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.example.functions.common.Env;

/** Singleton Cosmos client. Uses key locally, managed identity in Azure. */
public final class CosmosClientHolder {
    private static volatile CosmosAsyncContainer container;

    public static CosmosAsyncContainer container() {
        if (container == null) {
            synchronized (CosmosClientHolder.class) {
                if (container == null) {
                    String endpoint = Env.get("COSMOS_ENDPOINT");
                    String db = Env.get("COSMOS_DATABASE", "appdb");
                    String c = Env.get("COSMOS_CONTAINER", "orders");
                    String key = Env.get("COSMOS_KEY");

                    CosmosClientBuilder b = new CosmosClientBuilder().endpoint(endpoint);
                    if (key != null && !key.isBlank()) {
                        b.key(key);
                    } else {
                        b.credential(new DefaultAzureCredentialBuilder().build());
                    }
                    CosmosAsyncClient client = b.buildAsyncClient();
                    container = client.getDatabase(db).getContainer(c);
                }
            }
        }
        return container;
    }

    private CosmosClientHolder() {}
}
