import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaPreCheckStatusException;
import com.hedera.hashgraph.sdk.HederaReceiptStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageQuery;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeoutException;


class ConsensusPubSubExample {
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK");
    private static final String CONFIG_FILE = Dotenv.load().get("CONFIG_FILE");

    private ConsensusPubSubExample() {
    }

    public static void main(String[] args) throws TimeoutException, InterruptedException, HederaPreCheckStatusException, HederaReceiptStatusException {
        Client client;

        if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("previewnet")) {
            client = Client.forPreviewnet();
        } else {
            try {
                client = Client.fromConfigFile(CONFIG_FILE != null ? CONFIG_FILE : "");
            } catch (Exception e) {
                client = Client.forTestnet();
            }
        }

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        TransactionResponse transactionResponse = new TopicCreateTransaction()
            .execute(client);

        TransactionReceipt transactionReceipt = transactionResponse.getReceipt(client);

        TopicId topicId = Objects.requireNonNull(transactionReceipt.topicId);

        System.out.println("New topic created: " + topicId);

        Thread.sleep(5000);

        new TopicMessageQuery()
            .setTopicId(topicId)
            .subscribe(client, resp -> {
                String messageAsString = new String(resp.contents, StandardCharsets.UTF_8);

                System.out.println(resp.consensusTimestamp + " received topic message: " + messageAsString);
            });

        // keep the main thread from exiting because the listeners run on daemon threads
        // noinspection InfiniteLoopStatement
        for (int i = 0; ; i++) {
            new TopicMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage("hello, HCS! " + i)
                .execute(client)
                .getReceipt(client);

            Thread.sleep(2500);
        }
    }
}
