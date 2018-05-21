package com.tsuyoshihayashi.api;

import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Represents an abstract API endpoint
 *
 * @author Alexey Donov
 */
abstract class Endpoint {
    final @NotNull JSONParser parser = new JSONParser();
    final @NotNull Client client = ClientBuilder.newClient();
}
