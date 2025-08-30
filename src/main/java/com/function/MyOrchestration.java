package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.time.Duration;
import java.util.*;

import com.microsoft.durabletask.*;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;

/**
 * Please follow the below steps to run this durable function sample
 * 1. Send an HTTP GET/POST request to endpoint `StartHelloCities` to run a durable function
 * 2. Send request to statusQueryGetUri in `StartHelloCities` response to get the status of durable function
 * For more instructions, please refer https://aka.ms/durable-function-java
 * 
 * Please add com.microsoft:durabletask-azure-functions to your project dependencies
 * Please add `"extensions": { "durableTask": { "hubName": "JavaTestHub" }}` to your host.json
 */
public class MyOrchestration {
    /**
     * This HTTP-triggered function starts the orchestration.
     */
    @FunctionName("StartOrchestration")
    public HttpResponseMessage startOrchestration(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
 
        DurableTaskClient client = durableContext.getClient();
        NewOrchestrationInstanceOptions options = new NewOrchestrationInstanceOptions().setVersion("1.0");
        String instanceId = client.scheduleNewOrchestrationInstance("Cities", options);
        context.getLogger().info("Created new Java orchestration with instance ID = " + instanceId);
        return durableContext.createCheckStatusResponse(request, instanceId);
    }

    /**
     * This is the orchestrator function, which can schedule activity functions, create durable timers,
     * or wait for external events in a way that's completely fault-tolerant.
     */
    @FunctionName("Cities")
    public String citiesOrchestrator(
            @DurableOrchestrationTrigger(name = "ctx") TaskOrchestrationContext ctx) {
        ctx.setCustomStatus("Waiting for approval...");
        Duration timeout = Duration.ofHours(72);
        ctx.waitForExternalEvent("Continue", timeout, boolean.class).await();
        ctx.setCustomStatus("Approved");

        String subVersion = ctx.callSubOrchestrator("SubOrchestrator", null, String.class).await();

        // NewSubOrchestrationInstanceOptions options = new NewSubOrchestrationInstanceOptions((RetryPolicy)null);
        // options.setVersion("1.0");
        // String subVersion = ctx.callSubOrchestrator("SubOrchestrator", null, null, options, String.class).await();

        return "Version: " + ctx.getVersion() + ", SubVersion: " + subVersion;
    }

    @FunctionName("SubOrchestrator")
    public String subOrchestrator(
            @DurableOrchestrationTrigger(name = "ctx") TaskOrchestrationContext ctx) {
        return ctx.getVersion();
    }

    /**
     * This is the activity function that gets invoked by the orchestration.
     */
    @FunctionName("Capitalize")
    public String capitalize(
            @DurableActivityTrigger(name = "name") String name,
            final ExecutionContext context) {
        context.getLogger().info("Capitalizing: " + name);
        return name.toUpperCase();
    }
}