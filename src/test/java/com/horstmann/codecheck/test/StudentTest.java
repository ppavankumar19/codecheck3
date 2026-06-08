package com.horstmann.codecheck.test;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.Config;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.imsglobal.lti.launch.LtiOauthSigner;
import org.imsglobal.lti.launch.LtiSigner;
import org.imsglobal.lti.launch.LtiSigningException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@QuarkusTest
public class StudentTest extends TestUtil {
    @Inject
    Config config;

    String sampleProblem = "wiley/ch-bj4cc-c06_exp_6_105";

    @Test
    public void testProblemHtml() throws IOException, URISyntaxException {
        String contents = getHTML("files/" + sampleProblem);
        Assertions.assertTrue(contents.contains("horstmann_codecheck.setup.push"));
    }

    @Test public void testCheckNJS() throws IOException, URISyntaxException, InterruptedException {
        var requestBody = "{\"repo\":\"wiley\",\"problem\":\"ebook-py-3-ch06-sec01-cc-1\",\"listmultiply.py\":\"values = [2, 3, 5, 7, 11] \\n\\n# Multiply each element of the values list by 5.\\nfor i in range(len(values)) : values[i] = 5 * values[i]\\n\\n# Print the resulting list.\\nfor element in values :\\n   print(element)\\n\"}";
        JsonNode response = postJson("/checkNJS", requestBody);
        Assertions.assertEquals("3/3", response.get("score").asText());
    }

    @Test public void offlineAssignmentOmitsExternalLessons() throws IOException, URISyntaxException {
        String contents = getHTML("viewAssignment/2012131856g3agnrusxkj3mz6ckiubyga8");
        Assertions.assertTrue(contents.contains("/files/wiley/ebook-py-3-ch04-sec08-cc-5"));
        Assertions.assertTrue(contents.contains("offlineOmittedProblems"));
        Assertions.assertFalse(contents.contains("https://horstmann.com/interactivities/"));
    }

    @Test public void allExternalAssignmentFailsClearly() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUrl.toURI().resolve("viewAssignment/20121606051p1popwn8d84crehuhdhzvd9w"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.body().contains("no lessons that are available offline"));
    }

    String testUserID = "__test__.user_id.1";
    String testToolConsumerID = "__test__.tool_consumer_id.1";
    String testContextID = "__test__.context_id.1";
    String testResourceLinkID = "__test__.resource_link_id.1";
    String testResultSourcedID = "__test__.lis_result_sourcedid.1";

    @Test public void ltiLaunch() throws IOException, URISyntaxException, InterruptedException, LtiSigningException {
        var parameters = Map.of("user_id", testUserID,
                "tool_consumer_instance_guid", testToolConsumerID,
                "context_id", testContextID,
                "resource_link_id", testResourceLinkID,
                "lis_result_sourcedid", testResultSourcedID);
        // TODO "lis_outcome_service_url"

        LtiSigner ltiSigner = new LtiOauthSigner();
        var key = config.getString("com.horstmann.codecheck.test.ltikey");
        var secret = config.getString("com.horstmann.codecheck.test.ltisecret");
        Map<String, String> signedParameters = ltiSigner.signParameters(parameters, key, secret, resolve("files/" + sampleProblem), "POST");
        String responseBody = postFormData("files/" + sampleProblem, signedParameters);
        Assertions.assertTrue(responseBody.contains("horstmann_config.lti"));
    }

    @Test public void ltiSendAndRetrieve() throws URISyntaxException, IOException, InterruptedException {
        var score = "" + Math.random();
        var submissionID = testToolConsumerID + "/" + testContextID + "/" + testResourceLinkID + " " + testUserID;
        String outcomeServiceURL = "null"; // TODO
        var key = config.getString("com.horstmann.codecheck.test.ltikey");
        var requestBody = """
{
   "submissionID": "%s",
   "state": { "__test__": true },
   "score": %s,
   "lis_outcome_service_url": "%s",
   "lis_result_sourcedid": "%s",
   "oauth_consumer_key": "%s"
}
""".formatted(submissionID, score, outcomeServiceURL, testResultSourcedID, key);
        JsonNode response = postJson("/lti/send", requestBody);
        Assertions.assertEquals(score, "" + response.get("score").asDouble());
        requestBody = """
{
   "submissionID": "%s"
}
""".formatted(submissionID);
        response = postJson("/lti/retrieve", requestBody);
        Assertions.assertEquals(true, response.get("state").get("__test__").asBoolean());
    }

    private String assignmentID = "250215105043ykicmbulh13a0gtn72dizbl";
    private String studentCookie = "ypeg-ygob-ejyq-ceca";
    private String privateStudentKey = "8WWMHSQWK39RE37V0C75AQ0ID";
    private List<String> problemIDs = List.of("https://codecheck.us/files/2003281808cn6bxww72bhz4dxhftfllxh8q", "ebook-bjlo-1-ch04-sec2-walkthrough-1");

    @Test public void resumeAssignment() throws IOException, URISyntaxException, InterruptedException {
        String contents = getHTML("private/resume/" + assignmentID + "/" + studentCookie + "/" + privateStudentKey);
        for (var problemID : problemIDs) {
            Assertions.assertTrue(contents.contains(problemID));
        }
    }

    private String workRequest(int randomInt, String submissionTime) {
        return """
{
  "assignmentID": "%s",
  "workID" : "%s/%s",
  "problems":{
      "%s":{
        "score":1,
        "state":{
          "work":{
            "Rainfall.java":["      while (rainfall[i] != 9999) // %d","         if (rainfall[i] > 0)","            sum += rainfall[i];\\n            count++;","      if (count == 0)"]
          },
        "scoreText":"1/1"
      },
      "%s":{
        "score":0,
        "state":{"correct":0,"errors":0}}
      }
    },
  "tab":"%s",
  "submittedAt":"%s"
}
""".formatted(assignmentID, studentCookie, privateStudentKey,
                problemIDs.get(0), randomInt,
                problemIDs.get(1), problemIDs.get(0), submissionTime);
    }

    @Test public void submitWork() throws IOException, URISyntaxException, InterruptedException {
        int randomInt = RandomGenerator.getDefault().nextInt(10_000);
        String submissionTime = Instant.now().toString();
        String request = workRequest(randomInt, submissionTime);
        var response = postJson("/saveWork", request);
        Assertions.assertTrue(response.get("submittedAt") != null);
        String contents = getHTML("private/resume/" + assignmentID + "/" + studentCookie + "/" + privateStudentKey);
        Assertions.assertTrue(contents.contains("// " + randomInt));
    }

    @Test public void testNewerWork() throws URISyntaxException, IOException, InterruptedException {
        int randomInt = RandomGenerator.getDefault().nextInt(10_000);
        Instant now = Instant.now();
        Instant earlier = now.minusMillis(100);
        // put a work object with newer time stamp
        postJson("/saveWork", workRequest(randomInt, now.toString()));
        // put a work object with older time stamp
        postJson("/saveWork", workRequest(randomInt - 1, earlier.toString()));
        // read back to see that the one with newer timestamp has been saved
        String contents = getHTML("private/resume/" + assignmentID + "/" + studentCookie + "/" + privateStudentKey);
        Assertions.assertTrue(contents.contains("// " + randomInt));
    }

    @Test public void testNashorn() throws URISyntaxException, IOException, InterruptedException {
        String problemWithParam = "21020618549wpsq3oive47l2jaz1dmpyozg";
        String contents = getHTML("files/" + problemWithParam);
        Assertions.assertTrue(contents.contains("horstmann_codecheck.setup.push"));
        // TODO This just tests that Nashorn is present, would be good to test parameters
    }

    @Test public void testResourceLoading() throws URISyntaxException, IOException, InterruptedException {
        // This problem requires resource loading of codecheck.cpp, codecheck.h
        String request = """
{"repo":"wiley","problem":"ebook-bc-3-ch05-sec04-cc-3","even.cpp":"/**\\n   Return true when a number is even.\\n   @param n the number to check\\n   @return true when n is even\\n*/\\nbool is_even(int n)\\n{\\n   return true;\\n}\\n"}
""";
        var response = postJson("/checkNJS", request);
        Assertions.assertEquals("2/4", response.get("score").asText());
    }

    @Test public void testCORSHeaders() throws URISyntaxException, IOException, InterruptedException {
        String origin = "https://example.com";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUrl.toURI().resolve("/checkNJS"))
                .header("Access-Control-Request-Method", "POST")
                .header("Origin", origin)
                .HEAD()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(origin, response.headers().firstValue("access-control-allow-origin").orElse(""));
    }
}
