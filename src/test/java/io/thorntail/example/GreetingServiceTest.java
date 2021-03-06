/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.thorntail.example;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@DefaultDeployment
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GreetingServiceTest {
    @Test
    @RunAsClient
    public void _01_serviceInvocation() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("api")
                .path("greeting");

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("Hello, World!"));
    }

    @Test
    @RunAsClient
    public void _02_serviceStopped() {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget stop = client.target("http://localhost:8080")
                    .path("api")
                    .path("stop");

            // suspend process
            Response response = stop.request().get();
            assertEquals(200, response.getStatus());
        } finally {
            client.close();
        }

        awaitStatus(503, Duration.ofSeconds(10));
    }

    private void awaitStatus(int status, Duration duration) {
        await().atMost(duration.getSeconds(), TimeUnit.SECONDS).until(() -> {
            Client client = ClientBuilder.newClient(); // new connection
            try {
                WebTarget greeting = client.target("http://localhost:8080")
                        .path("api")
                        .path("greeting");

                Response response = greeting.request().get();
                return response.getStatus() == status;
            } finally {
                client.close();
            }
        });
    }
}
