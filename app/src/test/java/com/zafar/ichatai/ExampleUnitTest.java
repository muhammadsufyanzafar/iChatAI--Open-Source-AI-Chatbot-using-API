package com.zafar.ichatai;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testBuildRequestBody_emptyHistory() throws Exception {
        List<Message> history = new ArrayList<>();
        JSONObject body = AiClient.buildRequestBody("my-model", history);

        assertEquals("my-model", body.getString("model"));
        JSONArray messages = body.getJSONArray("messages");
        assertEquals(0, messages.length());
        assertEquals(0.9, body.getDouble("temperature"), 1e-5);
        assertEquals(0.1, body.getDouble("top_p"), 1e-5);
    }

    @Test
    public void testBuildRequestBody_withUserAndAiMessages() throws Exception {
        List<Message> history = new ArrayList<>();
        history.add(new Message("Hello", true));
        history.add(new Message("Hi there!", false));
        history.add(new Message("What is the capital of France?", true));

        JSONObject body = AiClient.buildRequestBody("test-model", history);

        assertEquals("test-model", body.getString("model"));
        JSONArray messages = body.getJSONArray("messages");
        assertEquals(3, messages.length());

        JSONObject msg1 = messages.getJSONObject(0);
        assertEquals("user", msg1.getString("role"));
        assertEquals("Hello", msg1.getString("content"));

        JSONObject msg2 = messages.getJSONObject(1);
        assertEquals("assistant", msg2.getString("role"));
        assertEquals("Hi there!", msg2.getString("content"));

        JSONObject msg3 = messages.getJSONObject(2);
        assertEquals("user", msg3.getString("role"));
        assertEquals("What is the capital of France?", msg3.getString("content"));
    }

    @Test
    public void testBuildRequestBody_skipsWelcomeAndThinking() throws Exception {
        List<Message> history = new ArrayList<>();
        history.add(new Message("New chat started. Ask me anything!", false));
        history.add(new Message("Hello", true));
        history.add(new Message("Thinking…", false));

        JSONObject body = AiClient.buildRequestBody("filter-model", history);

        JSONArray messages = body.getJSONArray("messages");
        assertEquals(1, messages.length());

        JSONObject msg = messages.getJSONObject(0);
        assertEquals("user", msg.getString("role"));
        assertEquals("Hello", msg.getString("content"));
    }
}