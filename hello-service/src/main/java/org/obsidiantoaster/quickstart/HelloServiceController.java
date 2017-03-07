package org.obsidiantoaster.quickstart;

/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.wildfly.swarm.health.Health;
import org.wildfly.swarm.health.HealthStatus;
import org.wildfly.swarm.spi.runtime.annotations.ConfigurationValue;

/**
 * @author Heiko Braun
 */
@Path("/")
@ApplicationScoped
public class HelloServiceController {

	private static final Logger LOG = Logger.getLogger(HelloServiceController.class);

	@Inject
	@ConfigurationValue("service.name.address")
	private Optional<String> nameServiceAddress;

	@GET
	@Path("/greeting")
	@Produces("application/json")
	public String getGreeting() {
		return String.format("Hello %s!", getName());
	}

	private String getName() {
		Optional<Response> response = requestName();
		if(response.isPresent())
			return response.get().readEntity(String.class);
		else
			return "Failure";
	}


	@GET
	@Path("/check")
	@Health
	public HealthStatus checkNameService() {

		if (isNameServiceUp()) {
			return HealthStatus.named("name-service-check").up();
		}

		return HealthStatus.named("name-service-check")
				.withAttribute("name-service", "Name service doesn't function correctly")
				.down();
	}

	private boolean isNameServiceUp() {
		Optional<Response> response = requestName();
		if(response.isPresent())
			return response.get().getStatus() == 200;
		else
			return false;
	}

	private Optional<Response> requestName() {
		Optional response = Optional.empty();

		try {
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(nameServiceAddress.get());
			response = Optional.of(target.request(MediaType.MEDIA_TYPE_WILDCARD).get());
		} catch (Exception e) {
			LOG.error("Failed to access name service: " + e.getMessage());
		}

		return response;
	}


}
